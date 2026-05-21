package theknife.controllers;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import theknife.Main;
import theknife.client.ui.widgets.MultiSelectComboBox;
import theknife.models.Ristorante;
import theknife.models.Utente;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller JavaFX per la vista di aggiunta di un nuovo ristorante.
 * Fa parte del pattern <b>MVC (Model-View-Controller)</b> nel ruolo di Controller.
 *
 * <p>Gestisce la convalida locale dei campi del modulo (nome, cucina, prezzo, coordinate, ecc.)
 * e invia la richiesta di salvataggio al server tramite la Facade {@link ClientTK} (esposta da {@code Main.getClient()})
 * all'interno di un thread secondario tramite {@link Task}. Al termine dell'inserimento con successo,
 * aggiorna i dati del controller genitore {@link DashboardRistoratoreController} e pianifica la chiusura
 * automatica della vista tramite un thread temporizzato che richiama {@link Platform#runLater(Runnable)}.</p>
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 * @version 2.0
 */
public class AggiungiRistoranteController implements Initializable {

    /** Campo di testo per l'inserimento del nome del ristorante. */
    @FXML private TextField nomeField;

    /** ComboBox per la scelta delle tipologie di cucina. */
    @FXML private MultiSelectComboBox<String> cucinaComboBox;

    /** ComboBox per la scelta della fascia di prezzo (es. €, €€, etc.). */
    @FXML private ComboBox<String> prezzoCombo;

    /** Campo di testo per l'indirizzo stradale del ristorante. */
    @FXML private TextField indirizzoField;

    /** Campo di testo per la città del ristorante. */
    @FXML private TextField cittaField;

    /** Campo di testo per la nazione del ristorante. */
    @FXML private TextField nazioneField;

    /** Campo di testo per la latitudine geografica. */
    @FXML private TextField latitudineField;

    /** Label errore inline per la latitudine. */
    @FXML private Label latitudineErrorLabel;

    /** Campo di testo per la longitudine geografica. */
    @FXML private TextField longitudineField;

    /** Label errore inline per la longitudine. */
    @FXML private Label longitudineErrorLabel;

    /** Campo di testo per il numero di telefono del ristorante. */
    @FXML private TextField telefonoField;

    /** Campo di testo per l'indirizzo del sito web. */
    @FXML private TextField sitoWebField;

    /** Area di testo per una descrizione approfondita del ristorante (minimo 50 caratteri). */
    @FXML private TextArea descrizioneArea;

    /** ComboBox per la scelta dei servizi offerti. */
    @FXML private MultiSelectComboBox<String> serviceComboBox;

    /** ComboBox per specificare un eventuale premio Michelin (es. stelle o Bib Gourmand). */
    @FXML private ComboBox<String> premioCombo;

    /** CheckBox indicante la presenza o meno della Stella Verde Michelin. */
    @FXML private CheckBox stellaVerdeCheck;

    /** CheckBox indicante se il ristorante offre il servizio di delivery. */
    @FXML private CheckBox deliveryCheck;

    /** CheckBox indicante se è supportata la prenotazione online dei tavoli. */
    @FXML private CheckBox prenotazioneOnlineCheck;

    /** Label per la visualizzazione di messaggi d'errore o convalida fallita. */
    @FXML private Label errorLabel;

    /** Label per la visualizzazione dei messaggi di avvenuta operazione. */
    @FXML private Label successLabel;

    /** L'utente ristoratore attualmente autenticato che sta inserendo il ristorante. */
    private Utente currentUser;

    /** Il controller genitore della dashboard del ristoratore, per aggiornare l'elenco ristoranti. */
    private DashboardRistoratoreController parentController;

    /**
     * Inizializza il controller JavaFX. Popola le ComboBox con le tipologie di cucina,
     * fasce di prezzo e riconoscimenti Michelin. Inoltre, imposta dei filtri di input (regex)
     * sui campi numerici per latitudine, longitudine e telefono.
     *
     * @param location l'URL di localizzazione del file FXML
     * @param resources il bundle delle risorse localizzate
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        prezzoCombo.setItems(FXCollections.observableArrayList("€", "€€", "€€€", "€€€€"));

        premioCombo.setItems(FXCollections.observableArrayList(
                "Nessuno", "Bib Gourmand", "1 Star", "2 Stars", "3 Stars"));
        premioCombo.setValue("Nessuno");

        // Load cuisines and services dynamically from database in background thread
        Task<Void> loadDataTask = new Task<>() {
            private List<String> cucineList;
            private List<String> serviziList;

            @Override
            protected Void call() throws Exception {
                cucineList = Main.getClient().getCucine();
                serviziList = Main.getClient().getServizi();
                return null;
            }

            @Override
            protected void succeeded() {
                if (cucineList != null) {
                    cucinaComboBox.setItems(FXCollections.observableArrayList(cucineList));
                }
                if (serviziList != null) {
                    serviceComboBox.setItems(FXCollections.observableArrayList(serviziList));
                }
            }

            @Override
            protected void failed() {
                System.err.println("Errore nel caricamento di cucine e servizi: " + getException().getMessage());
            }
        };
        new Thread(loadDataTask).start();

        latitudineField.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("-?\\d*\\.?\\d*")) latitudineField.setText(o);
        });
        longitudineField.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("-?\\d*\\.?\\d*")) longitudineField.setText(o);
        });
        telefonoField.textProperty().addListener((obs, o, n) -> {
            if (!n.matches("[\\d\\s\\+\\-\\(\\)]*")) telefonoField.setText(o);
        });
    }

    /**
     * Associa l'utente correntemente loggato al controller.
     *
     * @param user l'utente ristoratore proprietario
     */
    public void setCurrentUser(Utente user) { this.currentUser = user; }

    /**
     * Associa il controller della dashboard genitore per poter invocarne il refresh al successo del salvataggio.
     *
     * @param parent il {@link DashboardRistoratoreController} genitore
     */
    public void setParentController(DashboardRistoratoreController parent) { this.parentController = parent; }

    /**
     * Gestisce l'evento associato al click sul bottone Salva.
     *
     * <p>Pulisce i messaggi precedenti, effettua la convalida locale dei campi tramite {@link #validaCampi()}
     * e istanzia un nuovo oggetto {@link Ristorante} (DTO). Avvia quindi un {@link Task} asincrono per
     * inviare i dati al server tramite la Facade di rete. Al successo dell'operazione, effettua il refresh
     * della dashboard e chiude la finestra corrente dopo un delay di un secondo.</p>
     */
    @FXML
    private void handleSalva() {
        clearMessages();
        if (!validaCampi()) return;

        try {
            int prezzoLivello = prezzoCombo.getValue().length(); // €=1, €€=2, etc.
            String rico = premioCombo.getValue().equals("Nessuno") ? null : premioCombo.getValue();

            List<String> cucine = new ArrayList<>(cucinaComboBox.getSelectedItems());
            List<String> servizi = new ArrayList<>(serviceComboBox.getSelectedItems());

            Ristorante nuovo = new Ristorante(
                    0, // id generato dal server
                    nomeField.getText().trim(),
                    indirizzoField.getText().trim(),
                    cittaField.getText().trim(),
                    nazioneField.getText().trim(),
                    Double.parseDouble(latitudineField.getText().trim()),
                    Double.parseDouble(longitudineField.getText().trim()),
                    prezzoLivello,
                    telefonoField.getText().trim(),
                    "", // url guida
                    sitoWebField.getText().trim(),
                    rico,
                    stellaVerdeCheck.isSelected(),
                    descrizioneArea.getText().trim(),
                    deliveryCheck.isSelected(),
                    prenotazioneOnlineCheck.isSelected(),
                    currentUser.getId(),
                    cucine,
                    servizi);

            Task<Ristorante> task = new Task<>() {
                @Override
                protected Ristorante call() {
                    return Main.getClient().aggiungiRistorante(nuovo);
                }
            };

            task.setOnSucceeded(e -> {
                successLabel.setText("✅ Ristorante aggiunto con successo!");
                if (parentController != null) parentController.refreshData();

                new Thread(() -> {
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                    javafx.application.Platform.runLater(() -> AppNavigator.goBackOrClose(nomeField));
                }).start();
            });

            task.setOnFailed(e -> errorLabel.setText("❌ " + task.getException().getMessage()));

            new Thread(task).start();

        } catch (NumberFormatException e) {
            errorLabel.setText("❌ Errore: Latitudine e Longitudine devono essere numeri validi.");
        }
    }

    /**
     * Valida i campi del modulo per assicurarsi che tutti i dati obbligatori siano inseriti correttamente.
     *
     * @return {@code true} se la validazione ha successo, {@code false} se ci sono errori
     */
    private boolean validaCampi() {
        boolean ok = true;
        StringBuilder errori = new StringBuilder();

        if (nomeField.getText().trim().isEmpty()) errori.append("• Nome del ristorante è obbligatorio\n");
        if (cucinaComboBox.getSelectedItems().isEmpty()) errori.append("• Tipo di cucina è obbligatorio\n");
        if (prezzoCombo.getValue() == null) errori.append("• Fascia di prezzo è obbligatoria\n");
        if (indirizzoField.getText().trim().isEmpty()) errori.append("• Indirizzo è obbligatorio\n");
        if (cittaField.getText().trim().isEmpty()) errori.append("• Città è obbligatoria\n");
        if (nazioneField.getText().trim().isEmpty()) errori.append("• Nazione è obbligatoria\n");
        if (telefonoField.getText().trim().isEmpty()) errori.append("• Telefono è obbligatorio\n");
        if (descrizioneArea.getText().trim().isEmpty()) errori.append("• Descrizione è obbligatoria\n");
        else if (descrizioneArea.getText().trim().length() < 50) errori.append("• Descrizione: almeno 50 caratteri\n");

        // Validazione latitudine con errore inline
        if (latitudineField.getText().trim().isEmpty()) {
            setInlineError(latitudineErrorLabel, latitudineField, "Obbligatoria");
            ok = false;
        } else {
            try {
                double lat = Double.parseDouble(latitudineField.getText().trim());
                if (lat < -90 || lat > 90) {
                    setInlineError(latitudineErrorLabel, latitudineField, "Deve essere tra -90 e 90");
                    ok = false;
                }
            } catch (NumberFormatException e) {
                setInlineError(latitudineErrorLabel, latitudineField, "Numero non valido");
                ok = false;
            }
        }

        // Validazione longitudine con errore inline
        if (longitudineField.getText().trim().isEmpty()) {
            setInlineError(longitudineErrorLabel, longitudineField, "Obbligatoria");
            ok = false;
        } else {
            try {
                double lon = Double.parseDouble(longitudineField.getText().trim());
                if (lon < -180 || lon > 180) {
                    setInlineError(longitudineErrorLabel, longitudineField, "Deve essere tra -180 e 180");
                    ok = false;
                }
            } catch (NumberFormatException e) {
                setInlineError(longitudineErrorLabel, longitudineField, "Numero non valido");
                ok = false;
            }
        }

        if (errori.length() > 0) {
            errorLabel.setText("❌ " + errori.toString().trim());
            ok = false;
        }
        return ok;
    }

    private void setInlineError(Label errorLbl, TextField field, String msg) {
        errorLbl.setText("⚠ " + msg);
        errorLbl.setVisible(true);
        errorLbl.setManaged(true);
        field.setStyle(field.getStyle() + "-fx-border-color: #e74c3c;");
    }

    private void clearInlineError(Label errorLbl, TextField field) {
        errorLbl.setText("");
        errorLbl.setVisible(false);
        errorLbl.setManaged(false);
        field.setStyle(field.getStyle().replace("-fx-border-color: #e74c3c;", ""));
    }

    /**
     * Pulisce i messaggi d'errore e di successo dalle relative label nella UI.
     */
    private void clearMessages() {
        errorLabel.setText("");
        successLabel.setText("");
        clearInlineError(latitudineErrorLabel, latitudineField);
        clearInlineError(longitudineErrorLabel, longitudineField);
    }

    /**
     * Gestisce l'annullamento dell'inserimento, tornando alla schermata precedente.
     */
    @FXML
    private void handleAnnulla() { AppNavigator.goBackOrClose(nomeField); }
}
