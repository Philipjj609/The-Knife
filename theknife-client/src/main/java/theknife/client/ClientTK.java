package theknife.client;

import theknife.models.FiltriRicerca;
import theknife.models.*;
import theknife.network.Comando;
import theknife.network.Esito;
import theknife.network.Richiesta;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Facade di rete per la GUI.
 *
 * Mantiene una connessione TCP persistente con il ServerTK.
 * Ogni metodo pubblico:
 *   1. costruisce una Richiesta con il Comando e i parametri necessari
 *   2. la invia al server (sincronizzata per thread-safety)
 *   3. deserializza l'Esito e restituisce il dato oppure lancia RuntimeException
 *
 * I controller JavaFX devono chiamare questi metodi SEMPRE da un thread
 * separato (es. javafx.concurrent.Task) per non bloccare l'UI.
 *
 * Utilizzo:
 *   ClientTK client = new ClientTK("localhost", 9090);
 *   // ... uso nei controller ...
 *   client.close();
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */

public class ClientTK implements Closeable {

    private final Socket             socket;
    private final ObjectOutputStream oos;
    private final ObjectInputStream  ois;

    /**
     * Inizializza il client di rete aprendo una connessione socket verso l'host e la porta specificati.
     * Crea i flussi di input/output Object Streams.
     *
     * @param host l'indirizzo IP o l'hostname del server
     * @param port la porta di ascolto del server
     * @throws IOException se si verifica un errore durante l'apertura della connessione o la creazione degli stream
     */
    public ClientTK(String host, int port) throws IOException {
        socket = new Socket(host, port);
        // OOS PRIMA di OIS — entrambi i lati creano OOS prima di OIS
        // per evitare il deadlock sulla lettura dell'header di serializzazione
        oos = new ObjectOutputStream(socket.getOutputStream());
        oos.flush();
        ois = new ObjectInputStream(socket.getInputStream());
    }

    // -------------------------------------------------------------------------
    // Autenticazione e utenti
    // -------------------------------------------------------------------------

    /**
     * Esegue il login dell'utente inviando le credenziali al server.
     * Questo metodo è bloccante per la rete; deve essere eseguito al di fuori del thread della UI.
     *
     * @param username lo username dell'utente
     * @param password la password in chiaro da verificare
     * @return un {@link Optional} contenente l'utente se l'autenticazione ha successo, altrimenti vuoto
     * @throws RuntimeException se si verifica un errore di rete
     */
    public Optional<Utente> login(String username, String password) {
        Esito esito = invia(new Richiesta(Comando.LOGIN,
                Map.of("username", username, "password", password)));
        return esito.isSuccesso() ? Optional.of(esito.getDato()) : Optional.empty();
    }

    /**
     * Invia una richiesta di registrazione per un nuovo utente.
     * Questo metodo è bloccante per la rete; deve essere eseguito al di fuori del thread della UI.
     *
     * @param utente l'oggetto {@link Utente} da registrare
     * @param password la password in chiaro associata all'utente
     * @return l'utente appena registrato provvisto dell'ID assegnato dal database
     * @throws RuntimeException se si verifica un errore di rete o se la registrazione fallisce
     */
    public Utente registraUtente(Utente utente, String password) {
        return richiediDato(new Richiesta(Comando.REGISTRA_UTENTE,
                Map.of("utente", utente, "password", password)));
    }

    /**
     * Verifica la disponibilità di uno username sul server.
     * Questo metodo è bloccante per la rete; deve essere eseguito al di fuori del thread della UI.
     *
     * @param username lo username da controllare
     * @return true se lo username esiste già, false altrimenti
     * @throws RuntimeException se si verifica un errore di rete
     */
    public boolean usernameEsiste(String username) {
        return richiediDato(new Richiesta(Comando.USERNAME_ESISTE,
                Map.of("username", username)));
    }

    /**
     * Modifica i dati anagrafici dell'utente registrato.
     * Questo metodo è bloccante per la rete; deve essere eseguito al di fuori del thread della UI.
     *
     * @param utente l'utente con i dati aggiornati
     * @return l'utente aggiornato dal database
     * @throws RuntimeException se si verifica un errore di rete o se l'operazione fallisce
     */
    public Utente modificaUtente(Utente utente) {
        return richiediDato(new Richiesta(Comando.MODIFICA_UTENTE,
                Map.of("utente", utente)));
    }

    // -------------------------------------------------------------------------
    // Ristoranti
    // -------------------------------------------------------------------------

    /**
     * Richiede al server l'elenco di tutti i servizi registrati nel database.
     * Questo metodo è bloccante per la rete; deve essere eseguito al di fuori del thread della UI.
     *
     * @return la lista dei servizi disponibili
     * @throws RuntimeException se si verifica un errore di rete
     */
    public List<String> getServizi() {
        return richiediDato(new Richiesta(Comando.GET_SERVIZI, Map.of()));
    }

