package theknife.network;

import java.io.Serializable;

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
