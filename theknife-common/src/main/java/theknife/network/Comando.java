package theknife.network;

/**
 * Elenco dei comandi di rete supportati nel protocollo di comunicazione client-server di TheKnife.
 * Definisce le possibili azioni che il client può richiedere al server tramite l'invio di una {@link Richiesta}.
 * Ciascuna costante dell'enum rappresenta un'operazione del protocollo.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public enum Comando {

    /**
     * Esegue l'autenticazione di un utente nel sistema.
     * Parametri attesi: "username" (String), "password" (String).
     * Ritorna: {@link theknife.models.Utente} se le credenziali sono valide.
     */
    LOGIN,

    /**
     * Registra un nuovo utente (Cliente o Ristoratore).
     * Parametri attesi: "utente" (Utente), "password" (String).
     * Ritorna: l'oggetto {@link theknife.models.Utente} salvato ed associato a un ID.
     */
    REGISTRA_UTENTE,

    /**
     * Verifica la disponibilità di uno username.
     * Parametri attesi: "username" (String).
     * Ritorna: {@link Boolean} (true se già esistente, false altrimenti).
     */
    USERNAME_ESISTE,

    /**
     * Cerca ristoranti applicando un insieme di filtri.
     * Parametri attesi: "filtri" (FiltriRicerca).
     * Ritorna: {@link java.util.List} di {@link theknife.models.Ristorante}.
     */
    CERCA_RISTORANTI,

    /**
     * Recupera i dettagli anagrafici e logistici di un singolo ristorante per ID.
     * Parametri attesi: "id" (Long).
     * Ritorna: {@link theknife.models.Ristorante} corrispondente all'ID.
     */
    GET_RISTORANTE,

    /**
     * Registra un nuovo ristorante nel sistema (riservato ai ristoratori).
     * Parametri attesi: "ristorante" (Ristorante).
     * Ritorna: il {@link theknife.models.Ristorante} creato comprensivo di ID.
     */
    AGGIUNGI_RISTORANTE,

    /**
     * Recupera tutti i ristoranti di cui l'utente ristoratore è proprietario.
     * Parametri attesi: "proprietarioId" (Long).
     * Ritorna: {@link java.util.List} di {@link theknife.models.Ristorante}.
     */
    GET_RISTORANTI_PROPRIETARIO,

    /**
     * Recupera tutti i servizi disponibili nel sistema per scopi di filtraggio.
     * Ritorna: {@link java.util.List} di {@link String} (nomi servizi).
     */
    GET_SERVIZI,

    /**
     * Recupera tutti i tipi di cucina registrati nel database.
     * Ritorna: {@link java.util.List} di {@link String} (nomi cucine).
     */
    GET_CUCINE,

    /**
     * Recupera l'elenco di tutte le città che contengono almeno un ristorante.
     * Ritorna: {@link java.util.List} di {@link String} (nomi città).
     */
    GET_CITTA,

    /**
     * Recupera l'elenco di tutte le nazioni configurate per i ristoranti.
     * Ritorna: {@link java.util.List} di {@link String} (nomi nazioni).
     */
    GET_NAZIONI,

    /**
     * Recupera l'elenco di recensioni scritte per un determinato ristorante.
     * Parametri attesi: "ristoranteId" (Long).
     * Ritorna: {@link java.util.List} di {@link theknife.models.Recensione}.
     */
    GET_RECENSIONI_RISTORANTE,

    /**
     * Recupera tutte le recensioni inserite da uno specifico cliente.
     * Parametri attesi: "username" (String).
     * Ritorna: {@link java.util.List} di {@link theknife.models.Recensione}.
     */
    GET_RECENSIONI_CLIENTE,

    /**
     * Recupera le recensioni indirizzate a una lista di ristoranti (usato dalla dashboard ristoratore).
     * Parametri attesi: "ristoranteIds" (List di Long).
     * Ritorna: {@link java.util.List} di {@link theknife.models.Recensione}.
     */
    GET_RECENSIONI_RISTORATORI,

    /**
     * Consente a un cliente autenticato di scrivere una recensione per un ristorante.
     * Parametri attesi: "recensione" (Recensione).
     * Ritorna: la {@link theknife.models.Recensione} salvata con il relativo ID.
     */
    AGGIUNGI_RECENSIONE,

    /**
     * Consente ad un cliente di aggiornare il voto o il commento di una sua recensione.
     * Parametri attesi: "recensione" (Recensione).
     * Ritorna: {@link Boolean} (esito dell'operazione).
     */
    MODIFICA_RECENSIONE,

    /**
     * Consente ad un cliente di cancellare una propria recensione.
     * Parametri attesi: "id" (Long), "username" (String).
     * Ritorna: {@link Boolean} (esito dell'operazione).
     */
    ELIMINA_RECENSIONE,

    /**
     * Calcola la media aritmetica dei voti (valutazioni) ricevuti da un ristorante.
     * Parametri attesi: "ristoranteId" (Long).
     * Ritorna: {@link Double} (media delle recensioni).
     */
    GET_MEDIA_VALUTAZIONI,

    /**
     * Consente ad un ristoratore proprietario di rispondere ad una recensione ricevuta.
     * Parametri attesi: "risposta" (Risposta).
     * Ritorna: la {@link theknife.models.Risposta} memorizzata comprensiva di ID.
     */
    RISPONDI_RECENSIONE,

    /**
     * Consente ad un ristoratore proprietario di modificare una risposta gia inserita.
     * Parametri attesi: "risposta" (Risposta).
     * Ritorna: {@link Boolean} (esito dell'operazione).
     */
    MODIFICA_RISPOSTA,

    /**
     * Consente ad un ristoratore proprietario di eliminare una risposta gia inserita.
     * Parametri attesi: "id" (Long).
     * Ritorna: {@link Boolean} (esito dell'operazione).
     */
    ELIMINA_RISPOSTA,

    /**
     * Recupera tutti i ristoranti salvati come preferiti da un utente cliente.
     * Parametri attesi: "username" (String).
     * Ritorna: {@link java.util.List} di {@link theknife.models.Ristorante}.
     */
    GET_PREFERITI,

    /**
     * Aggiunge un ristorante all'elenco dei preferiti di un utente cliente.
     * Parametri attesi: "username" (String), "ristoranteId" (Long).
     * Ritorna: vuoto (Esito successo/errore).
     */
    AGGIUNGI_PREFERITO,

    /**
     * Rimuove un ristorante dall'elenco dei preferiti di un cliente.
     * Parametri attesi: "username" (String), "ristoranteId" (Long).
     * Ritorna: vuoto (Esito successo/errore).
     */
    RIMUOVI_PREFERITO,

    /**
     * Verifica se un determinato ristorante fa già parte dei preferiti di un cliente.
     * Parametri attesi: "username" (String), "ristoranteId" (Long).
     * Ritorna: {@link Boolean} (true se preferito, false altrimenti).
     */
    IS_PREFERITO,

    /**
     * Consente ad un utente registrato di aggiornare i propri dati anagrafici.
     * Parametri attesi: "utente" (Utente).
     * Ritorna: {@link theknife.models.Utente} (l'utente aggiornato dal database).
     */
    MODIFICA_UTENTE
}
