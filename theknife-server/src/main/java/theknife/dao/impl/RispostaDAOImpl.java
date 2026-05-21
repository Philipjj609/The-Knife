package theknife.dao.impl;

import theknife.dao.RispostaDAO;
import theknife.db.ConnectionPool;
import theknife.models.Risposta;

import java.sql.*;
import java.util.Optional;

/**
 * Implementazione JDBC dell'interfaccia RispostaDAO.
 *
 * Gestisce la memorizzazione e modifica delle risposte del ristoratore.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public class RispostaDAOImpl implements RispostaDAO {

    /**
     * {@inheritDoc}
     *
     * @param risposta la risposta da salvare
     * @param proprietarioId l'ID del proprietario del ristorante
     * @return la risposta salvata con l'ID generato dal database
     */
    @Override
    public Risposta save(Risposta risposta, long proprietarioId) {
        String sql = """
            INSERT INTO risposte (recensione_id, username_ristoratore, testo, data_risposta)
            SELECT rec.id, ?, ?, ?
            FROM recensioni rec
            JOIN ristoranti r ON r.id = rec.ristorante_id
            WHERE rec.id = ?
              AND r.proprietario_id = ?
            RETURNING id
            """;
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, risposta.getUsernameRistoratore());
            ps.setString(2, risposta.getTesto());
            ps.setTimestamp(3, Timestamp.valueOf(risposta.getDataRisposta()));
            ps.setLong(4, risposta.getRecensioneId());
            ps.setLong(5, proprietarioId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Recensione non trovata o non appartenente al ristoratore", "NOAUTH");
                }
                risposta.setId(rs.getLong("id"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(messaggioErroreRisposta(e), e);
        }
        return risposta;
    }

    /**
     * {@inheritDoc}
     *
     * @param risposta la risposta da aggiornare
     * @param proprietarioId l'ID del proprietario del ristorante
     * @return true se l'aggiornamento ha avuto successo, false altrimenti
     */
    @Override
    public boolean update(Risposta risposta, long proprietarioId) {
        String sql = """
            UPDATE risposte risp
            SET testo = ?, data_risposta = ?
            FROM recensioni rec
            JOIN ristoranti r ON r.id = rec.ristorante_id
            WHERE risp.recensione_id = rec.id
              AND risp.id = ?
              AND r.proprietario_id = ?
            """;
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, risposta.getTesto());
            ps.setTimestamp(2, Timestamp.valueOf(risposta.getDataRisposta()));
            ps.setLong(3, risposta.getId());
            ps.setLong(4, proprietarioId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException(messaggioErroreRisposta(e), e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param rispostaId l'ID della risposta da eliminare
     * @param proprietarioId l'ID del proprietario del ristorante
     * @return true se l'eliminazione ha avuto successo, false altrimenti
     */
    @Override
    public boolean delete(long rispostaId, long proprietarioId) {
        String sql = """
            DELETE FROM risposte risp
            USING recensioni rec
            JOIN ristoranti r ON r.id = rec.ristorante_id
            WHERE risp.recensione_id = rec.id
              AND risp.id = ?
              AND r.proprietario_id = ?
            """;
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, rispostaId);
            ps.setLong(2, proprietarioId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Errore delete risposta: " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @param recensioneId l'ID della recensione di cui cercare la risposta
     * @return un Optional contenente la risposta se presente, altrimenti vuoto
     */
    @Override
    public Optional<Risposta> findByRecensione(long recensioneId) {
        String sql = """
            SELECT id, recensione_id, username_ristoratore, testo, data_risposta
            FROM risposte WHERE recensione_id = ?
            """;
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, recensioneId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore findByRecensione: " + recensioneId, e);
        }
    }

    private Risposta mapRow(ResultSet rs) throws SQLException {
        Risposta r = new Risposta();
        r.setId(rs.getLong("id"));
        r.setRecensioneId(rs.getLong("recensione_id"));
        r.setUsernameRistoratore(rs.getString("username_ristoratore"));
        r.setTesto(rs.getString("testo"));
        r.setDataRisposta(rs.getTimestamp("data_risposta").toLocalDateTime());
        return r;
    }

    private String messaggioErroreRisposta(SQLException e) {
        String sqlState = e.getSQLState();
        if ("NOAUTH".equals(sqlState)) {
            return "Recensione non trovata o non appartenente al ristoratore";
        }
        if ("23505".equals(sqlState)) {
            return "Esiste gia una risposta per questa recensione";
        }
        if ("23503".equals(sqlState)) {
            return "Ristoratore o recensione non validi";
        }
        if ("23514".equals(sqlState)) {
            return "La risposta non puo essere vuota";
        }
        return "Errore save risposta per recensione: " + e.getMessage();
    }
}
