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

    Optional<Utente> findByUsername(String username);

    Optional<Utente> authenticate(String username, String password);

    boolean existsByUsername(String username);

    Utente save(Utente utente);
}
