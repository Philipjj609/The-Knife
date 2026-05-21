package theknife.dao.impl;

import theknife.models.FiltriRicerca;
import theknife.dao.RistoranteDAO;
import theknife.db.ConnectionPool;
import theknife.models.Ristorante;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Implementazione JDBC dell'interfaccia RistoranteDAO.
 *
 * Gestisce l'inserimento, il filtraggio e il caricamento dei dettagli dei ristoranti.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public class RistoranteDAOImpl implements RistoranteDAO {

    // Query base: JOIN per cucine e servizi, aggregati in array PostgreSQL.
    // Usata come prefisso per tutte le SELECT singole/filtrate.
    private static final String SELECT_BASE = """
        SELECT r.id, r.nome, r.indirizzo, r.citta, r.nazione,
               r.latitudine, r.longitudine, r.prezzo_livello,
               r.telefono, r.url, r.sito_web, r.riconoscimento,
               r.green_star, r.descrizione, r.delivery, r.prenotazione_online,
               r.proprietario_id,
               COALESCE(array_agg(DISTINCT c.nome) FILTER (WHERE c.nome IS NOT NULL), '{}') AS cucine_arr,
               COALESCE(array_agg(DISTINCT s.nome) FILTER (WHERE s.nome IS NOT NULL), '{}') AS servizi_arr
        FROM ristoranti r
        LEFT JOIN ristoranti_cucine rc ON rc.ristorante_id = r.id
        LEFT JOIN cucine c             ON c.id = rc.cucina_id
        LEFT JOIN ristoranti_servizi rs ON rs.ristorante_id = r.id
        LEFT JOIN servizi s             ON s.id = rs.servizio_id
        """;

    /**
     * {@inheritDoc}
     *
     * Esegue una query SELECT base ordinando per nome del ristorante.
     * Recupera le cucine e i servizi aggregandoli come array nativi PostgreSQL.
     */
    @Override
    public List<Ristorante> findAll() {
        String sql = SELECT_BASE + "GROUP BY r.id ORDER BY r.nome";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return collectRows(rs);
        } catch (SQLException e) {
            throw new RuntimeException("Errore findAll ristoranti", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Cerca il ristorante tramite ID primario eseguendo la query SELECT_BASE
     * filtrata per "r.id = ?".
     */
    @Override
    public Optional<Ristorante> findById(long id) {
        String sql = SELECT_BASE + "WHERE r.id = ? GROUP BY r.id";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore findById ristorante: " + id, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Recupera i ristoranti registrati da uno specifico utente con ruolo di ristoratore.
     */
    @Override
    public List<Ristorante> findByProprietario(long proprietarioId) {
        String sql = SELECT_BASE + "WHERE r.proprietario_id = ? GROUP BY r.id ORDER BY r.nome";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, proprietarioId);
            try (ResultSet rs = ps.executeQuery()) {
                return collectRows(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore findByProprietario: " + proprietarioId, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Costruisce dinamicamente la query SQL aggiungendo clausole WHERE in base
     * ai filtri non vuoti presenti nell'oggetto {@link FiltriRicerca}.
     * Supporta la ricerca testuale case-insensitive sia per nome/città/nazione che
     * per cucine e servizi associati tramite sottoquery EXISTS.
     */
    @Override
    public List<Ristorante> search(FiltriRicerca filtri) {
        if (filtri == null) return findAll();

        StringBuilder sql = new StringBuilder(SELECT_BASE).append("WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (isNotBlank(filtri.getNome())) {
            // Ricerca libera: il campo UI promette ristorante, cucina o localita.
            // Gli EXISTS interrogano le tabelle ponte senza alterare gli array aggregati
            // costruiti dalla SELECT principale.
            // NOTA: il primo carattere di contenuto deve essere uno spazio.
            // I text block Java applicano lo stripping dell'indentazione comune,
            // quindi se questa riga ha 18 spazi e la riga di chiusura ne ha 17,
            // restano 1 spazio prima di AND. Senza quello spazio, la query si
            // attacca al "WHERE 1=1" precedente producendo "1=1AND..." e
            // PostgreSQL fallisce con "spazzatura finale dopo letterale numerico".
            sql.append("""
                  AND (
                     r.nome ILIKE ?
                     OR r.citta ILIKE ?
                     OR r.nazione ILIKE ?
                     OR EXISTS (
                         SELECT 1
                         FROM ristoranti_cucine rc_search
                         JOIN cucine c_search ON c_search.id = rc_search.cucina_id
                         WHERE rc_search.ristorante_id = r.id
                           AND c_search.nome ILIKE ?
                     )
                     OR EXISTS (
                         SELECT 1
                         FROM ristoranti_servizi rs_search
                         JOIN servizi s_search ON s_search.id = rs_search.servizio_id
                         WHERE rs_search.ristorante_id = r.id
                           AND s_search.nome ILIKE ?
                     )
                 )""");
            String term = like(filtri.getNome());
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
            params.add(term);
        }
        if (isNotBlank(filtri.getCitta())) {
            sql.append(" AND r.citta ILIKE ?");
            params.add(like(filtri.getCitta()));
        }
        if (isNotBlank(filtri.getNazione())) {
            sql.append(" AND r.nazione ILIKE ?");
            params.add(like(filtri.getNazione()));
        }
        if (isNotBlank(filtri.getCucina())) {
            // Filtro N:M su cucina: passa solo i ristoranti collegati alla cucina richiesta.
            // NOTA: il primo carattere di contenuto deve essere uno spazio.
            // I text block Java applicano lo stripping dell'indentazione comune,
            // quindi se questa riga ha 18 spazi e la riga di chiusura ne ha 17,
            // restano 1 spazio prima di AND. Senza quello spazio, la query si
            // attacca al "WHERE 1=1" precedente producendo "1=1AND..." e
            // PostgreSQL fallisce con "spazzatura finale dopo letterale numerico".
            sql.append("""
                  AND EXISTS (
                     SELECT 1
                     FROM ristoranti_cucine rc_filter
                     JOIN cucine c_filter ON c_filter.id = rc_filter.cucina_id
                     WHERE rc_filter.ristorante_id = r.id
                       AND c_filter.nome ILIKE ?
                 )""");
            params.add(like(filtri.getCucina()));
        }
        if (isNotBlank(filtri.getServizio())) {
            // Filtro N:M su servizio/facility: usa la tabella ponte corretta.
            // NOTA: il primo carattere di contenuto deve essere uno spazio.
            // I text block Java applicano lo stripping dell'indentazione comune,
            // quindi se questa riga ha 18 spazi e la riga di chiusura ne ha 17,
            // restano 1 spazio prima di AND. Senza quello spazio, la query si
            // attacca al "WHERE 1=1" precedente producendo "1=1AND..." e
            // PostgreSQL fallisce con "spazzatura finale dopo letterale numerico".
            sql.append("""
                  AND EXISTS (
                     SELECT 1
                     FROM ristoranti_servizi rs_filter
                     JOIN servizi s_filter ON s_filter.id = rs_filter.servizio_id
                     WHERE rs_filter.ristorante_id = r.id
                       AND s_filter.nome ILIKE ?
                 )""");
            params.add(like(filtri.getServizio()));
        }
        if (filtri.getPrezzoLivello() != null) {
            sql.append(" AND r.prezzo_livello = ?");
            params.add(filtri.getPrezzoLivello());
        }
        if (isNotBlank(filtri.getRiconoscimento())) {
            sql.append(" AND r.riconoscimento = ?::riconoscimento_michelin");
            params.add(filtri.getRiconoscimento());
        }
        if (filtri.isSoloDelivery())      sql.append(" AND r.delivery = TRUE");
        if (filtri.isSoloPrenotazione())  sql.append(" AND r.prenotazione_online = TRUE");

        sql.append(" GROUP BY r.id ORDER BY r.nome");

        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                return collectRows(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore search ristoranti", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Salva il ristorante e gestisce la persistenza delle relazioni N:M con le tabelle
     * "cucine" e "servizi" inserendo o recuperando le chiavi esterne tramite tabelle di giunzione.
     * L'operazione è eseguita all'interno di una transazione atomica con rollback in caso di errore.
     */
    @Override
    public Ristorante save(Ristorante ristorante) {
        String insertRist = """
            INSERT INTO ristoranti
                (nome, indirizzo, citta, nazione, latitudine, longitudine, prezzo_livello,
                 telefono, url, sito_web, riconoscimento, green_star, descrizione,
                 delivery, prenotazione_online, proprietario_id)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::riconoscimento_michelin, ?, ?, ?, ?, ?)
            RETURNING id
            """;
        try (Connection conn = ConnectionPool.getConnection()) {
            conn.setAutoCommit(false);
            try {
                long ristoranteId;
                try (PreparedStatement ps = conn.prepareStatement(insertRist)) {
                    ps.setString(1, ristorante.getNome());
                    ps.setString(2, ristorante.getIndirizzo());
                    ps.setString(3, ristorante.getCitta());
                    ps.setString(4, ristorante.getNazione());
                    ps.setDouble(5, ristorante.getLatitudine());
                    ps.setDouble(6, ristorante.getLongitudine());
                    ps.setInt(7, ristorante.getPrezzoLivello());
                    ps.setString(8, ristorante.getTelefono());
                    ps.setString(9, ristorante.getUrl());
                    ps.setString(10, ristorante.getSitoWeb());
                    if (ristorante.getRiconoscimento() != null)
                        ps.setString(11, ristorante.getRiconoscimento());
                    else
                        ps.setNull(11, Types.OTHER);
                    ps.setBoolean(12, ristorante.isGreenStar());
                    ps.setString(13, ristorante.getDescrizione());
                    ps.setBoolean(14, ristorante.isDelivery());
                    ps.setBoolean(15, ristorante.isPrenotazioneOnline());
                    if (ristorante.getProprietarioId() > 0)
                        ps.setLong(16, ristorante.getProprietarioId());
                    else
                        ps.setNull(16, Types.BIGINT);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        ristoranteId = rs.getLong("id");
                        ristorante.setId(ristoranteId);
                    }
                }
                for (String cucina : ristorante.getCucine()) {
                    long cucinaId = upsertLookup(conn, "cucine", cucina);
                    linkJunction(conn, "ristoranti_cucine", "ristorante_id", "cucina_id", ristoranteId, cucinaId);
                }
                for (String servizio : ristorante.getServizi()) {
                    long servizioId = upsertLookup(conn, "servizi", servizio);
                    linkJunction(conn, "ristoranti_servizi", "ristorante_id", "servizio_id", ristoranteId, servizioId);
                }
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore save ristorante: " + ristorante.getNome(), e);
        }
        return ristorante;
    }

    /**
     * {@inheritDoc}
     *
     * Recupera l'elenco completo dei servizi in ordine alfabetico.
     */
    @Override
    public List<String> findAllServizi() {
        String sql = "SELECT nome FROM servizi ORDER BY nome";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<String> list = new ArrayList<>();
            while (rs.next()) list.add(rs.getString("nome"));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Errore findAllServizi", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Recupera l'elenco completo dei tipi di cucina in ordine alfabetico.
     */
    @Override
    public List<String> findAllCucine() {
        String sql = "SELECT nome FROM cucine ORDER BY nome";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<String> list = new ArrayList<>();
            while (rs.next()) list.add(rs.getString("nome"));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Errore findAllCucine", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Recupera tutte le città distinte in cui sono ubicati i ristoranti.
     */
    @Override
    public List<String> findAllCitta() {
        String sql = "SELECT DISTINCT citta FROM ristoranti WHERE citta IS NOT NULL ORDER BY citta";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<String> list = new ArrayList<>();
            while (rs.next()) list.add(rs.getString("citta"));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Errore findAllCitta", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Recupera tutte le nazioni distinte in cui sono ubicati i ristoranti.
     */
    @Override
    public List<String> findAllNazioni() {
        String sql = "SELECT DISTINCT nazione FROM ristoranti WHERE nazione IS NOT NULL ORDER BY nazione";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<String> list = new ArrayList<>();
            while (rs.next()) list.add(rs.getString("nazione"));
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Errore findAllNazioni", e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * Esegue un controllo di unicità a livello logico (nome + indirizzo) ignorando la distinzione
     * tra maiuscole e minuscole.
     */
    @Override
    public boolean existsByNomeAndIndirizzo(String nome, String indirizzo) {
        String sql = "SELECT 1 FROM ristoranti WHERE LOWER(nome) = LOWER(?) AND LOWER(indirizzo) = LOWER(?)";
        try (Connection conn = ConnectionPool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, nome);
            ps.setString(2, indirizzo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Errore existsByNomeAndIndirizzo", e);
        }
    }

    /**
     * Inserisce un valore in una tabella di lookup (es. cucine, servizi) se non è già presente,
     * e ne restituisce l'id associato.
     *
     * @param conn la connessione al database attiva
     * @param tabella il nome della tabella in cui inserire il valore
     * @param valore il valore testuale da inserire (es. "Italiana")
     * @return l'identificatore univoco del valore
     * @throws SQLException se si verifica un errore durante l'esecuzione SQL
     */
    private long upsertLookup(Connection conn, String tabella, String valore) throws SQLException {
        String upsert = "INSERT INTO " + tabella + " (nome) VALUES (?) ON CONFLICT (nome) DO NOTHING";
        String select = "SELECT id FROM " + tabella + " WHERE nome = ?";
        try (PreparedStatement ps = conn.prepareStatement(upsert)) {
            ps.setString(1, valore);
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement(select)) {
            ps.setString(1, valore);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong("id");
            }
        }
    }

    /**
     * Associa due entità inserendo una riga nella tabella di giunzione specificata.
     *
     * @param conn la connessione al database attiva
     * @param tabella la tabella di giunzione (es. "ristoranti_cucine")
     * @param col1 la colonna della prima chiave esterna
     * @param col2 la colonna della seconda chiave esterna
     * @param id1 il valore della prima chiave
     * @param id2 il valore della seconda chiave
     * @throws SQLException se si verifica un errore SQL durante l'inserimento
     */
    private void linkJunction(Connection conn, String tabella,
                               String col1, String col2,
                               long id1, long id2) throws SQLException {
        String sql = "INSERT INTO " + tabella + " (" + col1 + ", " + col2 + ") VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id1);
            ps.setLong(2, id2);
            ps.executeUpdate();
        }
    }

    /**
     * Associa i parametri forniti a un oggetto PreparedStatement dinamico.
     *
     * @param ps il PreparedStatement da configurare
     * @param params la lista dei parametri da associare
     * @throws SQLException se si verifica un errore di binding
     */
    private void bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object p = params.get(i);
            if (p instanceof String)  ps.setString(i + 1, (String) p);
            else if (p instanceof Integer) ps.setInt(i + 1, (Integer) p);
        }
    }

    /**
     * Colleziona tutte le righe di un ResultSet e le mappa in oggetti Ristorante.
     *
     * @param rs il ResultSet da scorrere
     * @return la lista di {@link Ristorante} estratti
     * @throws SQLException se si verifica un errore di lettura
     */
    private List<Ristorante> collectRows(ResultSet rs) throws SQLException {
        List<Ristorante> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        return list;
    }

    /**
     * Mappa una singola riga del ResultSet in un oggetto Ristorante.
     *
     * @param rs il ResultSet posizionato sulla riga corrente
     * @return il {@link Ristorante} mappato
     * @throws SQLException se si verifica un errore nel recupero delle colonne o degli array SQL
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
     * Converte un oggetto Array SQL di stringhe in una lista Java di stringhe.
     *
     * @param sqlArray l'array SQL da convertire
     * @return la lista di stringhe corrispondente, o una lista vuota se l'array è nullo
     * @throws SQLException se si verifica un errore durante la conversione nativa dell'array
     */
    private List<String> arrayToList(Array sqlArray) throws SQLException {
        if (sqlArray == null) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList((String[]) sqlArray.getArray()));
    }

    /**
     * Verifica se una stringa non è nulla e contiene caratteri non-white-space.
     *
     * @param s la stringa da controllare
     * @return true se la stringa è valida e compilata, false altrimenti
     */
    private boolean isNotBlank(String s) {
        return s != null && !s.isBlank();
    }

    /**
     * Confeziona una stringa per l'utilizzo in ricerche tramite operatore SQL LIKE (o ILIKE),
     * racchiudendola tra caratteri percentuale (%).
     *
     * @param value il testo da cercare
     * @return la stringa formattata per la clausola LIKE (es. "%valore%")
     */
    private String like(String value) {
        return "%" + value.trim() + "%";
    }
}
