package theknife.controllers;

import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import theknife.Main;
import theknife.models.Utente;

import java.time.LocalDate;

/**
 * Controller per la registrazione di un nuovo utente.
 * Valida i campi e invia la richiesta al server tramite ClientTK.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 * @version 2.0
 */

public class RegistrazioneController {

    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MIN_PASSWORD_LENGTH = 6;

    @FXML private TextField nomeField;
    @FXML private TextField cognomeField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private DatePicker dataNascitaPicker;
    @FXML private TextField domicilioField;
    @FXML private ComboBox<String> ruoloComboBox;
    @FXML private Label errorLabel;

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

    @FXML private void handleAnnulla() { AppNavigator.goBackOrClose(nomeField); }
    @FXML private void handleMouseEntered() {}
    @FXML private void handleMouseExited() {}

    private boolean requireNonEmpty(String value, String errorMsg) {
        if (value == null || value.trim().isEmpty()) { showError(errorMsg); return false; }
        return true;
    }

    private boolean requireMinLength(String value, int min, String errorMsg) {
        if (value.length() < min) { showError(errorMsg); return false; }
        return true;
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
