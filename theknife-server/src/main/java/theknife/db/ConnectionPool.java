package theknife.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Gestore del pool di connessioni JDBC verso il database PostgreSQL.
 *
 * Fornisce connessioni thread-safe tramite HikariCP.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public class ConnectionPool {

    private static volatile HikariDataSource dataSource;

    private ConnectionPool() {}

    /**
     * Inizializza il pool con le proprietà fornite.
     * Deve essere chiamato una volta sola prima di qualsiasi getConnection().
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

    public static Connection getConnection() throws SQLException {
        if (dataSource == null)
            throw new IllegalStateException("ConnectionPool non inizializzato. Chiamare init() prima dell'uso.");
        return dataSource.getConnection();
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) dataSource.close();
    }

    private static String resolve(Properties props, String key, String envKey) {
        String env = System.getenv(envKey);
        return (env != null && !env.isBlank()) ? env : props.getProperty(key);
    }
}
