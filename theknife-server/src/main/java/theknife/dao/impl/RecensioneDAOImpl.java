package theknife.dao.impl;

import theknife.dao.ErroreApplicativo;
import theknife.dao.RecensioneDAO;
import theknife.db.ConnectionPool;
import theknife.models.Recensione;
import theknife.models.Risposta;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementazione JDBC dell'interfaccia RecensioneDAO.
 *
 * Gestisce la memorizzazione e il recupero delle recensioni dal database.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public class RecensioneDAOImpl implements RecensioneDAO {

    // Carica recensione, autore e eventuale risposta in una sola query.
    // La JOIN su utenti rende il payload autosufficiente per il client.
    private static final String SELECT_BASE = """
        SELECT rec.id, rec.username_cliente, rec.ristorante_id, r.nome AS nome_ristorante,
               u.nome AS nome_cliente, u.cognome AS cognome_cliente,
               rec.valutazione, rec.titolo, rec.commento, rec.data_recensione,
               risp.id           AS risposta_id,
               risp.username_ristoratore,
               risp.testo        AS risposta_testo,
               risp.data_risposta
        FROM recensioni rec
        JOIN ristoranti r   ON r.id   = rec.ristorante_id
        JOIN utenti u       ON u.username = rec.username_cliente
        LEFT JOIN risposte risp ON risp.recensione_id = rec.id
        """;

    @Override
    public Optional<Recensione> findById(long id) {
        String sql = SELECT_BASE + "WHERE rec.id = ?";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore findById recensione: " + id, e);
        }
    }

    @Override
    public List<Recensione> findByRistorante(long ristoranteId) {
        return query(SELECT_BASE + "WHERE rec.ristorante_id = ? ORDER BY rec.data_recensione DESC",
                ps -> ps.setLong(1, ristoranteId));
    }

    @Override
    public List<Recensione> findByCliente(String username) {
        return query(SELECT_BASE + "WHERE rec.username_cliente = ? ORDER BY rec.data_recensione DESC",
                ps -> ps.setString(1, username));
    }

    @Override
    public List<Recensione> findByRistoranteIds(List<Long> ristoranteIds) {
        if (ristoranteIds == null || ristoranteIds.isEmpty()) return List.of();
        String placeholders = ristoranteIds.stream().map(x -> "?").collect(Collectors.joining(","));
        String sql = SELECT_BASE + "WHERE rec.ristorante_id IN (" + placeholders + ") ORDER BY rec.data_recensione DESC";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < ristoranteIds.size(); i++)
                ps.setLong(i + 1, ristoranteIds.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                return collectRows(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore findByRistoranteIds", e);
        }
    }

    @Override
    public Recensione save(Recensione recensione) {
        if (recensione.getDataRecensione() == null) {
            recensione.setDataRecensione(LocalDateTime.now());
        }

        String sql = """
            INSERT INTO recensioni (username_cliente, ristorante_id, valutazione, titolo, commento, data_recensione)
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING id
            """;
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, recensione.getUsernameCliente());
            ps.setLong(2, recensione.getRistoranteId());
            ps.setInt(3, recensione.getValutazione());
            ps.setString(4, recensione.getTitolo());
            ps.setString(5, recensione.getCommento());
            ps.setTimestamp(6, Timestamp.valueOf(recensione.getDataRecensione()));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                recensione.setId(rs.getLong("id"));
            }
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                throw new ErroreApplicativo(messaggioErroreRecensione(e));
            }
            throw new RuntimeException(messaggioErroreRecensione(e), e);
        }
        return recensione;
    }

    @Override
    public boolean update(Recensione recensione) {
        String sql = """
            UPDATE recensioni
               SET valutazione = ?, titolo = ?, commento = ?
             WHERE id = ? AND username_cliente = ?
            """;
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, recensione.getValutazione());
            ps.setString(2, recensione.getTitolo());
            ps.setString(3, recensione.getCommento());
            ps.setLong(4, recensione.getId());
            ps.setString(5, recensione.getUsernameCliente());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Errore update recensione: " + recensione.getId(), e);
        }
    }

    @Override
    public boolean delete(long id, String usernameCliente) {
        String sql = "DELETE FROM recensioni WHERE id = ? AND username_cliente = ?";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setString(2, usernameCliente);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Errore delete recensione: " + id, e);
        }
    }

    @Override
    public double getMediaValutazioni(long ristoranteId) {
        String sql = "SELECT COALESCE(AVG(valutazione), 0) FROM recensioni WHERE ristorante_id = ?";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, ristoranteId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore getMediaValutazioni: " + ristoranteId, e);
        }
    }

    // Helpers per evitare duplicazione
    @FunctionalInterface
    private interface ParamSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    private List<Recensione> query(String sql, ParamSetter setter) {
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setter.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return collectRows(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore query recensioni", e);
        }
    }

    private List<Recensione> collectRows(ResultSet rs) throws SQLException {
        List<Recensione> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    private Recensione mapRow(ResultSet rs) throws SQLException {
        Recensione rec = new Recensione();
        rec.setId(rs.getLong("id"));
        rec.setUsernameCliente(rs.getString("username_cliente"));
        rec.setNomeCliente(rs.getString("nome_cliente"));
        rec.setCognomeCliente(rs.getString("cognome_cliente"));
        rec.setRistoranteId(rs.getLong("ristorante_id"));
        rec.setNomeRistorante(rs.getString("nome_ristorante"));
        rec.setValutazione(rs.getInt("valutazione"));
        rec.setTitolo(rs.getString("titolo"));
        rec.setCommento(rs.getString("commento"));
        rec.setDataRecensione(rs.getTimestamp("data_recensione").toLocalDateTime());

        long rispostaId = rs.getLong("risposta_id");
        if (!rs.wasNull()) {
            Risposta risp = new Risposta();
            risp.setId(rispostaId);
            risp.setRecensioneId(rec.getId());
            risp.setUsernameRistoratore(rs.getString("username_ristoratore"));
            risp.setTesto(rs.getString("risposta_testo"));
            risp.setDataRisposta(rs.getTimestamp("data_risposta").toLocalDateTime());
            rec.setRisposta(risp);
        }
        return rec;
    }

    private String messaggioErroreRecensione(SQLException e) {
        String sqlState = e.getSQLState();
        if (sqlState == null) return "Errore save recensione";
        return switch (sqlState) {
            case "23505" -> "Hai gia lasciato una recensione per questo ristorante";
            case "23503" -> "Utente o ristorante non valido per la recensione";
            case "23514" -> "La valutazione deve essere compresa tra 1 e 5";
            default -> "Errore save recensione";
        };
    }
}
