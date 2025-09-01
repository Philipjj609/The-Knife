package theknife.controllers;

/*
 * @author Philip Jon Ji Ciuca
 * @numero_matricola 761446
 * @sede CO
 * @version: 1.0
 * */

/**
 * Controller principale della schermata home dell'applicazione.
 * Gestisce il caricamento delle viste guest o della dashboard in base
 * allo stato di autenticazione, l'apertura della finestra di login e
 * la visualizzazione del menu utente.
 *
 * Fornisce metodi per impostare e rimuovere l'utente loggato.
 *
 * @author Philip Jon Ji Ciuca
 * @version 1.0
 */
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import theknife.models.Utente;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class HomeController implements Initializable {
    @FXML
    private StackPane contentPane;
    @FXML
    private Button loginButton;

    private Utente utenteLoggato;

    /**
     * Inizializzazione del controller: carica la vista guest all'avvio.
     *
     * @param location  URL della risorsa FXML (ignored)
     * @param resources ResourceBundle eventualmente fornito (ignored)
     * @since 1.0
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Carica il contenuto guest all'avvio
        loadGuestContent();
    }

    /**
     * Restituisce l'utente attualmente loggato.
     *
     * @return Utente loggato oppure null se nessun utente è autenticato.
     * @since 1.0
     */
    public Utente getUtenteLoggato() {
        return utenteLoggato;
    }

    /**
     * Imposta l'utente loggato, aggiorna l'interfaccia e carica il contenuto corretto.
     *
     * @param utente Oggetto Utente autenticato, must be non-null per il login.
     * @since 1.0
     */
    public void setUtenteLoggato(Utente utente) {
        this.utenteLoggato = utente;
        updateUI();
        loadContent();
    }

    /**
     * Aggiorna lo stato della UI (pulsante di login) in base allo stato di autenticazione.
     * Non effettua operazioni di I/O.
     *
     * @since 1.0
     */
    private void updateUI() {
        boolean isLoggedIn = (utenteLoggato != null);

        if (isLoggedIn) {
            // Cambia il testo del pulsante e l'azione quando l'utente è loggato
            loginButton.setText(utenteLoggato.getUsername());
            loginButton.setOnAction(e -> handleUserMenu());
        } else {
            loginButton.setText("Accedi");
            loginButton.setOnAction(e -> handleLogin());
        }
    }

    /**
     * Carica il contenuto corretto nella contentPane in base allo stato di login.
     *
     * @since 1.0
     */
    private void loadContent() {
        if (utenteLoggato != null) {
            loadDashboard();
        } else {
            loadGuestContent();
        }
    }

    // Apre la finestra di login modale
    /**
     * Apre la finestra di login in modalità modale e passa il riferimento a questo controller.
     * <p>
     * La finestra attende la chiusura prima di ritornare il controllo.
     * </p>
     *
     * @since 1.0
     */
    @FXML
    private void handleLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login.fxml"));
            Parent root = loader.load();

            // Applica il CSS alla finestra di login
            Scene scene = new Scene(root, 500, 650);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

            // Passa il riferimento a HomeController al LoginController
            LoginController loginController = loader.getController();
            loginController.setHomeController(this);

            Stage loginStage = new Stage();
            loginStage.initModality(Modality.APPLICATION_MODAL);
            loginStage.setTitle("Accedi - The Knife");

            // Imposta l'icona della finestra
            theknife.Main.setApplicationIcon(loginStage);

            loginStage.setScene(scene);
            loginStage.setMinWidth(450);
            loginStage.setMinHeight(600);
            loginStage.showAndWait(); // Attende la chiusura della finestra

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Apre un semplice menu utente in una finestra modale passando il riferimento a questo controller.
     * <p>
     * Se il file FXML non è disponibile viene effettuato il logout come fallback.
     * </p>
     *
     * @since 1.0
     */
    private void handleUserMenu() {
        // Crea un menu semplice per l'utente loggato
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/userMenu.fxml"));
            Parent root = loader.load();

            // Passa il riferimento al HomeController
            UserMenuController controller = loader.getController();
            controller.setHomeController(this);

            Scene scene = new Scene(root, 300, 200);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());

            Stage menuStage = new Stage();
            menuStage.initModality(Modality.APPLICATION_MODAL);
            menuStage.setTitle("Menu Utente");

            // Imposta l'icona della finestra
            theknife.Main.setApplicationIcon(menuStage);

            menuStage.setScene(scene);
            menuStage.setResizable(false);
            menuStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
            // Se il file non esiste, facciamo logout diretto per ora
            handleLogout();
        }
    }

    /**
     * Esegue il logout dell'utente corrente, resetta lo stato e aggiorna la UI.
     *
     * @since 1.0
     */
    public void handleLogout() {
        utenteLoggato = null;
        updateUI();
        loadContent();
    }

    /**
     * Carica la dashboard appropriata per il ruolo dell'utente loggato.
     * <p>
     * Passa l'utente corrente al controller della dashboard.
     * </p>
     *
     * @since 1.0
     */
    private void loadDashboard() {
        try {
            String viewPath = utenteLoggato.getRuolo().equals("Cliente")
                    ? "/views/dashboardCliente.fxml"
                    : "/views/dashboardRistoratore.fxml";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(viewPath));
            Parent dashboard = loader.load();

            // Passa l'utente corrente al controller della dashboard
            if (utenteLoggato.getRuolo().equals("Cliente")) {
                DashboardClienteController controller = loader.getController();
                controller.setCurrentUser(utenteLoggato);
            } else if (utenteLoggato.getRuolo().equals("Ristoratore")) {
                DashboardRistoratoreController controller = loader.getController();
                controller.setCurrentUser(utenteLoggato);
            }

            contentPane.getChildren().setAll(dashboard);

        } catch (IOException e) {
            e.printStackTrace();
            // Fallback alla vista guest se la dashboard non si carica
            loadGuestContent();
        }
    }

    /**
     * Carica la vista guest nella contentPane.
     *
     * @since 1.0
     */
    private void loadGuestContent() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/guestView.fxml"));
            Parent guestView = loader.load();
            contentPane.getChildren().setAll(guestView);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}