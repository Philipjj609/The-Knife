package theknife.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import theknife.Main;
import theknife.models.Ristorante;
import theknife.models.Utente;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Predicate;

/**
 * Controller per la vista di esplorazione dei ristoranti.
 * Carica tutti i ristoranti una volta sola e filtra in locale.
 *
 * @author Philip Jon Ji Ciuca
 * @version 3.0
 */
public class EsploraRistorantiController implements Initializable {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> cuisineComboBox;
    @FXML private ComboBox<String> locationComboBox;
    @FXML private ComboBox<String> priceRangeComboBox;
    @FXML private ComboBox<String> starsComboBox;
    @FXML private ComboBox<String> serviceComboBox;
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
    @FXML private Label loadingLabel;

    private ObservableList<Ristorante> filteredRestaurants;
    private List<Ristorante> allRestaurants = new ArrayList<>();
    private Utente currentUser;
    private DashboardClienteController parentController;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        filteredRestaurants = FXCollections.observableArrayList();
        restaurantListView.setItems(filteredRestaurants);
        setupUI();

        priceRangeComboBox.setItems(FXCollections.observableArrayList("€", "€€", "€€€", "€€€€"));
        starsComboBox.setItems(FXCollections.observableArrayList(
                "1 Stella Michelin", "2 Stelle Michelin", "3 Stelle Michelin",
                "Bib Gourmand", "Selezionato Michelin"));

        cuisineComboBox.setEditable(true);
        locationComboBox.setEditable(true);

        loadingLabel.setText("Caricamento ristoranti...");
        loadingLabel.setVisible(true);
        searchButton.setDisable(true);
        resetButton.setDisable(true);

        Task<Void> initTask = new Task<>() {
            @Override
            protected Void call() {
                allRestaurants = Main.getClient().cercaRistoranti(null);

                List<String> servizi = Main.getClient().getServizi();
                List<String> cucine  = Main.getClient().getCucine();
                List<String> localita = Main.getClient().getLocalita();

                javafx.application.Platform.runLater(() -> {
                    serviceComboBox.setItems(FXCollections.observableArrayList(servizi));
                    cuisineComboBox.setItems(FXCollections.observableArrayList(cucine));
                    locationComboBox.setItems(FXCollections.observableArrayList(localita));
                    cuisineComboBox.setEditable(true);
                    locationComboBox.setEditable(true);
                });
                return null;
            }
        };

        initTask.setOnSucceeded(e -> {
            loadingLabel.setVisible(false);
            searchButton.setDisable(false);
            resetButton.setDisable(false);
            filteredRestaurants.setAll(allRestaurants);
            updateStatistics();
        });

        initTask.setOnFailed(e -> {
            loadingLabel.setText("Errore caricamento ristoranti");
            Throwable ex = initTask.getException();
            if (ex == null) {
                System.err.println("[EsploraRistoranti] Task fallito senza eccezione");
            } else {
                System.err.println("[EsploraRistoranti] Errore: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        });

        new Thread(initTask).start();
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
                    setText(null);
                    setGraphic(createRestaurantCard(r));
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

        restaurantListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Ristorante selected = restaurantListView.getSelectionModel().getSelectedItem();
                if (selected != null) openRestaurantDetails(selected);
            }
        });
    }

    private VBox createRestaurantCard(Ristorante ristorante) {
        VBox card = new VBox(10);
        card.getStyleClass().add("restaurant-card");

        HBox header = new HBox(12);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Text name = new Text(ristorante.getNome());
        name.getStyleClass().add("restaurant-name");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Label price = new Label(ristorante.getPrezzoStringa());
        price.getStyleClass().addAll("badge", "price-badge");

        header.getChildren().addAll(name, spacer, price);

        Text cuisine = new Text(String.join(", ", ristorante.getCucine()));
        cuisine.getStyleClass().add("restaurant-info");

        Text location = new Text(ristorante.getCitta()
                + (ristorante.getNazione() != null ? ", " + ristorante.getNazione() : ""));
        location.getStyleClass().add("restaurant-info");

        HBox badges = new HBox(8);
        if (ristorante.getStarCount() > 0) {
            Label michelin = new Label("★".repeat(ristorante.getStarCount()) + " Michelin");
            michelin.getStyleClass().addAll("badge", "michelin-badge");
            badges.getChildren().add(michelin);
        }
        if (ristorante.isGreenStar()) {
            Label green = new Label("Green Star");
            green.getStyleClass().addAll("badge", "success");
            badges.getChildren().add(green);
        }
        if (ristorante.isPrenotazioneOnline()) {
            Label booking = new Label("Prenotazione online");
            booking.getStyleClass().addAll("badge", "info");
            badges.getChildren().add(booking);
        }

        card.getChildren().addAll(header, cuisine, location, badges);
        return card;
    }

    @FXML
    private void handleSearch() {
        Predicate<Ristorante> predicate = buildPredicate();
        filteredRestaurants.setAll(allRestaurants.stream().filter(predicate).toList());
        updateStatistics();
    }

