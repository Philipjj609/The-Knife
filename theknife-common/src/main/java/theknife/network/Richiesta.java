package theknife.network;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe che rappresenta un messaggio di richiesta inviato dal client al server tramite socket TCP.
 * Implementa il pattern <b>DTO (Data Transfer Object)</b> ed è serializzabile.
 * Incapsula il comando dell'operazione da eseguire ({@link Comando}) e una mappa contenente i parametri correlati.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public class Richiesta implements Serializable {

    private final Comando            comando;
    private final Map<String, Object> parametri;

    /**
     * Costruttore per creare una richiesta senza parametri aggiuntivi.
     *
     * @param comando il comando del protocollo associato alla richiesta
     */
    public Richiesta(Comando comando) {
        this(comando, Collections.emptyMap());
    }

    /**
     * Costruttore completo per creare una richiesta con comando e parametri specifici.
     *
     * @param comando il comando del protocollo associato alla richiesta
     * @param parametri mappa contenente coppie chiave-valore con i parametri necessari
     */
    public Richiesta(Comando comando, Map<String, Object> parametri) {
        this.comando   = comando;
        this.parametri = new HashMap<>(parametri);
    }

    /**
     * Recupera un parametro specifico per chiave, eseguendo il cast implicito al tipo generico atteso.
     *
     * @param <T> il tipo previsto del parametro
     * @param chiave la chiave identificativa del parametro desiderato
     * @return il valore associato alla chiave o null se non presente
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String chiave) {
        return (T) parametri.get(chiave);
    }

    /**
     * Restituisce il comando del protocollo associato a questa richiesta.
     *
     * @return l'enum {@link Comando} della richiesta
     */
    public Comando getComando() {
        return comando;
    }
}
