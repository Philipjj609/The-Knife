package theknife.network;

import java.io.Serializable;

/**
 * Classe che rappresenta la risposta restituita dal server a seguito di una richiesta dal client.
 * Implementa il pattern <b>DTO (Data Transfer Object)</b> ed è serializzabile per il transito su socket TCP.
 * Consente di determinare se un'operazione backend è andata a buon fine, recuperando il dato di risposta
 * o la descrizione dell'eventuale errore riscontrato.
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

    /**
     * Costruttore privato per istanziare un Esito.
     *
     * @param successo indica se la richiesta è andata a buon fine
     * @param dato il corpo del messaggio di ritorno (può essere nullo)
     * @param errore il messaggio dell'errore (nullo se ha successo)
     */
    private Esito(boolean successo, Object dato, String errore) {
        this.successo = successo;
        this.dato     = dato;
        this.errore   = errore;
    }

    /**
     * Crea un Esito positivo di successo senza alcun dato di ritorno.
     *
     * @return un'istanza di {@link Esito} configurata come successo vuoto
     */
    public static Esito ok() {
        return new Esito(true, null, null);
    }

    /**
     * Crea un Esito positivo contenente un oggetto dati da restituire al client.
     *
     * @param dato l'oggetto contenente le informazioni restituite dal server
     * @return un'istanza di {@link Esito} configurata come successo con payload
     */
    public static Esito ok(Object dato) {
        return new Esito(true, dato, null);
    }

    /**
     * Crea un Esito negativo contenente la spiegazione dell'errore.
     *
     * @param messaggio descrizione dell'errore o dell'eccezione catturata
     * @return un'istanza di {@link Esito} configurata come fallimento
     */
    public static Esito errore(String messaggio) {
        return new Esito(false, null, messaggio);
    }

    /**
     * Verifica se la richiesta associata ha avuto successo sul server.
     *
     * @return true se l'operazione ha avuto successo, false altrimenti
     */
    public boolean isSuccesso() {
        return successo;
    }

    /**
     * Restituisce il dato restituito dal server, eseguendo il cast implicito al tipo generico atteso.
     *
     * @param <T> il tipo di dato previsto
     * @return l'oggetto payload dell'esito o null
     */
    @SuppressWarnings("unchecked")
    public <T> T getDato() {
        return (T) dato;
    }

    /**
     * Restituisce il messaggio dell'errore nel caso in cui l'operazione sia fallita.
     *
     * @return la descrizione testuale dell'errore o null se l'esito è positivo
     */
    public String getErrore() {
        return errore;
    }
}
