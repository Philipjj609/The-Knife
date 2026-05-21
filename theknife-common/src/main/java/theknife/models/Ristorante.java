package theknife.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Modello dati che rappresenta un ristorante registrato nel sistema.
 * Fa parte delle classi di dominio serializzabili scambiate tra client e server.
 * Contiene i dati anagrafici (nome, descrizione, contatti), geografici (indirizzo, città, nazione, coordinate),
 * tariffari (fascia di prezzo), i servizi offerti (es. prenotazione online, delivery),
 * i premi Michelin (stelle rosse e stella verde) e la lista delle cucine e dei servizi.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public class Ristorante implements Serializable {

    private long         id;
    private String       nome;
    private String       indirizzo;
    private String       citta;
    private String       nazione;
    private double       latitudine;
    private double       longitudine;
    private int          prezzoLivello;   // 1=€  2=€€  3=€€€  4=€€€€
    private String       telefono;
    private String       url;
    private String       sitoWeb;
    private String       riconoscimento;  // "1 Star" | "2 Stars" | "3 Stars" | "Bib Gourmand" | null
    private boolean      greenStar;
    private String       descrizione;
    private boolean      delivery;
    private boolean      prenotazioneOnline;
    private long         proprietarioId;  // 0 = nessun gestore registrato
    private List<String> cucine   = new ArrayList<>();
    private List<String> servizi  = new ArrayList<>();

    /**
     * Costruttore predefinito (vuoto).
     */
    public Ristorante() {}

    /**
     * Costruttore completo per inizializzare tutte le proprietà del ristorante.
     *
     * @param id l'ID univoco del ristorante
     * @param nome il nome del ristorante
     * @param indirizzo la via/piazza e il civico del ristorante
     * @param citta la città in cui si trova il ristorante
     * @param nazione la nazione del ristorante
     * @param latitudine la coordinata di latitudine GPS
     * @param longitudine la coordinata di longitudine GPS
     * @param prezzoLivello il livello di prezzo (da 1 a 4)
     * @param telefono il numero di telefono
     * @param url URL dell'immagine di copertina o del dettaglio
     * @param sitoWeb il sito web ufficiale del ristorante
     * @param riconoscimento la descrizione del riconoscimento Michelin (es. "1 Star", "Bib Gourmand")
     * @param greenStar indica se il ristorante possiede la stella verde Michelin per la sostenibilità
     * @param descrizione una breve descrizione testuale del locale
     * @param delivery indica se supporta il servizio di consegna a domicilio
     * @param prenotazioneOnline indica se supporta la prenotazione dei tavoli online
     * @param proprietarioId l'ID dell'utente ristoratore proprietario (0 se non assegnato)
     * @param cucine lista dei tipi di cucina proposti
     * @param servizi lista dei servizi accessori offerti
     */
    public Ristorante(long id, String nome, String indirizzo, String citta, String nazione,
                      double latitudine, double longitudine, int prezzoLivello,
                      String telefono, String url, String sitoWeb,
                      String riconoscimento, boolean greenStar, String descrizione,
                      boolean delivery, boolean prenotazioneOnline, long proprietarioId,
                      List<String> cucine, List<String> servizi) {
        this.id                = id;
        this.nome              = nome;
        this.indirizzo         = indirizzo;
        this.citta             = citta;
        this.nazione           = nazione;
        this.latitudine        = latitudine;
        this.longitudine       = longitudine;
        this.prezzoLivello     = prezzoLivello;
        this.telefono          = telefono;
        this.url               = url;
        this.sitoWeb           = sitoWeb;
        this.riconoscimento    = riconoscimento;
        this.greenStar         = greenStar;
        this.descrizione       = descrizione;
        this.delivery          = delivery;
        this.prenotazioneOnline = prenotazioneOnline;
        this.proprietarioId    = proprietarioId;
        this.cucine            = cucine  != null ? cucine  : new ArrayList<>();
        this.servizi           = servizi != null ? servizi : new ArrayList<>();
    }

    /**
     * Estrae il numero di stelle Michelin assegnate al ristorante a partire dalla stringa di riconoscimento.
     *
     * @return il numero di stelle (0, 1, 2, o 3)
     */
    public int getStarCount() {
        if (riconoscimento == null) return 0;
        if (riconoscimento.startsWith("3")) return 3;
        if (riconoscimento.startsWith("2")) return 2;
        if (riconoscimento.startsWith("1")) return 1;
        return 0;
    }



    /**
     * Restituisce la stringa di simboli valuta (€) corrispondente alla fascia di prezzo.
     *
     * @return stringa formattata (es. "€€" per fascia 2)
     */
    public String getPrezzoStringa() {
        return "€".repeat(Math.max(0, prezzoLivello));
    }

    /**
     * Restituisce una rappresentazione sintetica del ristorante per debug o elenchi.
     *
     * @return descrizione testuale del locale
     */
    @Override
    public String toString() {
        return nome + " — " + String.join(", ", cucine) + " (" + citta + ", " + nazione + ")";
    }

    /**
     * Restituisce l'ID del ristorante.
     *
     * @return l'ID
     */
    public long getId()                        { return id; }

    /**
     * Imposta l'ID del ristorante.
     *
     * @param id il nuovo ID
     */
    public void setId(long id)                 { this.id = id; }

    /**
     * Restituisce il nome del ristorante.
     *
     * @return il nome
     */
    public String getNome()                    { return nome; }

    /**
     * Imposta il nome del ristorante.
     *
     * @param nome il nome
     */
    public void setNome(String nome)           { this.nome = nome; }

    /**
     * Restituisce l'indirizzo del ristorante.
     *
     * @return l'indirizzo
     */
    public String getIndirizzo()               { return indirizzo; }

    /**
     * Imposta l'indirizzo del ristorante.
     *
     * @param indirizzo l'indirizzo
     */
    public void setIndirizzo(String indirizzo) { this.indirizzo = indirizzo; }

    /**
     * Restituisce la città.
     *
     * @return la città
     */
    public String getCitta()                   { return citta; }

    /**
     * Imposta la città.
     *
     * @param citta la città
     */
    public void setCitta(String citta)         { this.citta = citta; }

    /**
     * Restituisce la nazione.
     *
     * @return la nazione
     */
    public String getNazione()                 { return nazione; }

    /**
     * Imposta la nazione.
     *
     * @param nazione la nazione
     */
    public void setNazione(String nazione)     { this.nazione = nazione; }

    /**
     * Restituisce la latitudine geografica.
     *
     * @return la latitudine
     */
    public double getLatitudine()              { return latitudine; }

    /**
     * Imposta la latitudine geografica.
     *
     * @param lat la latitudine
     */
    public void setLatitudine(double lat)      { this.latitudine = lat; }

    /**
     * Restituisce la longitudine geografica.
     *
     * @return la longitudine
     */
    public double getLongitudine()             { return longitudine; }

    /**
     * Imposta la longitudine geografica.
     *
     * @param lon la longitudine
     */
    public void setLongitudine(double lon)     { this.longitudine = lon; }

    /**
     * Restituisce il livello di prezzo (1-4).
     *
     * @return il livello di prezzo
     */
    public int getPrezzoLivello()                      { return prezzoLivello; }

    /**
     * Imposta il livello di prezzo.
     *
     * @param prezzoLivello il livello di prezzo (da 1 a 4)
     */
    public void setPrezzoLivello(int prezzoLivello)    { this.prezzoLivello = prezzoLivello; }

    /**
     * Restituisce il recapito telefonico del ristorante.
     *
     * @return il numero di telefono
     */
    public String getTelefono()                { return telefono; }

    /**
     * Imposta il recapito telefonico del ristorante.
     *
     * @param telefono il recapito telefonico
     */
    public void setTelefono(String telefono)   { this.telefono = telefono; }

    /**
     * Restituisce l'URL dell'immagine del ristorante.
     *
     * @return l'URL
     */
    public String getUrl()                     { return url; }

    /**
     * Imposta l'URL dell'immagine del ristorante.
     *
     * @param url l'URL
     */
    public void setUrl(String url)             { this.url = url; }

    /**
     * Restituisce il sito web ufficiale.
     *
     * @return il sito web
     */
    public String getSitoWeb()                 { return sitoWeb; }

    /**
     * Imposta il sito web ufficiale.
     *
     * @param sitoWeb il sito web
     */
    public void setSitoWeb(String sitoWeb)     { this.sitoWeb = sitoWeb; }

    /**
     * Restituisce il riconoscimento Michelin (es. "3 Stars").
     *
     * @return il riconoscimento o null se assente
     */
    public String getRiconoscimento()                        { return riconoscimento; }

    /**
     * Imposta il riconoscimento Michelin.
     *
     * @param riconoscimento il riconoscimento
     */
    public void setRiconoscimento(String riconoscimento)     { this.riconoscimento = riconoscimento; }

    /**
     * Verifica se possiede la stella verde Michelin per la gastronomia sostenibile.
     *
     * @return true se possiede la stella verde, false altrimenti
     */
    public boolean isGreenStar()               { return greenStar; }

    /**
     * Imposta lo stato della stella verde Michelin.
     *
     * @param greenStar true se ha la stella verde, false altrimenti
     */
    public void setGreenStar(boolean greenStar){ this.greenStar = greenStar; }

    /**
     * Restituisce la descrizione testuale del locale.
     *
     * @return la descrizione
     */
    public String getDescrizione()                 { return descrizione; }

    /**
     * Imposta la descrizione del locale.
     *
     * @param descrizione la descrizione
     */
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }

    /**
     * Verifica se effettua servizio a domicilio (delivery).
     *
     * @return true se supportato, false altrimenti
     */
    public boolean isDelivery()                { return delivery; }

    /**
     * Imposta la disponibilità del servizio a domicilio.
     *
     * @param delivery true se supportato, false altrimenti
     */
    public void setDelivery(boolean delivery)  { this.delivery = delivery; }

    /**
     * Verifica se supporta la prenotazione dei tavoli online.
     *
     * @return true se supportata, false altrimenti
     */
    public boolean isPrenotazioneOnline()                        { return prenotazioneOnline; }

    /**
     * Imposta la disponibilità della prenotazione online.
     *
     * @param prenotazioneOnline true se supportata, false altrimenti
     */
    public void setPrenotazioneOnline(boolean prenotazioneOnline){ this.prenotazioneOnline = prenotazioneOnline; }

    /**
     * Restituisce l'ID dell'utente ristoratore proprietario del locale.
     *
     * @return l'ID del proprietario, oppure 0 se non ancora rivendicato
     */
    public long getProprietarioId()                      { return proprietarioId; }

    /**
     * Imposta l'ID del proprietario del locale.
     *
     * @param proprietarioId l'ID proprietario
     */
    public void setProprietarioId(long proprietarioId)   { this.proprietarioId = proprietarioId; }

    /**
     * Restituisce l'elenco dei tipi di cucina proposti.
     *
     * @return la lista delle cucine
     */
    public List<String> getCucine()                  { return cucine; }

    /**
     * Imposta l'elenco dei tipi di cucina proposti.
     *
     * @param cucine la lista delle cucine
     */
    public void setCucine(List<String> cucine)       { this.cucine = cucine; }

    /**
     * Restituisce l'elenco dei servizi aggiuntivi.
     *
     * @return la lista dei servizi
     */
    public List<String> getServizi()                 { return servizi; }

    /**
     * Imposta l'elenco dei servizi aggiuntivi.
     *
     * @param servizi la lista dei servizi
     */
    public void setServizi(List<String> servizi)     { this.servizi = servizi; }
}
