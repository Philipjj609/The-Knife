package theknife.models;

import java.io.Serializable;
import java.time.LocalDate;

public class Utente implements Serializable {

    private long   id;
    private String nome;
    private String cognome;
    private String username;
    private String passwordHash;
    private LocalDate dataNascita;
    private String domicilio;
    private String ruolo;

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

    public Utente(String nome, String cognome, String username,
                  String passwordHash, LocalDate dataNascita, String domicilio, String ruolo) {
        this(0, nome, cognome, username, passwordHash, dataNascita, domicilio, ruolo);
    }

    public Role getRuoloEnum() {
        return Role.fromString(ruolo);
    }

    @Override
    public String toString() {
        return username + " (" + nome + " " + cognome + ") — " + ruolo;
    }

    // Getters e Setters
    public long getId()                      { return id; }
    public void setId(long id)               { this.id = id; }

    public String getNome()                  { return nome; }
    public void setNome(String nome)         { this.nome = nome; }

    public String getCognome()               { return cognome; }
    public void setCognome(String cognome)   { this.cognome = cognome; }

    public String getUsername()              { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash()                    { return passwordHash; }
    public void setPasswordHash(String passwordHash)   { this.passwordHash = passwordHash; }

    public LocalDate getDataNascita()                  { return dataNascita; }
    public void setDataNascita(LocalDate dataNascita)  { this.dataNascita = dataNascita; }

    public String getDomicilio()               { return domicilio; }
    public void setDomicilio(String domicilio) { this.domicilio = domicilio; }

    public String getRuolo()             { return ruolo; }
    public void setRuolo(String ruolo)   { this.ruolo = ruolo; }
}
