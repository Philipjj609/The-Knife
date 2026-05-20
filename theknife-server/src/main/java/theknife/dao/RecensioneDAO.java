package theknife.dao;

import theknife.models.Recensione;

import java.util.List;
import java.util.Optional;

/**
 * Interfaccia Data Access Object per la persistenza delle recensioni.
 *
 * Definisce i metodi per il recupero, l'inserimento, la modifica e la rimozione.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public interface RecensioneDAO {

    Optional<Recensione> findById(long id);

    List<Recensione> findByRistorante(long ristoranteId);

    List<Recensione> findByCliente(String username);

    List<Recensione> findByRistoranteIds(List<Long> ristoranteIds);

    Recensione save(Recensione recensione);

    boolean update(Recensione recensione);

    boolean delete(long id, String usernameCliente);

    double getMediaValutazioni(long ristoranteId);
}
