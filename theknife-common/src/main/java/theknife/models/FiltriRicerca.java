package theknife.models;

import java.io.Serializable;

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

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final FiltriRicerca f = new FiltriRicerca();

        public Builder nome(String v)             { f.nome             = v; return this; }
        public Builder citta(String v)            { f.citta            = v; return this; }
        public Builder nazione(String v)          { f.nazione          = v; return this; }
        public Builder cucina(String v)           { f.cucina           = v; return this; }
        public Builder servizio(String v)         { f.servizio         = v; return this; }
        public Builder prezzoLivello(Integer v)   { f.prezzoLivello    = v; return this; }
        public Builder riconoscimento(String v)   { f.riconoscimento   = v; return this; }
        public Builder soloDelivery(boolean v)    { f.soloDelivery     = v; return this; }
        public Builder soloPrenotazione(boolean v){ f.soloPrenotazione = v; return this; }

        public FiltriRicerca build() { return f; }
    }

    public String  getNome()             { return nome; }
    public String  getCitta()            { return citta; }
    public String  getNazione()          { return nazione; }
    public String  getCucina()           { return cucina; }
    public String  getServizio()         { return servizio; }
    public Integer getPrezzoLivello()    { return prezzoLivello; }
    public String  getRiconoscimento()   { return riconoscimento; }
    public boolean isSoloDelivery()      { return soloDelivery; }
    public boolean isSoloPrenotazione()  { return soloPrenotazione; }
}
