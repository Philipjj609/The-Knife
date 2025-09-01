package theknife.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Rappresenta una recensione lasciata da un cliente per un ristorante.
 * <p>
 * Contiene informazioni come l'autore della recensione, il ristorante
 * a cui si riferisce, la valutazione (1-5), titolo, commento, data
 * e l'eventuale risposta del ristoratore.
 * </p>
 *
 * @author Philip Jon Ji Ciuca
 * @version 1.0
 * */
public class Recensione {
    private String id;
    private String usernameCliente;
    private String nomeRistorante;
    private int valutazione; // 1-5 stelle
    private String titolo;
    private String commento;
    private LocalDateTime dataRecensione;
    private Risposta risposta; // Risposta del ristoratore (opzionale)

    /**
     * Crea una nuova recensione vuota con ID generato e data corrente.
     * @since 1.0
     */
    public Recensione() {
        this.id = generateId();
        this.dataRecensione = LocalDateTime.now();
    }

    /**
     * Costruttore principale per creare una recensione con i campi essenziali.
     *
     * @param usernameCliente Username del cliente che ha scritto la recensione, must be non-null.
     * @param nomeRistorante Nome del ristorante a cui si riferisce la recensione, must be non-null.
     * @param valutazione Valutazione numerica da 1 a 5 (valori fuori range vengono ignorati dalla setter).
     * @param titolo Titolo della recensione.
     * @param commento Testo della recensione.
     * @since 1.0
     */
    public Recensione(String usernameCliente, String nomeRistorante, int valutazione,
            String titolo, String commento) {
        this();
        this.usernameCliente = usernameCliente;
        this.nomeRistorante = nomeRistorante;
        this.valutazione = valutazione;
        this.titolo = titolo;
        this.commento = commento;
    }

    /**
     * Genera un identificatore univoco per la recensione.
     * @return ID generato come String, non null.
     * @since 1.0
     */
    private String generateId() {
        return "REV_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000);
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsernameCliente() {
        return usernameCliente;
    }

    public void setUsernameCliente(String usernameCliente) {
        this.usernameCliente = usernameCliente;
    }

    public String getNomeRistorante() {
        return nomeRistorante;
    }

    public void setNomeRistorante(String nomeRistorante) {
        this.nomeRistorante = nomeRistorante;
    }

    public int getValutazione() {
        return valutazione;
    }

    public void setValutazione(int valutazione) {
        if (valutazione >= 1 && valutazione <= 5) {
            this.valutazione = valutazione;
        }
    }

    public String getTitolo() {
        return titolo;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }

    public String getCommento() {
        return commento;
    }

    public void setCommento(String commento) {
        this.commento = commento;
    }

    public LocalDateTime getDataRecensione() {
        return dataRecensione;
    }

    public void setDataRecensione(LocalDateTime dataRecensione) {
        this.dataRecensione = dataRecensione;
    }

    public Risposta getRisposta() {
        return risposta;
    }

    public void setRisposta(Risposta risposta) {
        this.risposta = risposta;
    }

    /**
     * Restituisce la data della recensione formattata come dd/MM/yyyy HH:mm.
     * @return String formattata della data della recensione.
     * @since 1.0
     */
    public String getDataRecensioneFormatted() {
        return dataRecensione.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    /**
     * Restituisce una rappresentazione a stelle della valutazione (es: ★★★☆☆).
     * @return String con stelle piene e vuote; se la valutazione non è impostata ritorna 5 stelle vuote.
     * @since 1.0
     */
    public String getStelle() {
        return "★".repeat(valutazione) + "☆".repeat(5 - valutazione);
    }

    /**
     * Rappresentazione testuale della recensione usata nelle ListView.
     * @return String contenente titolo, stelle, autore e data.
     * @since 1.0
     */
    @Override
    public String toString() {
        return String.format("%s - %s (%s) - %s",
                titolo, getStelle(), usernameCliente, getDataRecensioneFormatted());
    }
}
