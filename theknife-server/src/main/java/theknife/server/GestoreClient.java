package theknife.server;

import org.mindrot.jbcrypt.BCrypt;
import theknife.dao.*;
import theknife.dao.ErroreApplicativo;
import theknife.models.FiltriRicerca;
import theknife.models.Recensione;
import theknife.models.Risposta;
import theknife.models.Ristorante;
import theknife.models.Role;
import theknife.models.Utente;
import theknife.network.Esito;
import theknife.network.Richiesta;
import theknife.validation.RegistrazioneValidator;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;

/**
 * Gestisce la sessione di un singolo client su un thread dedicato.
 * La connessione è persistente: il ciclo legge richieste finché il client
 * non si disconnette o non si verifica un errore di rete.
 *
 * I DAO sono stateless (prendono connessioni dal pool per ogni operazione),
 * quindi condividerli tra più GestoreClient è thread-safe.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public class GestoreClient implements Runnable {

    /** Socket TCP per la comunicazione con il client */
    private final Socket        socket;
    /** DAO per la gestione dei dati utente */
    private final UtenteDAO     utenteDAO;
    /** DAO per la gestione dei dati dei ristoranti */
    private final RistoranteDAO ristoranteDAO;
    /** DAO per la gestione delle recensioni */
    private final RecensioneDAO recensioneDAO;
    /** DAO per la gestione delle risposte alle recensioni */
    private final RispostaDAO   rispostaDAO;
    /** DAO per la gestione delle preferenze/preferiti degli utenti */
    private final PreferitiDAO  preferitiDAO;
    /** Oggetto contenente l'utente attualmente autenticato nella sessione (null se ospite) */
    private Utente utenteAutenticato;

    /**
     * Costruisce un gestore di sessione per un client specifico collegando i DAO necessari.
     *
     * @param socket        il socket di connessione TCP con il client
     * @param utenteDAO     il DAO per le operazioni sugli utenti
     * @param ristoranteDAO il DAO per le operazioni sui ristoranti
     * @param recensioneDAO il DAO per le operazioni sulle recensioni
     * @param rispostaDAO   il DAO per le operazioni sulle risposte
     * @param preferitiDAO  il DAO per le operazioni sui preferiti
     */
    public GestoreClient(Socket socket,
                         UtenteDAO utenteDAO,
                         RistoranteDAO ristoranteDAO,
                         RecensioneDAO recensioneDAO,
                         RispostaDAO rispostaDAO,
                         PreferitiDAO preferitiDAO) {
        this.socket        = socket;
        this.utenteDAO     = utenteDAO;
        this.ristoranteDAO = ristoranteDAO;
        this.recensioneDAO = recensioneDAO;
        this.rispostaDAO   = rispostaDAO;
        this.preferitiDAO  = preferitiDAO;
    }

    /**
     * Avvia il thread dedicato alla sessione del client. Gestisce la lettura sequenziale
     * delle richieste serializzate su socket, ne esegue il dispatch e invia gli esiti corrispondenti.
     * La connessione permane fino alla chiusura esplicita o al verificarsi di un errore di I/O.
     */
    @Override
    public void run() {
        String indirizzo = socket.getRemoteSocketAddress().toString();
        log("Connesso: " + indirizzo);

        // OOS deve essere creato prima di OIS — entrambi i lati seguono questa convenzione
        try (ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream  ois = new ObjectInputStream(socket.getInputStream())) {

            oos.flush();   // invia l'header di serializzazione subito

            while (!socket.isClosed()) {
                Richiesta richiesta = (Richiesta) ois.readObject();
                log("← " + richiesta.getComando());

                Esito esito = dispatch(richiesta);

                oos.writeObject(esito);
                oos.flush();
                oos.reset();  // impedisce la cache degli oggetti nell'OOS tra una risposta e l'altra
            }

        } catch (EOFException | SocketException ignored) {
            // Disconnessione normale del client
        } catch (Throwable e) {
            // Throwable (non solo Exception) per catturare anche Error come
            // ExceptionInInitializerError / NoClassDefFoundError dal ConnectionPool
            System.err.printf("[%s] Errore fatale: %s: %s%n",
                    Thread.currentThread().getName(), e.getClass().getSimpleName(), e.getMessage());
            e.printStackTrace();
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
            log("Disconnesso: " + indirizzo);
        }
    }

    /**
     * Esegue il dispatch (smistamento) delle richieste inviate dal client basandosi sul tipo di comando.
     * Mappa i comandi di rete alle corrispondenti operazioni sui DAO e gestisce i controlli di sicurezza.
     * In caso di eccezioni applicative o errori gravi, restituisce un pacchetto di esito contenente il messaggio di errore.
     *
     * @param r la richiesta ricevuta dal client contenente il comando e i parametri
     * @return un DTO di tipo {@link Esito} contenente i dati di risposta o la descrizione dell'errore
     */
    private Esito dispatch(Richiesta r) {
        try {
            return switch (r.getComando()) {

                // --- Autenticazione e utenti ---
                case LOGIN -> {
                    String username = r.get("username");
                    String password = r.get("password");
                    yield utenteDAO.authenticate(username, password)
                            .map(utente -> {
                                utenteAutenticato = senzaPassword(utente);
                                return Esito.ok(utenteAutenticato);
                            })
                            .orElse(Esito.errore("Credenziali non valide"));
                }
                case REGISTRA_UTENTE -> {
                    Utente u = r.get("utente");
                    String password = r.get("password");
                    try {
                        RegistrazioneValidator.valida(u, password);
                    } catch (IllegalArgumentException e) {
                        throw new InvalidRegistrationException(e.getMessage());
                    }
                    if (utenteDAO.existsByUsername(u.getUsername()))
                        yield Esito.errore("Username già in uso");
                    u.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
                    yield Esito.ok(senzaPassword(utenteDAO.save(u)));
                }
                case USERNAME_ESISTE ->
                    Esito.ok(utenteDAO.existsByUsername(r.get("username")));
                case MODIFICA_UTENTE -> {
                    Utente u = r.get("utente");
                    if (utenteAutenticato == null || utenteAutenticato.getId() != u.getId()) {
                        yield Esito.errore("Operazione non autorizzata");
                    }
                    java.util.Optional<Utente> oDbUser = utenteDAO.findById(u.getId());
                    if (oDbUser.isEmpty()) {
                        yield Esito.errore("Utente non trovato");
                    }
                    Utente dbUser = oDbUser.get();
                    dbUser.setNome(u.getNome());
                    dbUser.setCognome(u.getCognome());
                    dbUser.setDataNascita(u.getDataNascita());
                    dbUser.setDomicilio(u.getDomicilio());
                    
                    if (utenteDAO.update(dbUser)) {
                        utenteAutenticato = senzaPassword(dbUser);
                        yield Esito.ok(utenteAutenticato);
                    } else {
                        yield Esito.errore("Impossibile salvare le modifiche dell'utente");
                    }
                }

                // --- Ristoranti ---
                case CERCA_RISTORANTI -> {
                    FiltriRicerca filtri = r.get("filtri");
                    yield Esito.ok(ristoranteDAO.search(filtri));
                }
                case GET_RISTORANTE -> {
                    long id = r.get("id");
                    yield ristoranteDAO.findById(id)
                            .map(Esito::ok)
                            .orElse(Esito.errore("Ristorante non trovato"));
                }
                case AGGIUNGI_RISTORANTE -> {
                    Ristorante rist = r.get("ristorante");
                    if (!isRistoratoreAutenticato() || rist.getProprietarioId() != utenteAutenticato.getId())
                        yield Esito.errore("Operazione non autorizzata");
                    if (ristoranteDAO.existsByNomeAndIndirizzo(rist.getNome(), rist.getIndirizzo()))
                        yield Esito.errore("Ristorante già presente nel sistema");
                    yield Esito.ok(ristoranteDAO.save(rist));
                }
                case GET_SERVIZI ->
                    Esito.ok(ristoranteDAO.findAllServizi());
                case GET_CUCINE ->
                    Esito.ok(ristoranteDAO.findAllCucine());
                case GET_CITTA ->
                    Esito.ok(ristoranteDAO.findAllCitta());
                case GET_NAZIONI ->
                    Esito.ok(ristoranteDAO.findAllNazioni());

                case GET_RISTORANTI_PROPRIETARIO -> {
                    long proprietarioId = r.get("proprietarioId");
                    if (!isRistoratoreAutenticato() || utenteAutenticato.getId() != proprietarioId)
                        yield Esito.errore("Operazione non autorizzata");
                    yield Esito.ok(ristoranteDAO.findByProprietario(proprietarioId));
                }

                // --- Recensioni ---
                case GET_RECENSIONI_RISTORANTE -> {
                    long ristoranteId = r.get("ristoranteId");
                    yield Esito.ok(recensioneDAO.findByRistorante(ristoranteId));
                }
                case GET_RECENSIONI_CLIENTE -> {
                    String username = r.get("username");
                    if (!isUtenteAutenticato(username))
                        yield Esito.errore("Operazione non autorizzata");
                    yield Esito.ok(recensioneDAO.findByCliente(username));
                }
                case GET_RECENSIONI_RISTORATORI -> {
                    if (!isRistoratoreAutenticato())
                        yield Esito.errore("Operazione non autorizzata");
                    List<Long> ids = r.get("ristoranteIds");
                    yield Esito.ok(recensioneDAO.findByRistoranteIds(ids));
                }
                case AGGIUNGI_RECENSIONE -> {
                    Recensione rec = r.get("recensione");
                    if (!isClienteAutenticato() || !utenteAutenticato.getUsername().equals(rec.getUsernameCliente()))
                        yield Esito.errore("Operazione non autorizzata");
                    yield Esito.ok(recensioneDAO.save(rec));
                }
                case MODIFICA_RECENSIONE -> {
                    Recensione rec = r.get("recensione");
                    if (!isClienteAutenticato() || !utenteAutenticato.getUsername().equals(rec.getUsernameCliente()))
                        yield Esito.errore("Operazione non autorizzata");
                    boolean aggiornata = recensioneDAO.update(rec);
                    yield aggiornata ? Esito.ok(true) : Esito.errore("Recensione non trovata o non autorizzata");
                }
                case ELIMINA_RECENSIONE -> {
                    long id = r.get("id");
                    String username = r.get("username");
                    if (!isClienteAutenticato() || !utenteAutenticato.getUsername().equals(username))
                        yield Esito.errore("Operazione non autorizzata");
                    boolean eliminata = recensioneDAO.delete(id, username);
                    yield eliminata ? Esito.ok(true) : Esito.errore("Recensione non trovata o non autorizzata");
                }
                case GET_MEDIA_VALUTAZIONI -> {
                    long ristoranteId = r.get("ristoranteId");
                    yield Esito.ok(recensioneDAO.getMediaValutazioni(ristoranteId));
                }

                // --- Risposte ---
                case RISPONDI_RECENSIONE -> {
                    Risposta risposta = r.get("risposta");
                    if (!isRistoratoreAutenticato() ||
                            !utenteAutenticato.getUsername().equals(risposta.getUsernameRistoratore()))
                        yield Esito.errore("Operazione non autorizzata");
                    yield Esito.ok(rispostaDAO.save(risposta, utenteAutenticato.getId()));
                }
                case MODIFICA_RISPOSTA -> {
                    Risposta risposta = r.get("risposta");
                    if (!isRistoratoreAutenticato() ||
                            !utenteAutenticato.getUsername().equals(risposta.getUsernameRistoratore()))
                        yield Esito.errore("Operazione non autorizzata");
                    boolean aggiornata = rispostaDAO.update(risposta, utenteAutenticato.getId());
                    yield aggiornata ? Esito.ok(true) : Esito.errore("Risposta non trovata o non autorizzata");
                }
                case ELIMINA_RISPOSTA -> {
                    long id = r.get("id");
                    if (!isRistoratoreAutenticato())
                        yield Esito.errore("Operazione non autorizzata");
                    boolean eliminata = rispostaDAO.delete(id, utenteAutenticato.getId());
                    yield eliminata ? Esito.ok(true) : Esito.errore("Risposta non trovata o non autorizzata");
                }

                // --- Preferiti ---
                case GET_PREFERITI -> {
                    String username = r.get("username");
                    if (!isUtenteAutenticato(username))
                        yield Esito.errore("Operazione non autorizzata");
                    yield Esito.ok(preferitiDAO.findByUtente(username));
                }
                case AGGIUNGI_PREFERITO -> {
                    String username = r.get("username");
                    if (!isUtenteAutenticato(username))
                        yield Esito.errore("Operazione non autorizzata");
                    preferitiDAO.add(username, (long) r.get("ristoranteId"));
                    yield Esito.ok();
                }
                case RIMUOVI_PREFERITO -> {
                    String username = r.get("username");
                    if (!isUtenteAutenticato(username))
                        yield Esito.errore("Operazione non autorizzata");
                    preferitiDAO.remove(username, (long) r.get("ristoranteId"));
                    yield Esito.ok();
                }
                case IS_PREFERITO -> {
                    String username = r.get("username");
                    if (!isUtenteAutenticato(username))
                        yield Esito.errore("Operazione non autorizzata");
                    boolean pref = preferitiDAO.isPreferito(username, (long) r.get("ristoranteId"));
                    yield Esito.ok(pref);
                }
            };
        } catch (ErroreApplicativo e) {
            return Esito.errore(e.getMessage());
        } catch (Throwable e) {
            System.err.printf("[%s] Errore dispatch %s: %s: %s%n",
                    Thread.currentThread().getName(), r.getComando(),
                    e.getClass().getSimpleName(), e.getMessage());
            if (e.getCause() != null) {
                System.err.printf("[%s]   Caused by: %s: %s%n",
                        Thread.currentThread().getName(),
                        e.getCause().getClass().getSimpleName(),
                        e.getCause().getMessage());
            }
            e.printStackTrace(System.err);
            return Esito.errore("Errore interno del server: " + e.getMessage());
        }
    }

    /**
     * Stampa un messaggio di log sulla console includendo il nome del thread corrente.
     *
     * @param msg il messaggio da loggare
     */
    private void log(String msg) {
        System.out.printf("[%s] %s%n", Thread.currentThread().getName(), msg);
    }

    /**
     * Verifica se il client si è autenticato con lo username specificato.
     *
     * @param username lo username da verificare
     * @return true se l'utente è autenticato e coincide con lo username, false altrimenti
     */
    private boolean isUtenteAutenticato(String username) {
        return utenteAutenticato != null && utenteAutenticato.getUsername().equals(username);
    }

    /**
     * Verifica se l'utente correntemente autenticato possiede il ruolo di cliente.
     *
     * @return true se l'utente è autenticato ed è un cliente, false altrimenti
     */
    private boolean isClienteAutenticato() {
        return utenteAutenticato != null && utenteAutenticato.getRuoloEnum() == Role.CLIENTE;
    }

    /**
     * Verifica se l'utente correntemente autenticato possiede il ruolo di ristoratore.
     *
     * @return true se l'utente è autenticato ed è un ristoratore, false altrimenti
     */
    private boolean isRistoratoreAutenticato() {
        return utenteAutenticato != null && utenteAutenticato.getRuoloEnum() == Role.RISTORATORE;
    }

    /**
     * Verifica se una stringa è nulla o formata da soli spazi bianchi.
     *
     * @param value la stringa da verificare
     * @return true se la stringa è vuota o nulla, false altrimenti
     */
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /**
     * Copia un oggetto utente escludendo l'hash della password per impedire la
     * trasmissione di dati riservati e sensibili verso il client.
     *
     * @param utente l'oggetto {@link Utente} originale completo
     * @return un nuovo oggetto {@link Utente} con la password impostata a null
     */
    private Utente senzaPassword(Utente utente) {
        return new Utente(
                utente.getId(),
                utente.getNome(),
                utente.getCognome(),
                utente.getUsername(),
                null,
                utente.getDataNascita(),
                utente.getDomicilio(),
                utente.getRuolo()
        );
    }
}
