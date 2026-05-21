package theknife.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import theknife.models.Utente;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Controller JavaFX per la vista del menu utente personalizzato (User Menu) dell'applicazione "The Knife".
 * Mostra le informazioni dell'utente autenticato (come lo username e il ruolo) e permette di effettuare il logout.
 *
 * <p>Fa parte del pattern <b>MVC (Model-View-Controller)</b> come Controller.
 * Le operazioni grafiche e di delegazione avvengono sul JavaFX Application Thread.</p>
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public class UserMenuController implements Initializable {

    /** Label per la visualizzazione dello username dell'utente loggato. */
    @FXML private Label usernameLabel;

    /** Label per la visualizzazione del ruolo (es. CLIENTE, RISTORATORE) dell'utente loggato. */
    @FXML private Label roleLabel;

    /** Riferimento al controller della home principale. */
    private HomeController homeController;

    /** L'utente correntemente autenticato nella sessione. */
    private Utente currentUser;

    /**
     * Metodo di inizializzazione richiamato automaticamente da JavaFX dopo il caricamento del file FXML.
     *
     * @param location l'URL utilizzato per risolvere i percorsi relativi dell'oggetto radice, o null
     * @param resources le risorse utilizzate per localizzare l'oggetto radice, o null
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Inizializzazione se necessaria
    }

    /**
     * Associa il controller principale Home e aggiorna le informazioni visive dell'utente.
     *
     * @param homeController il {@link HomeController} principale, deve essere diverso da null
     */
    public void setHomeController(HomeController homeController) {
        this.homeController = homeController;
        this.currentUser = homeController.getUtenteLoggato();
        updateLabels();
    }

    /**
     * Aggiorna il testo delle label grafiche con le informazioni dell'utente correntemente connesso.
     */
    private void updateLabels() {
        if (currentUser != null) {
            usernameLabel.setText(currentUser.getUsername());
            roleLabel.setText(currentUser.getRuolo());
        }
    }

    /**
     * Gestisce l'azione di logout dell'utente delegando l'operazione al controller principale Home.
     */
    @FXML
    private void handleLogout() {
        if (homeController != null) {
            homeController.handleLogout();
        }
    }

    /**
     * Ritorna alla schermata precedente o chiude la vista.
     */
    @FXML
    private void handleBack() {
        AppNavigator.goBackOrClose(usernameLabel);
    }
}
