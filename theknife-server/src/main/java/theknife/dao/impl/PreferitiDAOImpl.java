package theknife.dao.impl;

import theknife.dao.PreferitiDAO;
import theknife.db.ConnectionPool;
import theknife.models.Ristorante;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Implementazione JDBC dell'interfaccia PreferitiDAO.
 *
 * Gestisce la persistenza nel database delle preferenze sui ristoranti.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public class PreferitiDAOImpl implements PreferitiDAO {

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

    private List<String> arrayToList(Array sqlArray) throws SQLException {
        if (sqlArray == null) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList((String[]) sqlArray.getArray()));
    }

    private String messaggioErrorePreferito(SQLException e, String azione) {
        if ("23503".equals(e.getSQLState())) {
            return "Impossibile " + azione + " il preferito: utente o ristorante non valido";
        }
        return "Errore durante l'operazione sui preferiti";
    }
}
