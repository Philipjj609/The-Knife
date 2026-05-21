/**
 * Modulo server dell'applicazione The Knife.
 * Contiene i servizi, il livello di persistenza dei dati (DAO, PostgreSQL) e la gestione delle connessioni socket.
 */
module theknife.server {
    requires theknife.common;
    requires java.sql;
    requires com.zaxxer.hikari;
    requires org.postgresql.jdbc;
    requires jbcrypt;
}
