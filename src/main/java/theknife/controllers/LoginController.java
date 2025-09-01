package theknife.controllers;

import java.io.*;

/*
 * @author Philip Jon Ji Ciuca
 * @numero_matricola 761446
 * @sede CO
 * @version: 1.0
 * */

/**
 * Controller per la finestra di login.
 * Gestisce l'autenticazione dell'utente e l'apertura della finestra di registrazione.
 * Riceve un riferimento al HomeController per notificare l'avvenuto login.
 *
 * @author Philip Jon Ji Ciuca
 * @version 1.0
 */
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import theknife.models.Utente;
import theknife.utils.AuthManager;

public class LoginController {
    /** Campo input per lo username (FXML). */
    @FXML
    private TextField usernameField;
    /** Campo input per la password (FXML). */
    @FXML
    private PasswordField passwordField;
    /** Label per mostrare messaggi di errore (FXML). */
    @FXML
    private Label errorLabel;

    /** Riferimento al controller della Home per notifiche sul login. */
    private HomeController homeController;

    /**
     * Imposta il riferimento al HomeController.
     *
     * @param homeController Controller principale della Home, must be non-null.
     * @since 1.0
     */
    public void setHomeController(HomeController homeController) {
        this.homeController = homeController;
    }

    /**
     * Tenta di autenticare l'utente con le credenziali inserite.
     * <p>
     * Se l'autenticazione ha successo notifica il HomeController e chiude
     * la finestra di login. In caso di fallimento mostra un messaggio di errore.
     * </p>
     *
     * @since 1.0
     */
    @FXML
    private void handleLogin() {
        Utente utente = AuthManager.autenticaUtente(
                usernameField.getText(),
                passwordField.getText());

        if (utente != null) {
            homeController.setUtenteLoggato(utente); // Aggiorna HomeController
            ((Stage) usernameField.getScene().getWindow()).close(); // Chiude la finestra di login
        } else {
            errorLabel.setText("Username o password errati!");
        }
    }

    /**
     * Apre la finestra di registrazione come finestra separata.
     * <p>
     * Imposta dimensioni, stile e icona della finestra.
     * </p>
     *
     * @since 1.0
     */
    @FXML
    private void handleRegistrazione() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/registrazione.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Registrazione - The Knife");

            // Imposta l'icona della finestra
            theknife.Main.setApplicationIcon(stage);

            Scene scene = new Scene(root, 700, 800);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            stage.setScene(scene);
            stage.setMinWidth(650);
            stage.setMinHeight(750);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}