    /**
     * Richiede al server l'elenco di tutti i tipi di cucina disponibili.
     * Questo metodo è bloccante per la rete; deve essere eseguito al di fuori del thread della UI.
     *
     * @return la lista delle cucine registrate
     * @throws RuntimeException se si verifica un errore di rete
     */
    public List<String> getCucine() {
        return richiediDato(new Richiesta(Comando.GET_CUCINE, Map.of()));
    }

    /**
     * Richiede l'elenco di tutte le città in cui sono presenti ristoranti.
     * Questo metodo è bloccante per la rete; deve essere eseguito al di fuori del thread della UI.
     *
     * @return la lista delle città
     * @throws RuntimeException se si verifica un errore di rete
     */
    public List<String> getCitta() {
        return richiediDato(new Richiesta(Comando.GET_CITTA, Map.of()));
    }

    /**
     * Richiede l'elenco di tutte le nazioni in cui sono presenti ristoranti.
     * Questo metodo è bloccante per la rete; deve essere eseguito al di fuori del thread della UI.
     *
     * @return la lista delle nazioni
     * @throws RuntimeException se si verifica un errore di rete
     */
    public List<String> getNazioni() {
        return richiediDato(new Richiesta(Comando.GET_NAZIONI, Map.of()));
    }

    /**
     * Effettua una ricerca di ristoranti basata su filtri multipli.
     * Questo metodo è bloccante per la rete; deve essere eseguito al di fuori del thread della UI.
     *
     * @param filtri i criteri di ricerca (se nullo, viene inviata una ricerca vuota)
     * @return l'elenco dei ristoranti filtrati
     * @throws RuntimeException se si verifica un errore di rete
     */
    public List<Ristorante> cercaRistoranti(FiltriRicerca filtri) {
        FiltriRicerca safeFiltri = (filtri != null) ? filtri : FiltriRicerca.builder().build();
        return richiediDato(new Richiesta(Comando.CERCA_RISTORANTI,
                Map.of("filtri", safeFiltri)));
    }

    /**
     * Recupera un ristorante dal server a partire dall'ID.
     * Questo metodo è bloccante per la rete; deve essere eseguito al di fuori del thread della UI.
     *
     * @param id l'id del ristorante da cercare
     * @return un {@link Optional} con il ristorante trovato, altrimenti vuoto
     * @throws RuntimeException se si verifica un errore di rete
     */
    public Optional<Ristorante> getRistorante(long id) {
        Esito e = invia(new Richiesta(Comando.GET_RISTORANTE, Map.of("id", id)));
        return e.isSuccesso() ? Optional.of(e.getDato()) : Optional.empty();
    }

    /**
     * Invia un nuovo ristorante da inserire nel sistema.
     * Questo metodo è bloccante per la rete; deve essere eseguito al di fuori del thread della UI.
     *
     * @param ristorante il ristorante da aggiungere
     * @return il ristorante salvato con l'ID autogenerato
     * @throws RuntimeException se si verifica un errore di rete o se la registrazione del ristorante fallisce
     */
    public Ristorante aggiungiRistorante(Ristorante ristorante) {
        return richiediDato(new Richiesta(Comando.AGGIUNGI_RISTORANTE,
                Map.of("ristorante", ristorante)));
    }

    /**
     * Recupera tutti i ristoranti associati ad un certo proprietario.
     * Questo metodo è bloccante per la rete; deve essere eseguito al di fuori del thread della UI.
     *
     * @param proprietarioId l'id dell'utente ristoratore
     * @return l'elenco dei ristoranti posseduti dal proprietario
     * @throws RuntimeException se si verifica un errore di rete
     */
    public List<Ristorante> getRistorantiProprietario(long proprietarioId) {
        return richiediDato(new Richiesta(Comando.GET_RISTORANTI_PROPRIETARIO,
                Map.of("proprietarioId", proprietarioId)));
    }

    // -------------------------------------------------------------------------
    // Recensioni
    // -------------------------------------------------------------------------

    /**
     * Recupera tutte le recensioni per un determinato ristorante.
     * Questo metodo è bloccante per la rete; deve essere eseguito al di fuori del thread della UI.
     *
     * @param ristoranteId l'id del ristorante
     * @return l'elenco delle recensioni per quel ristorante
     * @throws RuntimeException se si verifica un errore di rete
     */
    public List<Recensione> getRecensioniRistorante(long ristoranteId) {
        return richiediDato(new Richiesta(Comando.GET_RECENSIONI_RISTORANTE,
                Map.of("ristoranteId", ristoranteId)));
    }

