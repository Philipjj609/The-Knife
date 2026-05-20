package theknife.dao;

/**
 * Eccezione lanciata dai DAO quando si verifica un errore previsto
 * il cui messaggio è destinato direttamente all'utente finale.
 *
 * A differenza di RuntimeException, il GestoreClient propaga il
 * messaggio di questa eccezione senza prefissarlo con "Errore interno
 * del server", perché il testo è già pensato per essere mostrato in UI.
 *
 * Esempi di casi d'uso: violazioni di vincoli con messaggio chiaro
 * (es. recensione duplicata), validazioni applicative fallite.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public class ErroreApplicativo extends RuntimeException {

    public ErroreApplicativo(String messaggio) {
        super(messaggio);
    }
}
