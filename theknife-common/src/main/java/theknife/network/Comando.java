package theknife.network;

/**
 * Elenco dei comandi di rete supportati nel protocollo di comunicazione client-server.
 *
 * Definisce le possibili azioni che il client può richiedere.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public enum Comando {

    // Autenticazione e utenti
    LOGIN,
    REGISTRA_UTENTE,
    USERNAME_ESISTE,

    // Ristoranti
    CERCA_RISTORANTI,
    GET_RISTORANTE,
    AGGIUNGI_RISTORANTE,
    GET_RISTORANTI_PROPRIETARIO,
    GET_SERVIZI,

    // Recensioni
    GET_RECENSIONI_RISTORANTE,
    GET_RECENSIONI_CLIENTE,
    GET_RECENSIONI_RISTORATORI,
    AGGIUNGI_RECENSIONE,
    MODIFICA_RECENSIONE,
    ELIMINA_RECENSIONE,
    GET_MEDIA_VALUTAZIONI,

    // Risposte alle recensioni
    RISPONDI_RECENSIONE,

    // Preferiti
    GET_PREFERITI,
    AGGIUNGI_PREFERITO,
    RIMUOVI_PREFERITO,
    IS_PREFERITO
}
