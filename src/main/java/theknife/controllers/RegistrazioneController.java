package theknife.controllers;

/*
 * @author Philip Jon Ji Ciuca
 * @numero_matricola 761446
 * @sede CO
 * @version: 1.0
 * */

/**
 * Controller per la registrazione di un nuovo utente.
 * Gestisce la validazione dei campi, la creazione dell'oggetto Utente
 * e il salvataggio tramite FileManager/AuthManager. Mostra messaggi di
 * errore o successo all'utente.
 *
 * @author Philip Jon Ji Ciuca
 * @version 1.0
 */
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import theknife.models.Utente;
import theknife.utils.AuthManager;
import theknife.utils.FileManager;
import org.mindrot.jbcrypt.BCrypt;
import java.time.format.DateTimeFormatter;

public class RegistrazioneController {

    private static final int MIN_USERNAME_LENGTH = 3;
    private static final int MIN_PASSWORD_LENGTH = 6;
    /** Campo input per il nome dell'utente (FXML). */
    @FXML
    private TextField nomeField;
    /** Campo input per il cognome dell'utente (FXML). */
    @FXML
    private TextField cognomeField;
    /** Campo input per lo username dell'utente (FXML). */
    @FXML
    private TextField usernameField;
    /** Campo input per la password (FXML). */
    @FXML
    private PasswordField passwordField;
    /** DatePicker per la data di nascita (FXML). */
    @FXML
    private DatePicker dataNascitaPicker;
    /** Campo input per il domicilio (FXML). */
    @FXML
    private TextField domicilioField;
    /** ComboBox per la selezione del ruolo (FXML). */
    @FXML
    private ComboBox<String> ruoloComboBox;
    /** Label per mostrare errori all'utente (FXML). */
    @FXML
    private Label errorLabel;

    /**
     * Gestisce l'azione di registrazione quando l'utente preme il pulsante.
     * <p>
     * Esegue la validazione dei campi, verifica l'esistenza dell'username,
     * crea l'oggetto Utente con password hashata e lo salva tramite FileManager.
     * In caso di successo mostra un Alert informativo e chiude la finestra.
     * </p>
     *
     * @since 1.0
     * @author Philip Jon Ji Ciuca
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

        if (AuthManager.usernameEsistente(usernameField.getText().trim())) {
            showError("Nome utente già in uso. Scegline un altro.");
            return;
        }

        try {
            String dataNascita = dataNascitaPicker.getValue() != null
                    ? dataNascitaPicker.getValue().format(DateTimeFormatter.ISO_DATE)
                    : "";

            Utente nuovoUtente = new Utente(
                    nomeField.getText().trim(),
                    cognomeField.getText().trim(),
                    usernameField.getText().trim(),
                    BCrypt.hashpw(passwordField.getText(), BCrypt.gensalt()),
                    dataNascita,
                    domicilioField.getText().trim(),
                    ruoloComboBox.getValue());

            FileManager.salvaUtente(nuovoUtente);
            AuthManager.invalidateCache();

            // Mostra successo e chiudi
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Registrazione Completata");
            successAlert.setHeaderText("Account creato con successo!");
            successAlert.setContentText("Benvenuto in The Knife, " + nuovoUtente.getNome()
                    + "!\nOra puoi effettuare il login con le tue credenziali.");
            successAlert.showAndWait();

            // Chiudi la finestra
            ((Stage) nomeField.getScene().getWindow()).close();

        } catch (Exception e) {
            showError("❌ Errore durante la registrazione. Riprova.");
            e.printStackTrace();
        }
    }

    /**
     * Chiude la finestra di registrazione senza salvare.
     *
     * @since 1.0
     */
    @FXML
    private void handleAnnulla() {
        ((Stage) nomeField.getScene().getWindow()).close();
    }

    /**
     * Effetto al passaggio del mouse sul pulsante principale (placeholder).
     * <p>
     * Non ha effetti collaterali attualmente ma è mantenuto per eventuali
     * miglioramenti della UI (CSS, animazioni, ecc.).
     * </p>
     *
     * @since 1.0
     */
    @FXML
    private void handleMouseEntered() {
        // Effetto hover per il pulsante principale
    }

    /**
     * Rimuove l'effetto hover quando il mouse esce dall'area del pulsante.
     *
     * @since 1.0
     */
    @FXML
    private void handleMouseExited() {
        // Rimuove effetto hover
    }

    /**
     * Metodo di inizializzazione chiamato automaticamente da JavaFX.
     * <p>
     * Inizializza lo stato della UI relativo agli errori e aggiunge listener
     * per nascondere i messaggi di errore quando l'utente modifica i campi.
     * </p>
     *
     * @since 1.0
     */
    @FXML
    private void initialize() {
        // Migliora la gestione degli errori
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);

        // Aggiungi listener per nascondere errori quando l'utente modifica i campi
        nomeField.textProperty().addListener((observable, oldValue, newValue) -> hideError());
        cognomeField.textProperty().addListener((observable, oldValue, newValue) -> hideError());
        usernameField.textProperty().addListener((observable, oldValue, newValue) -> hideError());
        passwordField.textProperty().addListener((observable, oldValue, newValue) -> hideError());
        domicilioField.textProperty().addListener((observable, oldValue, newValue) -> hideError());
        ruoloComboBox.valueProperty().addListener((observable, oldValue, newValue) -> hideError());
    }

    private boolean requireNonEmpty(String value, String errorMsg) {
        if (value == null || value.trim().isEmpty()) {
            showError(errorMsg);
            return false;
        }
        return true;
    }

    private boolean requireMinLength(String value, int min, String errorMsg) {
        if (value.length() < min) {
            showError(errorMsg);
            return false;
        }
        return true;
    }

    /**
     * Mostra un messaggio di errore nella label dedicata.
     *
     * @param message Messaggio di errore da mostrare, must be non-null.
     * @since 1.0
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    /**
     * Nasconde la label di errore.
     *
     * @since 1.0
     */
    private void hideError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}