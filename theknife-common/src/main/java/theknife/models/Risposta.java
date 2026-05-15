package theknife.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
