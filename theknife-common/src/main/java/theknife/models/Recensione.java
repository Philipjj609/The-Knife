package theknife.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Recensione implements Serializable {

    private long          id;
    private String        usernameCliente;
    private long          ristoranteId;
    private String        nomeRistorante;  // popolato via JOIN, usato per la visualizzazione
    private int           valutazione;
    private String        titolo;
    private String        commento;
    private LocalDateTime dataRecensione;
    private Risposta      risposta;

    public Recensione() {
        this.dataRecensione = LocalDateTime.now();
    }

    public Recensione(String usernameCliente, long ristoranteId,
                      int valutazione, String titolo, String commento) {
        this();
        this.usernameCliente = usernameCliente;
        this.ristoranteId    = ristoranteId;
        this.valutazione     = valutazione;
        this.titolo          = titolo;
        this.commento        = commento;
    }

    public String getStelle() {
        return "★".repeat(valutazione) + "☆".repeat(5 - valutazione);
    }

    public String getDataRecensioneFormatted() {
        return dataRecensione.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    @Override
    public String toString() {
        return String.format("%s — %s (%s) — %s",
                titolo, getStelle(), usernameCliente, getDataRecensioneFormatted());
    }

    // Getters e Setters
    public long getId()                          { return id; }
    public void setId(long id)                   { this.id = id; }

    public String getUsernameCliente()                         { return usernameCliente; }
    public void setUsernameCliente(String usernameCliente)     { this.usernameCliente = usernameCliente; }

    public long getRistoranteId()                      { return ristoranteId; }
    public void setRistoranteId(long ristoranteId)     { this.ristoranteId = ristoranteId; }

    public String getNomeRistorante()                        { return nomeRistorante; }
    public void setNomeRistorante(String nomeRistorante)     { this.nomeRistorante = nomeRistorante; }

    public int getValutazione()                    { return valutazione; }
    public void setValutazione(int valutazione) {
        if (valutazione >= 1 && valutazione <= 5) this.valutazione = valutazione;
    }

    public String getTitolo()                  { return titolo; }
    public void setTitolo(String titolo)       { this.titolo = titolo; }

    public String getCommento()                { return commento; }
    public void setCommento(String commento)   { this.commento = commento; }

    public LocalDateTime getDataRecensione()                       { return dataRecensione; }
    public void setDataRecensione(LocalDateTime dataRecensione)    { this.dataRecensione = dataRecensione; }

    public Risposta getRisposta()              { return risposta; }
    public void setRisposta(Risposta risposta) { this.risposta = risposta; }
}
