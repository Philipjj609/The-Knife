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

    public Optional<Utente> login(String username, String password) {
        Esito esito = invia(new Richiesta(Comando.LOGIN,
                Map.of("username", username, "password", password)));
        return esito.isSuccesso() ? Optional.of(esito.getDato()) : Optional.empty();
    }

    public Utente registraUtente(Utente utente, String password) {
        return richiediDato(new Richiesta(Comando.REGISTRA_UTENTE,
                Map.of("utente", utente, "password", password)));
    }

    public boolean usernameEsiste(String username) {
        return richiediDato(new Richiesta(Comando.USERNAME_ESISTE,
                Map.of("username", username)));
    }

    // -------------------------------------------------------------------------
    // Ristoranti
    // -------------------------------------------------------------------------

    public List<Ristorante> cercaRistoranti(FiltriRicerca filtri) {
        return richiediDato(new Richiesta(Comando.CERCA_RISTORANTI,
                Map.of("filtri", filtri)));
    }

    public Optional<Ristorante> getRistorante(long id) {
        Esito e = invia(new Richiesta(Comando.GET_RISTORANTE, Map.of("id", id)));
        return e.isSuccesso() ? Optional.of(e.getDato()) : Optional.empty();
    }

    public Ristorante aggiungiRistorante(Ristorante ristorante) {
        return richiediDato(new Richiesta(Comando.AGGIUNGI_RISTORANTE,
                Map.of("ristorante", ristorante)));
    }

    public List<Ristorante> getRistorantiProprietario(long proprietarioId) {
        return richiediDato(new Richiesta(Comando.GET_RISTORANTI_PROPRIETARIO,
                Map.of("proprietarioId", proprietarioId)));
    }

    // -------------------------------------------------------------------------
    // Recensioni
    // -------------------------------------------------------------------------

    public List<Recensione> getRecensioniRistorante(long ristoranteId) {
        return richiediDato(new Richiesta(Comando.GET_RECENSIONI_RISTORANTE,
                Map.of("ristoranteId", ristoranteId)));
    }

    public List<Recensione> getRecensioniCliente(String username) {
        return richiediDato(new Richiesta(Comando.GET_RECENSIONI_CLIENTE,
                Map.of("username", username)));
    }

    public List<Recensione> getRecensioniRistoratori(List<Long> ristoranteIds) {
        return richiediDato(new Richiesta(Comando.GET_RECENSIONI_RISTORATORI,
                Map.of("ristoranteIds", ristoranteIds)));
    }

    public Recensione aggiungiRecensione(Recensione recensione) {
        return richiediDato(new Richiesta(Comando.AGGIUNGI_RECENSIONE,
                Map.of("recensione", recensione)));
    }

    public boolean modificaRecensione(Recensione recensione) {
        return richiediDato(new Richiesta(Comando.MODIFICA_RECENSIONE,
                Map.of("recensione", recensione)));
    }

    public boolean eliminaRecensione(long id, String username) {
        return richiediDato(new Richiesta(Comando.ELIMINA_RECENSIONE,
                Map.of("id", id, "username", username)));
    }

    public double getMediaValutazioni(long ristoranteId) {
        return richiediDato(new Richiesta(Comando.GET_MEDIA_VALUTAZIONI,
                Map.of("ristoranteId", ristoranteId)));
    }

    // -------------------------------------------------------------------------
    // Risposte alle recensioni
    // -------------------------------------------------------------------------

    public Risposta rispondiRecensione(Risposta risposta) {
        return richiediDato(new Richiesta(Comando.RISPONDI_RECENSIONE,
                Map.of("risposta", risposta)));
    }

    // -------------------------------------------------------------------------
    // Preferiti
    // -------------------------------------------------------------------------

    public List<Ristorante> getPreferiti(String username) {
        return richiediDato(new Richiesta(Comando.GET_PREFERITI,
                Map.of("username", username)));
    }

    public void aggiungiPreferito(String username, long ristoranteId) {
        Esito e = invia(new Richiesta(Comando.AGGIUNGI_PREFERITO,
                Map.of("username", username, "ristoranteId", ristoranteId)));
        if (!e.isSuccesso()) throw new RuntimeException(e.getErrore());
    }

    public void rimuoviPreferito(String username, long ristoranteId) {
        Esito e = invia(new Richiesta(Comando.RIMUOVI_PREFERITO,
                Map.of("username", username, "ristoranteId", ristoranteId)));
        if (!e.isSuccesso()) throw new RuntimeException(e.getErrore());
    }

    public boolean isPreferito(String username, long ristoranteId) {
        return richiediDato(new Richiesta(Comando.IS_PREFERITO,
                Map.of("username", username, "ristoranteId", ristoranteId)));
    }

    // -------------------------------------------------------------------------
    // Infrastruttura
    // -------------------------------------------------------------------------

    /**
     * synchronized: un solo thread alla volta può usare la coppia OOS/OIS.
     * I controller invocano questo metodo da Task JavaFX (thread separati);
     * la sincronizzazione impedisce che due Task intercalino i loro messaggi.
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

    private <T> T richiediDato(Richiesta richiesta) {
        Esito esito = invia(richiesta);
        if (!esito.isSuccesso()) throw new RuntimeException(esito.getErrore());
        return esito.getDato();
    }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
