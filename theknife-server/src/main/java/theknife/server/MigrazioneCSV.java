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
 * Classe di utilità ed entry-point autonomo per eseguire la migrazione dei dati
 * dei ristoranti a partire da un file CSV Michelin all'interno del database TheKnife.
 * Utilizza {@link RistoranteDAO} per salvare le entità.
 *
 * <p>Uso da terminale:
 * <pre>
 *   java -cp theknife-server-shaded.jar theknife.server.MigrazioneCSV &lt;percorso.csv&gt;
 * </pre>
 *
 * <p>Esempio:
 * <pre>
 *   java -cp theknife-server-1.0.0-shaded.jar theknife.server.MigrazioneCSV dbtk/michelin_my_maps.csv
 * </pre>
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public class MigrazioneCSV {

    /**
     * Entry-point principale che esegue il processo di migrazione dei dati dal file CSV al database.
     *
     * @param args argomenti passati da riga di comando; il primo argomento opzionale specifica il percorso del file CSV
     * @throws Exception se si verificano errori di I/O o SQL durante la migrazione dei dati
     */
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

    /**
     * Carica le configurazioni di database dal file `db.properties` situato nel classpath.
     *
     * @return le proprietà caricate dal file di configurazione
     * @throws IOException se il file delle proprietà non è presente o non può essere letto
     */
    private static Properties caricaConfig() throws IOException {
        Properties props = new Properties();
        try (InputStream in = MigrazioneCSV.class.getResourceAsStream("/db.properties")) {
            if (in == null) throw new IOException("db.properties non trovato nel classpath");
            props.load(in);
        }
        return props;
    }

    /**
     * Analizza una singola riga di testo in formato CSV per mapparne i dati in un oggetto {@link Ristorante}.
     *
     * @param linea la riga del file CSV da decodificare
     * @return l'oggetto {@link Ristorante} corrispondente alla riga letta, o null se i dati sono insufficienti
     */
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

    /**
     * Mappa la stringa dei simboli valuta (€, $) in un intero indicante il livello di prezzo (da 1 a 4).
     *
     * @param s la stringa dei simboli valuta
     * @return un numero intero compreso tra 1 e 4
     */
    private static int mapPrezzo(String s) {
        if (s == null || s.isBlank()) return 1;
        long n = s.chars().filter(c -> c == '€' || c == '$').count();
        return (int) Math.max(1, Math.min(4, n > 0 ? n : s.trim().length()));
    }

    /**
     * Valida e normalizza la stringa di riconoscimento Michelin (es. stelle o Bib Gourmand).
     *
     * @param s la stringa del riconoscimento da mappare
     * @return la stringa normalizzata corrispondente all'enum, o null se non corrisponde a nessun valore valido
     */
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

    /**
     * Divide una stringa separata da virgole in una lista di singoli valori stringa ripuliti dagli spazi.
     *
     * @param s la stringa con valori multipli separati da virgola
     * @return la lista dei singoli elementi estratti
     */
    private static List<String> splitValues(String s) {
        List<String> result = new ArrayList<>();
        if (s == null || s.isBlank()) return result;
        for (String v : s.split(",")) {
            String t = v.trim();
            if (!t.isEmpty()) result.add(t);
        }
        return result;
    }

    /**
     * Restituisce vero se la stringa in input indica un valore logico positivo (es. "sì", "si", "yes").
     *
     * @param s la stringa da valutare
     * @return true se la stringa corrisponde a un valore positivo, false altrimenti
     */
    private static boolean isVero(String s) {
        return "sì".equalsIgnoreCase(s) || "si".equalsIgnoreCase(s) || "yes".equalsIgnoreCase(s);
    }

    /**
     * Restituisce null se la stringa passata è vuota o contiene solo spazi bianchi,
     * altrimenti restituisce la stringa stessa.
     *
     * @param s la stringa da verificare
     * @return la stringa originale, o null se vuota o blank
     */
    private static String nullIfEmpty(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    /**
     * Converte in modo sicuro una stringa in un valore numerico decimale (double).
     * In caso di eccezione restituisce 0.0.
     *
     * @param s la stringa contenente il valore decimale
     * @return il valore double convertito, o 0.0 in caso di fallimento
     */
    private static double parseDouble(String s) {
        try { return Double.parseDouble(s.trim()); }
        catch (Exception e) { return 0.0; }
    }

    /**
     * Parser personalizzato per righe in formato CSV in grado di ignorare le virgole
     * contenute all'interno dei campi delimitati da virgolette doppie.
     *
     * @param linea la riga di testo del file CSV da analizzare
     * @return una lista di stringhe rappresentanti i singoli campi estratti
     */
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
}
