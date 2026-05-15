package theknife.network;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Richiesta implements Serializable {

    private final Comando            comando;
    private final Map<String, Object> parametri;

    public Richiesta(Comando comando) {
        this(comando, Collections.emptyMap());
    }

    public Richiesta(Comando comando, Map<String, Object> parametri) {
        this.comando   = comando;
        this.parametri = new HashMap<>(parametri);
    }

    @SuppressWarnings("unchecked")
    public <T> T get(String chiave) {
        return (T) parametri.get(chiave);
    }

    public Comando getComando() {
        return comando;
    }
}
