package theknife.controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import theknife.Main;
import theknife.models.Utente;

import java.io.IOException;
import java.util.Optional;

/**
 * Controller per la finestra di login.
 * Gestisce l'autenticazione dell'utente tramite ClientTK.
 *
 * @author Philip Jon Ji Ciuca
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
                ((Stage) usernameField.getScene().getWindow()).close();
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/registrazione.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Registrazione - The Knife");
            Main.setApplicationIcon(stage);

            Scene scene = new Scene(root, 700, 800);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            stage.setScene(scene);
            stage.setMinWidth(650);
            stage.setMinHeight(750);
            stage.show();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Errore");
            alert.setHeaderText(null);
            alert.setContentText("Impossibile aprire la finestra di registrazione.");
            alert.showAndWait();
        }
    }
}
