package theknife.models;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Modello dati che rappresenta un utente iscritto alla piattaforma (Cliente o Ristoratore).
 * Fa parte dei modelli serializzabili per lo scambio dati client-server.
 * Contiene informazioni come le credenziali di accesso, i dati anagrafici (nome, cognome, data di nascita, domicilio)
 * e il ruolo associato all'utente.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public class Utente implements Serializable {

    private long   id;
    private String nome;
    private String cognome;
    private String username;
    private String passwordHash;
    private LocalDate dataNascita;
    private String domicilio;
    private String ruolo;

    /**
     * Costruttore completo che inizializza l'utente con tutte le sue proprietà, compreso l'ID del database.
     *
     * @param id l'ID univoco dell'utente
     * @param nome il nome dell'utente
     * @param cognome il cognome dell'utente
     * @param username lo username univoco dell'utente per l'accesso
     * @param passwordHash l'hash della password dell'utente
     * @param dataNascita la data di nascita dell'utente
     * @param domicilio la città o indirizzo di domicilio dell'utente
     * @param ruolo la stringa che identifica il ruolo dell'utente (es. "Cliente", "Ristoratore")
     */
    public Utente(long id, String nome, String cognome, String username,
                  String passwordHash, LocalDate dataNascita, String domicilio, String ruolo) {
        this.id           = id;
        this.nome         = nome;
        this.cognome      = cognome;
        this.username     = username;
        this.passwordHash = passwordHash;
        this.dataNascita  = dataNascita;
        this.domicilio    = domicilio;
        this.ruolo        = ruolo;
    }

    /**
     * Costruttore che inizializza un utente sprovvisto di ID (ID impostato a 0),
     * tipicamente usato in fase di registrazione o prima del salvataggio nel database.
     *
     * @param nome il nome dell'utente
     * @param cognome il cognome dell'utente
     * @param username lo username dell'utente
     * @param passwordHash l'hash della password
     * @param dataNascita la data di nascita dell'utente
     * @param domicilio la città/indirizzo di domicilio dell'utente
     * @param ruolo il ruolo dell'utente
     */
    public Utente(String nome, String cognome, String username,
                  String passwordHash, LocalDate dataNascita, String domicilio, String ruolo) {
        this(0, nome, cognome, username, passwordHash, dataNascita, domicilio, ruolo);
    }

    /**
     * Converte la stringa testuale del ruolo nel corrispondente valore enum {@link Role}.
     *
     * @return il valore enum corrispondente al ruolo dell'utente
     */
    public Role getRuoloEnum() {
        return Role.fromString(ruolo);
    }

    /**
     * Restituisce una rappresentazione sintetica dell'utente.
     *
     * @return descrizione testuale dell'utente
     */
    @Override
    public String toString() {
        return username + " (" + nome + " " + cognome + ") — " + ruolo;
    }

    /**
     * Restituisce l'ID dell'utente.
     *
     * @return l'ID
     */
    public long getId()                      { return id; }

    /**
     * Imposta l'ID dell'utente.
     *
     * @param id il nuovo ID
     */
    public void setId(long id)               { this.id = id; }

    /**
     * Restituisce il nome dell'utente.
     *
     * @return il nome
     */
    public String getNome()                  { return nome; }

    /**
     * Imposta il nome dell'utente.
     *
     * @param nome il nome
     */
    public void setNome(String nome)         { this.nome = nome; }

    /**
     * Restituisce il cognome dell'utente.
     *
     * @return il cognome
     */
    public String getCognome()               { return cognome; }

    /**
     * Imposta il cognome dell'utente.
     *
     * @param cognome il cognome
     */
    public void setCognome(String cognome)   { this.cognome = cognome; }

    /**
     * Restituisce lo username.
     *
     * @return lo username
     */
    public String getUsername()              { return username; }

    /**
     * Imposta lo username.
     *
     * @param username lo username
     */
    public void setUsername(String username) { this.username = username; }

    /**
     * Restituisce l'hash della password dell'utente.
     *
     * @return l'hash della password
     */
    public String getPasswordHash()                    { return passwordHash; }

    /**
     * Imposta l'hash della password dell'utente.
     *
     * @param passwordHash il nuovo hash password
     */
    public void setPasswordHash(String passwordHash)   { this.passwordHash = passwordHash; }

    /**
     * Restituisce la data di nascita dell'utente.
     *
     * @return la data di nascita
     */
    public LocalDate getDataNascita()                  { return dataNascita; }

    /**
     * Imposta la data di nascita dell'utente.
     *
     * @param dataNascita la data di nascita
     */
    public void setDataNascita(LocalDate dataNascita)  { this.dataNascita = dataNascita; }

    /**
     * Restituisce il domicilio dell'utente.
     *
     * @return il domicilio
     */
    public String getDomicilio()               { return domicilio; }

    /**
     * Imposta il domicilio dell'utente.
     *
     * @param domicilio il domicilio
     */
    public void setDomicilio(String domicilio) { this.domicilio = domicilio; }

    /**
     * Restituisce la stringa del ruolo dell'utente.
     *
     * @return la stringa ruolo
     */
    public String getRuolo()             { return ruolo; }

    /**
     * Imposta il ruolo dell'utente.
     *
     * @param ruolo la stringa ruolo
     */
    public void setRuolo(String ruolo)   { this.ruolo = ruolo; }
}
