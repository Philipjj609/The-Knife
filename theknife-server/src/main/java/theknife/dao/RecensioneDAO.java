package theknife.dao;

import theknife.models.Recensione;

import java.util.List;
import java.util.Optional;

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
