package theknife.server;

import org.mindrot.jbcrypt.BCrypt;
import theknife.dao.*;
import theknife.models.FiltriRicerca;
import theknife.models.Recensione;
import theknife.models.Risposta;
import theknife.models.Ristorante;
import theknife.models.Role;
import theknife.models.Utente;
import theknife.network.Esito;
import theknife.network.Richiesta;

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
 */
public class GestoreClient implements Runnable {

    private final Socket        socket;
    private final UtenteDAO     utenteDAO;
    private final RistoranteDAO ristoranteDAO;
    private final RecensioneDAO recensioneDAO;
    private final RispostaDAO   rispostaDAO;
    private final PreferitiDAO  preferitiDAO;
    private Utente utenteAutenticato;

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

    // -------------------------------------------------------------------------
    // Dispatcher: mappa ogni Comando alle chiamate DAO corrispondenti.
    // Tutti gli errori applicativi vengono catturati e restituiti come Esito.errore().
    // -------------------------------------------------------------------------
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
                    if (isBlank(password) || password.length() < 6)
                        yield Esito.errore("La password deve contenere almeno 6 caratteri");
                    if (utenteDAO.existsByUsername(u.getUsername()))
                        yield Esito.errore("Username già in uso");
                    u.setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
                    yield Esito.ok(senzaPassword(utenteDAO.save(u)));
                }
                case USERNAME_ESISTE ->
                    Esito.ok(utenteDAO.existsByUsername(r.get("username")));

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
        } catch (Throwable e) {
            System.err.printf("[%s] Errore dispatch %s: %s: %s%n",
                    Thread.currentThread().getName(), r.getComando(),
                    e.getClass().getSimpleName(), e.getMessage());
            return Esito.errore("Errore interno del server: " + e.getMessage());
        }
    }

    private void log(String msg) {
        System.out.printf("[%s] %s%n", Thread.currentThread().getName(), msg);
    }

    private boolean isUtenteAutenticato(String username) {
        return utenteAutenticato != null && utenteAutenticato.getUsername().equals(username);
    }

    private boolean isClienteAutenticato() {
        return utenteAutenticato != null && utenteAutenticato.getRuoloEnum() == Role.CLIENTE;
    }

    private boolean isRistoratoreAutenticato() {
        return utenteAutenticato != null && utenteAutenticato.getRuoloEnum() == Role.RISTORATORE;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

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
