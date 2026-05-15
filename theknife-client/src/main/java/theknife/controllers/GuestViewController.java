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

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Controller per la vista guest (ospite non autenticato).
 * Usa ClientTK per la ricerca dei ristoranti lato server.
 *
 * @author Philip Jon Ji Ciuca
 * @version 2.0
 */
public class GuestViewController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> cuisineComboBox;
    @FXML private ComboBox<String> locationComboBox;
    @FXML private ComboBox<String> priceRangeComboBox;
    @FXML private ComboBox<String> starsComboBox;
    @FXML private CheckBox deliveryCheckBox;
    @FXML private CheckBox onlineBookingCheckBox;
    @FXML private Button searchButton;
    @FXML private Button resetButton;
    @FXML private ListView<Ristorante> restaurantListView;
    @FXML private Button detailsButton;
    @FXML private Button mapButton;
    @FXML private Text totalRestaurantsLabel;
    @FXML private Text michelinStarsLabel;
    @FXML private Text greenStarsLabel;

    private ObservableList<Ristorante> filteredRestaurants;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        filteredRestaurants = FXCollections.observableArrayList();
        restaurantListView.setItems(filteredRestaurants);
        setupUI();

        starsComboBox.setItems(FXCollections.observableArrayList(
                "1 Stella", "2 Stelle", "3 Stelle", "Stelle Verdi"));

        handleSearch(); // carica tutti i ristoranti inizialmente
    }

    private void setupUI() {
        restaurantListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Ristorante r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) {
                    setText(null); setGraphic(null);
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

        restaurantListView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            boolean sel = n != null;
            detailsButton.setDisable(!sel);
            mapButton.setDisable(!sel);
        });
    }

    @FXML
    private void handleSearch() {
        FiltriRicerca.Builder builder = FiltriRicerca.builder();

        String searchText = searchField.getText() != null ? searchField.getText().trim() : "";
        if (!searchText.isEmpty()) builder.nome(searchText);

        if (cuisineComboBox.getValue() != null) builder.cucina(cuisineComboBox.getValue());
        if (locationComboBox.getValue() != null) builder.citta(locationComboBox.getValue());

        if (priceRangeComboBox.getValue() != null) {
            builder.prezzoLivello(priceRangeComboBox.getValue().length());
        }

        if (starsComboBox.getValue() != null) {
            switch (starsComboBox.getValue()) {
                case "1 Stella" -> builder.riconoscimento("1 Star");
                case "2 Stelle" -> builder.riconoscimento("2 Stars");
                case "3 Stelle" -> builder.riconoscimento("3 Stars");
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
        cuisineComboBox.setValue(null);
        locationComboBox.setValue(null);
        priceRangeComboBox.setValue(null);
        starsComboBox.setValue(null);
        deliveryCheckBox.setSelected(false);
        onlineBookingCheckBox.setSelected(false);
        handleSearch();
    }

    @FXML
    private void handleDetails() {
        Ristorante sel = restaurantListView.getSelectionModel().getSelectedItem();
        if (sel != null) openRestaurantDetails(sel);
    }

    @FXML
    private void handleMap() {
        Ristorante sel = restaurantListView.getSelectionModel().getSelectedItem();
        if (sel != null) showOnMap(sel);
    }

    private void openRestaurantDetails(Ristorante restaurant) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/dettaglioRistorante.fxml"));
            Parent root = loader.load();

            DettaglioRistoranteController controller = loader.getController();
            controller.setRistorante(restaurant);
            controller.setCurrentUser(null);

            Stage stage = new Stage();
            stage.setTitle("Dettaglio Ristorante - " + restaurant.getNome());
            Main.setApplicationIcon(stage);
            stage.setScene(new Scene(root, 900, 700));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
            showAlert("Info", "Apri mappa", String.format("Coordinate: %f, %f",
                    restaurant.getLatitudine(), restaurant.getLongitudine()));
        }
    }

    private void updateStatistics() {
        totalRestaurantsLabel.setText(String.valueOf(filteredRestaurants.size()));
        michelinStarsLabel.setText(String.valueOf(
                filteredRestaurants.stream().filter(r -> r.getStarCount() > 0).count()));
        greenStarsLabel.setText(String.valueOf(
                filteredRestaurants.stream().filter(Ristorante::isGreenStar).count()));
    }

    private void showAlert(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
