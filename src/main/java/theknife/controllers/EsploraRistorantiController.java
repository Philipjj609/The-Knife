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
import theknife.models.Utente;
import theknife.services.PreferitiManager;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller per la vista di esplorazione dei ristoranti.
 * Fornisce filtri di ricerca, visualizzazione della lista e azioni
 * per aprire dettagli, aggiungere ai preferiti e mostrare la posizione.
 *
 * @author Philip Jon Ji Ciuca
 * @version 1.0
 * */
public class EsploraRistorantiController implements Initializable {

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
    private Button aggiungiPreferitiButton;
    @FXML
    private Button mapButton;
    @FXML
    private Text totalRestaurantsLabel;
    @FXML
    private Text michelinStarsLabel;
    @FXML
    private Text greenStarsLabel;
    @FXML
    private Text favoritesLabel;

    private ObservableList<Ristorante> allRestaurants;
    private ObservableList<Ristorante> filteredRestaurants;
    private Utente currentUser;
    private DashboardClienteController parentController;

    /**
     * Inizializza il controller impostando i dati e l'interfaccia utente.
     *
     * @param location URL della risorsa FXML (ignored)
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
     * Imposta l'utente corrente per la vista (opzionale).
     *
     * @param user Utente corrente, può essere null per guest.
     * @since 1.0
     */
    public void setCurrentUser(Utente user) {
        this.currentUser = user;
        updateStatistics();
    }

    /**
     * Imposta il controller genitore che ha aperto questa vista.
     *
     * @param parent Controller genitore (DashboardClienteController).
     * @since 1.0
     */
    public void setParentController(DashboardClienteController parent) {
        this.parentController = parent;
    }

    /**
     * Carica i dati iniziali dai ristoranti disponibili e popola i filtri.
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
     * Configura la UI e il comportamento della ListView e dei pulsanti.
     * @since 1.0
     */
    private void setupUI() {
        // Configura ListView con celle personalizzate
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

                    // Indica se è nei preferiti
                    String favoriteIcon = "";
                    if (currentUser != null
                            && PreferitiManager.isPreferito(currentUser.getUsername(), restaurant.getName())) {
                        favoriteIcon = "❤️ ";
                    }

                    String text = String.format("%s%s%s - %s\n%s | %s",
                            favoriteIcon,
                            stars.isEmpty() ? "" : stars + " ",
                            restaurant.getName(),
                            restaurant.getCuisine(),
                            restaurant.getLocation(),
                            restaurant.getPrice() != null ? restaurant.getPrice() : "N/A");
                    setText(text);
                }
            }
        });

        // Abilita/disabilita pulsanti basandosi sulla selezione
        restaurantListView.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    boolean hasSelection = newValue != null;
                    detailsButton.setDisable(!hasSelection);
                    aggiungiPreferitiButton.setDisable(!hasSelection);
                    mapButton.setDisable(!hasSelection);

                    // Aggiorna il testo del pulsante preferiti
                    if (hasSelection && currentUser != null) {
                        boolean isFavorite = PreferitiManager.isPreferito(currentUser.getUsername(),
                                newValue.getName());
                        aggiungiPreferitiButton.setText(isFavorite ? "❤️ Rimuovi Preferito" : "🤍 Aggiungi Preferito");
                    }
                });
    }

    /**
     * Apre la finestra di dettaglio per il ristorante selezionato.
     *
     * @param restaurant Ristorante selezionato, must be non-null.
     * @since 1.0
     */
    private void openRestaurantDetails(Ristorante restaurant) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/dettaglioRistorante.fxml"));
            Parent root = loader.load();

            DettaglioRistoranteController controller = loader.getController();
            controller.setRistorante(restaurant);
            controller.setCurrentUser(currentUser.getUsername());

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
     * Aggiunge o rimuove il ristorante dai preferiti dell'utente corrente.
     *
     * @param restaurant Ristorante da togglare nei preferiti.
     * @since 1.0
     */
    private void toggleFavorite(Ristorante restaurant) {
        if (currentUser == null)
            return;

        boolean isFavorite = PreferitiManager.isPreferito(currentUser.getUsername(), restaurant.getName());

        if (isFavorite) {
            PreferitiManager.rimuoviPreferito(currentUser.getUsername(), restaurant.getName());
        } else {
            PreferitiManager.aggiungiPreferito(currentUser.getUsername(), restaurant.getName());
        }

        // Aggiorna la vista
        refreshView();
        if (parentController != null) {
            parentController.refreshData();
        }
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

                    boolean matchesStars = selectedStars == null ||
                            switch (selectedStars) {
                                case "1 Stella" -> r.getStars() == 1;
                                case "2 Stelle" -> r.getStars() == 2;
                                case "3 Stelle" -> r.getStars() == 3;
                                case "Stelle Verdi" -> r.getGreenStar() != null && !r.getGreenStar().trim().isEmpty()
                                        && !r.getGreenStar().equalsIgnoreCase("N/A");
                                default -> true;
                            };

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
     * Resetta tutti i filtri e mostra tutti i ristoranti.
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
     * Apre i dettagli del ristorante selezionato nella lista.
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
     * Gestisce l'aggiunta/rimozione dai preferiti tramite il pulsante.
     * @since 1.0
     */
    @FXML
    private void handleAggiungiPreferiti() {
        Ristorante selected = restaurantListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            toggleFavorite(selected);
        }
    }

    /**
     * Mostra la posizione del ristorante selezionato sulla mappa.
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
     * Mostra una finestra con la mappa del ristorante o visualizza un fallback.
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
     * Chiude la finestra corrente e torna al dashboard.
     * @since 1.0
     */
    @FXML
    private void tornaDashboard() {
        Stage stage = (Stage) searchField.getScene().getWindow();
        stage.close();
    }

    /**
     * Aggiorna le statistiche visibili nella UI (totale, stelle, preferiti).
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

        long favorites = 0;
        if (currentUser != null) {
            favorites = filteredRestaurants.stream()
                    .filter(r -> PreferitiManager.isPreferito(currentUser.getUsername(), r.getName()))
                    .count();
        }

        totalRestaurantsLabel.setText(String.valueOf(total));
        michelinStarsLabel.setText(String.valueOf(michelinStars));
        greenStarsLabel.setText(String.valueOf(greenStars));
        favoritesLabel.setText(String.valueOf(favorites));
    }

    /**
     * Mostra un alert informativo all'utente.
     *
     * @param title Titolo dell'alert.
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

    /**
     * Ricarica la vista mantenendo i filtri attuali.
     * @since 1.0
     */
    public void refreshView() {
        // Ricarica la lista mantenendo i filtri attuali
        restaurantListView.refresh();
        updateStatistics();
    }
}
