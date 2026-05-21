package theknife.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Gestore del pool di connessioni JDBC verso il database PostgreSQL.
 * Utilizza la libreria <b>HikariCP</b> per fornire connessioni al database ad alte prestazioni
 * e thread-safe, adatte ad un contesto concorrente multi-client.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public class ConnectionPool {

    private static volatile HikariDataSource dataSource;

    /**
     * Costruttore privato per prevenire l'istanziazione di questa classe utility.
     */
    private ConnectionPool() {}

    /**
     * Inizializza il pool di connessioni HikariCP con le proprietà configurate.
     * Risolve le credenziali provando prima a leggerle dalle variabili d'ambiente (se presenti)
     * e successivamente dalle proprietà specificate nel file properties passato come argomento.
     *
     * @param props l'oggetto {@link Properties} contenente i valori di fallback per la connessione al DB
     */
    public static synchronized void init(Properties props) {
        if (dataSource != null && !dataSource.isClosed()) return;

        HikariConfig cfg = new HikariConfig();
        cfg.setJdbcUrl(resolve(props, "db.url", "THEKNIFE_DB_URL"));
        cfg.setUsername(resolve(props, "db.user", "THEKNIFE_DB_USER"));
        cfg.setPassword(resolve(props, "db.password", "THEKNIFE_DB_PASSWORD"));
        cfg.setMaximumPoolSize(Integer.parseInt(props.getProperty("db.pool.maxSize", "10")));
        cfg.setMinimumIdle(Integer.parseInt(props.getProperty("db.pool.minIdle", "2")));
        cfg.setAutoCommit(true);

        dataSource = new HikariDataSource(cfg);
    }

    /**
     * Estrae ed acquisisce una connessione attiva dal pool HikariCP.
     *
     * @return un oggetto {@link Connection} valido e pronto all'uso
     * @throws SQLException se si verifica un errore durante l'acquisizione della connessione dal pool
     * @throws IllegalStateException se il pool non è stato inizializzato chiamando preventivamente {@link #init(Properties)}
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null)
            throw new IllegalStateException("ConnectionPool non inizializzato. Chiamare init() prima dell'uso.");
        return dataSource.getConnection();
    }

    /**
     * Chiude in sicurezza il pool di connessioni rilasciando tutte le risorse di rete allocate da HikariCP.
     */
    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }

    /**
     * Risolve il valore di una configurazione, verificando prima la presenza di una specifica variabile d'ambiente
     * e ripiegando sul valore presente nell'oggetto Properties in caso di assenza.
     *
     * @param props l'oggetto properties di fallback
     * @param key la chiave da cercare nel file di proprietà
     * @param envKey il nome della variabile d'ambiente di priorità superiore
     * @return la stringa configurata per la proprietà richiesta
     */
    private static String resolve(Properties props, String key, String envKey) {
        String env = System.getenv(envKey);
        return (env != null && !env.isBlank()) ? env : props.getProperty(key);
    }
}
