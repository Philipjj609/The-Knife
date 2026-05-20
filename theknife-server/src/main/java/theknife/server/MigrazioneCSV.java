package theknife.server;

import theknife.dao.RistoranteDAO;
import theknife.dao.impl.RistoranteDAOImpl;
import theknife.db.ConnectionPool;
import theknife.models.Ristorante;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Utility di migrazione: importa i ristoranti da michelin_my_maps.csv nel DB.
 *
 * Uso:
 *   java -cp theknife-server-shaded.jar theknife.server.MigrazioneCSV <percorso.csv>
 *
 * Esempio:
 *   java -cp theknife-server-1.0.0-shaded.jar theknife.server.MigrazioneCSV dbtk/michelin_my_maps.csv
 */
public class MigrazioneCSV {

    public static void main(String[] args) throws Exception {
        String csvPath = args.length > 0 ? args[0] : "dbtk/michelin_my_maps.csv";
        File csvFile = new File(csvPath);

        if (!csvFile.exists()) {
            System.err.println("File CSV non trovato: " + csvFile.getAbsolutePath());
            System.exit(1);
        }

        // Carica la configurazione e inizializza il pool
        Properties props = caricaConfig();
        ConnectionPool.init(props);

        System.out.println("Connessione al database...");
        try (var c = ConnectionPool.getConnection()) {
            System.out.println("Database raggiunto.");
        } catch (Exception e) {
            System.err.println("Impossibile connettersi al DB: " + e.getMessage());
            System.exit(1);
        }

        RistoranteDAO dao = new RistoranteDAOImpl();
        int importati = 0, saltati = 0, errori = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(csvFile), StandardCharsets.UTF_8))) {

            reader.readLine(); // salta intestazione

            String linea;
            while ((linea = reader.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty()) continue;

                try {
                    Ristorante r = parseRiga(linea);
                    if (r == null) { errori++; continue; }

                    if (dao.existsByNomeAndIndirizzo(r.getNome(), r.getIndirizzo())) {
                        saltati++;
                    } else {
                        dao.save(r);
                        importati++;
                        if (importati % 100 == 0)
                            System.out.printf("  %d ristoranti importati...%n", importati);
                    }
                } catch (Exception e) {
                    errori++;
                    System.err.println("  Errore riga: " + e.getMessage()
                            + " → " + linea.substring(0, Math.min(60, linea.length())));
                }
            }
        }

        System.out.printf("%nMigrazione completata: %d importati, %d già presenti, %d errori%n",
                importati, saltati, errori);

        ConnectionPool.close();
    }

    // -------------------------------------------------------------------------

    private static Properties caricaConfig() throws IOException {
        Properties props = new Properties();
        try (InputStream in = MigrazioneCSV.class.getResourceAsStream("/db.properties")) {
            if (in == null) throw new IOException("db.properties non trovato nel classpath");
            props.load(in);
        }
        return props;
    }

    private static Ristorante parseRiga(String linea) {
        List<String> campi = parseCSV(linea);
        if (campi.size() < 14) return null;

        String nome        = campi.get(0);
        String indirizzo   = campi.get(1);
        String location    = campi.get(2);
        String prezzoStr   = campi.get(3);
        String cuisineStr  = campi.get(4);
        double longitudine = parseDouble(campi.get(5));
        double latitudine  = parseDouble(campi.get(6));
        String telefono    = campi.get(7);
        String url         = campi.get(8);
        String sitoWeb     = campi.get(9);
        String award       = campi.get(10);
        String greenStarStr = campi.size() > 11 ? campi.get(11) : "0";
        String facilities   = campi.size() > 12 ? campi.get(12) : "";
        String descrizione  = campi.size() > 13 ? campi.get(13) : "";
        String deliveryStr  = campi.size() > 14 ? campi.get(14) : "No";
        String prenotStr    = campi.size() > 15 ? campi.get(15) : "No";

        String[] locParts = location.split(",", 2);
        String citta  = locParts.length > 0 ? locParts[0].trim() : "";
        String nazione = locParts.length > 1 ? locParts[1].trim() : "";

        List<String> cucine  = splitValues(cuisineStr);
        List<String> servizi = splitValues(facilities);

        return new Ristorante(
                0L,
                nome, indirizzo, citta, nazione,
                latitudine, longitudine,
                mapPrezzo(prezzoStr),
                nullIfEmpty(telefono), nullIfEmpty(url), nullIfEmpty(sitoWeb),
                mapAward(award),
                "1".equals(greenStarStr),
                nullIfEmpty(descrizione),
                isVero(deliveryStr),
                isVero(prenotStr),
                0L,
                cucine, servizi
        );
    }

    private static int mapPrezzo(String s) {
        if (s == null || s.isBlank()) return 1;
        long n = s.chars().filter(c -> c == '€' || c == '$').count();
        return (int) Math.max(1, Math.min(4, n > 0 ? n : s.trim().length()));
    }

    private static String mapAward(String s) {
        if (s == null || s.isBlank()) return null;
        return switch (s.trim()) {
            case "3 Stars"              -> "3 Stars";
            case "2 Stars"              -> "2 Stars";
            case "1 Star"               -> "1 Star";
            case "Bib Gourmand"         -> "Bib Gourmand";
            case "Selected Restaurants" -> "Selected Restaurants";
            default                     -> null;
        };
    }

    private static List<String> splitValues(String s) {
        List<String> result = new ArrayList<>();
        if (s == null || s.isBlank()) return result;
        for (String v : s.split(",")) {
            String t = v.trim();
            if (!t.isEmpty()) result.add(t);
        }
        return result;
    }

    private static boolean isVero(String s) {
        return "sì".equalsIgnoreCase(s) || "si".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s);
    }

    private static String nullIfEmpty(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static double parseDouble(String s) {
        try { return Double.parseDouble(s.trim()); }
        catch (Exception e) { return 0.0; }
    }

    private static List<String> parseCSV(String linea) {
        List<String> campi = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuote = false;

        for (int i = 0; i < linea.length(); i++) {
            char c = linea.charAt(i);
            if (inQuote) {
                if (c == '"' && i + 1 < linea.length() && linea.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else if (c == '"') {
                    inQuote = false;
                } else {
                    cur.append(c);
                }
            } else {
                if (c == '"') {
                    inQuote = true;
                } else if (c == ',') {
                    campi.add(cur.toString().trim());
                    cur.setLength(0);
                } else {
                    cur.append(c);
                }
            }
        }
        campi.add(cur.toString().trim());
        return campi;
    }

    // In MigrazioneCSV.java — aggiungi questo metodo pubblico
    public static void importa(String csvPath) throws Exception {
        File csvFile = new File(csvPath);
        if (!csvFile.exists()) {
            throw new IOException("File CSV non trovato: " + csvFile.getAbsolutePath());
        }

        RistoranteDAO dao = new RistoranteDAOImpl();
        int importati = 0, saltati = 0, errori = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(csvFile), StandardCharsets.UTF_8))) {

            reader.readLine(); // salta intestazione

            String linea;
            while ((linea = reader.readLine()) != null) {
                linea = linea.trim();
                if (linea.isEmpty()) continue;

                try {
                    Ristorante r = parseRiga(linea);
                    if (r == null) { errori++; continue; }

                    if (dao.existsByNomeAndIndirizzo(r.getNome(), r.getIndirizzo())) {
                        saltati++;
                    } else {
                        dao.save(r);
                        importati++;
                        if (importati % 100 == 0)
                            System.out.printf("  %d ristoranti importati...%n", importati);
                    }
                } catch (Exception e) {
                    errori++;
                    System.err.println("  Errore riga: " + e.getMessage()
                            + " → " + linea.substring(0, Math.min(60, linea.length())));
                }
            }
        }

        System.out.printf("%nMigrazione completata: %d importati, %d già presenti, %d errori%n",
                importati, saltati, errori);
    }

}
