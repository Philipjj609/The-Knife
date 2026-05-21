package theknife.controllers;

/*
 * @author Philip Jon Ji Ciuca
 * @numero_matricola 761446
 * @sede CO
 * @version: 1.0
 * */

/**
 * Controller principale della schermata Home dell'applicazione "The Knife".
 * Fa parte del pattern <b>MVC (Model-View-Controller)</b> nel ruolo di Controller.
 *
 * <p>Gestisce lo stato complessivo di navigazione e visualizzazione del client:
 * all'avvio mostra una schermata di benvenuto programmata e permette all'utente
 * di autenticarsi (aprendo il login in modalità modale) o accedere come ospite.
 * In base al ruolo dell'utente loggato (Cliente o Ristoratore), coordina il caricamento
 * dinamico delle rispettive dashboard nel contenitore centrale {@link StackPane},
 * delegando la navigazione ad {@link AppNavigator}.</p>
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 * @version 1.0
 */
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import theknife.models.Role;
import theknife.models.Utente;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    /** Contenitore StackPane principale in cui vengono caricate dinamicamente le varie viste dell'applicazione. */
    @FXML
    private StackPane contentPane;

    /** La barra di navigazione superiore (navbar). */
    @FXML
    private HBox navbar;

    /** Pulsante posizionato nella barra superiore per accedere, visualizzare il profilo o effettuare il logout. */
    @FXML
    private Button loginButton;

    /** Pulsante per tornare alla dashboard dal flusso di esplorazione ristoranti. */
    @FXML
    private Button tornaDashboardButton;

    /** Oggetto contenente i dettagli dell'utente attualmente autenticato. Pari a null se non autenticato. */
    private Utente utenteLoggato;

    /** Flag che indica se l'applicazione è in esecuzione in modalità ospite (non autenticato). */
    private boolean guestMode;

    /**
     * Inizializzazione del controller: carica la vista guest all'avvio.
     *
     * @param location  URL della risorsa FXML (ignored)
     * @param resources ResourceBundle eventualmente fornito (ignored)
     * @since 1.0
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        AppNavigator.initialize(contentPane, this);

        contentPane.getChildren().addListener((javafx.collections.ListChangeListener.Change<? extends javafx.scene.Node> c) -> {
            updateTornaDashboardButtonVisibility();
        });

        loadWelcomeContent();
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
        this.guestMode = false;
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
        } else if (guestMode) {
            loadGuestContent();
        } else {
            loadWelcomeContent();
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
            AppNavigator.show("/views/login.fxml", (LoginController controller) ->
                    controller.setHomeController(this));
        } catch (IOException e) {
            showError("Impossibile caricare il login.");
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
    /**
     * Mostra o nasconde la barra di navigazione superiore (navbar) e ne aggiorna il posizionamento.
     *
     * @param show true se la navbar deve essere visibile e occupare spazio, false altrimenti
     */
    public void showNavbar(boolean show) {
        if (navbar != null) {
            navbar.setVisible(show);
            navbar.setManaged(show);
        }
    }

    /**
     * Imposta o rimuove il padding del pannello principale per consentire il layout a schermo intero.
     *
     * @param pad true per ripristinare il padding a 32px, false per azzerarlo
     */
    public void setContentPanePadding(boolean pad) {
        if (contentPane != null) {
            if (pad) {
                contentPane.setStyle("-fx-padding: 32;");
            } else {
                contentPane.setStyle("-fx-padding: 0;");
            }
        }
    }

    private void handleUserMenu() {
        try {
            showNavbar(false);
            setContentPanePadding(false);
            AppNavigator.show("/views/userMenu.fxml", (UserMenuController controller) ->
                    controller.setHomeController(this));
        } catch (IOException e) {
            showNavbar(true);
            setContentPanePadding(true);
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
        guestMode = false;
        AppNavigator.clearHistory();
        updateUI();
        loadContent();
    }

    /**
     * Gestisce l'accesso dell'utente come ospite (non autenticato).
     * Imposta il flag guestMode a true, aggiorna la barra superiore della UI
     * e carica la vista guest.
     */
    private void handleGuestAccess() {
        guestMode = true;
        updateUI();
        loadGuestContent();
    }

    /**
     * Carica la dashboard appropriata in base al ruolo dell'utente loggato.
     *
     * <p>In caso di {@link Role#CLIENTE}, viene caricata la vista {@code dashboardCliente.fxml}
     * configurando il relativo {@link DashboardClienteController}. In caso di {@link Role#RISTORATORE},
     * viene caricata {@code dashboardRistoratore.fxml} configurando {@link DashboardRistoratoreController}.
     * Qualora il caricamento della vista fallisca lanciando una {@link IOException}, mostra
     * un alert di errore ed esegue il fallback caricando la vista guest.</p>
     */
    private void loadDashboard() {
        try {
            showNavbar(true);
            setContentPanePadding(true);
            Role ruolo = utenteLoggato.getRuoloEnum();
            String viewPath = (ruolo == Role.CLIENTE)
                    ? "/views/dashboardCliente.fxml"
                    : "/views/dashboardRistoratore.fxml";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(viewPath));
            Parent dashboard = loader.load();

            if (ruolo == Role.CLIENTE) {
                DashboardClienteController controller = loader.getController();
                controller.setCurrentUser(utenteLoggato);
            } else if (ruolo == Role.RISTORATORE) {
                DashboardRistoratoreController controller = loader.getController();
                controller.setCurrentUser(utenteLoggato);
            }

            contentPane.getChildren().setAll(dashboard);
            AppNavigator.clearHistory();

        } catch (IOException e) {
            showError("Impossibile caricare la dashboard.");
            loadGuestContent();
        }
    }

    /**
     * Carica nel pannello centrale dell'interfaccia la vista di ricerca unificata ({@code esploraRistoranti.fxml})
     * configurata in modalità ospite (senza utente loggato e con controlli dedicati nascosti).
     * Qualora il caricamento fallisca a causa di una {@link IOException}, mostra un alert di errore.
     */
    private void loadGuestContent() {
        try {
            showNavbar(true);
            setContentPanePadding(true);
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/esploraRistoranti.fxml"));
            Parent searchView = loader.load();
            
            EsploraRistorantiController controller = loader.getController();
            controller.setSessionState(false, null);

            contentPane.getChildren().setAll(searchView);
            AppNavigator.clearHistory();
        } catch (IOException e) {
            showError("Impossibile caricare la vista principale.");
        }
    }

    /**
     * Carica e crea programmaticamente (via codice JavaFX) il contenuto di benvenuto iniziale
     * contenente il titolo, il sottotitolo e i pulsanti per accedere o continuare come ospite.
     */
    private void loadWelcomeContent() {
        showNavbar(true);
        setContentPanePadding(true);
        VBox panel = new VBox(18);
        panel.setAlignment(Pos.CENTER);
        panel.setMaxWidth(520);
        panel.getStyleClass().add("card");

        Text title = new Text("Benvenuto in The Knife");
        title.getStyleClass().add("title");

        Label subtitle = new Label("Accedi con il tuo account oppure continua come ospite.");
        subtitle.getStyleClass().addAll("body-text", "text-muted");
        subtitle.setWrapText(true);
        subtitle.setAlignment(Pos.CENTER);

        Button login = new Button("Accedi");
        login.getStyleClass().addAll("button", "primary");
        login.setPrefWidth(180);
        login.setOnAction(e -> handleLogin());

        Button guest = new Button("Entra come ospite");
        guest.getStyleClass().addAll("button", "outline");
        guest.setPrefWidth(180);
        guest.setOnAction(e -> handleGuestAccess());

        HBox actions = new HBox(12, login, guest);
        actions.setAlignment(Pos.CENTER);

        panel.getChildren().addAll(title, subtitle, actions);
        contentPane.getChildren().setAll(panel);
        AppNavigator.clearHistory();
    }

    /**
     * Mostra una finestra di dialogo di tipo {@link Alert.AlertType#ERROR} con un messaggio personalizzato.
     *
     * @param message il messaggio descrittivo dell'errore da visualizzare.
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Aggiorna la visibilità del pulsante "Torna alla Dashboard" nella navbar in base al pannello visualizzato.
     */
    private void updateTornaDashboardButtonVisibility() {
        if (contentPane == null || contentPane.getChildren().isEmpty()) {
            setTornaDashboardButtonVisible(false);
            return;
        }
        javafx.scene.Node currentView = contentPane.getChildren().get(0);
        String id = currentView.getId();

        boolean isClientLogged = (utenteLoggato != null && utenteLoggato.getRuoloEnum() == Role.CLIENTE);
        boolean showButton = isClientLogged && 
            ("esploraRistorantiRoot".equals(id) || "dettaglioRistoranteRoot".equals(id));

        setTornaDashboardButtonVisible(showButton);
    }

    /**
     * Mostra o nasconde il pulsante per tornare alla dashboard.
     *
     * @param visible true per mostrarlo, false altrimenti
     */
    public void setTornaDashboardButtonVisible(boolean visible) {
        if (tornaDashboardButton != null) {
            tornaDashboardButton.setVisible(visible);
            tornaDashboardButton.setManaged(visible);
        }
    }

    /**
     * Gestisce l'azione di click sul pulsante "Torna alla Dashboard".
     */
    @FXML
    private void handleTornaDashboard() {
        loadDashboard();
    }
}
