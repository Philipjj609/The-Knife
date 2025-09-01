package theknife.services;

import theknife.models.Ristorante;
import theknife.Main;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gestisce i preferiti degli utenti.
 * <p>
 * Fornisce metodi statici per caricare/salvare la mappa username -> ristoranti preferiti
 * e per aggiungere/rimuovere/controllare i preferiti di un utente.
 * </p>
 *
 * @author Philip Jon Ji Ciuca
 * @version 1.0
 * */
public class PreferitiManager {
    private static final String PREFERITI_FILE = "src/main/resources/data/preferiti.csv";
    private static Map<String, Set<String>> preferiti = new HashMap<>(); // username -> set di nomi ristoranti

    static {
        caricaPreferiti();
    }

    /**
     * Carica i preferiti da un file CSV nella mappa dei preferiti.
     * <p>
     * Il file deve trovarsi nel percorso specificato da {@link #PREFERITI_FILE}.
     * Ogni riga del file deve contenere un nome utente e un ristorante preferito, separati da una virgola.
     * </p>
     */
    public static void caricaPreferiti() {
        preferiti.clear();
        File file = new File(PREFERITI_FILE);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    String username = parts[0];
                    String nomeRistorante = parts[1];
                    
                    preferiti.computeIfAbsent(username, k -> new HashSet<>()).add(nomeRistorante);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Salva i preferiti correnti nella mappa dei preferiti su un file CSV.
     * <p>
     * Il file verrà creato o sovrascritto nel percorso specificato da {@link #PREFERITI_FILE}.
     * </p>
     */
    public static void salvaPreferiti() {
        File file = new File(PREFERITI_FILE);
        file.getParentFile().mkdirs();
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("username,nomeRistorante");
            
            for (Map.Entry<String, Set<String>> entry : preferiti.entrySet()) {
                String username = entry.getKey();
                for (String nomeRistorante : entry.getValue()) {
                    writer.println(username + "," + nomeRistorante);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Aggiunge un ristorante ai preferiti di un utente.
     *
     * @param username      il nome dell'utente
     * @param nomeRistorante il nome del ristorante da aggiungere ai preferiti
     */
    public static void aggiungiPreferito(String username, String nomeRistorante) {
        preferiti.computeIfAbsent(username, k -> new HashSet<>()).add(nomeRistorante);
        salvaPreferiti();
    }

    /**
     * Rimuove un ristorante dai preferiti di un utente.
     *
     * @param username      il nome dell'utente
     * @param nomeRistorante il nome del ristorante da rimuovere dai preferiti
     */
    public static void rimuoviPreferito(String username, String nomeRistorante) {
        Set<String> userPreferiti = preferiti.get(username);
        if (userPreferiti != null) {
            userPreferiti.remove(nomeRistorante);
            if (userPreferiti.isEmpty()) {
                preferiti.remove(username);
            }
            salvaPreferiti();
        }
    }

    /**
     * Controlla se un ristorante è nei preferiti di un utente.
     *
     * @param username      il nome dell'utente
     * @param nomeRistorante il nome del ristorante da controllare
     * @return true se il ristorante è nei preferiti dell'utente, false altrimenti
     */
    public static boolean isPreferito(String username, String nomeRistorante) {
        Set<String> userPreferiti = preferiti.get(username);
        return userPreferiti != null && userPreferiti.contains(nomeRistorante);
    }

    /**
     * Restituisce una lista di ristoranti preferiti per un dato utente.
     *
     * @param username il nome dell'utente
     * @return una lista di ristoranti preferiti dall'utente, o una lista vuota se non ce ne sono
     */
    public static List<Ristorante> getPreferitiPerUtente(String username) {
        if (Main.ristoranti == null) {
            return new ArrayList<>();
        }

        Set<String> nomiPreferiti = preferiti.getOrDefault(username, new HashSet<>());
        
        return Main.ristoranti.stream()
                .filter(r -> nomiPreferiti.contains(r.getName()))
                .collect(Collectors.toList());
    }

    /**
     * Restituisce i nomi dei ristoranti preferiti per un dato utente.
     *
     * @param username il nome dell'utente
     * @return un insieme di nomi di ristoranti preferiti dall'utente
     */
    public static Set<String> getNomiPreferitiPerUtente(String username) {
        return new HashSet<>(preferiti.getOrDefault(username, new HashSet<>()));
    }

    /**
     * Restituisce il numero di ristoranti preferiti da un utente.
     *
     * @param username il nome dell'utente
     * @return il numero di ristoranti nei preferiti dell'utente
     */
    public static int getNumeroPreferiti(String username) {
        Set<String> userPreferiti = preferiti.get(username);
        return userPreferiti != null ? userPreferiti.size() : 0;
    }
}