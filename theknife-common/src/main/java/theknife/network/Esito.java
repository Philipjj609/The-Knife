package theknife.network;

import java.io.Serializable;

/**
 * Enumerazione degli esiti possibili per una richiesta di rete.
 *
 * Indica se l'operazione sul server è andata a buon fine o se si sono verificati errori.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public class Esito implements Serializable {

    private final boolean successo;
    private final Object  dato;
    private final String  errore;

    private Esito(boolean successo, Object dato, String errore) {
        this.successo = successo;
        this.dato     = dato;
        this.errore   = errore;
    }

    public static Esito ok() {
        return new Esito(true, null, null);
    }

    public static Esito ok(Object dato) {
        return new Esito(true, dato, null);
    }

    public static Esito errore(String messaggio) {
        return new Esito(false, null, messaggio);
    }

    public boolean isSuccesso() {
        return successo;
    }

    @SuppressWarnings("unchecked")
    public <T> T getDato() {
        return (T) dato;
    }

    public String getErrore() {
        return errore;
    }
}
