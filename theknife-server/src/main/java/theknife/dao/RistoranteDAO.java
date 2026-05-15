package theknife.dao;

import theknife.models.FiltriRicerca;
import theknife.models.Ristorante;

import java.util.List;
import java.util.Optional;

public interface RistoranteDAO {

    List<Ristorante> findAll();

    Optional<Ristorante> findById(long id);

    List<Ristorante> search(FiltriRicerca filtri);

    List<Ristorante> findByProprietario(long proprietarioId);

    Ristorante save(Ristorante ristorante);

    boolean existsByNomeAndIndirizzo(String nome, String indirizzo);
}
