package theknife.dao.impl;

import org.mindrot.jbcrypt.BCrypt;
import theknife.dao.UtenteDAO;
import theknife.db.ConnectionPool;
import theknife.models.Utente;

import java.sql.*;
import java.util.Optional;

/**
 * Implementazione JDBC dell'interfaccia UtenteDAO.
 *
 * Gestisce il login, la registrazione e il controllo dei duplicati degli utenti.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public class UtenteDAOImpl implements UtenteDAO {

    private static final String SELECT_BASE =
        "SELECT id, nome, cognome, username, password_hash, data_nascita, domicilio, ruolo " +
        "FROM utenti ";

    /**
     * {@inheritDoc}
     *
     * Esegue una query SELECT filtrando per lo username fornito.
     *
     * @param username lo username dell'utente da cercare
     * @return un Optional contenente l'utente se trovato, altrimenti vuoto
     */
    @Override
    public Optional<Utente> findByUsername(String username) {
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BASE + "WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore findByUsername: " + username, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Verifica la password dell'utente confrontandola con l'hash memorizzato tramite BCrypt.
     *
     * @param username lo username dell'utente
     * @param password la password in chiaro dell'utente
     * @return un Optional contenente l'utente autenticato se le credenziali sono corrette, altrimenti vuoto
     */
    @Override
    public Optional<Utente> authenticate(String username, String password) {
        return findByUsername(username)
                .filter(u -> BCrypt.checkpw(password, u.getPasswordHash()));
    }

    /**
     * {@inheritDoc}
     *
     * Controlla la presenza dello username a livello case-insensitive.
     *
     * @param username lo username da controllare
     * @return true se lo username esiste già, false altrimenti
     */
    @Override
    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM utenti WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore existsByUsername: " + username, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Inserisce un record nella tabella utenti, con hashing della password già avvenuto a monte.
     * Restituisce l'utente con l'ID autogenerato.
     *
     * @param utente l'utente da salvare
     * @return l'utente salvato comprensivo del nuovo ID generato
     */
    @Override
    public Utente save(Utente utente) {
        String sql = """
            INSERT INTO utenti (nome, cognome, username, password_hash, data_nascita, domicilio, ruolo)
            VALUES (?, ?, ?, ?, ?, ?, ?::ruolo_utente)
            RETURNING id
            """;
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, utente.getNome());
            ps.setString(2, utente.getCognome());
            ps.setString(3, utente.getUsername());
            ps.setString(4, utente.getPasswordHash());
            if (utente.getDataNascita() != null)
                ps.setDate(5, Date.valueOf(utente.getDataNascita()));
            else
                ps.setNull(5, Types.DATE);
            ps.setString(6, utente.getDomicilio());
            ps.setString(7, utente.getRuolo());
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                utente.setId(rs.getLong("id"));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore save utente: " + utente.getUsername(), e);
        }
        return utente;
    }

    /**
     * {@inheritDoc}
     *
     * Recupera un utente dal database tramite il suo ID primario.
     *
     * @param id l'id dell'utente da cercare
     * @return un Optional contenente l'utente se trovato, altrimenti vuoto
     */
    @Override
    public Optional<Utente> findById(long id) {
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BASE + "WHERE id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore findById: " + id, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Aggiorna nome, cognome, data di nascita e domicilio per l'utente corrispondente.
     *
     * @param utente l'utente con i dati aggiornati
     * @return true se l'aggiornamento ha avuto successo, false altrimenti
     */
    @Override
    public boolean update(Utente utente) {
        String sql = """
            UPDATE utenti
            SET nome = ?, cognome = ?, data_nascita = ?, domicilio = ?
            WHERE id = ?
            """;
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, utente.getNome());
            ps.setString(2, utente.getCognome());
            if (utente.getDataNascita() != null)
                ps.setDate(3, Date.valueOf(utente.getDataNascita()));
            else
                ps.setNull(3, Types.DATE);
            ps.setString(4, utente.getDomicilio());
            ps.setLong(5, utente.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Errore update utente: " + utente.getUsername(), e);
        }
    }

    /**
     * Mappa una riga del ResultSet correntemente posizionato in un oggetto Utente.
     *
     * @param rs il ResultSet posizionato sulla riga da estrarre
     * @return un oggetto {@link Utente} contenente i dati estratti
     * @throws SQLException se si verifica un errore durante il recupero dei valori delle colonne
     */
    private Utente mapRow(ResultSet rs) throws SQLException {
        Date dataSql = rs.getDate("data_nascita");
        return new Utente(
            rs.getLong("id"),
            rs.getString("nome"),
            rs.getString("cognome"),
            rs.getString("username"),
            rs.getString("password_hash"),
            dataSql != null ? dataSql.toLocalDate() : null,
            rs.getString("domicilio"),
            rs.getString("ruolo")
        );
    }
}
