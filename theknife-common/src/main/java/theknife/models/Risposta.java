package theknife.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Modello dati che rappresenta la risposta di un ristoratore ad una recensione.
 *
 * Contiene il testo della risposta e il riferimento alla recensione originale.
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

    public Risposta() {
        this.dataRisposta = LocalDateTime.now();
    }

    public Risposta(String usernameRistoratore, long recensioneId, String testo) {
        this();
        this.usernameRistoratore = usernameRistoratore;
        this.recensioneId        = recensioneId;
        this.testo               = testo;
    }

    public String getDataRispostaFormatted() {
        return dataRisposta.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    @Override
    public String toString() {
        return "Risposta del ristoratore (" + getDataRispostaFormatted() + "): " + testo;
    }

    // Getters e Setters
    public long getId()                        { return id; }
    public void setId(long id)                 { this.id = id; }

    public long getRecensioneId()                      { return recensioneId; }
    public void setRecensioneId(long recensioneId)     { this.recensioneId = recensioneId; }

    public String getUsernameRistoratore()                         { return usernameRistoratore; }
    public void setUsernameRistoratore(String usernameRistoratore) { this.usernameRistoratore = usernameRistoratore; }

    public String getTesto()                   { return testo; }
    public void setTesto(String testo)         { this.testo = testo; }

    public LocalDateTime getDataRisposta()                     { return dataRisposta; }
    public void setDataRisposta(LocalDateTime dataRisposta)    { this.dataRisposta = dataRisposta; }
}
