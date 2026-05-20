package theknife.dao;

import theknife.models.Risposta;

import java.util.Optional;

public interface RispostaDAO {

    Risposta save(Risposta risposta, long proprietarioId);

    Optional<Risposta> findByRecensione(long recensioneId);
}
