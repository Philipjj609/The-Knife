package theknife.server;

import theknife.db.ConnectionPool;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.Properties;
import java.util.Scanner;

/**
 * Classe di utilità che verifica l'esistenza e lo stato di popolazione del database TheKnife.
 * Gestisce l'installazione automatica dello schema SQL (se assente) e l'importazione
 * del dataset iniziale di ristoranti da file CSV.
 *
 * Flusso:
 * <ol>
 *   <li>Il database PostgreSQL specificato esiste? Se no, stampa le istruzioni e termina.</li>
 *   <li>Lo schema è applicato (la tabella "ristoranti" è presente)? Se no, esegue il file schema.sql.</li>
 *   <li>Il database contiene già dati? Se no, importa i dati dal file CSV predefinito.</li>
 * </ol>
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public class DBCheck {

    /**
     * Costruttore privato per prevenire l'istanziazione di questa classe utility.
     */
    private DBCheck() {}

    /**
     * Verifica l'esistenza fisica del database PostgreSQL.
     * Se il database non esiste, stampa un pannello informativo di errore sulla console del server
     * ed interrompe l'esecuzione dell'applicazione tramite {@link System#exit(int)} con codice 1.
     *
     * @param props l'oggetto {@link Properties} contenente i parametri di connessione (URL, username, password)
     * @throws RuntimeException se si verifica un errore SQL diverso dal "database non esistente" (es. credenziali errate o server offline)
     */
    public static void verificaEsistenza(Properties props) {
        String url      = props.getProperty("db.url");
        String user     = props.getProperty("db.user");
        String password = props.getProperty("db.password", "");

        // --- PASSO 1: il database esiste? ---
        if (!databaseEsiste(url, user, password)) {
            String nomeDb = estraiNomeDb(url);
            System.err.println();
            System.err.println("╔══════════════════════════════════════════════════════════════╗");
            System.err.println("║              DATABASE NON TROVATO — AZIONE RICHIESTA         ║");
            System.err.println("╠══════════════════════════════════════════════════════════════╣");
            System.err.printf( "║  Database:  %-49s║%n", nomeDb);
            System.err.printf( "║  URL:       %-49s║%n", url);
            System.err.println("╠══════════════════════════════════════════════════════════════╣");
            System.err.println("║  Crea il database manualmente con:                           ║");
            System.err.printf( "║    CREATE DATABASE \"%s\"                                    ║%n", nomeDb);
            System.err.println("║  oppure tramite pgAdmin / psql, poi riavvia il server.       ║");
            System.err.println("╚══════════════════════════════════════════════════════════════╝");
            System.err.println();
            System.exit(1);
        }

        System.out.println("[DBCheck] Database trovato.");
    }

    /**
     * Verifica l'applicazione dello schema SQL e la presenza del popolamento dati iniziale.
     * Se lo schema non è applicato, esegue lo script SQL. Se i dati sono vuoti, importa il CSV.
     * Presuppone che {@link ConnectionPool} sia già inizializzato.
     *
     * @param props l'oggetto {@link Properties} di configurazione del server e del database
     * @throws RuntimeException se si riscontrano eccezioni SQL bloccanti durante il controllo
     */
    public static void verificaSchemaEDati(Properties props) {
        // --- PASSO 2: schema applicato? ---
        try (Connection conn = ConnectionPool.getConnection()) {
            if (!schemaApplicato(conn)) {
                System.out.println("[DBCheck] Schema non trovato. Applicazione schema.sql...");
                applicaSchema(conn);
                System.out.println("[DBCheck] Schema applicato.");
            } else {
                System.out.println("[DBCheck] Schema già presente.");
            }

            // --- PASSO 3: dati presenti? ---
            if (datiPresenti(conn)) {
                System.out.println("[DBCheck] Dati presenti nel database. Avvio completato.");
                return;
            }

            System.out.println("[DBCheck] Il database è vuoto.");
            importaDaCSV();

        } catch (SQLException e) {
            throw new RuntimeException("[DBCheck] Errore di connessione al database: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // PASSO 1 — Verifica esistenza del database
    // -------------------------------------------------------------------------

    /**
     * Tenta una connessione diretta tramite DriverManager per stabilire se il database PostgreSQL esiste.
     *
     * @param url la stringa JDBC di connessione
     * @param user il nome utente
     * @param password la password
     * @return true se la connessione avviene con successo, false se fallisce a causa del DB non esistente
     * @throws RuntimeException per qualsiasi altro errore SQL di rete o di credenziali errate
     */
    private static boolean databaseEsiste(String url, String user, String password) {
        try (Connection c = DriverManager.getConnection(url, user, password)) {
            return true;
        } catch (SQLException e) {
            String msg = e.getMessage();
            // PostgreSQL: "FATAL: database \"xyz\" does not exist"
            if (msg != null && msg.toLowerCase().contains("non esiste")) {
                return false;
            }
            // Qualsiasi altro errore (credenziali sbagliate, server giù, ecc.)
            // viene rilanciato per non nascondere problemi reali
            throw new RuntimeException("[DBCheck] Impossibile connettersi al database: " + msg, e);
        }
    }

    // -------------------------------------------------------------------------
    // PASSO 2 — Verifica/applicazione schema
    // -------------------------------------------------------------------------

    /**
     * Controlla se la tabella "ristoranti" esiste nel catalogo "public" del DB.
     * Funge da controllo indicativo per stabilire se lo schema SQL è già presente.
     *
     * @param conn la connessione SQL attiva
     * @return true se la tabella esiste, false altrimenti
     * @throws SQLException se si verifica un errore durante il recupero dei metadati
     */
    private static boolean schemaApplicato(Connection conn) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, "public", "ristoranti", new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    /**
     * Legge lo script di schema "schema.sql" dalle risorse del classpath e lo esegue,
     * dividendo gli statement separati dal carattere punto e virgola (;).
     *
     * @param conn la connessione SQL attiva su cui applicare lo schema
     * @throws RuntimeException se si verificano errori di I/O o durante l'esecuzione SQL delle istruzioni
     */
    private static void applicaSchema(Connection conn) {
        try (InputStream in = DBCheck.class.getResourceAsStream("/schema.sql")) {
            if (in == null) throw new IOException("schema.sql non trovato nel classpath");
            String schema = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            try (Statement stmt = conn.createStatement()) {
                // Esegue ogni statement separato da ";"
                for (String chunk : schema.split(";")) {
                    // Rimuove commenti a riga singola
                    String pulito = chunk.replaceAll("(?m)^\\s*--.*$", "").trim();
                    if (!pulito.isEmpty()) {
                        stmt.execute(chunk.trim());
                    }
                }
            }
        } catch (IOException | SQLException e) {
            throw new RuntimeException("[DBCheck] Errore durante l'applicazione dello schema: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // PASSO 3 — Verifica presenza dati
    // -------------------------------------------------------------------------

    /**
     * Esegue una query di conteggio sulla tabella "ristoranti" per capire se è presente del contenuto.
     *
     * @param conn la connessione SQL attiva
     * @return true se sono presenti record all'interno dei ristoranti, false altrimenti
     * @throws SQLException se si verifica un errore durante l'esecuzione della query
     */
    private static boolean datiPresenti(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery("SELECT COUNT(*) FROM ristoranti")) {
            rs.next();
            return rs.getLong(1) > 0;
        }
    }

    // -------------------------------------------------------------------------
    // PASSO 3b — Import CSV tramite ImportCSV
    // -------------------------------------------------------------------------

    /**
     * Cerca ed importa il file CSV dei ristoranti "michelin_my_maps.csv" all'interno del DB.
     * Se il file non esiste, salta l'importazione ed avvia il database vuoto.
     */
    private static void importaDaCSV() {
        String percorsoCSV = "dbtk/michelin_my_maps.csv";
        File csvFile = new File(percorsoCSV);

        if (!csvFile.exists()) {
            System.err.println("[DBCheck] File CSV non trovato: " + csvFile.getAbsolutePath());
            System.err.println("[DBCheck] Il server si avvierà con il database vuoto.");
            return;
        }

        System.out.printf("[DBCheck] CSV trovato. Import da: %s%n", csvFile.getAbsolutePath());

        try (Connection conn = ConnectionPool.getConnection()) {
            ImportCSV.importa(conn, percorsoCSV);
            System.out.println("[DBCheck] Import completato.");

            try (Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery(
                         "SELECT " +
                                 "(SELECT count(*) FROM ristoranti) AS ristoranti, " +
                                 "(SELECT count(*) FROM cucine)     AS cucine, " +
                                 "(SELECT count(*) FROM servizi)    AS servizi")) {
                rs.next();
                System.out.printf("[DBCheck] Riepilogo → ristoranti: %d | cucine: %d | servizi: %d%n",
                        rs.getLong("ristoranti"),
                        rs.getLong("cucine"),
                        rs.getLong("servizi"));
            }

        } catch (Exception e) {
            System.err.println("[DBCheck] ATTENZIONE: Import CSV fallito — " + e.getMessage());
            System.err.println("[DBCheck] Il server si avvierà con il database vuoto.");
        }
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    /**
     * Estrae il nome del database a partire da un URL JDBC di connessione.
     *
     * @param jdbcUrl stringa URL JDBC completa (es. "jdbc:postgresql://localhost:5432/dbtk?ssl=false")
     * @return il solo nome del database (es. "dbtk")
     */
    private static String estraiNomeDb(String jdbcUrl) {
        String path = jdbcUrl.substring(jdbcUrl.lastIndexOf('/') + 1);
        int q = path.indexOf('?');
        return q >= 0 ? path.substring(0, q) : path;
    }
}
