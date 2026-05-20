package theknife.server;

import theknife.dao.*;
import theknife.dao.impl.*;
import theknife.db.ConnectionPool;

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

public class ServerTK {

    public static void main(String[] args) throws Exception {
        System.out.println("=== TheKnife Server ===");

        Properties defaults = caricaConfig();
        Properties props    = promptCredenziali(defaults);

        // Verifica o crea il database
        assicuraDatabase(props);

        // Inizializza il pool di connessioni con le credenziali risolte
        ConnectionPool.init(props);

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
     * Se il database non esiste lo crea e applica schema.sql dal classpath.
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

    private static String estraiNomeDb(String jdbcUrl) {
        String path = jdbcUrl.substring(jdbcUrl.lastIndexOf('/') + 1);
        int q = path.indexOf('?');
        return q >= 0 ? path.substring(0, q) : path;
    }

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

    private static Properties caricaConfig() throws IOException {
        Properties props = new Properties();
        try (InputStream in = ServerTK.class.getResourceAsStream("/db.properties")) {
            if (in == null) throw new IOException("db.properties non trovato nel classpath");
            props.load(in);
        }
        return props;
    }
}
