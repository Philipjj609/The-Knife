package theknife.server;

import theknife.dao.*;
import theknife.dao.impl.*;
import theknife.db.ConnectionPool;
import theknife.server.DBCheck;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Entry point del server TheKnife.
 *
 * Al lancio chiede le credenziali del database (con valori di default da db.properties).
 * Se il database non esiste lo crea e applica lo schema automaticamente.
 *
 * Avvio: java -cp theknife-server-shaded.jar theknife.server.ServerTK
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */

/**
 * Entry-point principale dell'applicazione server per <i>The Knife</i>.
 * 
 * Si occupa di:
 * <ul>
 *   <li>Richiedere le credenziali del database (pgAdmin / PostgreSQL) a console o risolverle da file.</li>
 *   <li>Verificare l'esistenza del database e dello schema tramite {@link DBCheck}.</li>
 *   <li>Inizializzare il pool di connessioni globali con {@link ConnectionPool}.</li>
 *   <li>Avviare un socket server su porta dedicata che accetta connessioni client in arrivo.</li>
 *   <li>Spedire ogni client a un {@link GestoreClient} in esecuzione su un thread di un {@link ExecutorService} pool fisso.</li>
 * </ul>
 *
 * <p>Avvio da terminale:
 * <pre>
 *   java -cp theknife-server-shaded.jar theknife.server.ServerTK
 * </pre>
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public class ServerTK {

    /**
     * Entry-point che avvia il server, caricando la configurazione, richiedendo le credenziali,
     * effettuando i check sul DB ed entrando nel loop infinito di accettazione dei socket client.
     *
     * @param args argomenti passati da riga di comando (non utilizzati)
     * @throws Exception per errori generici durante l'avvio del server o del socket
     */
    public static void main(String[] args) throws Exception {
        System.out.println("=== TheKnife Server ===");

        Properties defaults = caricaConfig();
        Properties props    = promptCredenziali(defaults);

        // Verifica l'esistenza del database
        DBCheck.verificaEsistenza(props);

        // Inizializza il pool di connessioni con le credenziali risolte
        ConnectionPool.init(props);

        // Verifica lo schema ed effettua il popolamento se necessario
        DBCheck.verificaSchemaEDati(props);

        int port     = Integer.parseInt(props.getProperty("server.port",        "9090"));
        int poolSize = Integer.parseInt(props.getProperty("server.thread.pool", "20"));

        // DAO condivisi — stateless, thread-safe tramite HikariCP
        UtenteDAO     utenteDAO     = new UtenteDAOImpl();
        RistoranteDAO ristoranteDAO = new RistoranteDAOImpl();
        RecensioneDAO recensioneDAO = new RecensioneDAOImpl();
        RispostaDAO   rispostaDAO   = new RispostaDAOImpl();
        PreferitiDAO  preferitiDAO  = new PreferitiDAOImpl();

        ExecutorService pool = Executors.newFixedThreadPool(poolSize);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("ServerTK: shutdown in corso...");
            pool.shutdown();
            try {
                if (!pool.awaitTermination(10, TimeUnit.SECONDS)) pool.shutdownNow();
            } catch (InterruptedException e) {
                pool.shutdownNow();
            }
            ConnectionPool.close();
            System.out.println("ServerTK: spento.");
        }));

        System.out.printf("ServerTK avviato — porta %d, pool %d thread%n", port, poolSize);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (!Thread.currentThread().isInterrupted()) {
                Socket client = serverSocket.accept();
                pool.submit(new GestoreClient(client, utenteDAO, ristoranteDAO,
                        recensioneDAO, rispostaDAO, preferitiDAO));
            }
        }
    }

    // -------------------------------------------------------------------------

    /**
     * Interroga l'amministratore del server tramite console di sistema per acquisire
     * i parametri di connessione e le credenziali di PostgreSQL. In assenza di valori immessi,
     * vengono scelti i parametri di default letti da db.properties.
     *
     * @param defaults i parametri di configurazione predefiniti caricati da file
     * @return un oggetto {@link Properties} con i parametri inseriti o predefiniti
     */
    private static Properties promptCredenziali(Properties defaults) {
        Scanner scanner = new Scanner(System.in);
        Properties props = new Properties(defaults);

        String urlDefault  = defaults.getProperty("db.url",  "jdbc:postgresql://localhost:5432/dbtk");
        String userDefault = defaults.getProperty("db.user", "postgres");

        System.out.println("Configurazione database (premi INVIO per il valore predefinito):");
        System.out.printf("  URL database [%s]: ", urlDefault);
        String input = scanner.nextLine().trim();
        props.setProperty("db.url",  input.isEmpty() ? urlDefault : input);

        System.out.printf("  Username     [%s]: ", userDefault);
        input = scanner.nextLine().trim();
        props.setProperty("db.user", input.isEmpty() ? userDefault : input);

        System.out.print("  Password: ");
        String pwd = scanner.nextLine().trim();
        props.setProperty("db.password",
                pwd.isEmpty() ? defaults.getProperty("db.password", "") : pwd);

        System.out.println();
        return props;
    }

    /**
     * Tenta una connessione e se il database non esiste, effettua la chiamata per crearlo
     * ed applicare lo script SQL schema.sql.
     *
     * @param props le proprietà con le credenziali del database
     * @throws Exception in caso di errore di connessione SQL o fallimento I/O
     */
    private static void assicuraDatabase(Properties props) throws Exception {
        String url      = props.getProperty("db.url");
        String user     = props.getProperty("db.user");
        String password = props.getProperty("db.password");

        try (Connection c = DriverManager.getConnection(url, user, password)) {
            System.out.println("Database trovato.");
            return;
        } catch (SQLException e) {
            if (e.getMessage() == null || !e.getMessage().toLowerCase().contains("does not exist")) {
                System.err.println("ERRORE connessione DB: " + e.getMessage());
                System.err.println("Verifica che PostgreSQL sia avviato e le credenziali siano corrette.");
                throw e;
            }
        }

        String dbName      = estraiNomeDb(url);
        String postgresUrl = url.substring(0, url.lastIndexOf('/') + 1) + "postgres";

        System.out.printf("Database '%s' non trovato. Creazione in corso...%n", dbName);

        try (Connection c = DriverManager.getConnection(postgresUrl, user, password);
             Statement  s = c.createStatement()) {
            s.execute("CREATE DATABASE \"" + dbName + "\"");
        }

        System.out.println("Applicazione schema...");
        try (Connection c = DriverManager.getConnection(url, user, password)) {
            applicaSchema(c);
        }
        System.out.printf("Database '%s' creato e schema applicato.%n%n", dbName);
    }

    /**
     * Estrae il nome del database relazionale analizzando l'URL di connessione JDBC PostgreSQL.
     *
     * @param jdbcUrl stringa dell'URL di connessione JDBC
     * @return il nome del database
     */
    private static String estraiNomeDb(String jdbcUrl) {
        String path = jdbcUrl.substring(jdbcUrl.lastIndexOf('/') + 1);
        int q = path.indexOf('?');
        return q >= 0 ? path.substring(0, q) : path;
    }

    /**
     * Legge ed esegue lo script SQL schema.sql caricandolo dal classpath.
     *
     * @param conn la connessione JDBC attiva su cui applicare lo schema
     * @throws Exception in caso di errore SQL o problemi di lettura I/O del file
     */
    private static void applicaSchema(Connection conn) throws Exception {
        try (InputStream in = ServerTK.class.getResourceAsStream("/schema.sql")) {
            if (in == null) throw new IOException("schema.sql non trovato nel classpath del server");
            String schema = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            try (Statement stmt = conn.createStatement()) {
                for (String chunk : schema.split(";")) {
                    String senzaCommenti = chunk.replaceAll("(?m)^\\s*--.*$", "").trim();
                    if (!senzaCommenti.isEmpty()) {
                        stmt.execute(chunk.trim());
                    }
                }
            }
        }
    }

    /**
     * Carica il file `db.properties` contenente i parametri di configurazione del server.
     *
     * @return le proprietà configurate
     * @throws IOException se il file delle proprietà non è reperibile nel classpath o non leggibile
     */
    private static Properties caricaConfig() throws IOException {
        Properties props = new Properties();
        try (InputStream in = ServerTK.class.getResourceAsStream("/db.properties")) {
            if (in == null) throw new IOException("db.properties non trovato nel classpath");
            props.load(in);
        }
        return props;
    }
}
