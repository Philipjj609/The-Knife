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
 * Controller per la finestra di login.
 * Gestisce l'autenticazione dell'utente tramite ClientTK.
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 * @version 2.0
 */

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;

    private HomeController homeController;

    public void setHomeController(HomeController homeController) {
        this.homeController = homeController;
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Inserisci username e password.");
            return;
        }

        Task<Optional<Utente>> task = new Task<>() {
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

    @FXML
    private void handleBack() {
        AppNavigator.goBackOrClose(usernameField);
    }
}
