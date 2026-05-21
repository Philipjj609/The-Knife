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

    /**
     * Recupera tutti i ristoranti presenti nel database, ordinati per nome.
     *
     * @return una lista di tutti i {@link Ristorante} registrati
     * @throws RuntimeException se si verifica un errore durante l'accesso al database
     */
    List<Ristorante> findAll();

    /**
     * Trova un ristorante specifico in base al suo identificatore univoco.
     *
     * @param id l'id del ristorante da cercare
     * @return un {@link Optional} contenente il ristorante se trovato, altrimenti vuoto
     * @throws RuntimeException se si verifica un errore durante la query al database
     */
    Optional<Ristorante> findById(long id);

    /**
     * Cerca i ristoranti che corrispondono ai filtri di ricerca forniti.
     * I filtri includono nome/parola chiave, città, nazione, cucina, servizi offerti,
     * livello di prezzo, riconoscimento Michelin, delivery e prenotazione online.
     *
     * @param filtri l'oggetto {@link FiltriRicerca} contenente i criteri di ricerca
     * @return una lista di {@link Ristorante} che soddisfano tutti i filtri specificati
     * @throws RuntimeException se si verifica un errore durante l'esecuzione della ricerca nel database
     */
    List<Ristorante> search(FiltriRicerca filtri);

    /**
     * Trova tutti i ristoranti associati a uno specifico proprietario (ristoratore).
     *
     * @param proprietarioId l'id dell'utente proprietario
     * @return la lista di {@link Ristorante} posseduti dal ristoratore
     * @throws RuntimeException se si verifica un errore durante l'interrogazione del database
     */
    List<Ristorante> findByProprietario(long proprietarioId);

    /**
     * Salva un nuovo ristorante nel database (con le relative relazioni con cucine e servizi),
     * assegnandogli un ID autogenerato.
     *
     * @param ristorante il {@link Ristorante} da salvare
     * @return il ristorante salvato, completo di ID autogenerato
     * @throws RuntimeException se si verifica un errore durante il salvataggio o il commit transazionale
     */
    Ristorante save(Ristorante ristorante);

    /**
     * Verifica se esiste già un ristorante con lo stesso nome e indirizzo (case-insensitive).
     * Utile per evitare inserimenti duplicati dello stesso locale.
     *
     * @param nome il nome del ristorante
     * @param indirizzo l'indirizzo del ristorante
     * @return true se il ristorante esiste già, false altrimenti
     * @throws RuntimeException se si verifica un errore nel database
     */
    boolean existsByNomeAndIndirizzo(String nome, String indirizzo);

    /**
     * Recupera tutti i nomi dei servizi (es. parcheggio, Wi-Fi) disponibili nel database.
     *
     * @return una lista di stringhe con i nomi dei servizi
     * @throws RuntimeException se si verifica un errore nel database
     */
    List<String> findAllServizi();

    /**
     * Recupera tutti i nomi dei tipi di cucina (es. italiana, sushi) registrati nel database.
     *
     * @return una lista di stringhe con i nomi dei tipi di cucina
     * @throws RuntimeException se si verifica un errore nel database
     */
    List<String> findAllCucine();

    /**
     * Recupera tutte le città distinte in cui è presente almeno un ristorante.
     *
     * @return una lista di stringhe con i nomi delle città
     * @throws RuntimeException se si verifica un errore nel database
     */
    List<String> findAllCitta();

    /**
     * Recupera tutte le nazioni distinte in cui è presente almeno un ristorante.
     *
     * @return una lista di stringhe con i nomi delle nazioni
     * @throws RuntimeException se si verifica un errore nel database
     */
    List<String> findAllNazioni();
}
