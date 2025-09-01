package theknife.utils;

import java.io.*;
import java.util.*;

/*
 * @author Philip Jon Ji Ciuca
 * @numero_matricola 761446
 * @sede CO
 * @version: 1.0
 * */

import java.util.List;

import org.mindrot.jbcrypt.BCrypt;
import theknife.models.Utente;
import theknife.utils.FileManager;

/**
 * Utility per l'autenticazione degli utenti.
 * <p>
 * Fornisce metodi per verificare l'esistenza di username e per autenticare
 * un utente confrontando la password fornita con l'hash salvato.
 * </p>
 *
 * @author Philip Jon Ji Ciuca
 * @version 1.0
 */
public class AuthManager {
    /**
     * Verifica se uno username esiste già nel sistema.
     *
     * @param username Username da verificare, must be non-null.
     * @return true se lo username esiste già (case-insensitive), false altrimenti.
     * @since 1.0
     */
    public static boolean usernameEsistente(String username) {
        List<Utente> utenti = FileManager.caricaUtenti();
        return utenti.stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(username));
    }

    /**
     * Autentica un utente con username e password plaintext.
     *
     * @param username Username dell'utente.
     * @param password Password in chiaro da verificare contro l'hash salvato.
     * @return Oggetto Utente se l'autenticazione ha successo; null altrimenti.
     * @since 1.0
     */
    public static Utente autenticaUtente(String username, String password) {
        List<Utente> utenti = FileManager.caricaUtenti();

        for (Utente u : utenti) {
            boolean usernameMatch = u.getUsername().equalsIgnoreCase(username);
            if (usernameMatch) {
                boolean passwordMatch = BCrypt.checkpw(password, u.getPasswordHash());
                if (passwordMatch) {
                    return u;
                }
            }
        }
        return null;
    }
}
