package theknife.server;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;

/**
 * Importa un CSV Michelin nel database TheKnife.
 *
 * La logica replica fedelmente import_michelin.sql:
 *   1. Carica le righe del CSV in memoria (riga per riga).
 *   2. Inserisce/aggiorna i ristoranti (upsert su nome+indirizzo).
 *   3. Popola le lookup "cucine" e "servizi".
 *   4. Crea i legami N:M ristoranti_cucine e ristoranti_servizi.
 *
 * Tutto avviene in un'unica transazione; in caso di errore viene fatto rollback.
 */
public class ImportCSV {

    private ImportCSV() {}

    /**
     * Esegue l'import del CSV sulla connessione fornita.
     * La connessione deve essere aperta e non in autocommit quando viene chiamato
     * questo metodo (la gestione della transazione è interna).
     *
     * @param conn       connessione JDBC già aperta
     * @param percorsoCSV percorso assoluto del file CSV Michelin
     */
    public static void importa(Connection conn, String percorsoCSV) throws Exception {
        boolean autoCommitOriginale = conn.getAutoCommit();
        conn.setAutoCommit(false);

        try {
            caricaERielabora(conn, percorsoCSV);
            conn.commit();
        } catch (Exception e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(autoCommitOriginale);
        }
    }

    // -------------------------------------------------------------------------

    private static void caricaERielabora(Connection conn, String percorsoCSV) throws Exception {

        // --- 1. Tabella temporanea in memoria (emulata con una TEMP TABLE PostgreSQL) ---
        try (Statement s = conn.createStatement()) {
            s.execute("""
                CREATE TEMP TABLE IF NOT EXISTS import_michelin_tmp (
                    name                        TEXT,
                    address                     TEXT,
                    location                    TEXT,
                    price                       TEXT,
                    cuisine                     TEXT,
                    longitude                   TEXT,
                    latitude                    TEXT,
                    phone_number                TEXT,
                    url                         TEXT,
                    website_url                 TEXT,
                    award                       TEXT,
                    green_star                  TEXT,
                    facilities_and_services     TEXT,
                    description                 TEXT,
                    delivery_available          TEXT,
                    online_booking_available    TEXT
                ) ON COMMIT DROP
                """);
        }

        // --- 2. Caricamento riga per riga dal CSV Java (alternativa a \copy) ---
        inserisciRigheCSV(conn, percorsoCSV);

        // --- 3. Upsert ristoranti ---
        try (Statement s = conn.createStatement()) {
            s.execute("""
                INSERT INTO ristoranti (
                    nome, indirizzo, citta, nazione, latitudine, longitudine, prezzo_livello,
                    telefono, url, sito_web, riconoscimento, green_star, descrizione,
                    delivery, prenotazione_online
                )
                SELECT DISTINCT ON (btrim(name), NULLIF(btrim(address), ''))
                       btrim(name),
                       NULLIF(btrim(address), ''),
                       NULLIF(btrim(split_part(location, ',', 1)), ''),
                       CASE WHEN position(',' IN location) > 0
                            THEN NULLIF(btrim(regexp_replace(location, '^.*,', '')), '')
                            ELSE NULL END,
                       CASE WHEN btrim(latitude)  ~ '^-?[0-9]+(\\.[0-9]+)?$'
                            THEN btrim(latitude)::numeric  ELSE NULL END,
                       CASE WHEN btrim(longitude) ~ '^-?[0-9]+(\\.[0-9]+)?$'
                            THEN btrim(longitude)::numeric ELSE NULL END,
                       CASE WHEN NULLIF(btrim(price), '') IS NULL THEN NULL
                            ELSE LEAST(char_length(btrim(price)), 4)::smallint END,
                       NULLIF(btrim(phone_number), ''),
                       NULLIF(btrim(url), ''),
                       NULLIF(btrim(website_url), ''),
                       CASE WHEN btrim(award) IN ('1 Star','2 Stars','3 Stars','Selected Restaurants','Bib Gourmand')
                            THEN btrim(award)::riconoscimento_michelin ELSE NULL END,
                       lower(btrim(green_star)) IN ('1','true','t','yes','y','si','sì'),
                       NULLIF(btrim(description), ''),
                       lower(btrim(delivery_available))         IN ('1','true','t','yes','y','si','sì'),
                       lower(btrim(online_booking_available))   IN ('1','true','t','yes','y','si','sì')
                FROM import_michelin_tmp
                WHERE NULLIF(btrim(name), '') IS NOT NULL
                ORDER BY btrim(name), NULLIF(btrim(address), '')
                ON CONFLICT (nome, indirizzo) DO UPDATE SET
                    citta               = EXCLUDED.citta,
                    nazione             = EXCLUDED.nazione,
                    latitudine          = EXCLUDED.latitudine,
                    longitudine         = EXCLUDED.longitudine,
                    prezzo_livello      = EXCLUDED.prezzo_livello,
                    telefono            = EXCLUDED.telefono,
                    url                 = EXCLUDED.url,
                    sito_web            = EXCLUDED.sito_web,
                    riconoscimento      = EXCLUDED.riconoscimento,
                    green_star          = EXCLUDED.green_star,
                    descrizione         = EXCLUDED.descrizione,
                    delivery            = EXCLUDED.delivery,
                    prenotazione_online = EXCLUDED.prenotazione_online
                """);
        }

        // --- 4. Lookup cucine ---
        try (Statement s = conn.createStatement()) {
            s.execute("""
                INSERT INTO cucine (nome)
                SELECT DISTINCT btrim(value)
                FROM import_michelin_tmp,
                     regexp_split_to_table(coalesce(cuisine, ''), '\\s*,\\s*') AS value
                WHERE NULLIF(btrim(value), '') IS NOT NULL
                ON CONFLICT (nome) DO NOTHING
                """);
        }

        // --- 5. Lookup servizi ---
        try (Statement s = conn.createStatement()) {
            s.execute("""
                INSERT INTO servizi (nome)
                SELECT DISTINCT btrim(value)
                FROM import_michelin_tmp,
                     regexp_split_to_table(coalesce(facilities_and_services, ''), '\\s*,\\s*') AS value
                WHERE NULLIF(btrim(value), '') IS NOT NULL
                ON CONFLICT (nome) DO NOTHING
                """);
        }

        // --- 6. Associazioni ristoranti_cucine ---
        try (Statement s = conn.createStatement()) {
            s.execute("""
                INSERT INTO ristoranti_cucine (ristorante_id, cucina_id)
                SELECT r.id, c.id
                FROM (
                    SELECT DISTINCT btrim(name) AS nome,
                                    NULLIF(btrim(address), '') AS indirizzo,
                                    btrim(value) AS cucina
                    FROM import_michelin_tmp,
                         regexp_split_to_table(coalesce(cuisine, ''), '\\s*,\\s*') AS value
                    WHERE NULLIF(btrim(name), '') IS NOT NULL
                      AND NULLIF(btrim(value), '') IS NOT NULL
                ) src
                JOIN ristoranti r ON r.nome = src.nome
                                 AND r.indirizzo IS NOT DISTINCT FROM src.indirizzo
                JOIN cucine c     ON c.nome = src.cucina
                ON CONFLICT DO NOTHING
                """);
        }

        // --- 7. Associazioni ristoranti_servizi ---
        try (Statement s = conn.createStatement()) {
            s.execute("""
                INSERT INTO ristoranti_servizi (ristorante_id, servizio_id)
                SELECT r.id, sv.id
                FROM (
                    SELECT DISTINCT btrim(name) AS nome,
                                    NULLIF(btrim(address), '') AS indirizzo,
                                    btrim(value) AS servizio
                    FROM import_michelin_tmp,
                         regexp_split_to_table(coalesce(facilities_and_services, ''), '\\s*,\\s*') AS value
                    WHERE NULLIF(btrim(name), '') IS NOT NULL
                      AND NULLIF(btrim(value), '') IS NOT NULL
                ) src
                JOIN ristoranti r ON r.nome = src.nome
                                 AND r.indirizzo IS NOT DISTINCT FROM src.indirizzo
                JOIN servizi sv   ON sv.nome = src.servizio
                ON CONFLICT DO NOTHING
                """);
        }
    }

