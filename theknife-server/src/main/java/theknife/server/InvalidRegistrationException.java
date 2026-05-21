package theknife.server;

import theknife.dao.ErroreApplicativo;

/**
 * Eccezione personalizzata lanciata lato server quando la validazione dei dati
 * di registrazione utente fallisce.
 *
 * <p>Eredita da {@link ErroreApplicativo} in modo che il server possa catturarla
 * e propagare il relativo messaggio di errore direttamente al client, senza
 * prefissarlo con messaggi generici di errore interno.</p>
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public class InvalidRegistrationException extends ErroreApplicativo {

    private static final long serialVersionUID = 1L;

    /**
     * Costruisce una nuova eccezione con il messaggio descrittivo specificato.
     *
     * @param messaggio il messaggio descrittivo dell'errore di validazione
     */
    public InvalidRegistrationException(String messaggio) {
        super(messaggio);
    }
}
