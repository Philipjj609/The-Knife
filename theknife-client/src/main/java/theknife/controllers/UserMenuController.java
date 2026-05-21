package theknife.controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import theknife.Main;
import theknife.models.Utente;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

/**
 * Controller JavaFX per la vista del menu utente personalizzato (User Menu) dell'applicazione "The Knife".
 * Mostra le informazioni dell'utente autenticato (come lo username e il ruolo) e permette di
 * modificare i dati personali (Nome, Cognome, Data di Nascita, Domicilio) o effettuare il logout.
 *
 * <p>Fa parte del pattern <b>MVC (Model-View-Controller)</b> come Controller.
 * Le operazioni grafiche e di delegazione avvengono sul JavaFX Application Thread.</p>
 */
public class UserMenuController implements Initializable {

    @FXML private Label usernameTitleLabel;
    @FXML private Label roleTitleLabel;
    @FXML private TextField usernameField;
    @FXML private TextField ruoloField;
    @FXML private TextField nomeField;
    @FXML private TextField cognomeField;
    @FXML private DatePicker dataNascitaPicker;
    @FXML private TextField domicilioField;

    @FXML private Button editButton;
    @FXML private Button saveButton;
    @FXML private Button cancelButton;

    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    /** Riferimento al controller della home principale. */
    private HomeController homeController;

    /** L'utente correntemente autenticato nella sessione. */
    private Utente currentUser;

    /**
     * Metodo di inizializzazione richiamato automaticamente da JavaFX dopo il caricamento del file FXML.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setEditable(false);
    }

    /**
     * Associa il controller principale Home e aggiorna le informazioni visive dell'utente.
     *
     * @param homeController il {@link HomeController} principale, deve essere diverso da null
     */
    public void setHomeController(HomeController homeController) {
        this.homeController = homeController;
        this.currentUser = homeController.getUtenteLoggato();
        updateFields();
    }

    /**
     * Aggiorna i campi grafici con le informazioni dell'utente correntemente connesso.
     */
    private void updateFields() {
        if (currentUser != null) {
            usernameTitleLabel.setText(currentUser.getUsername());
            roleTitleLabel.setText(currentUser.getRuolo().toUpperCase());
            usernameField.setText(currentUser.getUsername());
            ruoloField.setText(currentUser.getRuolo());
            nomeField.setText(currentUser.getNome());
            cognomeField.setText(currentUser.getCognome());
            dataNascitaPicker.setValue(currentUser.getDataNascita());
            domicilioField.setText(currentUser.getDomicilio());
        }
    }

    /**
     * Abilita o disabilita i campi di input e mostra/nasconde i pulsanti di azione.
     */
    private void setEditable(boolean editable) {
        nomeField.setDisable(!editable);
        cognomeField.setDisable(!editable);
        dataNascitaPicker.setEditable(false);
        dataNascitaPicker.setMouseTransparent(!editable);
        dataNascitaPicker.setFocusTraversable(editable);
        domicilioField.setDisable(!editable);

        editButton.setVisible(!editable);
        editButton.setManaged(!editable);
        saveButton.setVisible(editable);
        saveButton.setManaged(editable);
        cancelButton.setVisible(editable);
        cancelButton.setManaged(editable);
    }

    /**
     * Gestisce l'azione di click sul pulsante "Modifica Profilo".
     */
    @FXML
    private void handleEdit() {
        clearMessages();
        setEditable(true);
    }

    /**
     * Gestisce l'azione di click sul pulsante "Annulla".
     */
    @FXML
    private void handleCancel() {
        clearMessages();
        updateFields();
        setEditable(false);
    }

    /**
     * Gestisce l'azione di click sul pulsante "Salva Modifiche".
     */
    @FXML
    private void handleSave() {
        clearMessages();
        String nome = nomeField.getText().trim();
        String cognome = cognomeField.getText().trim();
        LocalDate dataNascita = dataNascitaPicker.getValue();
        String domicilio = domicilioField.getText().trim();

        if (nome.isEmpty() || cognome.isEmpty()) {
            errorLabel.setText("❌ Nome e Cognome sono campi obbligatori.");
            return;
        }

        Utente updated = new Utente(
                currentUser.getId(),
                nome,
                cognome,
                currentUser.getUsername(),
                null,
                dataNascita,
                domicilio,
                currentUser.getRuolo()
        );

        saveButton.setDisable(true);
        cancelButton.setDisable(true);

        Task<Utente> task = new Task<>() {
            @Override
            protected Utente call() throws Exception {
                return Main.getClient().modificaUtente(updated);
            }
        };

        task.setOnSucceeded(e -> {
            saveButton.setDisable(false);
            cancelButton.setDisable(false);
            currentUser = task.getValue();
            if (homeController != null) {
                homeController.setUtenteLoggato(currentUser);
            }
            updateFields();
            setEditable(false);
            successLabel.setText("✅ Dati aggiornati con successo!");
        });

        task.setOnFailed(e -> {
            saveButton.setDisable(false);
            cancelButton.setDisable(false);
            errorLabel.setText("❌ Errore durante l'aggiornamento: " + task.getException().getMessage());
        });

        new Thread(task).start();
    }

    /**
     * Gestisce l'azione di logout dell'utente delegando l'operazione al controller principale Home.
     */
    @FXML
    private void handleLogout() {
        if (homeController != null) {
            AppNavigator.goBackOrClose(usernameTitleLabel);
            homeController.handleLogout();
        }
    }

    /**
     * Ritorna alla schermata precedente o chiude la vista.
     */
    @FXML
    private void handleBack() {
        AppNavigator.goBackOrClose(usernameTitleLabel);
    }

    private void clearMessages() {
        errorLabel.setText("");
        successLabel.setText("");
    }
}
