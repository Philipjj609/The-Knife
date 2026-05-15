package theknife.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import theknife.models.Utente;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller per il menu utente che appare dopo il login.
 * Mostra le informazioni di base dell'utente loggato e fornisce
 * azioni come il logout. Riceve un riferimento al HomeController
 * per eseguire operazioni legate alla sessione.
 *
 * @author Philip Jon Ji Ciuca
 * @version 1.0
 */
public class UserMenuController implements Initializable {
    @FXML
    private Label usernameLabel;
    @FXML
    private Label roleLabel;

    private HomeController homeController;
    private Utente currentUser;

    /**
     * Inizializzazione del controller (JavaFX lifecycle).
     *
     * @param location  URL della risorsa FXML (ignored)
     * @param resources ResourceBundle eventualmente fornito (ignored)
     * @since 1.0
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Inizializzazione se necessaria
    }

    /**
     * Imposta il riferimento al HomeController e aggiorna le informazioni utente.
     *
     * @param homeController Controller principale Home, must be non-null.
     * @since 1.0
     */
    public void setHomeController(HomeController homeController) {
        this.homeController = homeController;
        this.currentUser = homeController.getUtenteLoggato();
        updateLabels();
    }

    /**
     * Aggiorna le label della UI con i dati dell'utente corrente.
     * Non ha effetti collaterali oltre l'aggiornamento visivo.
     *
     * @since 1.0
     */
    private void updateLabels() {
        if (currentUser != null) {
            usernameLabel.setText(currentUser.getUsername());
            roleLabel.setText(currentUser.getRuolo());
        }
    }

    /**
     * Esegue il logout dell'utente delegando al HomeController e chiude
     * il menu corrente.
     *
     * @since 1.0
     */
    @FXML
    private void handleLogout() {
        if (homeController != null) {
            homeController.handleLogout();
        }
        // Chiudi la finestra del menu
        ((Stage) usernameLabel.getScene().getWindow()).close();
    }
}