    @FXML
    private void handleReset() {
        searchField.clear();
        cuisineComboBox.setValue(null);
        locationComboBox.setValue(null);
        priceRangeComboBox.setValue(null);
        starsComboBox.setValue(null);
        serviceComboBox.setValue(null);
        deliveryCheckBox.setSelected(false);
        onlineBookingCheckBox.setSelected(false);
        filteredRestaurants.setAll(allRestaurants);
        updateStatistics();
    }

    @FXML
    private void handleAggiorna() {
        loadingLabel.setText("Aggiornamento ristoranti...");
        loadingLabel.setVisible(true);
        searchButton.setDisable(true);
        resetButton.setDisable(true);

        Task<List<Ristorante>> aggiornaTask = new Task<>() {
            @Override
            protected List<Ristorante> call() {
                return Main.getClient().cercaRistoranti(null);
            }
        };

        aggiornaTask.setOnSucceeded(e -> {
            allRestaurants = aggiornaTask.getValue();
            loadingLabel.setVisible(false);
            searchButton.setDisable(false);
            resetButton.setDisable(false);
            handleSearch();
        });

        aggiornaTask.setOnFailed(e -> {
            loadingLabel.setText("Errore aggiornamento");
            searchButton.setDisable(false);
            resetButton.setDisable(false);
            Throwable ex = aggiornaTask.getException();
            if (ex == null) {
                System.err.println("[EsploraRistoranti] Aggiorna: task fallito senza eccezione");
            } else {
                System.err.println("[EsploraRistoranti] Aggiorna: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        });

        new Thread(aggiornaTask).start();
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

    private Predicate<Ristorante> buildPredicate() {
        Predicate<Ristorante> p = r -> true;

        String searchText = searchField.getText() != null ? searchField.getText().trim() : "";
        if (!searchText.isEmpty()) {
            String lower = searchText.toLowerCase();
            p = p.and(r ->
                r.getNome().toLowerCase().contains(lower)
                || r.getCitta().toLowerCase().contains(lower)
                || r.getCucine().stream().anyMatch(c -> c.toLowerCase().contains(lower))
            );
        }

        String cucina = cuisineComboBox.getValue();
        if (cucina != null && !cucina.isBlank()) {
            String lower = cucina.toLowerCase();
            p = p.and(r -> r.getCucine().stream().anyMatch(c -> c.toLowerCase().contains(lower)));
        }

        String citta = locationComboBox.getValue();
        if (citta != null && !citta.isBlank()) {
            String lower = citta.toLowerCase();
            p = p.and(r -> r.getCitta() != null && r.getCitta().toLowerCase().contains(lower));
        }

        if (priceRangeComboBox.getValue() != null) {
            int livello = priceRangeComboBox.getValue().length();
            p = p.and(r -> r.getPrezzoLivello() == livello);
        }

        if (starsComboBox.getValue() != null) {
            String riconoscimento = switch (starsComboBox.getValue()) {
                case "1 Stella Michelin"    -> "1 Star";
                case "2 Stelle Michelin"    -> "2 Stars";
                case "3 Stelle Michelin"    -> "3 Stars";
                case "Bib Gourmand"         -> "Bib Gourmand";
                case "Selezionato Michelin" -> "Selected Restaurants";
                default -> null;
            };
            if (riconoscimento != null) {
                final String r2 = riconoscimento;
                p = p.and(r -> r2.equals(r.getRiconoscimento()));
            }
        }

        String servizio = serviceComboBox.getValue();
        if (servizio != null && !servizio.isBlank()) {
            String lower = servizio.toLowerCase();
            p = p.and(r -> r.getServizi().stream().anyMatch(s -> s.toLowerCase().contains(lower)));
        }

        if (deliveryCheckBox.isSelected())       p = p.and(Ristorante::isDelivery);
        if (onlineBookingCheckBox.isSelected())  p = p.and(Ristorante::isPrenotazioneOnline);

        return p;
    }

    private void openRestaurantDetails(Ristorante restaurant) {
        try {
            AppNavigator.show("/views/dettaglioRistorante.fxml", (DettaglioRistoranteController controller) -> {
                controller.setRistorante(restaurant);
                if (currentUser != null) controller.setCurrentUser(currentUser.getUsername());
                if (parentController != null) controller.setDashboardClienteParentController(parentController);
            });
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
            AppNavigator.show("/views/mapDialog.fxml", (MapDialogController controller) ->
                    controller.setRestaurant(restaurant));
        } catch (IOException e) {
            String mapUrl = String.format("https://www.google.com/maps?q=%f,%f",
                    restaurant.getLatitudine(), restaurant.getLongitudine());
            showAlert("Info", "Apri mappa", "URL: " + mapUrl);
        }
    }

    @FXML
    private void tornaDashboard() {
        AppNavigator.goBackOrClose(searchField);
    }

    private void updateStatistics() {
        int total = filteredRestaurants.size();
        long michelin = filteredRestaurants.stream().filter(r -> r.getStarCount() > 0).count();
        long green = filteredRestaurants.stream().filter(Ristorante::isGreenStar).count();

        totalRestaurantsLabel.setText(String.valueOf(total));
        michelinStarsLabel.setText(String.valueOf(michelin));
        greenStarsLabel.setText(String.valueOf(green));
        favoritesLabel.setText("0");
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
}
