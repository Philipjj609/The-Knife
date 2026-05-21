package theknife.dao;

import theknife.models.Recensione;

import java.util.List;
import java.util.Optional;

/**
 * Interfaccia Data Access Object (pattern <b>DAO</b>) per la gestione della persistenza delle recensioni.
 * Definisce i metodi necessari per la creazione, consultazione, modifica, eliminazione e aggregazione
 * dei giudizi rilasciati dagli utenti di tipo cliente sui vari ristoranti.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public interface RecensioneDAO {

    /**
     * Recupera una singola recensione tramite il suo identificativo univoco.
     *
     * @param id l'identificativo univoco della recensione
     * @return un {@link Optional} contenente la recensione trovata, o vuoto se non esiste
     * @throws ErroreApplicativo in caso di errore di lettura SQL
     */
    Optional<Recensione> findById(long id);

    /**
     * Recupera la lista di tutte le recensioni rilasciate per un determinato ristorante.
     *
     * @param ristoranteId l'identificativo univoco del ristorante
     * @return la lista di oggetti {@link Recensione} associati al ristorante
     * @throws ErroreApplicativo in caso di fallimento SQL
     */
    List<Recensione> findByRistorante(long ristoranteId);

    /**
     * Recupera la lista di tutte le recensioni scritte da un determinato cliente.
     *
     * @param username lo username del cliente
     * @return la lista di oggetti {@link Recensione} scritti dall'utente
     * @throws ErroreApplicativo in caso di fallimento SQL
     */
    List<Recensione> findByCliente(String username);

    /**
     * Recupera l'elenco di tutte le recensioni associate ad una lista di ristoranti.
     * Utilizzato tipicamente dai ristoratori per monitorare le recensioni dei propri locali.
     *
     * @param ristoranteIds la lista degli identificativi dei ristoranti
     * @return la lista di oggetti {@link Recensione} dei ristoranti specificati
     * @throws ErroreApplicativo in caso di errore nella query SQL IN
     */
    List<Recensione> findByRistoranteIds(List<Long> ristoranteIds);

    /**
     * Salva una nuova recensione nel sistema, registrandola nel database.
     *
     * @param recensione l'oggetto recensione da persistere
     * @return la recensione salvata completata con l'identificativo autogenerato dal database
     * @throws ErroreApplicativo in caso di recensione duplicata (vincolo univoco utente-ristorante)
     *                            o altri errori SQL
     */
    Recensione save(Recensione recensione);

    /**
     * Modifica il testo ed il voto di una recensione già esistente.
     *
     * @param recensione l'oggetto recensione contenente i nuovi valori da aggiornare
     * @return true se l'aggiornamento è avvenuto con successo, false altrimenti
     * @throws ErroreApplicativo in caso di vincoli violati o fallimento query SQL
     */
    boolean update(Recensione recensione);

    /**
     * Elimina una recensione specifica dal database, verificando preventivamente
     * la proprietà del cliente per evitare eliminazioni non autorizzate.
     *
     * @param id               l'identificativo della recensione da cancellare
     * @param usernameCliente lo username del cliente proprietario della recensione
     * @return true se l'eliminazione ha avuto successo, false altrimenti
     * @throws ErroreApplicativo in caso di errore SQL
     */
    boolean delete(long id, String usernameCliente);

    /**
     * Calcola la media aritmetica arrotondata delle valutazioni (stelle) ricevute da un ristorante.
     *
     * @param ristoranteId l'identificativo del ristorante
     * @return la media delle valutazioni come double (0.0 se non ci sono ancora recensioni)
     * @throws ErroreApplicativo in caso di fallimento SQL
     */
    double getMediaValutazioni(long ristoranteId);
}
