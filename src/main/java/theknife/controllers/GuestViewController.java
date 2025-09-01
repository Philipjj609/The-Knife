package theknife.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import theknife.Main;
import theknife.models.Ristorante;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller per la vista guest (ospite non autenticato).
 * Fornisce funzionalità di ricerca e filtro dei ristoranti per utenti non loggati
 * e permette di aprire la vista dettagliata o la mappa di un ristorante.
 *
 * @author Philip Jon Ji Ciuca
 * @version 1.0
 */
public class GuestViewController implements Initializable {

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> cuisineComboBox;
    @FXML
    private ComboBox<String> locationComboBox;
    @FXML
    private ComboBox<String> priceRangeComboBox;
    @FXML
    private ComboBox<String> starsComboBox;
    @FXML
    private CheckBox deliveryCheckBox;
    @FXML
    private CheckBox onlineBookingCheckBox;
    @FXML
    private Button searchButton;
    @FXML
    private Button resetButton;
    @FXML
    private ListView<Ristorante> restaurantListView;
    @FXML
    private Button detailsButton;
    @FXML
    private Button mapButton;
    @FXML
    private Text totalRestaurantsLabel;
    @FXML
    private Text michelinStarsLabel;
    @FXML
    private Text greenStarsLabel;

    private ObservableList<Ristorante> allRestaurants;
    private ObservableList<Ristorante> filteredRestaurants;

    /**
     * Inizializza il controller: carica i dati e configura l'interfaccia.
     *
     * @param location  URL della risorsa FXML (ignored)
     * @param resources ResourceBundle eventualmente fornito (ignored)
     * @since 1.0
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupData();
        setupUI();
        updateStatistics();
    }

    /**
     * Carica i dati iniziali dei ristoranti e popola i filtri.
     * @since 1.0
     */
    private void setupData() {
        allRestaurants = FXCollections.observableArrayList(Main.ristoranti);
        filteredRestaurants = FXCollections.observableArrayList(allRestaurants);
        restaurantListView.setItems(filteredRestaurants);

        // Popola ComboBox cucine
        List<String> cuisines = allRestaurants.stream()
                .map(Ristorante::getCuisine)
                .filter(c -> c != null && !c.trim().isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        cuisineComboBox.setItems(FXCollections.observableArrayList(cuisines));

        // Popola ComboBox località
        List<String> locations = allRestaurants.stream()
                .map(Ristorante::getLocation)
                .filter(l -> l != null && !l.trim().isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        locationComboBox.setItems(FXCollections.observableArrayList(locations));

        // Popola ComboBox fascia di prezzo
        List<String> priceRanges = allRestaurants.stream()
                .map(Ristorante::getPrice)
                .filter(p -> p != null && !p.trim().isEmpty())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        priceRangeComboBox.setItems(FXCollections.observableArrayList(priceRanges));

        // Popola ComboBox stelle
        ObservableList<String> starsOptions = FXCollections.observableArrayList(
                "1 Stella", "2 Stelle", "3 Stelle", "Stelle Verdi");
        starsComboBox.setItems(starsOptions);
    }

    /**
     * Configura la UI, la ListView e gli handler per i pulsanti.
     * @since 1.0
     */
    private void setupUI() {        // Configura ListView con celle personalizzate
        restaurantListView.setCellFactory(listView -> new ListCell<Ristorante>() {
            @Override
            protected void updateItem(Ristorante restaurant, boolean empty) {
                super.updateItem(restaurant, empty);
                if (empty || restaurant == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String stars = "";
                    if (restaurant.getStars() > 0) {
                        stars += "⭐";
                    }
                    if (restaurant.getGreenStar() != null && !restaurant.getGreenStar().trim().isEmpty()
                            && !restaurant.getGreenStar().equalsIgnoreCase("N/A")) {
                        stars += "🌟";
                    }

                    String text = String.format("%s%s - %s\n%s | %s",
                            stars.isEmpty() ? "" : stars + " ",
                            restaurant.getName(),
                            restaurant.getCuisine(),
                            restaurant.getLocation(),
                            restaurant.getPrice() != null ? restaurant.getPrice() : "N/A");
                    setText(text);
                }
            }
        });

        // Abilita/disabilita pulsanti basandosi sulla selezione        restaurantListView.getSelectionModel().selectedItemProperty().addListener(
        restaurantListView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            boolean hasSelection = newValue != null;
            detailsButton.setDisable(!hasSelection);
            mapButton.setDisable(!hasSelection);
        });
    }

