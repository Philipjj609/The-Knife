package theknife.dao;

import theknife.models.FiltriRicerca;
import theknife.models.Ristorante;

import java.util.List;
import java.util.Optional;

/**
 * Interfaccia Data Access Object per la gestione dei ristoranti.
 *
 * Definisce le operazioni per la ricerca con filtri e l'inserimento di nuovi ristoranti.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public interface RistoranteDAO {

    List<Ristorante> findAll();

    Optional<Ristorante> findById(long id);

    List<Ristorante> search(FiltriRicerca filtri);

    List<Ristorante> findByProprietario(long proprietarioId);

    Ristorante save(Ristorante ristorante);

    boolean existsByNomeAndIndirizzo(String nome, String indirizzo);

    List<String> findAllServizi();

    List<String> findAllCucine();

    List<String> findAllCitta();

    List<String> findAllNazioni();
}
