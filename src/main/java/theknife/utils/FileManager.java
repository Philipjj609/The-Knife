package theknife.utils;

import java.io.*;
import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import theknife.models.Ristorante;
import theknife.models.Utente;
import theknife.services.RistorantiManager;

/**
 * Utility per la gestione dei file CSV dell'applicazione.
 * <p>
 * Fornisce metodi per caricare e salvare ristoranti e utenti dai file CSV
 * presenti nella cartella resources/data. Contiene anche funzioni di utilità
 * per il parsing/escaping delle righe CSV.
 * </p>
 *
 * @author Philip Jon Ji Ciuca
 * @version 1.0
 */
public class FileManager {

    /**
     * Carica i ristoranti da un file CSV specificato dal percorso.
     * Supporta formati sia con le nuove colonne (16) che con il formato precedente (14).
     *
     * @param filePath Percorso del file CSV.
     * @return Lista di oggetti Ristorante letti dal file; lista vuota in caso di errore.
     * @since 1.0
     */
    public static List<Ristorante> caricaRistorantiDaCSV(String filePath) {
        List<Ristorante> ristoranti = new ArrayList<>();

        try (BufferedReader br = getBufferedReader(filePath)) {
            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue; // Skip header
                }

                String[] values = parseCSVLine(line);

                if (values.length >= 16) { // Aggiornato per le nuove colonne
                    try {
                        Ristorante r = new Ristorante(
                                cleanValue(values[0]), // Name
                                cleanValue(values[1]), // Address
                                cleanValue(values[2]), // Location
                                cleanValue(values[3]), // Price
                                cleanValue(values[4]), // Cuisine
                                parseDouble(values[5]), // Longitude
                                parseDouble(values[6]), // Latitude
                                cleanValue(values[7]), // PhoneNumber
                                cleanValue(values[8]), // Url
                                cleanValue(values[9]), // WebsiteUrl
                                cleanValue(values[10]), // Award
                                cleanValue(values[11]), // GreenStar
                                cleanValue(values[12]), // FacilitiesAndServices
                                cleanValue(values[13]), // Description
                                cleanValue(values[14]), // DeliveryAvailable
                                cleanValue(values[15]) // OnlineBookingAvailable
                        );
                        ristoranti.add(r);
                    } catch (Exception e) {
                        System.err.println(
                                "Errore nel parsing del ristorante: " + cleanValue(values[0]) + " - " + e.getMessage());
                    }
                } else if (values.length >= 14) {
                    // Compatibilità con i vecchi CSV che non hanno le nuove colonne
                    try {
                        Ristorante r = new Ristorante(
                                cleanValue(values[0]), // Name
                                cleanValue(values[1]), // Address
                                cleanValue(values[2]), // Location
                                cleanValue(values[3]), // Price
                                cleanValue(values[4]), // Cuisine
                                parseDouble(values[5]), // Longitude
                                parseDouble(values[6]), // Latitude
                                cleanValue(values[7]), // PhoneNumber
                                cleanValue(values[8]), // Url
                                cleanValue(values[9]), // WebsiteUrl
                                cleanValue(values[10]), // Award
                                cleanValue(values[11]), // GreenStar
                                cleanValue(values[12]), // FacilitiesAndServices
                                cleanValue(values[13]) // Description
                        );
                        ristoranti.add(r);
                    } catch (Exception e) {
                        System.err.println(
                                "Errore nel parsing del ristorante: " + cleanValue(values[0]) + " - " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Errore nel caricamento del file CSV: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("Caricati " + ristoranti.size() + " ristoranti dal CSV");
        return ristoranti;
    }

    /**
     * Carica tutti gli utenti dal file utenti.csv.
     *
     * @return Lista di oggetti Utente; lista vuota se il file non esiste o in caso di errore.
     * @since 1.0
     */
    public static List<Utente> caricaUtenti() {
        List<Utente> utenti = new ArrayList<>();
        String filePath = "data/utenti.csv";

        try (BufferedReader br = getBufferedReader(filePath)) {
            String line;

            while ((line = br.readLine()) != null) {
                String[] values = parseCSVLine(line);

                if (values.length >= 7) {
                    try {
                        Utente u = new Utente(
                                cleanValue(values[0]), // Nome
                                cleanValue(values[1]), // Cognome
                                cleanValue(values[2]), // Username
                                cleanValue(values[3]), // PasswordHash
                                cleanValue(values[4]), // DataNascita
                                cleanValue(values[5]), // Domicilio
                                cleanValue(values[6]) // Ruolo
                        );
                        utenti.add(u);
                    } catch (Exception e) {
                        System.err.println("Errore nel parsing dell'utente: " + e.getMessage());
                    }
                } else {
                    System.out.println("Record utente incompleto: " + Arrays.toString(values));
                }
            }
        } catch (IOException e) {
            System.out.println("Errore nel caricamento degli utenti: " + e.getMessage());
            e.printStackTrace();
        }
        return utenti;
    }

    /**
     * Aggiunge un nuovo utente al file utenti.csv (append).
     *
     * @param utente Oggetto Utente da salvare, must be non-null.
     * @since 1.0
     */
    public static void salvaUtente(Utente utente) {
        String resourcePath = "data/michelin_my_maps.csv";

        try (PrintWriter pw = getPrintWriterForWrite(resourcePath, true)) {
            pw.printf("%s,%s,%s,%s,%s,%s,%s%n",
                    escapeValue(utente.getNome()),
                    escapeValue(utente.getCognome()),
                    escapeValue(utente.getUsername()),
                    escapeValue(utente.getPasswordHash()),
                    escapeValue(utente.getDataNascita()),
                    escapeValue(utente.getDomicilio()),
                    escapeValue(utente.getRuolo()));
        } catch (IOException e) {
            System.err.println("Errore nel salvataggio dell'utente: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Utility methods
    /**
     * Parsea una riga CSV rispettando i campi racchiusi tra virgolette.
     * <p>
     * Restituisce un array con i valori trovati nella riga, preservando
     * le virgole che si trovano all'interno di campi tra virgolette.
     * </p>
     *
     * @param line La riga CSV da parsare, may be null or empty.
     * @return Array di String contenente i campi letti dalla riga.
     * @since 1.0
     */
    private static String[] parseCSVLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder currentField = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(currentField.toString());
                currentField = new StringBuilder();
            } else {
                currentField.append(c);
            }
        }
        result.add(currentField.toString());

        return result.toArray(new String[0]);
    }

    /**
     * Pulisce un valore letto dal CSV rimuovendo virgolette esterne e spazi.
     *
     * @param value Valore grezzo letto dal CSV, può essere null.
     * @return String pulita e senza virgolette esterne; stringa vuota se il valore è null.
     * @since 1.0
     */
    private static String cleanValue(String value) {
        if (value == null)
            return "";
        return value.trim().replaceAll("^\"|\"$", "");
    }

    /**
     * Escapa un valore per essere salvato correttamente in un CSV.
     * <p>
     * Se il valore contiene virgole, virgolette o newline viene racchiuso
     * tra virgolette e le virgolette interne vengono doppiate.
     * </p>
     *
     * @param value Valore da escapare, può essere null.
     * @return Valore pronto per il CSV; stringa vuota se il valore è null.
     * @since 1.0
     */
    private static String escapeValue(String value) {
        if (value == null)
            return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Converte una stringa in double gestendo valori null, vuoti o "N/A".
     *
     * @param value Stringa da convertire.
     * @return Valore double parsato o 0.0 in caso di valore non parsabile.
     * @since 1.0
     */
    private static double parseDouble(String value) {
        try {
            if (value == null || value.trim().isEmpty() || value.equalsIgnoreCase("N/A")) {
                return 0.0;
            }
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * Aggiunge un nuovo ristorante al file CSV
     */
    public static boolean aggiungiRistoranteAlCSV(Ristorante ristorante) {
        String resourcePath = "src/main/resources/data/michelin_my_maps.csv";

        try (PrintWriter pw = getPrintWriterForWrite(resourcePath, true)) {
            StringBuilder sb = new StringBuilder();

            // Formato aggiornato con le nuove colonne:
            // Name,Address,Location,Price,Cuisine,Longitude,Latitude,PhoneNumber,Url,WebsiteUrl,Award,GreenStar,FacilitiesAndServices,Description,DeliveryAvailable,OnlineBookingAvailable
            sb.append(escapeValue(ristorante.getName())).append(",");
            sb.append(escapeValue(ristorante.getAddress())).append(",");
            sb.append(escapeValue(ristorante.getLocation())).append(",");
            sb.append(escapeValue(ristorante.getPrice())).append(",");
            sb.append(escapeValue(ristorante.getCuisine())).append(",");
            sb.append(ristorante.getLongitude()).append(",");
            sb.append(ristorante.getLatitude()).append(",");
            sb.append(escapeValue(ristorante.getPhoneNumber())).append(",");
            sb.append(escapeValue(ristorante.getUrl())).append(",");
            sb.append(escapeValue(ristorante.getWebsiteUrl())).append(",");
            sb.append(escapeValue(ristorante.getAward())).append(",");
            sb.append(ristorante.getGreenStar()).append(",");
            sb.append(escapeValue(ristorante.getFacilitiesAndServices())).append(",");
            sb.append(escapeValue(ristorante.getDescription())).append(",");
            sb.append(ristorante.isDeliveryAvailable() ? "Sì" : "No").append(",");
            sb.append(ristorante.isOnlineBookingAvailable() ? "Sì" : "No");

            pw.println(sb.toString());

            // Aggiungi anche la proprietà al sistema
            RistorantiManager.aggiungiProprietario(ristorante.getProprietario(), ristorante.getName());

            return true;

        } catch (IOException e) {
            System.err.println("Errore nel salvataggio del ristorante: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica se esiste già un ristorante con il nome specificato
     */
    public static boolean esisteRistorante(String nomeRistorante) {
        if (nomeRistorante == null || nomeRistorante.trim().isEmpty()) {
            return false;
        }

        String resourcePath = "src/main/resources/data/michelin_my_maps.csv";

        try (BufferedReader br = getBufferedReader(resourcePath)) {
            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue; // Skip header
                }

                String[] values = parseCSVLine(line);
                if (values.length > 0) {
                    String nomeEsistente = cleanValue(values[0]);
                    if (nomeEsistente.equalsIgnoreCase(nomeRistorante.trim())) {
                        return true;
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Errore nella verifica del ristorante: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
    }

    // Added helpers to support classpath reading and external writing when running as jar
    private static BufferedReader getBufferedReader(String filePath) throws IOException {
        // Try opening as a regular file first
        File f = new File(filePath);
        if (f.exists()) {
            return new BufferedReader(new FileReader(f));
        }

        // If path looks like a project resource, strip the prefix
        String resourcePath = filePath;
        String prefix = "src/main/resources/";
        if (resourcePath.startsWith(prefix)) {
            resourcePath = resourcePath.substring(prefix.length());
        }

        InputStream is = FileManager.class.getClassLoader().getResourceAsStream(resourcePath);
        if (is != null) {
            return new BufferedReader(new InputStreamReader(is));
        }

        // Try also without leading slash
        is = FileManager.class.getResourceAsStream("/" + resourcePath);
        if (is != null) {
            return new BufferedReader(new InputStreamReader(is));
        }

        throw new FileNotFoundException(filePath + " (Impossibile trovare il percorso specificato o la risorsa nel classpath)");
    }

    private static PrintWriter getPrintWriterForWrite(String resourcePath, boolean append) throws IOException {
        File f = new File(resourcePath);
        if (f.exists() && f.canWrite()) {
            return new PrintWriter(new FileWriter(f, append));
        }

        // Fallback: crea/usa cartella 'data' nella working directory per permettere scrittura quando si esegue il jar
        Path dataDir = Paths.get("data");
        if (!Files.exists(dataDir)) {
            Files.createDirectories(dataDir);
        }
        String filename = new File(resourcePath).getName();
        Path out = dataDir.resolve(filename);
        return new PrintWriter(new FileWriter(out.toFile(), append));
    }
}