    /**
     * Esegue la ricerca con i filtri impostati e aggiorna la lista filtrata.
     * @since 1.0
     */
    @FXML
    private void handleSearch() {
        String searchText = searchField.getText().toLowerCase().trim();
        String selectedCuisine = cuisineComboBox.getValue();
        String selectedLocation = locationComboBox.getValue();
        String selectedPriceRange = priceRangeComboBox.getValue();
        String selectedStars = starsComboBox.getValue();
        boolean isDeliverySelected = deliveryCheckBox.isSelected();
        boolean isOnlineBookingSelected = onlineBookingCheckBox.isSelected();

        List<Ristorante> filtered = allRestaurants.stream()
                .filter(r -> {
                    boolean matchesText = searchText.isEmpty() ||
                            r.getName().toLowerCase().contains(searchText) ||
                            (r.getCuisine() != null && r.getCuisine().toLowerCase().contains(searchText)) ||
                            (r.getLocation() != null && r.getLocation().toLowerCase().contains(searchText));

                    boolean matchesCuisine = selectedCuisine == null ||
                            (r.getCuisine() != null && r.getCuisine().equals(selectedCuisine));

                    boolean matchesLocation = selectedLocation == null ||
                            (r.getLocation() != null && r.getLocation().equals(selectedLocation));

                    boolean matchesPriceRange = selectedPriceRange == null ||
                            (r.getPrice() != null && r.getPrice().equals(selectedPriceRange));

                    boolean matchesStars = true;
                    if (selectedStars != null) {
                        switch (selectedStars) {
                            case "1 Stella":
                                matchesStars = r.getStars() == 1;
                                break;
                            case "2 Stelle":
                                matchesStars = r.getStars() == 2;
                                break;
                            case "3 Stelle":
                                matchesStars = r.getStars() == 3;
                                break;
                            case "Stelle Verdi":
                                matchesStars = r.getGreenStar() != null && !r.getGreenStar().trim().isEmpty()
                                        && !r.getGreenStar().equalsIgnoreCase("N/A");
                                break;
                            default:
                                matchesStars = true;
                        }
                    }

                    boolean matchesDelivery = !isDeliverySelected || r.isDeliveryAvailable();
                    boolean matchesOnlineBooking = !isOnlineBookingSelected || r.isOnlineBookingAvailable();

                    return matchesText && matchesCuisine && matchesLocation && matchesPriceRange &&
                            matchesStars && matchesDelivery && matchesOnlineBooking;
                })
                .collect(Collectors.toList());

        filteredRestaurants.setAll(filtered);
        updateStatistics();
    }

    /**
     * Resetta i filtri e mostra tutti i ristoranti.
     * @since 1.0
     */
    @FXML
    private void handleReset() {
        searchField.clear();
        cuisineComboBox.setValue(null);
        locationComboBox.setValue(null);
        priceRangeComboBox.setValue(null);
        starsComboBox.setValue(null);
        deliveryCheckBox.setSelected(false);
        onlineBookingCheckBox.setSelected(false);
        filteredRestaurants.setAll(allRestaurants);
        updateStatistics();
    }

    /**
     * Apre i dettagli del ristorante selezionato.
     * @since 1.0
     */
    @FXML
    private void handleDetails() {
        Ristorante selected = restaurantListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openRestaurantDetails(selected);
        }
    }

    /**
     * Apre la vista dettagliata per il ristorante specificato.
     * @param restaurant Ristorante da aprire, must be non-null.
     * @since 1.0
     */
    private void openRestaurantDetails(Ristorante restaurant) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/dettaglioRistorante.fxml"));
            Parent root = loader.load();

            DettaglioRistoranteController controller = loader.getController();
            controller.setRistorante(restaurant);
            // Per gli ospiti, non passiamo l'username (sarà null)
            controller.setCurrentUser(null);

            Stage stage = new Stage();
            stage.setTitle("Dettaglio Ristorante - " + restaurant.getName());

            // Imposta l'icona della finestra
            theknife.Main.setApplicationIcon(stage);

            stage.setScene(new Scene(root, 900, 700));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Mostra la posizione selezionata su una mappa o fallback se non disponibile.
     * @since 1.0
     */
    @FXML
    private void handleMap() {
        Ristorante selected = restaurantListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            showOnMap(selected);
        }
    }

    /**
     * Mostra la mappa del ristorante in una dialog o mostra un alert di fallback.
     * @param restaurant Ristorante da mostrare.
     * @since 1.0
     */
    private void showOnMap(Ristorante restaurant) {
        if (restaurant.getLatitude() == 0.0 && restaurant.getLongitude() == 0.0) {
            showAlert("Info", "Posizione non disponibile",
                    "Le coordinate GPS per questo ristorante non sono disponibili.");
            return;
        }

        // Crea finestra personalizzata per la mappa
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/mapDialog.fxml"));
            Parent root = loader.load();

            MapDialogController controller = loader.getController();
            controller.setRestaurant(restaurant);

            Stage stage = new Stage();
            stage.setTitle("Posizione - " + restaurant.getName());
            stage.setScene(new Scene(root, 500, 300));
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            // Fallback alla vecchia implementazione
            String mapUrl = String.format("https://www.google.com/maps?q=%f,%f",
                    restaurant.getLatitude(), restaurant.getLongitude());

            showAlert("Info", "Apri mappa",
                    "Coordinate: " + restaurant.getLatitude() + ", " + restaurant.getLongitude() +
                            "\nURL: " + mapUrl);
        }
    }

    /**
     * Aggiorna le statistiche della vista basate sui ristoranti filtrati.
     * @since 1.0
     */
    private void updateStatistics() {
        int total = filteredRestaurants.size();
        long michelinStars = filteredRestaurants.stream()
                .filter(r -> r.getStars() > 0)
                .count();
        long greenStars = filteredRestaurants.stream()
                .filter(r -> r.getGreenStar() != null && !r.getGreenStar().trim().isEmpty()
                        && !r.getGreenStar().equalsIgnoreCase("N/A"))
                .count();

        totalRestaurantsLabel.setText(String.valueOf(total));
        michelinStarsLabel.setText(String.valueOf(michelinStars));
        greenStarsLabel.setText(String.valueOf(greenStars));
    }

    /**
     * Mostra un alert informativo.
     *
     * @param title  Titolo dell'alert.
     * @param header Header dell'alert.
     * @param message Messaggio di dettaglio.
     * @since 1.0
     */
    private void showAlert(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}