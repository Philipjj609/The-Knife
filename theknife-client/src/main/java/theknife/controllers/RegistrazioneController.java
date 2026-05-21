package theknife.controllers;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import theknife.Main;
import theknife.models.Utente;

import java.time.LocalDate;

/**
 * Controller JavaFX per la vista di registrazione di un nuovo utente.
 * Valida i campi inseriti nella form ed effettua la chiamata di rete asincrona per il salvataggio sul server.
 *
 * <p>Fa parte del pattern <b>MVC (Model-View-Controller)</b> come Controller.
 * Le operazioni di rete (come il controllo di esistenza dello username e il salvataggio) sono eseguite
 * in un thread di background tramite {@link Task} per mantenere la UI fluida.</p>
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public class RegistrazioneController {

    /** Lunghezza minima richiesta per lo username. */
    private static final int MIN_USERNAME_LENGTH = 3;

    /** Lunghezza minima richiesta per la password. */
    private static final int MIN_PASSWORD_LENGTH = 6;

    /** Campo di testo per l'inserimento del nome dell'utente. */
    @FXML private TextField nomeField;

    /** Campo di testo per l'inserimento del cognome dell'utente. */
    @FXML private TextField cognomeField;

    /** Campo di testo per l'inserimento dello username desiderato. */
    @FXML private TextField usernameField;

    /** Campo per l'inserimento della password. */
    @FXML private PasswordField passwordField;

    /** Picker per la selezione della data di nascita. */
    @FXML private DatePicker dataNascitaPicker;

    /** Campo di testo per l'inserimento del comune o dell'indirizzo di domicilio. */
    @FXML private TextField domicilioField;

    /** ComboBox per la selezione del ruolo dell'utente (CLIENTE o RISTORATORE). */
    @FXML private ComboBox<String> ruoloComboBox;

    /** Label per mostrare i messaggi di errore di validazione o di rete. */
    @FXML private Label errorLabel;

    /**
     * Inizializza la form di registrazione. Nasconde la label di errore
     * ed associa dei listener ai campi per far scomparire l'errore non appena l'utente modifica il testo.
     */
    @FXML
    private void initialize() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        nomeField.textProperty().addListener((obs, o, n) -> hideError());
        cognomeField.textProperty().addListener((obs, o, n) -> hideError());
        usernameField.textProperty().addListener((obs, o, n) -> hideError());
        passwordField.textProperty().addListener((obs, o, n) -> hideError());
        domicilioField.textProperty().addListener((obs, o, n) -> hideError());
        ruoloComboBox.valueProperty().addListener((obs, o, n) -> hideError());
    }

    /**
     * Gestisce l'evento di pressione sul pulsante di Registrazione.
     * Esegue la validazione formale di tutti i campi. Se corretti, avvia un thread
     * asincrono (tramite {@link Task}) che verifica l'unicità dello username
     * e salva il nuovo utente. Mostra un messaggio di successo e chiude la schermata in caso positivo.
     */
    @FXML
    private void handleRegistrati() {
        if (!requireNonEmpty(nomeField.getText(), "Il nome è obbligatorio!")) return;
        if (!requireNonEmpty(cognomeField.getText(), "Il cognome è obbligatorio!")) return;
        if (!requireNonEmpty(usernameField.getText(), "Il nome utente è obbligatorio!")) return;
        if (!requireMinLength(usernameField.getText().trim(), MIN_USERNAME_LENGTH,
                "Il nome utente deve contenere almeno " + MIN_USERNAME_LENGTH + " caratteri!")) return;
        if (!requireNonEmpty(passwordField.getText(), "La password è obbligatoria!")) return;
        if (!requireMinLength(passwordField.getText(), MIN_PASSWORD_LENGTH,
                "La password deve contenere almeno " + MIN_PASSWORD_LENGTH + " caratteri!")) return;
        if (!requireNonEmpty(domicilioField.getText(), "Il domicilio è obbligatorio!")) return;

        if (ruoloComboBox.getValue() == null) {
            showError("Seleziona il tipo di account!");
            return;
        }

        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        LocalDate dataNascita = dataNascitaPicker.getValue();

        Utente nuovoUtente = new Utente(
                nomeField.getText().trim(),
                cognomeField.getText().trim(),
                username,
                null,
                dataNascita,
                domicilioField.getText().trim(),
                ruoloComboBox.getValue());

        Task<Utente> task = new Task<>() {
            @Override
            protected Utente call() {
                if (Main.getClient().usernameEsiste(username))
                    throw new RuntimeException("Nome utente già in uso. Scegline un altro.");
                return Main.getClient().registraUtente(nuovoUtente, password);
            }
        };

        task.setOnSucceeded(e -> {
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Registrazione Completata");
            successAlert.setHeaderText("Account creato con successo!");
            successAlert.setContentText("Benvenuto in The Knife, " + nuovoUtente.getNome()
                    + "!\nOra puoi effettuare il login con le tue credenziali.");
            successAlert.showAndWait();
            AppNavigator.goBackOrClose(nomeField);
        });

        task.setOnFailed(e -> showError(task.getException().getMessage()));

        new Thread(task).start();
    }

    /**
     * Gestisce l'azione di annullamento ritornando alla schermata precedente.
     */
    @FXML private void handleAnnulla() { AppNavigator.goBackOrClose(nomeField); }

    /** Gestore vuoto per l'evento di ingresso del puntatore del mouse (se configurato in FXML). */
    @FXML private void handleMouseEntered() {}

    /** Gestore vuoto per l'evento di uscita del puntatore del mouse (se configurato in FXML). */
    @FXML private void handleMouseExited() {}

    /**
     * Verifica che il valore testuale non sia vuoto, altrimenti visualizza l'errore.
     *
     * @param value il valore testuale
     * @param errorMsg il messaggio d'errore da mostrare
     * @return true se non vuoto, false altrimenti
     */
    private boolean requireNonEmpty(String value, String errorMsg) {
        if (value == null || value.trim().isEmpty()) { showError(errorMsg); return false; }
        return true;
    }

    /**
     * Verifica che il valore testuale rispetti la lunghezza minima indicata.
     *
     * @param value il valore testuale
     * @param min la lunghezza minima
     * @param errorMsg il messaggio d'errore da mostrare
     * @return true se rispetta la lunghezza, false altrimenti
     */
    private boolean requireMinLength(String value, int min, String errorMsg) {
        if (value.length() < min) { showError(errorMsg); return false; }
        return true;
    }

    /**
     * Mostra il messaggio di errore impostando lo stato visivo della label.
     *
     * @param message il testo dell'errore
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    /**
     * Nasconde la label di errore liberando lo spazio nel layout.
     */
    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
