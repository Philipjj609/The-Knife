package theknife.models;

import java.io.Serializable;

/**
 * Modello dati per i parametri di ricerca e filtraggio dei ristoranti.
 * Partecipa al design pattern <b>Builder</b> per la costruzione controllata dei filtri.
 * Viene inviato come parametro all'interno dei DTO di richiesta per le ricerche dei ristoranti.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public class FiltriRicerca implements Serializable {

    private String  nome;
    private String  citta;
    private String  nazione;
    private String  cucina;
    private String  servizio;
    private Integer prezzoLivello;
    private String  riconoscimento;
    private boolean soloDelivery;
    private boolean soloPrenotazione;

    private FiltriRicerca() {}

    /**
     * Crea un nuovo costruttore (Builder) per configurare i filtri di ricerca.
     *
     * @return un'istanza di {@link Builder} per la composizione fluida dei parametri
     */
    public static Builder builder() { return new Builder(); }

    /**
     * Classe interna che implementa il pattern <b>Builder</b> per {@link FiltriRicerca}.
     */
    public static class Builder {
        private final FiltriRicerca f = new FiltriRicerca();

        /**
         * Imposta il filtro sul nome del ristorante.
         *
         * @param v il nome o parte del nome da cercare
         * @return questa istanza del builder
         */
        public Builder nome(String v)             { f.nome             = v; return this; }

        /**
         * Imposta il filtro sulla città del ristorante.
         *
         * @param v la città in cui cercare
         * @return questa istanza del builder
         */
        public Builder citta(String v)            { f.citta            = v; return this; }

        /**
         * Imposta il filtro sulla nazione del ristorante.
         *
         * @param v la nazione in cui cercare
         * @return questa istanza del builder
         */
        public Builder nazione(String v)          { f.nazione          = v; return this; }

        /**
         * Imposta il filtro sul tipo di cucina del ristorante.
         *
         * @param v il tipo di cucina (es. Italiana, Cinese)
         * @return questa istanza del builder
         */
        public Builder cucina(String v)           { f.cucina           = v; return this; }

        /**
         * Imposta il filtro sui servizi offerti dal ristorante.
         *
         * @param v il servizio specifico richiesto (es. Parcheggio, Wi-Fi)
         * @return questa istanza del builder
         */
        public Builder servizio(String v)         { f.servizio         = v; return this; }

        /**
         * Imposta il filtro sulla fascia di prezzo del ristorante.
         *
         * @param v livello di prezzo intero (es. da 1 a 4)
         * @return questa istanza del builder
         */
        public Builder prezzoLivello(Integer v)   { f.prezzoLivello    = v; return this; }

        /**
         * Imposta il filtro sui riconoscimenti/premi del ristorante.
         *
         * @param v il nome del riconoscimento (es. Stella Michelin)
         * @return questa istanza del builder
         */
        public Builder riconoscimento(String v)   { f.riconoscimento   = v; return this; }

        /**
         * Filtra solo i ristoranti che offrono servizio di consegna a domicilio (delivery).
         *
         * @param v true per mostrare solo ristoranti con delivery, false altrimenti
         * @return questa istanza del builder
         */
        public Builder soloDelivery(boolean v)    { f.soloDelivery     = v; return this; }

        /**
         * Filtra solo i ristoranti che accettano prenotazioni online.
         *
         * @param v true per mostrare solo ristoranti prenotabili online, false altrimenti
         * @return questa istanza del builder
         */
        public Builder soloPrenotazione(boolean v){ f.soloPrenotazione = v; return this; }

        /**
         * Costruisce e restituisce l'oggetto {@link FiltriRicerca} configurato.
         *
         * @return l'istanza di {@link FiltriRicerca} completata
         */
        public FiltriRicerca build() { return f; }
    }

    /**
     * Restituisce il filtro sul nome del ristorante.
     *
     * @return il nome del ristorante filtrato o null se non impostato
     */
    public String  getNome()             { return nome; }

    /**
     * Restituisce il filtro sulla città.
     *
     * @return la città filtrata o null se non impostata
     */
    public String  getCitta()            { return citta; }

    /**
     * Restituisce il filtro sulla nazione.
     *
     * @return la nazione filtrata o null se non impostata
     */
    public String  getNazione()          { return nazione; }

    /**
     * Restituisce il filtro sul tipo di cucina.
     *
     * @return il tipo di cucina filtrato o null se non impostato
     */
    public String  getCucina()           { return cucina; }

    /**
     * Restituisce il filtro sui servizi.
     *
     * @return il servizio filtrato o null se non impostato
     */
    public String  getServizio()         { return servizio; }

    /**
     * Restituisce il filtro sul livello di prezzo.
     *
     * @return il livello di prezzo filtrato o null se non impostato
     */
    public Integer getPrezzoLivello()    { return prezzoLivello; }

    /**
     * Restituisce il filtro sui riconoscimenti.
     *
     * @return il riconoscimento filtrato o null se non impostato
     */
    public String  getRiconoscimento()   { return riconoscimento; }

    /**
     * Indica se la ricerca è limitata ai soli ristoranti con delivery.
     *
     * @return true se attiva la ricerca per solo delivery, false altrimenti
     */
    public boolean isSoloDelivery()      { return soloDelivery; }

    /**
     * Indica se la ricerca è limitata ai soli ristoranti che supportano prenotazioni online.
     *
     * @return true se attiva la ricerca per soli ristoranti prenotabili online, false altrimenti
     */
    public boolean isSoloPrenotazione()  { return soloPrenotazione; }
}
