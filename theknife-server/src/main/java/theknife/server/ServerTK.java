package theknife.server;

import theknife.dao.*;
import theknife.dao.impl.*;
import theknife.db.ConnectionPool;
import theknife.server.DBCheck;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

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
