package theknife.dao.impl;

import theknife.dao.RispostaDAO;
import theknife.db.ConnectionPool;
import theknife.models.Risposta;

import java.sql.*;
import java.util.Optional;

public class RispostaDAOImpl implements RispostaDAO {

    @Override
    public Risposta save(Risposta risposta) {
        String sql = """
            INSERT INTO risposte (recensione_id, username_ristoratore, testo, data_risposta)
            VALUES (?, ?, ?, ?)
            RETURNING id
            """;
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, risposta.getRecensioneId());
            ps.setString(2, risposta.getUsernameRistoratore());
            ps.setString(3, risposta.getTesto());
            ps.setTimestamp(4, Timestamp.valueOf(risposta.getDataRisposta()));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                risposta.setId(rs.getLong("id"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore save risposta per recensione: " + risposta.getRecensioneId(), e);
        }
        return risposta;
    }

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
}
