package theknife.services;

import theknife.models.Recensione;
import theknife.models.Risposta;
import theknife.models.Ristorante;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Gestisce il caricamento, il salvataggio e le operazioni sulle recensioni.
 * <p>
 * Fornisce metodi statici per leggere e scrivere il file CSV delle recensioni,
 * aggiungere recensioni e risposte, e ottenere viste filtrate delle recensioni
 * per ristorante, cliente o ristoratore.
 * </p>
 *
 * @author Philip Jon Ji Ciuca
 * @version 1.0
 */
public class RecensioniManager {
    private static final String RECENSIONI_FILE = "src/main/resources/data/recensioni.csv";
    private static List<Recensione> recensioni = new ArrayList<>();

    static {
        caricaRecensioni();
    }

    /**
     * Carica tutte le recensioni dal file CSV nella memoria (lista statica).
     * <p>
     * Se il file non esiste, il metodo ritorna silenziosamente lasciando la
     * lista delle recensioni vuota.
     * </p>
     * @since 1.0
     */
    public static void caricaRecensioni() {
        recensioni.clear();
        File file = new File(RECENSIONI_FILE);
        if (!file.exists()) {
            return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                if (parts.length >= 7) {
                    Recensione recensione = new Recensione();
                    recensione.setId(parts[0]);
                    recensione.setUsernameCliente(parts[1]);
                    recensione.setNomeRistorante(parts[2]);
                    recensione.setValutazione(Integer.parseInt(parts[3]));
                    recensione.setTitolo(parts[4].replace("\"", ""));
                    recensione.setCommento(parts[5].replace("\"", ""));
                    recensione.setDataRecensione(LocalDateTime.parse(parts[6]));

                    // Carica risposta se presente
                    if (parts.length >= 10 && !parts[7].isEmpty()) {
                        Risposta risposta = new Risposta();
                        risposta.setId(parts[7]);
                        risposta.setUsernameRistoratore(parts[8]);
                        risposta.setTesto(parts[9].replace("\"", ""));
                        if (parts.length >= 11) {
                            risposta.setDataRisposta(LocalDateTime.parse(parts[10]));
                        }
                        recensione.setRisposta(risposta);
                    }

                    recensioni.add(recensione);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Salva tutte le recensioni correnti nel file CSV.
     * Il file viene creato se non esiste.
     * @since 1.0
     */
    public static void salvaRecensioni() {
        File file = new File(RECENSIONI_FILE);
        file.getParentFile().mkdirs();

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println(
                    "id,usernameCliente,nomeRistorante,valutazione,titolo,commento,dataRecensione,rispostaId,usernameRistoratore,testoRisposta,dataRisposta");

            for (Recensione r : recensioni) {
                StringBuilder sb = new StringBuilder();
                sb.append(r.getId()).append(",");
                sb.append(r.getUsernameCliente()).append(",");
                sb.append(r.getNomeRistorante()).append(",");
                sb.append(r.getValutazione()).append(",");
                sb.append("\"").append(r.getTitolo()).append("\",");
                sb.append("\"").append(r.getCommento()).append("\",");
                sb.append(r.getDataRecensione()).append(",");

                if (r.getRisposta() != null) {
                    sb.append(r.getRisposta().getId()).append(",");
                    sb.append(r.getRisposta().getUsernameRistoratore()).append(",");
                    sb.append("\"").append(r.getRisposta().getTesto()).append("\",");
                    sb.append(r.getRisposta().getDataRisposta());
                } else {
                    sb.append(",,,");
                }

                writer.println(sb.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Aggiunge una recensione alla lista in memoria e la salva su disco.
     *
     * @param recensione Recensione da aggiungere, must be non-null.
     * @since 1.0
     */
    public static void aggiungiRecensione(Recensione recensione) {
        recensioni.add(recensione);
        salvaRecensioni();
    }

    /**
     * Restituisce le recensioni per un dato ristorante, ordinate dalla più
     * recente alla più vecchia.
     *
     * @param nomeRistorante Nome del ristorante da filtrare, must be non-null.
     * @return Lista ordinata di Recensione; lista vuota se non trovate.
     * @since 1.0
     */
    public static List<Recensione> getRecensioniPerRistorante(String nomeRistorante) {
        return recensioni.stream()
                .filter(r -> r.getNomeRistorante().equals(nomeRistorante))
                .sorted((r1, r2) -> r2.getDataRecensione().compareTo(r1.getDataRecensione()))
                .collect(Collectors.toList());
    }

    /**
     * Restituisce le recensioni scritte da un dato cliente.
     *
     * @param usernameCliente Username del cliente, must be non-null.
     * @return Lista ordinata di Recensione; lista vuota se non trovate.
     * @since 1.0
     */
    public static List<Recensione> getRecensioniPerCliente(String usernameCliente) {
        return recensioni.stream()
                .filter(r -> r.getUsernameCliente().equals(usernameCliente))
                .sorted((r1, r2) -> r2.getDataRecensione().compareTo(r1.getDataRecensione()))
                .collect(Collectors.toList());
    }

    /**
     * Restituisce le recensioni per tutti i ristoranti di un ristoratore.
     *
     * @param usernameRistoratore Username del ristoratore, must be non-null.
     * @return Lista ordinata di Recensione; lista vuota se non trovate.
     * @since 1.0
     */
    public static List<Recensione> getRecensioniPerRistoratore(String usernameRistoratore) {
        // Trova i ristoranti del ristoratore e poi le recensioni per quei ristoranti
        List<String> nomiRistoranti = RistorantiManager.getRistorantiPerProprietario(usernameRistoratore)
                .stream()
                .map(Ristorante::getName)
                .collect(Collectors.toList());

        return recensioni.stream()
                .filter(r -> nomiRistoranti.contains(r.getNomeRistorante()))
                .sorted((r1, r2) -> r2.getDataRecensione().compareTo(r1.getDataRecensione()))
                .collect(Collectors.toList());
    }

    /**
     * Aggiunge una risposta associata all'ID di una recensione.
     *
     * @param recensioneId ID della recensione a cui associare la risposta.
     * @param risposta Risposta da salvare.
     * @since 1.0
     */
    public static void aggiungiRisposta(String recensioneId, Risposta risposta) {
        recensioni.stream()
                .filter(r -> r.getId().equals(recensioneId))
                .findFirst()
                .ifPresent(r -> {
                    r.setRisposta(risposta);
                    salvaRecensioni();
                });
    }

    /**
     * Aggiunge una risposta a un oggetto Recensione esistente.
     *
     * @param recensione Oggetto Recensione target, must be non-null.
     * @param risposta Risposta da associare.
     * @since 1.0
     */
    public static void aggiungiRisposta(Recensione recensione, Risposta risposta) {
        // Trova la recensione e aggiungi la risposta
        for (Recensione r : recensioni) {
            if (r.getId().equals(recensione.getId())) {
                r.setRisposta(risposta);
                salvaRecensioni();
                break;
            }
        }
    }

    /**
     * Calcola la media delle valutazioni per un ristorante.
     *
     * @param nomeRistorante Nome del ristorante.
     * @return Valore medio delle valutazioni (0.0 se non ci sono recensioni).
     * @since 1.0
     */
    public static double getMediaValutazioni(String nomeRistorante) {
        List<Recensione> recensioniRistorante = getRecensioniPerRistorante(nomeRistorante);
        if (recensioniRistorante.isEmpty()) {
            return 0.0;
        }

        return recensioniRistorante.stream()
                .mapToInt(Recensione::getValutazione)
                .average()
                .orElse(0.0);
    }

    /**
     * Restituisce tutte le recensioni attualmente caricate in memoria.
     *
     * @return Nuova lista contenente tutte le recensioni.
     * @since 1.0
     */
    public static List<Recensione> getAllRecensioni() {
        return new ArrayList<>(recensioni);
    }
}