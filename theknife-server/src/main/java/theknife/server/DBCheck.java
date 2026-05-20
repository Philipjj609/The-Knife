package theknife.server;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.Properties;
import java.util.Scanner;

/**
 * Verifica l'esistenza e la popolazione del database TheKnife.
 *
 * Flusso:
 *   1. Il database esiste?
 *      NO  → stampa istruzioni e termina (non crea automaticamente).
 *      SÌ  → prosegui.
 *   2. Lo schema è applicato (tabella "ristoranti" presente)?
 *      NO  → applica schema.sql dal classpath.
 *   3. Il database contiene già dei dati (ristoranti)?
 *      SÌ  → fine, tutto ok.
 *      NO  → chiedi all'utente il percorso del CSV Michelin e importa.
 *
 * Uso da ServerTK:
 *   <pre>
 *     DBCheck.verifica(props);   // lancia RuntimeException se il DB non esiste
 *     ConnectionPool.init(props);
 *   </pre>
 */
public class DBCheck {

    private DBCheck() {}

    /**
     * Punto d'ingresso principale.
     *
     * @param props proprietà con db.url / db.user / db.password già risolte
     * @throws RuntimeException se il database non esiste (interrompe il boot del server)
     */
    public static void verifica(Properties props) {
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

        // --- PASSO 2: schema applicato? ---
        try (Connection conn = DriverManager.getConnection(url, user, password)) {
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
            importaDaCSV(conn);

        } catch (SQLException e) {
            throw new RuntimeException("[DBCheck] Errore di connessione al database: " + e.getMessage(), e);
        }
    }

    // -------------------------------------------------------------------------
    // PASSO 1 — Verifica esistenza del database
    // -------------------------------------------------------------------------

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
     * Controlla se la tabella "ristoranti" esiste — proxy per "schema applicato".
     */
    private static boolean schemaApplicato(Connection conn) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, "public", "ristoranti", new String[]{"TABLE"})) {
            return rs.next();
        }
    }

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

    private static void importaDaCSV(Connection conn) {
        String percorsoCSV = "dbtk/michelin_my_maps.csv";
        File csvFile = new File(percorsoCSV);

        if (!csvFile.exists()) {
            System.err.println("[DBCheck] File CSV non trovato: " + csvFile.getAbsolutePath());
            System.err.println("[DBCheck] Il server si avvierà con il database vuoto.");
            return;
        }

        System.out.printf("[DBCheck] CSV trovato. Import da: %s%n", csvFile.getAbsolutePath());

        try {
            MigrazioneCSV.importa(percorsoCSV);
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

    private static String estraiNomeDb(String jdbcUrl) {
        String path = jdbcUrl.substring(jdbcUrl.lastIndexOf('/') + 1);
        int q = path.indexOf('?');
        return q >= 0 ? path.substring(0, q) : path;
    }
}