    /**
     * Recupera tutte le recensioni inserite da uno specifico cliente.
     * Questo metodo è bloccante per la rete; deve essere eseguito al di fuori del thread della UI.
     *
     * @param username lo username del cliente
     * @return la lista delle recensioni del cliente
     * @throws RuntimeException se si verifica un errore di rete
     */
    public List<Recensione> getRecensioniCliente(String username) {
        return richiediDato(new Richiesta(Comando.GET_RECENSIONI_CLIENTE,
                Map.of("username", username)));
    }

    /**
     * Recupera le recensioni destinate ai ristoranti gestiti da un insieme di ID.
     * Questo metodo è bloccante per la rete; deve essere eseguito al di fuori del thread della UI.
     *
     * @param ristoranteIds gli identificativi dei ristoranti gestiti dal ristoratore
     * @return la lista delle recensioni ricevute per tali ristoranti
     * @throws RuntimeException se si verifica un errore di rete
     */
    public List<Recensione> getRecensioniRistoratori(List<Long> ristoranteIds) {
        return richiediDato(new Richiesta(Comando.GET_RECENSIONI_RISTORATORI,
                Map.of("ristoranteIds", ristoranteIds)));
    }

    /**
     * Inserisce una nuova recensione nel sistema.
     * Questo metodo è bloccante per la rete; deve essere eseguito al di fuori del thread della UI.
     *
     * @param recensione l'oggetto recensione da salvare
     * @return la recensione salvata con l'ID autogenerato
     * @throws RuntimeException se si verifica un errore di rete
     */
    public Recensione aggiungiRecensione(Recensione recensione) {
        return richiediDato(new Richiesta(Comando.AGGIUNGI_RECENSIONE,
                Map.of("recensione", recensione)));
    }

    /**
     * Modifica il testo o il voto di una recensione esistente.
     * Questo metodo è bloccante per la rete; deve essere eseguito al di fuori del thread della UI.
     *
     * @param recensione la recensione aggiornata
     * @return true se la modifica è avvenuta con successo, false altrimenti
     * @throws RuntimeException se si verifica un errore di rete
     */
    public boolean modificaRecensione(Recensione recensione) {
        return richiediDato(new Richiesta(Comando.MODIFICA_RECENSIONE,
                Map.of("recensione", recensione)));
    }

    /**
     * Elimina una recensione in base al suo ID.
     * Questo metodo è bloccante per la rete; deve essere eseguito al di fuori del thread della UI.
     *
     * @param id l'id della recensione
     * @param username lo username dell'utente per verifica permessi sul server
     * @return true se l'eliminazione è avvenuta con successo, false altrimenti
     * @throws RuntimeException se si verifica un errore di rete
     */
    public boolean eliminaRecensione(long id, String username) {
        return richiediDato(new Richiesta(Comando.ELIMINA_RECENSIONE,
                Map.of("id", id, "username", username)));
    }

    /**
     * Recupera la media aritmetica delle valutazioni di un ristorante.
     * Questo metodo è bloccante per la rete; deve essere eseguito al di fuori del thread della UI.
     *
     * @param ristoranteId l'id del ristorante
     * @return il valore medio delle valutazioni
     * @throws RuntimeException se si verifica un errore di rete
     */
    public double getMediaValutazioni(long ristoranteId) {
        return richiediDato(new Richiesta(Comando.GET_MEDIA_VALUTAZIONI,
                Map.of("ristoranteId", ristoranteId)));
    }

    // -------------------------------------------------------------------------
    // Risposte alle recensioni
    // -------------------------------------------------------------------------

    /**
     * Invia la risposta di un ristoratore ad una recensione esistente.
     * Questo metodo è bloccante per la rete; deve essere eseguito al di fuori del thread della UI.
     *
     * @param risposta l'oggetto risposta da inserire
     * @return la risposta salvata con l'ID autogenerato
     * @throws RuntimeException se si verifica un errore di rete
     */
    public Risposta rispondiRecensione(Risposta risposta) {
        return richiediDato(new Richiesta(Comando.RISPONDI_RECENSIONE,
                Map.of("risposta", risposta)));
    }

    /**
     * Modifica una risposta esistente del ristoratore autenticato.
     * Questo metodo e bloccante per la rete; deve essere eseguito al di fuori del thread della UI.
     *
     * @param risposta la risposta con ID e testo aggiornato
     * @return true se la modifica e avvenuta con successo, false altrimenti
     * @throws RuntimeException se si verifica un errore di rete
     */
    public boolean modificaRisposta(Risposta risposta) {
        return richiediDato(new Richiesta(Comando.MODIFICA_RISPOSTA,
                Map.of("risposta", risposta)));
    }

