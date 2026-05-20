package theknife.network;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Contenitore per i dati inviati sulla rete dal client al server.
 *
 * Associa un comando specifico ai relativi parametri dell'operazione.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
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
