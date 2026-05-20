package theknife.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import theknife.Main;
import theknife.models.FiltriRicerca;
import theknife.models.Ristorante;
import theknife.models.Utente;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller per la vista di esplorazione dei ristoranti.
 * Usa ClientTK per la ricerca con filtri lato server.
 *
 * @author Philip Jon Ji Ciuca
 * @version 2.0
 */
public class EsploraRistorantiController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> cuisineComboBox;
    @FXML private ComboBox<String> serviceComboBox;
    @FXML private ComboBox<String> locationComboBox;
    @FXML private ComboBox<String> priceRangeComboBox;
    @FXML private ComboBox<String> starsComboBox;
    @FXML private CheckBox deliveryCheckBox;
    @FXML private CheckBox onlineBookingCheckBox;
    @FXML private Button searchButton;
    @FXML private Button resetButton;
    @FXML private ListView<Ristorante> restaurantListView;
    @FXML private Button detailsButton;
    @FXML private Button aggiungiPreferitiButton;
    @FXML private Button mapButton;
    @FXML private Text totalRestaurantsLabel;
    @FXML private Text michelinStarsLabel;
    @FXML private Text greenStarsLabel;
    @FXML private Text favoritesLabel;

    private ObservableList<Ristorante> filteredRestaurants;
    private Utente currentUser;
    private DashboardClienteController parentController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        filteredRestaurants = FXCollections.observableArrayList();
        restaurantListView.setItems(filteredRestaurants);
        setupUI();

        // Filtro cucina: testo libero (LIKE sul server)
        cuisineComboBox.setEditable(true);
        serviceComboBox.setEditable(true);

        // Filtro località: testo libero (LIKE sul server)
        locationComboBox.setEditable(true);

        // Filtro fascia di prezzo
        priceRangeComboBox.setItems(FXCollections.observableArrayList("€", "€€", "€€€", "€€€€"));

        // Filtro riconoscimento Michelin
        starsComboBox.setItems(FXCollections.observableArrayList(
                "1 Stella Michelin", "2 Stelle Michelin", "3 Stelle Michelin",
                "Bib Gourmand", "Selezionato Michelin"));

        // Carica tutti i ristoranti iniziali (senza filtri)
        handleSearch();
    }

    public void setCurrentUser(Utente user) {
        this.currentUser = user;
        updateStatistics();
    }

    public void setParentController(DashboardClienteController parent) {
        this.parentController = parent;
    }

    private void setupUI() {
        restaurantListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Ristorante r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String stars = "";
                    if (r.getStarCount() > 0) stars += "⭐";
                    if (r.isGreenStar()) stars += "🌟";

                    String text = String.format("%s%s - %s\n%s | %s",
                            stars.isEmpty() ? "" : stars + " ",
                            r.getNome(),
                            String.join(", ", r.getCucine()),
                            r.getCitta(),
                            r.getPrezzoStringa());
                    setText(text);
                }
            }
        });

        restaurantListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    boolean hasSelection = newVal != null;
                    detailsButton.setDisable(!hasSelection);
                    aggiungiPreferitiButton.setDisable(!hasSelection);
                    mapButton.setDisable(!hasSelection);
                });
    }

    @FXML
    private void handleSearch() {
        FiltriRicerca.Builder builder = FiltriRicerca.builder();

        String searchText = searchField.getText() != null ? searchField.getText().trim() : "";
        if (!searchText.isEmpty()) builder.nome(searchText);

        String cucina = comboText(cuisineComboBox);
        if (!cucina.isEmpty()) builder.cucina(cucina);

        String servizio = comboText(serviceComboBox);
        if (!servizio.isEmpty()) builder.servizio(servizio);

        String citta = comboText(locationComboBox);
        if (!citta.isEmpty()) builder.citta(citta);

        if (priceRangeComboBox.getValue() != null) {
            int livello = priceRangeComboBox.getValue().length(); // €=1, €€=2, etc.
            builder.prezzoLivello(livello);
        }

        if (starsComboBox.getValue() != null) {
            switch (starsComboBox.getValue()) {
                case "1 Stella Michelin"   -> builder.riconoscimento("1 Star");
                case "2 Stelle Michelin"   -> builder.riconoscimento("2 Stars");
                case "3 Stelle Michelin"   -> builder.riconoscimento("3 Stars");
                case "Bib Gourmand"        -> builder.riconoscimento("Bib Gourmand");
                case "Selezionato Michelin"-> builder.riconoscimento("Selected Restaurants");
            }
        }

        builder.soloDelivery(deliveryCheckBox.isSelected());
        builder.soloPrenotazione(onlineBookingCheckBox.isSelected());

        FiltriRicerca filtri = builder.build();

        Task<List<Ristorante>> task = new Task<>() {
            @Override
            protected List<Ristorante> call() {
                return Main.getClient().cercaRistoranti(filtri);
            }
        };

        task.setOnSucceeded(e -> {
            filteredRestaurants.setAll(task.getValue());
            updateStatistics();
        });

        new Thread(task).start();
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        clearCombo(cuisineComboBox);
        clearCombo(serviceComboBox);
        clearCombo(locationComboBox);
        priceRangeComboBox.setValue(null);
        starsComboBox.setValue(null);
        deliveryCheckBox.setSelected(false);
        onlineBookingCheckBox.setSelected(false);
        handleSearch();
    }

    @FXML
    private void handleDetails() {
        Ristorante selected = restaurantListView.getSelectionModel().getSelectedItem();
        if (selected != null) openRestaurantDetails(selected);
    }

    @FXML
    private void handleAggiungiPreferiti() {
        Ristorante selected = restaurantListView.getSelectionModel().getSelectedItem();
        if (selected != null && currentUser != null) toggleFavorite(selected);
    }

    @FXML
    private void handleMap() {
        Ristorante selected = restaurantListView.getSelectionModel().getSelectedItem();
        if (selected != null) showOnMap(selected);
    }

    private void openRestaurantDetails(Ristorante restaurant) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/dettaglioRistorante.fxml"));
            Parent root = loader.load();

            DettaglioRistoranteController controller = loader.getController();
            controller.setRistorante(restaurant);
            if (currentUser != null) controller.setCurrentUser(currentUser.getUsername());

            Stage stage = new Stage();
            stage.setTitle("Dettaglio Ristorante - " + restaurant.getNome());
            Main.setApplicationIcon(stage);
            stage.setScene(new Scene(root, 900, 700));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void toggleFavorite(Ristorante restaurant) {
        new Thread(() -> {
            boolean isFav = Main.getClient().isPreferito(currentUser.getUsername(), restaurant.getId());
            if (isFav) {
                Main.getClient().rimuoviPreferito(currentUser.getUsername(), restaurant.getId());
            } else {
                Main.getClient().aggiungiPreferito(currentUser.getUsername(), restaurant.getId());
            }
            javafx.application.Platform.runLater(() -> {
                refreshView();
                if (parentController != null) parentController.refreshData();
            });
        }).start();
    }

    private void showOnMap(Ristorante restaurant) {
        if (restaurant.getLatitudine() == 0.0 && restaurant.getLongitudine() == 0.0) {
            showAlert("Info", "Posizione non disponibile",
                    "Le coordinate GPS per questo ristorante non sono disponibili.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/mapDialog.fxml"));
            Parent root = loader.load();

            MapDialogController controller = loader.getController();
            controller.setRestaurant(restaurant);

            Stage stage = new Stage();
            stage.setTitle("Posizione - " + restaurant.getNome());
            stage.setScene(new Scene(root, 500, 300));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            String mapUrl = String.format("https://www.google.com/maps?q=%f,%f",
                    restaurant.getLatitudine(), restaurant.getLongitudine());
            showAlert("Info", "Apri mappa", "URL: " + mapUrl);
        }
    }

    @FXML
    private void tornaDashboard() {
        ((Stage) searchField.getScene().getWindow()).close();
    }

    private void updateStatistics() {
        int total = filteredRestaurants.size();
        long michelin = filteredRestaurants.stream().filter(r -> r.getStarCount() > 0).count();
        long green = filteredRestaurants.stream().filter(Ristorante::isGreenStar).count();

        totalRestaurantsLabel.setText(String.valueOf(total));
        michelinStarsLabel.setText(String.valueOf(michelin));
        greenStarsLabel.setText(String.valueOf(green));
        favoritesLabel.setText("0"); // sarà aggiornato async se necessario
    }

    private void showAlert(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void refreshView() {
        restaurantListView.refresh();
        updateStatistics();
    }

    private String comboText(ComboBox<String> comboBox) {
        String text = comboBox.isEditable() && comboBox.getEditor() != null
                ? comboBox.getEditor().getText()
                : comboBox.getValue();
        return text != null ? text.trim() : "";
    }

    private void clearCombo(ComboBox<String> comboBox) {
        comboBox.setValue(null);
        if (comboBox.getEditor() != null) comboBox.getEditor().clear();
    }
}
