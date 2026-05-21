package theknife.dao;

import theknife.models.Utente;

import java.util.Optional;

/**
 * Interfaccia Data Access Object per la persistenza degli utenti.
 *
 * Definisce le operazioni per l'autenticazione, la registrazione e la verifica di esistenza.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public interface UtenteDAO {

    /**
     * Trova un utente in base al suo username.
     *
     * @param username lo username dell'utente da cercare
     * @return un {@link Optional} contenente l'utente se trovato, altrimenti vuoto
     * @throws RuntimeException se si verifica un errore durante la query al database
     */
    Optional<Utente> findByUsername(String username);

    /**
     * Autentica un utente verificando le sue credenziali (username e password).
     *
     * @param username lo username dell'utente
     * @param password la password in chiaro da verificare
     * @return un {@link Optional} contenente l'utente autenticato se le credenziali sono corrette, altrimenti vuoto
     * @throws RuntimeException se si verifica un errore durante il recupero dei dati dell'utente
     */
    Optional<Utente> authenticate(String username, String password);

    /**
     * Verifica se esiste già un utente registrato con lo username specificato (case-insensitive).
     *
     * @param username lo username da controllare
     * @return true se lo username è già registrato, false altrimenti
     * @throws RuntimeException se si verifica un errore nel database
     */
    boolean existsByUsername(String username);

    /**
     * Registra un nuovo utente nel database, assegnandogli un ID autogenerato.
     *
     * @param utente l'oggetto {@link Utente} da salvare con i suoi dettagli
     * @return l'utente salvato, completo di ID assegnato dal database
     * @throws RuntimeException se si verifica un errore durante l'inserimento nel database
     */
    Utente save(Utente utente);
}
