package theknife.dao;

import theknife.models.Ristorante;

import java.util.List;

/**
 * Interfaccia Data Access Object (pattern <b>DAO</b>) per la gestione delle preferenze dei clienti.
 * Gestisce l'associazione molti-a-molti tra gli utenti registrati ed i loro ristoranti preferiti.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public interface PreferitiDAO {

    /**
     * Aggiunge un ristorante alla lista dei preferiti di un utente.
     *
     * @param username     lo username dell'utente che vuole salvare il ristorante
     * @param ristoranteId l'identificativo univoco del ristorante da aggiungere
     * @throws ErroreApplicativo se l'utente ha già salvato il ristorante o in caso di anomalie SQL
     */
    void add(String username, long ristoranteId);

    /**
     * Rimuove un ristorante dalla lista dei preferiti dell'utente.
     *
     * @param username     lo username dell'utente
     * @param ristoranteId l'identificativo univoco del ristorante da rimuovere
     * @throws ErroreApplicativo in caso di anomalie SQL o se l'associazione non esiste
     */
    void remove(String username, long ristoranteId);

    /**
     * Verifica se un determinato ristorante è salvato tra i preferiti di un utente.
     *
     * @param username     lo username dell'utente
     * @param ristoranteId l'identificativo univoco del ristorante
     * @return true se il ristorante è salvato tra i preferiti, false altrimenti
     * @throws ErroreApplicativo in caso di anomalie di connessione o esecuzione SQL
     */
    boolean isPreferito(String username, long ristoranteId);

    /**
     * Recupera l'elenco completo di tutti i ristoranti contrassegnati come preferiti
     * da un utente specifico.
     *
     * @param username lo username dell'utente di cui caricare i preferiti
     * @return la lista di oggetti {@link Ristorante} preferiti dell'utente
     * @throws ErroreApplicativo in caso di fallimento della query SQL
     */
    List<Ristorante> findByUtente(String username);
}
