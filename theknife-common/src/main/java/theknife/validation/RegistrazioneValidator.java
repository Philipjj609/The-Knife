package theknife.validation;

import java.time.LocalDate;
import theknife.models.Utente;

/**
 * Validatore condiviso per i dati di registrazione degli utenti di "The Knife".
 * Contiene le regole di validazione formale applicate sia lato client (JavaFX)
 * prima dell'invio della richiesta, sia lato server per prevenire bypass della UI.
 *
 * <p>Le regole di validazione sono:</p>
 * <ul>
 *   <li><b>Nome e cognome</b>: non vuoti, senza numeri o caratteri speciali. Ammettono
 *       lettere (anche accentate), apostrofi, trattini e spazi interni (es. "D'Angelo", "Rossi-Rossi").</li>
*     <li><b>Data di nascita</b>: non futura, non oltre 120 anni fa.</li>
 *   <li><b>Username</b>: minimo 3 caratteri, non vuoto.</li>
 *   <li><b>Password</b>: minima robustezza (almeno 8 caratteri, almeno una lettera e un numero).</li>
 * </ul>
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public class RegistrazioneValidator {

    /** Regex robusta per Nome e Cognome: consente lettere accentate, spazi interni, trattini e apostrofi. */
    private static final String REGEX_NOME_COGNOME = "^[\\p{L}]+([\\s'’-]+[\\p{L}]+)*$";

    /** Costruttore privato per evitare l'istanziazione di questa classe utility. */
    private RegistrazioneValidator() {}

    /**
     * Valida formalmente i dati di registrazione di un utente e della sua password.
     *
     * @param utente l'oggetto {@link Utente} da validare
     * @param password la password in chiaro da validare
     * @throws IllegalArgumentException se uno qualsiasi dei controlli di validazione fallisce
     */
    public static void valida(Utente utente, String password) {
        if (utente == null) {
            throw new IllegalArgumentException("I dati dell'utente sono obbligatori.");
        }

        // Validazione Nome
        String nome = utente.getNome();
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Il nome è obbligatorio.");
        }
        nome = nome.trim();
        if (!nome.matches(REGEX_NOME_COGNOME)) {
            throw new IllegalArgumentException("Il nome contiene numeri o caratteri speciali non consentiti.");
        }

        // Validazione Cognome
        String cognome = utente.getCognome();
        if (cognome == null || cognome.trim().isEmpty()) {
            throw new IllegalArgumentException("Il cognome è obbligatorio.");
        }
        cognome = cognome.trim();
        if (!cognome.matches(REGEX_NOME_COGNOME)) {
            throw new IllegalArgumentException("Il cognome contiene numeri o caratteri speciali non consentiti.");
        }

        // Validazione Username
        String username = utente.getUsername();
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Il nome utente è obbligatorio.");
        }
        if (username.trim().length() < 3) {
            throw new IllegalArgumentException("Il nome utente deve contenere almeno 3 caratteri.");
        }

        // Validazione Data di Nascita
        LocalDate dataNascita = utente.getDataNascita();
        if (dataNascita == null) {
            throw new IllegalArgumentException("La data di nascita è obbligatoria.");
        }
        LocalDate oggi = LocalDate.now();
        if (dataNascita.isAfter(oggi)) {
            throw new IllegalArgumentException("La data di nascita non può essere nel futuro.");
        }
        if (dataNascita.isBefore(oggi.minusYears(120))) {
            throw new IllegalArgumentException("La data di nascita non può essere antecedente a 120 anni fa.");
        }

        // Validazione Domicilio
        String domicilio = utente.getDomicilio();
        if (domicilio == null || domicilio.trim().isEmpty()) {
            throw new IllegalArgumentException("Il domicilio è obbligatorio.");
        }

        // Validazione Ruolo
        String ruolo = utente.getRuolo();
        if (ruolo == null || ruolo.trim().isEmpty()) {
            throw new IllegalArgumentException("Il ruolo dell'account è obbligatorio.");
        }

        // Validazione Password
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("La password è obbligatoria.");
        }
        if (password.length() < 8) {
            throw new IllegalArgumentException("La password deve contenere almeno 8 caratteri.");
        }
        if (!password.matches(".*[A-Za-z\\p{L}].*")) {
            throw new IllegalArgumentException("La password deve contenere almeno una lettera.");
        }
        if (!password.matches(".*[0-9].*")) {
            throw new IllegalArgumentException("La password deve contenere almeno un numero.");
        }
    }
}