    /**
     * Legge il CSV riga per riga e lo inserisce nella tabella temporanea
     * tramite un PreparedStatement con batch insert.
     *
     * Gestisce:
     *   - riga di intestazione (skippata)
     *   - campi quotati con virgole interne
     *   - encoding UTF-8
     */
    private static void inserisciRigheCSV(Connection conn, String percorsoCSV) throws Exception {
        String sql = """
            INSERT INTO import_michelin_tmp
                (name, address, location, price, cuisine, longitude, latitude,
                 phone_number, url, website_url, award, green_star,
                 facilities_and_services, description, delivery_available, online_booking_available)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;

        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(percorsoCSV), StandardCharsets.UTF_8));
             PreparedStatement ps = conn.prepareStatement(sql)) {

            String headerLine = br.readLine(); // salta intestazione
            if (headerLine == null) throw new IOException("Il file CSV è vuoto");

            String line;
            int batchSize = 0;
            final int MAX_BATCH = 500;

            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;

                String[] campi = splitCSV(line);

                // Il CSV Michelin ha 16 colonne; righe malformate vengono saltate
                if (campi.length < 16) continue;

                for (int i = 0; i < 16; i++) {
                    String valore = campi[i].trim();
                    ps.setString(i + 1, valore.isEmpty() ? null : valore);
                }

                ps.addBatch();
                batchSize++;

                if (batchSize >= MAX_BATCH) {
                    ps.executeBatch();
                    batchSize = 0;
                }
            }

            if (batchSize > 0) ps.executeBatch();
        }
    }

    /**
     * Parser CSV minimale che gestisce campi quotati con virgole interne.
     * Supporta il formato RFC 4180 (campi tra doppi apici, apici doppi escaped).
     */
    private static String[] splitCSV(String line) {
        java.util.List<String> campi = new java.util.ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuote = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inQuote) {
                if (c == '"') {
                    // doppio apice escaped?
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        sb.append('"');
                        i++;
                    } else {
                        inQuote = false;
                    }
                } else {
                    sb.append(c);
                }
            } else {
                if (c == '"') {
                    inQuote = true;
                } else if (c == ',') {
                    campi.add(sb.toString());
                    sb.setLength(0);
                } else {
                    sb.append(c);
                }
            }
        }
        campi.add(sb.toString());

        return campi.toArray(new String[0]);
    }
}
