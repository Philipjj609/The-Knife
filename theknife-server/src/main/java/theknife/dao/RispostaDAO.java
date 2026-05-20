package theknife.dao;

import theknife.models.Risposta;

import java.util.Optional;

/**
 * Interfaccia Data Access Object per la gestione delle risposte alle recensioni.
 *
 * Definisce le operazioni di inserimento e modifica delle risposte.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public interface RispostaDAO {

    Risposta save(Risposta risposta, long proprietarioId);

    Optional<Risposta> findByRecensione(long recensioneId);
}