    /**
     * Elimina una risposta esistente del ristoratore autenticato.
     * Questo metodo e bloccante per la rete; deve essere eseguito al di fuori del thread della UI.
     *
     * @param id l'ID della risposta da eliminare
     * @return true se l'eliminazione e avvenuta con successo, false altrimenti
     * @throws RuntimeException se si verifica un errore di rete
     */
    public boolean eliminaRisposta(long id) {
        return richiediDato(new Richiesta(Comando.ELIMINA_RISPOSTA,
                Map.of("id", id)));
    }

    // -------------------------------------------------------------------------
    // Preferiti
    // -------------------------------------------------------------------------

    /**
     * Recupera l'elenco dei ristoranti preferiti di un utente.
     * Questo metodo è bloccante per la rete; deve essere eseguito al di fuori del thread della UI.
     *
     * @param username lo username dell'utente
     * @return la lista di ristoranti contrassegnati come preferiti
     * @throws RuntimeException se si verifica un errore di rete
     */
    public List<Ristorante> getPreferiti(String username) {
        return richiediDato(new Richiesta(Comando.GET_PREFERITI,
                Map.of("username", username)));
    }

    /**
     * Aggiunge un ristorante ai preferiti di un utente.
     * Questo metodo è bloccante per la rete; deve essere eseguito al di fuori del thread della UI.
     *
     * @param username lo username dell'utente
     * @param ristoranteId l'id del ristorante da aggiungere
     * @throws RuntimeException se l'aggiunta fallisce o si verifica un errore di rete
     */
    public void aggiungiPreferito(String username, long ristoranteId) {
        Esito e = invia(new Richiesta(Comando.AGGIUNGI_PREFERITO,
                Map.of("username", username, "ristoranteId", ristoranteId)));
        if (!e.isSuccesso()) throw new RuntimeException(e.getErrore());
    }

    /**
     * Rimuove un ristorante dai preferiti di un utente.
     * Questo metodo è bloccante per la rete; deve essere eseguito al di fuori del thread della UI.
     *
     * @param username lo username dell'utente
     * @param ristoranteId l'id del ristorante da rimuovere
     * @throws RuntimeException se la rimozione fallisce o si verifica un errore di rete
     */
    public void rimuoviPreferito(String username, long ristoranteId) {
        Esito e = invia(new Richiesta(Comando.RIMUOVI_PREFERITO,
                Map.of("username", username, "ristoranteId", ristoranteId)));
        if (!e.isSuccesso()) throw new RuntimeException(e.getErrore());
    }

    /**
     * Controlla se un ristorante è tra i preferiti di un utente.
     * Questo metodo è bloccante per la rete; deve essere eseguito al di fuori del thread della UI.
     *
     * @param username lo username dell'utente
     * @param ristoranteId l'id del ristorante
     * @return true se il ristorante è tra i preferiti dell'utente, false altrimenti
     * @throws RuntimeException se si verifica un errore di rete
     */
    public boolean isPreferito(String username, long ristoranteId) {
        return richiediDato(new Richiesta(Comando.IS_PREFERITO,
                Map.of("username", username, "ristoranteId", ristoranteId)));
    }

    // -------------------------------------------------------------------------
    // Infrastruttura
    // -------------------------------------------------------------------------

    /**
     * Invia una richiesta al server e riceve la risposta (Esito).
     * Questo metodo è sincronizzato per impedire a thread concorrenti (es. differenti Task JavaFX)
     * di accavallare i propri messaggi sullo stream di input/output.
     *
     * @param richiesta l'oggetto {@link Richiesta} da trasmettere
     * @return l'{@link Esito} restituito dal server
     * @throws RuntimeException in caso di errori di serializzazione, I/O o disconnessione
     */
    private synchronized Esito invia(Richiesta richiesta) {
        try {
            oos.writeObject(richiesta);
            oos.flush();
            oos.reset();
            return (Esito) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Errore di rete durante " + richiesta.getComando(), e);
        }
    }

    /**
     * Richiede un dato specifico al server, lanciando un'eccezione se la risposta indica fallimento.
     *
     * @param <T> il tipo generico del dato atteso
     * @param richiesta l'oggetto Richiesta
     * @return il dato restituito dal server in caso di successo
     * @throws RuntimeException se il server restituisce un esito negativo o se si verifica un errore di rete
     */
    private <T> T richiediDato(Richiesta richiesta) {
        Esito esito = invia(richiesta);
        if (!esito.isSuccesso()) throw new RuntimeException(esito.getErrore());
        return esito.getDato();
    }

    /**
     * Chiude in sicurezza il socket di comunicazione rilasciando le risorse di I/O.
     *
     * @throws IOException se si verifica un errore durante la chiusura del socket
     */
    @Override
    public void close() throws IOException {
        socket.close();
    }
}
