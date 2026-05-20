package theknife.controllers;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;
import theknife.Main;
import theknife.models.Ristorante;
import theknife.models.Utente;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller per la finestra di aggiunta di un nuovo ristorante.
 * Usa ClientTK per il salvataggio sul server.
 *
 * @author Philip Jon Ji Ciuca
 * @version 2.0
 */
public class AggiungiRistoranteController implements Initializable {

    @FXML private TextField nomeField;
    @FXML private ComboBox<String> cucinaCombo;
    @FXML private ComboBox<String> prezzoCombo;
    @FXML private TextField indirizzoField;
    @FXML private TextField localitaField;
    @FXML private TextField latitudineField;
    @FXML private TextField longitudineField;
    @FXML private TextField telefonoField;
    @FXML private TextField sitoWebField;
    @FXML private TextArea descrizioneArea;
    @FXML private TextArea serviziArea;
    @FXML private ComboBox<String> premioCombo;
    @FXML private CheckBox stellaVerdeCheck;
    @FXML private CheckBox deliveryCheck;
    @FXML private CheckBox prenotazioneOnlineCheck;
    @FXML private Label errorLabel;
    @FXML private Label successLabel;

    private Utente currentUser;
    private DashboardRistoratoreController parentController;

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

    public void setCurrentUser(Utente user) { this.currentUser = user; }
    public void setParentController(DashboardRistoratoreController parent) { this.parentController = parent; }

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
                for (String servizio : serviziArea.getText().split(",")) {
                    String pulito = servizio.trim();
                    if (!pulito.isEmpty()) servizi.add(pulito);
                }
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
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                    javafx.application.Platform.runLater(this::handleAnnulla);
                }).start();
            });

            task.setOnFailed(e -> errorLabel.setText("❌ " + task.getException().getMessage()));

            new Thread(task).start();

        } catch (NumberFormatException e) {
            errorLabel.setText("❌ Errore: Latitudine e Longitudine devono essere numeri validi.");
        }
    }

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

    private void clearMessages() { errorLabel.setText(""); successLabel.setText(""); }

    @FXML
    private void handleAnnulla() { ((Stage) nomeField.getScene().getWindow()).close(); }
}
