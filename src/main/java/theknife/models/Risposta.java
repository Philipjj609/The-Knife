package theknife.models;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Rappresenta la risposta di un ristoratore ad una recensione.
 * <p>
 * Contiene informazioni sull'autore della risposta, il testo e la data.
 * Può essere collegata ad una Recensione tramite il suo ID.
 * </p>
 *
 * @author Philip Jon Ji Ciuca
 * @version 1.0
 * */
public class Risposta {
    private String id;
    private String usernameRistoratore;
    private String recensioneId;
    private String testo;
    private LocalDateTime dataRisposta;

    /**
     * Crea una nuova Risposta vuota con ID generato e data corrente.
     * @since 1.0
     */
    public Risposta() {
        this.id = generateId();
        this.dataRisposta = LocalDateTime.now();
    }

    /**
     * Costruttore che associa la risposta ad un ristoratore e ad una recensione.
     *
     * @param usernameRistoratore Username del ristoratore che risponde, must be non-null.
     * @param recensioneId ID della recensione a cui si riferisce la risposta.
     * @param testo Testo della risposta.
     * @since 1.0
     */
    public Risposta(String usernameRistoratore, String recensioneId, String testo) {
        this();
        this.usernameRistoratore = usernameRistoratore;
        this.recensioneId = recensioneId;
        this.testo = testo;
    }

    /**
     * Costruttore alternativo che non specifica l'ID della recensione.
     *
     * @param usernameRistoratore Username del ristoratore.
     * @param testo Testo della risposta.
     * @since 1.0
     */
    public Risposta(String usernameRistoratore, String testo) {
        this();
        this.usernameRistoratore = usernameRistoratore;
        this.testo = testo;
    }

    /**
     * Genera un identificatore unico per la risposta.
     * @return ID generato come String.
     * @since 1.0
     */
    private String generateId() {
        return "RESP_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000);
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsernameRistoratore() {
        return usernameRistoratore;
    }

    public void setUsernameRistoratore(String usernameRistoratore) {
        this.usernameRistoratore = usernameRistoratore;
    }

    public String getRecensioneId() {
        return recensioneId;
    }

    public void setRecensioneId(String recensioneId) {
        this.recensioneId = recensioneId;
    }

    public String getTesto() {
        return testo;
    }

    public void setTesto(String testo) {
        this.testo = testo;
    }

    public LocalDateTime getDataRisposta() {
        return dataRisposta;
    }

    public void setDataRisposta(LocalDateTime dataRisposta) {
        this.dataRisposta = dataRisposta;
    }

    public String getDataRispostaFormatted() {
        return dataRisposta.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    @Override
    public String toString() {
        return String.format("Risposta del ristoratore (%s): %s",
                getDataRispostaFormatted(), testo);
    }
}
