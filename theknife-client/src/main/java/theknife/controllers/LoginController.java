package theknife.controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import theknife.Main;
import theknife.models.Utente;

import java.io.IOException;
import java.util.Optional;

/**
 * Controller JavaFX per la vista di Login dell'applicazione "The Knife".
 * Gestisce l'interazione per l'autenticazione degli utenti.
 *
 * <p>Fa parte del pattern <b>MVC (Model-View-Controller)</b> come Controller.
 * Esegue le chiamate di autenticazione in modo asincrono tramite un thread di background
 * (usando {@link Task}) per evitare il blocco del JavaFX Application Thread,
 * aggiornando l'interfaccia grafica nei callback di successo e fallimento.</p>
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public class LoginController {

    /**
     * Costruttore di default per {@link LoginController}.
     * Necessario per l'inizializzazione tramite FXML loader.
     */
    public LoginController() {}

    /** Campo di testo per l'inserimento dello username. */
    @FXML private TextField usernameField;

    /** Campo per l'inserimento della password. */
    @FXML private PasswordField passwordField;

    /** Label per la visualizzazione di messaggi di errore nella UI. */
    @FXML private Label errorLabel;

    /** Controller della home page principale, utilizzato per aggiornare lo stato dell'utente loggato. */
    private HomeController homeController;

    /**
     * Associa il controller della home principale.
     *
     * @param homeController l'istanza del {@link HomeController}
     */
    public void setHomeController(HomeController homeController) {
        this.homeController = homeController;
    }

    /**
     * Gestisce l'evento di pressione del pulsante Accedi.
     * Valida l'input locale ed esegue il tentativo di login sul server tramite {@link theknife.client.ClientTK#login(String, String)}
     * all'interno di un thread separato. I risultati vengono poi applicati sul JavaFX Application Thread.
     */
    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Inserisci username e password.");
            return;
        }

        Task<Optional<Utente>> task = new Task<>() {
            /**
             * Esegue il tentativo di autenticazione sul server in un thread separato.
             *
             * @return un Optional contenente l'utente se il login ha successo, altrimenti vuoto
             */
            @Override
            protected Optional<Utente> call() {
                return Main.getClient().login(username, password);
            }
        };

        task.setOnSucceeded(e -> {
            Optional<Utente> result = task.getValue();
            if (result.isPresent()) {
                homeController.setUtenteLoggato(result.get());
            } else {
                errorLabel.setText("Username o password errati!");
            }
        });

        task.setOnFailed(e -> {
            errorLabel.setText("Errore di connessione al server.");
            task.getException().printStackTrace();
        });

        new Thread(task).start();
    }

    /**
     * Gestisce l'evento di navigazione verso la schermata di registrazione di un nuovo utente.
     */
    @FXML
    private void handleRegistrazione() {
        try {
            AppNavigator.show("/views/registrazione.fxml", null);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Errore");
            alert.setHeaderText(null);
            alert.setContentText("Impossibile caricare la registrazione.");
            alert.showAndWait();
        }
    }

    /**
     * Gestisce l'evento di ritorno alla schermata o chiusura del dialogo.
     */
    @FXML
    private void handleBack() {
        AppNavigator.goBackOrClose(usernameField);
    }
}
