package theknife.dao;

import theknife.models.Risposta;

import java.util.Optional;

/**
 * Interfaccia Data Access Object (pattern <b>DAO</b>) per la persistenza delle risposte alle recensioni.
 * Consente ai ristoratori di commentare i feedback lasciati dai clienti sui propri ristoranti.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public interface RispostaDAO {

    /**
     * Salva o aggiorna una risposta collegata a una recensione.
     * Verifica preliminarmente che il ristoratore che effettua l'operazione sia l'effettivo
     * proprietario del ristorante recensito.
     *
     * @param risposta       l'oggetto risposta da salvare
     * @param proprietarioId l'ID dell'utente ristoratore proprietario
     * @return l'oggetto {@link Risposta} persistito, con l'ID valorizzato
     * @throws ErroreApplicativo se l'utente non è autorizzato (non è proprietario del locale)
     *                            o in caso di violazione di vincoli
     */
    Risposta save(Risposta risposta, long proprietarioId);

    /**
     * Aggiorna il testo di una risposta esistente, verificando che il ristoratore
     * autenticato sia proprietario del ristorante recensito.
     *
     * @param risposta       la risposta con ID e testo aggiornato
     * @param proprietarioId l'ID dell'utente ristoratore proprietario
     * @return true se la risposta e stata aggiornata, false altrimenti
     */
    boolean update(Risposta risposta, long proprietarioId);

    /**
     * Elimina una risposta esistente, verificando che il ristoratore autenticato
     * sia proprietario del ristorante recensito.
     *
     * @param rispostaId     l'ID della risposta da eliminare
     * @param proprietarioId l'ID dell'utente ristoratore proprietario
     * @return true se la risposta e stata eliminata, false altrimenti
     */
    boolean delete(long rispostaId, long proprietarioId);

    /**
     * Recupera l'eventuale risposta associata a una determinata recensione.
     *
     * @param recensioneId l'identificativo univoco della recensione
     * @return un {@link Optional} contenente l'oggetto {@link Risposta} se presente, altrimenti vuoto
     * @throws ErroreApplicativo in caso di errore di interrogazione SQL
     */
    Optional<Risposta> findByRecensione(long recensioneId);
}
