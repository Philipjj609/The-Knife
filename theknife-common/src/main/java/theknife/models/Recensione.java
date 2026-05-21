package theknife.models;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Modello dati che rappresenta una recensione effettuata da un cliente per un ristorante.
 * Fa parte dei DTO/modelli serializzabili per il trasferimento dati client-server.
 * Contiene informazioni come il voto numerico (da 1 a 5), il titolo, il commento,
 * la data di pubblicazione, i dettagli dell'autore e l'eventuale risposta associata.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public class Recensione implements Serializable {

    private long          id;
    private String        usernameCliente;
    private String        nomeCliente;
    private String        cognomeCliente;
    private long          ristoranteId;
    private String        nomeRistorante;  // popolato via JOIN, usato per la visualizzazione
    private int           valutazione;
    private String        titolo;
    private String        commento;
    private LocalDateTime dataRecensione;
    private Risposta      risposta;

    /**
     * Costruttore predefinito che inizializza la data della recensione al momento corrente.
     */
    public Recensione() {
        this.dataRecensione = LocalDateTime.now();
    }

    /**
     * Costruttore completo per creare una recensione con i dettagli essenziali.
     * Inizializza anche la data al momento corrente chiamando il costruttore di default.
     *
     * @param usernameCliente lo username del cliente che inserisce la recensione
     * @param ristoranteId l'ID del ristorante recensito
     * @param valutazione voto numerico (stelle da 1 a 5)
     * @param titolo il titolo della recensione
     * @param commento il testo descrittivo della recensione
     */
    public Recensione(String usernameCliente, long ristoranteId,
                      int valutazione, String titolo, String commento) {
        this();
        this.usernameCliente = usernameCliente;
        this.ristoranteId    = ristoranteId;
        this.valutazione     = valutazione;
        this.titolo          = titolo;
        this.commento        = commento;
    }

    /**
     * Restituisce una rappresentazione grafica a stelle della valutazione (es. "★★★☆☆").
     *
     * @return stringa formattata contenente caratteri stella pieni e vuoti
     */
    public String getStelle() {
        return "★".repeat(valutazione) + "☆".repeat(5 - valutazione);
    }

    /**
     * Restituisce la data della recensione formattata come stringa.
     * Formato utilizzato: "dd/MM/yyyy HH:mm".
     *
     * @return la data formattata come stringa
     */
    public String getDataRecensioneFormatted() {
        return dataRecensione.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    /**
     * Restituisce il nome completo dell'autore della recensione (Nome Cognome).
     * Se non presenti, restituisce lo username del cliente.
     *
     * @return nome visualizzabile dell'autore
     */
    public String getAutoreDisplayName() {
        String nomeCompleto = String.join(" ",
                 nomeCliente != null ? nomeCliente.trim() : "",
                 cognomeCliente != null ? cognomeCliente.trim() : "").trim();
        return !nomeCompleto.isEmpty() ? nomeCompleto : usernameCliente;
    }

    /**
     * Restituisce la descrizione sintetica della recensione.
     *
     * @return rappresentazione testuale
     */
    @Override
    public String toString() {
        return String.format("%s — %s (%s) — %s",
                titolo, getStelle(), usernameCliente, getDataRecensioneFormatted());
    }

    /**
     * Restituisce l'ID univoco della recensione.
     *
     * @return l'ID
     */
    public long getId()                          { return id; }

    /**
     * Imposta l'ID univoco della recensione.
     *
     * @param id il nuovo ID
     */
    public void setId(long id)                   { this.id = id; }

    /**
     * Restituisce lo username del cliente autore.
     *
     * @return lo username
     */
    public String getUsernameCliente()                         { return usernameCliente; }

    /**
     * Imposta lo username del cliente autore.
     *
     * @param usernameCliente il nuovo username cliente
     */
    public void setUsernameCliente(String usernameCliente)     { this.usernameCliente = usernameCliente; }

    /**
     * Restituisce il nome dell'autore.
     *
     * @return il nome
     */
    public String getNomeCliente()                 { return nomeCliente; }

    /**
     * Imposta il nome dell'autore.
     *
     * @param nomeCliente il nome
     */
    public void setNomeCliente(String nomeCliente) { this.nomeCliente = nomeCliente; }

    /**
     * Restituisce il cognome dell'autore.
     *
     * @return il cognome
     */
    public String getCognomeCliente()                       { return cognomeCliente; }

    /**
     * Imposta il cognome dell'autore.
     *
     * @param cognomeCliente il cognome
     */
    public void setCognomeCliente(String cognomeCliente)     { this.cognomeCliente = cognomeCliente; }

    /**
     * Restituisce l'ID del ristorante a cui si riferisce la recensione.
     *
     * @return l'ID ristorante
     */
    public long getRistoranteId()                      { return ristoranteId; }

    /**
     * Imposta l'ID del ristorante.
     *
     * @param ristoranteId l'ID ristorante
     */
    public void setRistoranteId(long ristoranteId)     { this.ristoranteId = ristoranteId; }

    /**
     * Restituisce il nome del ristorante (popolato per scopi di visualizzazione).
     *
     * @return il nome del ristorante
     */
    public String getNomeRistorante()                        { return nomeRistorante; }

    /**
     * Imposta il nome del ristorante.
     *
     * @param nomeRistorante il nome del ristorante
     */
    public void setNomeRistorante(String nomeRistorante)     { this.nomeRistorante = nomeRistorante; }

    /**
     * Restituisce il valore numerico della valutazione (da 1 a 5).
     *
     * @return il voto espresso in stelle (1-5)
     */
    public int getValutazione()                    { return valutazione; }

    /**
     * Imposta la valutazione. Il valore viene memorizzato solo se compreso tra 1 e 5 (inclusi).
     *
     * @param valutazione il nuovo voto (1-5)
     */
    public void setValutazione(int valutazione) {
        if (valutazione >= 1 && valutazione <= 5) this.valutazione = valutazione;
    }

    /**
     * Restituisce il titolo sintattico della recensione.
     *
     * @return il titolo
     */
    public String getTitolo()                  { return titolo; }

    /**
     * Imposta il titolo della recensione.
     *
     * @param titolo il titolo
     */
    public void setTitolo(String titolo)       { this.titolo = titolo; }

    /**
     * Restituisce il testo del commento descrittivo.
     *
     * @return il commento
     */
    public String getCommento()                { return commento; }

    /**
     * Imposta il commento descrittivo della recensione.
     *
     * @param commento il commento
     */
    public void setCommento(String commento)   { this.commento = commento; }

    /**
     * Restituisce la data e l'ora in cui la recensione è stata inserita.
     *
     * @return data e ora di creazione
     */
    public LocalDateTime getDataRecensione()                       { return dataRecensione; }

    /**
     * Imposta la data e l'ora della recensione.
     *
     * @param dataRecensione la data di creazione
     */
    public void setDataRecensione(LocalDateTime dataRecensione)    { this.dataRecensione = dataRecensione; }

    /**
     * Restituisce l'eventuale risposta inviata dal ristoratore per questa recensione.
     *
     * @return la risposta o null se non presente
     */
    public Risposta getRisposta()              { return risposta; }

    /**
     * Associa una risposta del ristoratore alla recensione.
     *
     * @param risposta la risposta
     */
    public void setRisposta(Risposta risposta) { this.risposta = risposta; }
}
