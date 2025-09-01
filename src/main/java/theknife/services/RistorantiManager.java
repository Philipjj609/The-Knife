package theknife.services;

import theknife.models.Ristorante;
import theknife.Main;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gestisce l'associazione proprietario -> ristoranti e operazioni correlate.
 * <p>
 * Fornisce metodi per caricare/salvare la mappa dei proprietari, aggiungere o
 * rimuovere proprietari e recuperare i ristoranti associati ad un proprietario.
 * </p>
 *
 * @author Philip Jon Ji Ciuca
 * @version 1.0
 */
public class RistorantiManager {
    private static final String PROPRIETARI_FILE = "src/main/resources/data/proprietari_ristoranti.csv";
    private static Map<String, Set<String>> proprietari = new HashMap<>(); // username -> set di nomi ristoranti

    static {
        caricaProprietari();
    }

    /**
     * Carica i proprietari da un file CSV e popola la mappa dei proprietari.
     * Il file deve trovarsi nel percorso specificato da PROPRIETARI_FILE.
     * I dati esistenti nella mappa vengono sovrascritti.
     */
    public static void caricaProprietari() {
        proprietari.clear();
        File file = new File(PROPRIETARI_FILE);
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

                    proprietari.computeIfAbsent(username, k -> new HashSet<>()).add(nomeRistorante);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Salva la mappa dei proprietari su un file CSV.
     * Il file viene creato nel percorso specificato da PROPRIETARI_FILE.
     */
    public static void salvaProprietari() {
        File file = new File(PROPRIETARI_FILE);
        file.getParentFile().mkdirs();

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("username,nomeRistorante");

            for (Map.Entry<String, Set<String>> entry : proprietari.entrySet()) {
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
     * Aggiunge un ristorante a un proprietario.
     *
     * @param username       L'username del proprietario.
     * @param nomeRistorante Il nome del ristorante da aggiungere.
     */
    public static void aggiungiProprietario(String username, String nomeRistorante) {
        proprietari.computeIfAbsent(username, k -> new HashSet<>()).add(nomeRistorante);
        salvaProprietari();
    }

    /**
     * Rimuove un ristorante da un proprietario.
     *
     * @param username       L'username del proprietario.
     * @param nomeRistorante Il nome del ristorante da rimuovere.
     */
    public static void rimuoviProprietario(String username, String nomeRistorante) {
        Set<String> userRistoranti = proprietari.get(username);
        if (userRistoranti != null) {
            userRistoranti.remove(nomeRistorante);
            if (userRistoranti.isEmpty()) {
                proprietari.remove(username);
            }
            salvaProprietari();
        }
    }

    /**
     * Verifica se un utente è proprietario di un determinato ristorante.
     *
     * @param username       L'username del proprietario.
     * @param nomeRistorante Il nome del ristorante.
     * @return true se l'utente è proprietario del ristorante, false altrimenti.
     */
    public static boolean isProprietario(String username, String nomeRistorante) {
        Set<String> userRistoranti = proprietari.get(username);
        return userRistoranti != null && userRistoranti.contains(nomeRistorante);
    }

    /**
     * Restituisce la lista dei ristoranti associati a un proprietario.
     *
     * @param username L'username del proprietario.
     * @return Una lista di oggetti Ristorante associati al proprietario.
     */
    public static List<Ristorante> getRistorantiPerProprietario(String username) {
        if (Main.ristoranti == null) {
            return new ArrayList<>();
        }

        Set<String> nomiRistoranti = proprietari.getOrDefault(username, new HashSet<>());

        return Main.ristoranti.stream()
                .filter(r -> nomiRistoranti.contains(r.getName()))
                .collect(Collectors.toList());
    }

    /**
     * Restituisce i nomi dei ristoranti associati a un proprietario.
     *
     * @param username L'username del proprietario.
     * @return Un insieme di stringhe contenente i nomi dei ristoranti.
     */
    public static Set<String> getNomiRistorantiPerProprietario(String username) {
        return new HashSet<>(proprietari.getOrDefault(username, new HashSet<>()));
    }

    /**
     * Restituisce il numero di ristoranti associati a un proprietario.
     *
     * @param username L'username del proprietario.
     * @return Il numero di ristoranti associati.
     */
    public static int getNumeroRistoranti(String username) {
        Set<String> userRistoranti = proprietari.get(username);
        return userRistoranti != null ? userRistoranti.size() : 0;
    }

    /**
     * Restituisce il proprietario di un determinato ristorante.
     *
     * @param nomeRistorante Il nome del ristorante.
     * @return L'username del proprietario, oppure null se non trovato.
     */
    public static String getProprietarioRistorante(String nomeRistorante) {
        for (Map.Entry<String, Set<String>> entry : proprietari.entrySet()) {
            if (entry.getValue().contains(nomeRistorante)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Restituisce una lista di tutti i proprietari.
     *
     * @return Una lista di stringhe contenente gli username di tutti i proprietari.
     */
    public static List<String> getTuttiProprietari() {
        return new ArrayList<>(proprietari.keySet());
    }
}