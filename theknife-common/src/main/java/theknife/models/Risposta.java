package theknife.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Modello dati che rappresenta la risposta inserita da un ristoratore ad una recensione.
 * Fa parte dei DTO/modelli serializzabili scambiati tra client e server.
 * Contiene il testo di risposta, i riferimenti all'autore (ristoratore) e alla recensione correlata,
 * più la data e l'ora di pubblicazione.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public class Risposta implements Serializable {

    private long          id;
    private long          recensioneId;
    private String        usernameRistoratore;
    private String        testo;
    private LocalDateTime dataRisposta;

    /**
     * Costruttore predefinito che inizializza la data della risposta al momento corrente.
     */
    public Risposta() {
        this.dataRisposta = LocalDateTime.now();
    }

    /**
     * Costruttore completo per creare una risposta con i dettagli essenziali.
     * Inizializza anche la data al momento corrente chiamando il costruttore di default.
     *
     * @param usernameRistoratore lo username del ristoratore autore
     * @param recensioneId l'ID della recensione a cui si risponde
     * @param testo il commento descrittivo di risposta
     */
    public Risposta(String usernameRistoratore, long recensioneId, String testo) {
        this();
        this.usernameRistoratore = usernameRistoratore;
        this.recensioneId        = recensioneId;
        this.testo               = testo;
    }

    /**
     * Restituisce la data della risposta formattata come stringa.
     * Formato utilizzato: "dd/MM/yyyy HH:mm".
     *
     * @return la data formattata come stringa
     */
    public String getDataRispostaFormatted() {
        return dataRisposta.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    /**
     * Restituisce una rappresentazione sintetica della risposta.
     *
     * @return descrizione testuale della risposta
     */
    @Override
    public String toString() {
        return "Risposta del ristoratore (" + getDataRispostaFormatted() + "): " + testo;
    }

    /**
     * Restituisce l'ID univoco della risposta.
     *
     * @return l'ID
     */
    public long getId()                        { return id; }

    /**
     * Imposta l'ID univoco della risposta.
     *
     * @param id il nuovo ID
     */
    public void setId(long id)                 { this.id = id; }

    /**
     * Restituisce l'ID della recensione associata.
     *
     * @return l'ID della recensione
     */
    public long getRecensioneId()                      { return recensioneId; }

    /**
     * Imposta l'ID della recensione associata.
     *
     * @param recensioneId il nuovo ID recensione
     */
    public void setRecensioneId(long recensioneId)     { this.recensioneId = recensioneId; }

    /**
     * Restituisce lo username del ristoratore.
     *
     * @return lo username del ristoratore
     */
    public String getUsernameRistoratore()                         { return usernameRistoratore; }

    /**
     * Imposta lo username del ristoratore.
     *
     * @param usernameRistoratore lo username del ristoratore
     */
    public void setUsernameRistoratore(String usernameRistoratore) { this.usernameRistoratore = usernameRistoratore; }

    /**
     * Restituisce il testo della risposta.
     *
     * @return il testo
     */
    public String getTesto()                   { return testo; }

    /**
     * Imposta il testo della risposta.
     *
     * @param testo il testo
     */
    public void setTesto(String testo)         { this.testo = testo; }

    /**
     * Restituisce la data e l'ora della risposta.
     *
     * @return data e ora di creazione
     */
    public LocalDateTime getDataRisposta()                     { return dataRisposta; }

    /**
     * Imposta la data e l'ora della risposta.
     *
     * @param dataRisposta la data di creazione
     */
    public void setDataRisposta(LocalDateTime dataRisposta)    { this.dataRisposta = dataRisposta; }
}
