package theknife.utils;

import java.util.List;

/*
 * @author Philip Jon Ji Ciuca
 * @numero_matricola 761446
 * @sede CO
 * @version: 1.0
 * */

import org.mindrot.jbcrypt.BCrypt;
import theknife.models.Utente;

/**
 * Utility per l'autenticazione degli utenti.
 * <p>
 * Mantiene una cache in memoria degli utenti caricata al primo accesso.
 * Chiamare {@link #invalidateCache()} dopo ogni scrittura su utenti.csv
 * per garantire che i dati siano aggiornati.
 * </p>
 *
 * @author Philip Jon Ji Ciuca
 * @version 1.0
 */
public class AuthManager {

    private static List<Utente> cachedUtenti = null;

    private static List<Utente> getUtenti() {
        if (cachedUtenti == null) {
            cachedUtenti = FileManager.caricaUtenti();
        }
        return cachedUtenti;
    }

    /**
     * Invalida la cache degli utenti. Deve essere chiamato dopo ogni salvataggio
     * di un nuovo utente tramite {@link FileManager#salvaUtente(Utente)}.
     *
     * @since 1.0
     */
    public static void invalidateCache() {
        cachedUtenti = null;
    }

    /**
     * Verifica se uno username esiste già nel sistema.
     *
     * @param username Username da verificare, must be non-null.
     * @return true se lo username esiste già (case-insensitive), false altrimenti.
     * @since 1.0
     */
    public static boolean usernameEsistente(String username) {
        return getUtenti().stream().anyMatch(u -> u.getUsername().equalsIgnoreCase(username));
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
        for (Utente u : getUtenti()) {
            if (u.getUsername().equalsIgnoreCase(username)) {
                if (BCrypt.checkpw(password, u.getPasswordHash())) {
                    return u;
                }
            }
        }
        return null;
    }
}
