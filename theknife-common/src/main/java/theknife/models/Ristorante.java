package theknife.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Modello dati che rappresenta un ristorante nel sistema.
 *
 * Contiene dettagli anagrafici, geografici, contatti, servizi e premi associati.
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

    public Ristorante() {}

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

    public int getStarCount() {
        if (riconoscimento == null) return 0;
        if (riconoscimento.startsWith("3")) return 3;
        if (riconoscimento.startsWith("2")) return 2;
        if (riconoscimento.startsWith("1")) return 1;
        return 0;
    }

    public boolean hasMichelinStar() {
        return getStarCount() > 0;
    }

    public String getPrezzoStringa() {
        return "€".repeat(Math.max(0, prezzoLivello));
    }

    @Override
    public String toString() {
        return nome + " — " + String.join(", ", cucine) + " (" + citta + ", " + nazione + ")";
    }

    // Getters e Setters
    public long getId()                        { return id; }
    public void setId(long id)                 { this.id = id; }

    public String getNome()                    { return nome; }
    public void setNome(String nome)           { this.nome = nome; }

    public String getIndirizzo()               { return indirizzo; }
    public void setIndirizzo(String indirizzo) { this.indirizzo = indirizzo; }

    public String getCitta()                   { return citta; }
    public void setCitta(String citta)         { this.citta = citta; }

    public String getNazione()                 { return nazione; }
    public void setNazione(String nazione)     { this.nazione = nazione; }

    public double getLatitudine()              { return latitudine; }
    public void setLatitudine(double lat)      { this.latitudine = lat; }

    public double getLongitudine()             { return longitudine; }
    public void setLongitudine(double lon)     { this.longitudine = lon; }

    public int getPrezzoLivello()                      { return prezzoLivello; }
    public void setPrezzoLivello(int prezzoLivello)    { this.prezzoLivello = prezzoLivello; }

    public String getTelefono()                { return telefono; }
    public void setTelefono(String telefono)   { this.telefono = telefono; }

    public String getUrl()                     { return url; }
    public void setUrl(String url)             { this.url = url; }

    public String getSitoWeb()                 { return sitoWeb; }
    public void setSitoWeb(String sitoWeb)     { this.sitoWeb = sitoWeb; }

    public String getRiconoscimento()                        { return riconoscimento; }
    public void setRiconoscimento(String riconoscimento)     { this.riconoscimento = riconoscimento; }

    public boolean isGreenStar()               { return greenStar; }
    public void setGreenStar(boolean greenStar){ this.greenStar = greenStar; }

    public String getDescrizione()                 { return descrizione; }
    public void setDescrizione(String descrizione) { this.descrizione = descrizione; }

    public boolean isDelivery()                { return delivery; }
    public void setDelivery(boolean delivery)  { this.delivery = delivery; }

    public boolean isPrenotazioneOnline()                        { return prenotazioneOnline; }
    public void setPrenotazioneOnline(boolean prenotazioneOnline){ this.prenotazioneOnline = prenotazioneOnline; }

    public long getProprietarioId()                      { return proprietarioId; }
    public void setProprietarioId(long proprietarioId)   { this.proprietarioId = proprietarioId; }

    public List<String> getCucine()                  { return cucine; }
    public void setCucine(List<String> cucine)       { this.cucine = cucine; }

    public List<String> getServizi()                 { return servizi; }
    public void setServizi(List<String> servizi)     { this.servizi = servizi; }
}
