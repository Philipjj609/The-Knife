package theknife.dao;

import theknife.models.Utente;

import java.util.Optional;

public interface UtenteDAO {

    Optional<Utente> findByUsername(String username);

    Optional<Utente> authenticate(String username, String password);

    boolean existsByUsername(String username);

    Utente save(Utente utente);
}
