package theknife.dao.impl;

import theknife.dao.PreferitiDAO;
import theknife.db.ConnectionPool;
import theknife.models.Ristorante;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementazione concreta dell'interfaccia {@link PreferitiDAO} basata su database relazionale PostgreSQL.
 *
 * Questa classe implementa il pattern <b>DAO</b> (Data Access Object) ed è stateless. 
 * Ogni operazione acquisisce in modo thread-safe una connessione dal gestore globale {@link ConnectionPool}.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public class PreferitiDAOImpl implements PreferitiDAO {

    /**
     * {@inheritDoc}
     * Esegue una query di INSERT sulla tabella dei preferiti. Gestisce le eccezioni SQL convertendole
     * in eccezioni di runtime descrittive.
     *
     * @param username     lo username dell'utente
     * @param ristoranteId l'identificativo univoco del ristorante da inserire
     * @throws RuntimeException in caso di errori di connessione SQL o vincoli di integrità referenziale
     */
    @Override
    public void add(String username, long ristoranteId) {
        String sql = "INSERT INTO preferiti (username, ristorante_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setLong(2, ristoranteId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(messaggioErrorePreferito(e, "aggiungere"), e);
        }
    }

    /**
     * {@inheritDoc}
     * Esegue una query di DELETE per rimuovere l'associazione tra l'utente ed il ristorante preferito.
     *
     * @param username     lo username dell'utente
     * @param ristoranteId l'identificativo del ristorante da rimuovere
     * @throws RuntimeException in caso di errori gravi di esecuzione SQL
     */
    @Override
    public void remove(String username, long ristoranteId) {
        String sql = "DELETE FROM preferiti WHERE username = ? AND ristorante_id = ?";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setLong(2, ristoranteId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(messaggioErrorePreferito(e, "rimuovere"), e);
        }
    }

    /**
     * {@inheritDoc}
     * Controlla l'esistenza di un record nella tabella preferiti filtrato per username e ID ristorante.
     *
     * @param username     lo username dell'utente
     * @param ristoranteId l'identificativo del ristorante
     * @return true se il ristorante è tra i preferiti dell'utente, false altrimenti
     * @throws RuntimeException in caso di errore di connettività al database
     */
    @Override
    public boolean isPreferito(String username, long ristoranteId) {
        String sql = "SELECT 1 FROM preferiti WHERE username = ? AND ristorante_id = ?";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setLong(2, ristoranteId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore isPreferito: " + username, e);
        }
    }

    /**
     * {@inheritDoc}
     * Recupera l'elenco dei ristoranti preferiti ordinati per data decrescente di aggiunta.
     * Effettua l'aggregazione degli array delle cucine e dei servizi per ridurre il numero di query.
     *
     * @param username lo username dell'utente
     * @return la lista di oggetti {@link Ristorante} preferiti
     * @throws RuntimeException in caso di anomalie di esecuzione della query relazionale
     */
    @Override
    public List<Ristorante> findByUtente(String username) {
        // Stessa struttura di RistoranteDAOImpl ma filtrata su preferiti
        String sql = """
            SELECT r.id, r.nome, r.indirizzo, r.citta, r.nazione,
                   r.latitudine, r.longitudine, r.prezzo_livello,
                   r.telefono, r.url, r.sito_web, r.riconoscimento,
                   r.green_star, r.descrizione, r.delivery, r.prenotazione_online,
                   r.proprietario_id,
                   COALESCE(array_agg(DISTINCT c.nome) FILTER (WHERE c.nome IS NOT NULL), '{}') AS cucine_arr,
                   COALESCE(array_agg(DISTINCT s.nome) FILTER (WHERE s.nome IS NOT NULL), '{}') AS servizi_arr
            FROM ristoranti r
            JOIN preferiti p            ON p.ristorante_id = r.id
            LEFT JOIN ristoranti_cucine rc ON rc.ristorante_id = r.id
            LEFT JOIN cucine c             ON c.id = rc.cucina_id
            LEFT JOIN ristoranti_servizi rs ON rs.ristorante_id = r.id
            LEFT JOIN servizi s             ON s.id = rs.servizio_id
            WHERE p.username = ?
            GROUP BY r.id, p.data_aggiunta
            ORDER BY p.data_aggiunta DESC
            """;
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                List<Ristorante> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore findByUtente preferiti: " + username, e);
        }
    }

    /**
     * Mappa una singola riga estratta dal {@link ResultSet} in un oggetto {@link Ristorante}.
     *
     * @param rs il ResultSet SQL posizionato sulla riga corrente
     * @return un'istanza compilata del modello {@link Ristorante}
     * @throws SQLException in caso di errore di lettura delle colonne SQL
     */
    private Ristorante mapRow(ResultSet rs) throws SQLException {
        List<String> cucine  = arrayToList(rs.getArray("cucine_arr"));
        List<String> servizi = arrayToList(rs.getArray("servizi_arr"));
        return new Ristorante(
            rs.getLong("id"),
            rs.getString("nome"),
            rs.getString("indirizzo"),
            rs.getString("citta"),
            rs.getString("nazione"),
            rs.getDouble("latitudine"),
            rs.getDouble("longitudine"),
            rs.getInt("prezzo_livello"),
            rs.getString("telefono"),
            rs.getString("url"),
            rs.getString("sito_web"),
            rs.getString("riconoscimento"),
            rs.getBoolean("green_star"),
            rs.getString("descrizione"),
            rs.getBoolean("delivery"),
            rs.getBoolean("prenotazione_online"),
            rs.getLong("proprietario_id"),
            cucine,
            servizi
        );
    }

    /**
     * Converte un oggetto {@link Array} SQL contenente stringhe in una lista Java di tipo {@link List}.
     *
     * @param sqlArray l'array SQL recuperato da database
     * @return una lista Java contenente gli elementi dell'array, o una lista vuota in caso di parametro nullo
     * @throws SQLException se si verifica un errore durante il recupero dei dati dell'array SQL
     */
    private List<String> arrayToList(Array sqlArray) throws SQLException {
        if (sqlArray == null) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList((String[]) sqlArray.getArray()));
    }

    /**
     * Genera un messaggio di errore personalizzato in base allo stato dell'eccezione {@link SQLException}.
     * Riconosce le violazioni di vincolo di chiave esterna (SQLState 23503).
     *
     * @param e     l'eccezione SQL intercettata
     * @param azione la stringa descrittiva dell'azione in corso (es. "aggiungere", "rimuovere")
     * @return una stringa descrittiva contenente il motivo dell'errore
     */
    private String messaggioErrorePreferito(SQLException e, String azione) {
        if ("23503".equals(e.getSQLState())) {
            return "Impossibile " + azione + " il preferito: utente o ristorante non valido";
        }
        return "Errore durante l'operazione sui preferiti";
    }
}
