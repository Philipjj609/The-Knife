package theknife.dao;

import theknife.models.Risposta;

import java.util.Optional;

public interface RispostaDAO {

    Risposta save(Risposta risposta);

    Optional<Risposta> findByRecensione(long recensioneId);
}
