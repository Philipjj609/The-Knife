package theknife.models;

/*
 * @author Philip Jon Ji Ciuca
 * @numero_matricola 761446
 * @sede CO
 * @version: 1.0
 * */

/**
 * Rappresenta un utente del sistema The Knife.
 * <p>
 * Contiene i dati anagrafici e le credenziali (hash della password) e
 * il ruolo dell'utente (ad esempio "Cliente" o "Ristoratore").
 * </p>
 *
 * @author Philip Jon Ji Ciuca
 * @version 1.0
 */
public class Utente {
    private String nome;
    private String cognome;
    private String username;
    private String passwordHash;
    private String dataNascita; // Formato ISO (YYYY-MM-DD)
    private String domicilio;
    private String ruolo; // "Cliente" o "Ristoratore"

    /**
     * Crea un nuovo oggetto Utente con i dati forniti.
     *
     * @param nome Nome dell'utente.
     * @param cognome Cognome dell'utente.
     * @param username Username unico dell'utente.
     * @param passwordHash Hash della password (non la password in chiaro).
     * @param dataNascita Data di nascita in formato ISO (YYYY-MM-DD) o stringa vuota.
     * @param domicilio Indirizzo di domicilio.
     * @param ruolo Ruolo dell'utente, ad esempio "Cliente" o "Ristoratore".
     * @since 1.0
     */
    public Utente(String nome, String cognome, String username, String passwordHash,
            String dataNascita, String domicilio, String ruolo) {
        this.nome = nome;
        this.cognome = cognome;
        this.username = username;
        this.passwordHash = passwordHash;
        this.dataNascita = dataNascita;
        this.domicilio = domicilio;
        this.ruolo = ruolo;
    }

    /**
     * Restituisce il nome dell'utente.
     * @return Nome dell'utente.
     * @since 1.0
     */
    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    /**
     * Restituisce il cognome dell'utente.
     * @return Cognome dell'utente.
     * @since 1.0
     */
    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    /**
     * Restituisce lo username dell'utente.
     * @return Username dell'utente.
     * @since 1.0
     */
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Restituisce l'hash della password dell'utente.
     * @return Hash della password.
     * @since 1.0
     */
    public String getPassword() {
        return passwordHash;
    }

    public void setPassword(String password) {
        this.passwordHash = password;
    }

    /**
     * Restituisce l'hash della password dell'utente.
     * @return Hash della password.
     * @since 1.0
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Restituisce la data di nascita dell'utente.
     * @return Data di nascita in formato ISO (YYYY-MM-DD) o stringa vuota.
     * @since 1.0
     */
    public String getDataNascita() {
        return dataNascita;
    }

    public void setDataNascita(String dataNascita) {
        this.dataNascita = dataNascita;
    }

    /**
     * Restituisce l'indirizzo di domicilio dell'utente.
     * @return Indirizzo di domicilio.
     * @since 1.0
     */
    public String getDomicilio() {
        return domicilio;
    }

    public void setDomicilio(String domicilio) {
        this.domicilio = domicilio;
    }

    /**
     * Restituisce il ruolo dell'utente.
     * @return Ruolo dell'utente, ad esempio "Cliente" o "Ristoratore".
     * @since 1.0
     */
    public String getRuolo() {
        return ruolo;
    }

    public void setRuolo(String ruolo) {
        this.ruolo = ruolo;
    }

    @Override
    public String toString() {
        return "Utente{" +
                "nome='" + nome + '\'' +
                ", cognome='" + cognome + '\'' +
                ", username='" + username + '\'' +
                ", passwordHash='" + passwordHash + '\'' +
                ", dataNascita='" + dataNascita + '\'' +
                ", domicilio='" + domicilio + '\'' +
                ", ruolo='" + ruolo + '\'' +
                '}';
    }
}