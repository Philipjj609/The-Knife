package theknife.controllers;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import theknife.Main;
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

    /** ComboBox per la scelta della tipologia di cucina principale. */
    @FXML private ComboBox<String> cucinaCombo;

    /** ComboBox per la scelta della fascia di prezzo (es. €, €€, etc.). */
    @FXML private ComboBox<String> prezzoCombo;

    /** Campo di testo per l'indirizzo stradale del ristorante. */
    @FXML private TextField indirizzoField;

    /** Campo di testo per il comune/località del ristorante. */
    @FXML private TextField localitaField;

    /** Campo di testo per la latitudine geografica. */
    @FXML private TextField latitudineField;

    /** Campo di testo per la longitudine geografica. */
    @FXML private TextField longitudineField;

    /** Campo di testo per il numero di telefono del ristorante. */
    @FXML private TextField telefonoField;

    /** Campo di testo per l'indirizzo del sito web. */
    @FXML private TextField sitoWebField;

    /** Area di testo per una descrizione approfondita del ristorante (minimo 50 caratteri). */
    @FXML private TextArea descrizioneArea;

    /** Area di testo per elencare servizi offerti e particolarità. */
    @FXML private TextArea serviziArea;

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
        cucinaCombo.setItems(FXCollections.observableArrayList(
                "Italiana", "Mediterranea", "Francese", "Giapponese", "Cinese",
                "Indiana", "Messicana", "Americana", "Fusion", "Creative",
                "Contemporary", "Seafood", "Vegetariana", "Pizza", "Altro"));

        prezzoCombo.setItems(FXCollections.observableArrayList("€", "€€", "€€€", "€€€€"));

        premioCombo.setItems(FXCollections.observableArrayList(
                "Nessuno", "Bib Gourmand", "1 Star", "2 Stars", "3 Stars"));
        premioCombo.setValue("Nessuno");

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

            List<String> cucine = new ArrayList<>();
            cucine.add(cucinaCombo.getValue());

            List<String> servizi = new ArrayList<>();
            if (!serviziArea.getText().trim().isEmpty()) {
                servizi.add(serviziArea.getText().trim());
            }

            Ristorante nuovo = new Ristorante(
                    0, // id generato dal server
                    nomeField.getText().trim(),
                    indirizzoField.getText().trim(),
                    localitaField.getText().trim(),
                    null, // nazione
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
        StringBuilder errori = new StringBuilder();
        if (nomeField.getText().trim().isEmpty()) errori.append("• Nome del ristorante è obbligatorio\n");
        if (cucinaCombo.getValue() == null) errori.append("• Tipo di cucina è obbligatorio\n");
        if (prezzoCombo.getValue() == null) errori.append("• Fascia di prezzo è obbligatoria\n");
        if (indirizzoField.getText().trim().isEmpty()) errori.append("• Indirizzo è obbligatorio\n");
        if (localitaField.getText().trim().isEmpty()) errori.append("• Località è obbligatoria\n");
        if (latitudineField.getText().trim().isEmpty()) errori.append("• Latitudine è obbligatoria\n");
        if (longitudineField.getText().trim().isEmpty()) errori.append("• Longitudine è obbligatoria\n");
        if (telefonoField.getText().trim().isEmpty()) errori.append("• Telefono è obbligatorio\n");
        if (descrizioneArea.getText().trim().isEmpty()) errori.append("• Descrizione è obbligatoria\n");
        else if (descrizioneArea.getText().trim().length() < 50) errori.append("• Descrizione: almeno 50 caratteri\n");

        if (errori.length() > 0) {
            errorLabel.setText("❌ Errori:\n" + errori);
            return false;
        }
        return true;
    }

    /**
     * Pulisce i messaggi d'errore e di successo dalle relative label nella UI.
     */
    private void clearMessages() { errorLabel.setText(""); successLabel.setText(""); }

    /**
     * Gestisce l'annullamento dell'inserimento, tornando alla schermata precedente.
     */
    @FXML
    private void handleAnnulla() { AppNavigator.goBackOrClose(nomeField); }
}
