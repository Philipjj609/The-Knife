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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Predicate;

/**
 * Controller JavaFX per la vista guest dell'applicazione (utente non autenticato).
 * Fa parte del pattern <b>MVC (Model-View-Controller)</b> nel ruolo di Controller.
 *
 * <p>Consente a un utente non autenticato (ospite) di consultare l'elenco dei ristoranti
 * presenti sul portale. Carica asincronamente i ristoranti e i filtri di ricerca dal server
 * tramite {@link Task} ed effettua ricerche e filtraggi reattivi in locale,
 * senza possibilità di interagire con le funzioni riservate (come preferiti o recensioni).</p>
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 * @version 3.0
 */
public class GuestViewController implements Initializable {

    /** Campo di testo per la ricerca libera per parole chiave. */
    @FXML private TextField searchField;

    /** ComboBox per filtrare i ristoranti in base alla cucina. */
    @FXML private ComboBox<String> cuisineComboBox;

    /** ComboBox per filtrare i ristoranti per città. */
    @FXML private ComboBox<String> cittaComboBox;

    /** ComboBox per filtrare i ristoranti per nazione. */
    @FXML private ComboBox<String> nazioneComboBox;

    /** ComboBox per filtrare per livello/fascia di prezzo. */
    @FXML private ComboBox<String> priceRangeComboBox;

    /** ComboBox per filtrare per riconoscimento Michelin. */
    @FXML private ComboBox<String> starsComboBox;

    /** ComboBox per filtrare per servizio aggiuntivo offerto. */
    @FXML private ComboBox<String> serviceComboBox;

    /** Checkbox per filtrare solo i ristoranti con servizio a domicilio. */
    @FXML private CheckBox deliveryCheckBox;

    /** Checkbox per filtrare solo i ristoranti che accettano prenotazioni online. */
    @FXML private CheckBox onlineBookingCheckBox;

    /** Checkbox per filtrare solo i ristoranti insigniti di Stella Verde Michelin. */
    @FXML private CheckBox greenStarCheckBox;

    /** Pulsante per attivare la ricerca applicando i filtri. */
    @FXML private Button searchButton;

    /** Pulsante per ripulire tutti i parametri di filtro. */
    @FXML private Button resetButton;

    /** ListView per mostrare l'elenco dei ristoranti filtrati. */
    @FXML private ListView<Ristorante> restaurantListView;

    /** Pulsante per accedere al dettaglio del ristorante selezionato. */
    @FXML private Button detailsButton;

    /** Pulsante per localizzare il ristorante selezionato sulla mappa. */
    @FXML private Button mapButton;

    /** Etichetta di testo per mostrare il numero totale di ristoranti corrispondenti ai filtri. */
    @FXML private Text totalRestaurantsLabel;

    /** Etichetta di testo per mostrare il numero di ristoranti stellati corrispondenti ai filtri. */
    @FXML private Text michelinStarsLabel;

    /** Etichetta di testo per mostrare il numero di ristoranti con stella verde Michelin corrispondenti ai filtri. */
    @FXML private Text greenStarsLabel;

    /** Etichetta di caricamento che indica l'avanzamento delle operazioni di rete. */
    @FXML private Label loadingLabel;

    /** Lista osservabile contenente l'elenco dei ristoranti correntemente filtrati. */
    private ObservableList<Ristorante> filteredRestaurants;

    /** Lista completa di tutti i ristoranti caricata all'avvio dal server. */
    private List<Ristorante> allRestaurants = new ArrayList<>();

    /**
     * Inizializza il controller. Configura la ListView, imposta le liste dei filtri statici,
     * e avvia un {@link Task} asincrono su un thread secondario per il caricamento iniziale dei
     * ristoranti e delle opzioni di ricerca dal server, evitando di bloccare la UI principale.
     *
     * @param location  l'URL della risorsa FXML (non utilizzato direttamente).
     * @param resources il ResourceBundle per l'internazionalizzazione (non utilizzato direttamente).
     */
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
        cittaComboBox.setEditable(true);
        nazioneComboBox.setEditable(true);

        loadingLabel.setText("Caricamento ristoranti...");
        loadingLabel.setVisible(true);
        searchButton.setDisable(true);
        resetButton.setDisable(true);

        Task<Void> initTask = new Task<>() {
            @Override
            protected Void call() {
                allRestaurants = Main.getClient().cercaRistoranti(null);

                List<String> servizi  = Main.getClient().getServizi();
                List<String> cucine   = Main.getClient().getCucine();
                List<String> citta    = Main.getClient().getCitta();
                List<String> nazioni  = Main.getClient().getNazioni();

                javafx.application.Platform.runLater(() -> {
                    serviceComboBox.setItems(FXCollections.observableArrayList(servizi));
                    cuisineComboBox.setItems(FXCollections.observableArrayList(cucine));
                    cittaComboBox.setItems(FXCollections.observableArrayList(citta));
                    nazioneComboBox.setItems(FXCollections.observableArrayList(nazioni));
                    cuisineComboBox.setEditable(true);
                    cittaComboBox.setEditable(true);
                    nazioneComboBox.setEditable(true);
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
                System.err.println("[GuestView] Task fallito senza eccezione");
            } else {
                System.err.println("[GuestView] Errore: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        });

        new Thread(initTask).start();
    }

    /**
     * Configura la ListView impostando la cell factory personalizzata per renderizzare
     * ciascun ristorante tramite una card visiva, e configura i listener di selezione.
     */
    private void setupUI() {
        restaurantListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Ristorante r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) {
                    setText(null); setGraphic(null);
                } else {
                    setText(null);
                    setGraphic(createRestaurantCard(r));
                }
            }
        });

        restaurantListView.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            boolean sel = n != null;
            detailsButton.setDisable(!sel);
            mapButton.setDisable(!sel);
        });

        restaurantListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                Ristorante selected = restaurantListView.getSelectionModel().getSelectedItem();
                if (selected != null) openRestaurantDetails(selected);
            }
        });
    }

    /**
     * Crea un componente grafico (VBox) per visualizzare in modo elegante i dati di un ristorante,
     * inclusi nome, cucina, località, prezzo e i vari badge di riconoscimento (Stelle Michelin,
     * Stella Verde, Prenotazione Online).
     *
     * @param ristorante il ristorante da rappresentare.
     * @return un {@link VBox} configurato con tutti i controlli grafici.
     */
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

    /**
     * Gestisce l'azione di ricerca e filtraggio dei ristoranti.
     * Filtra localmente l'elenco completo caricato in memoria e aggiorna la ListView.
     */
    @FXML
    private void handleSearch() {
        Predicate<Ristorante> predicate = buildPredicate();
        filteredRestaurants.setAll(allRestaurants.stream().filter(predicate).toList());
        updateStatistics();
    }

    /**
     * Ripristina tutti i filtri di ricerca ai valori originari e rinfresca la lista completa dei ristoranti.
     */
    @FXML
    private void handleReset() {
        searchField.clear();
        cuisineComboBox.setValue(null);
        cittaComboBox.setValue(null);
        nazioneComboBox.setValue(null);
        priceRangeComboBox.setValue(null);
        starsComboBox.setValue(null);
        serviceComboBox.setValue(null);
        deliveryCheckBox.setSelected(false);
        onlineBookingCheckBox.setSelected(false);
        greenStarCheckBox.setSelected(false);
        filteredRestaurants.setAll(allRestaurants);
        updateStatistics();
    }

    /**
     * Aggiorna asincronamente la lista dei ristoranti interrogando nuovamente il server.
     * Utilizza un {@link Task} asincrono per non congelare la UI durante la chiamata socket
     * verso {@code ClientTK}.
     */
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
                System.err.println("[GuestView] Aggiorna: task fallito senza eccezione");
            } else {
                System.err.println("[GuestView] Aggiorna: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        });

        new Thread(aggiornaTask).start();
    }

    /**
     * Mostra la finestra dei dettagli per il ristorante selezionato.
     */
    @FXML
    private void handleDetails() {
        Ristorante sel = restaurantListView.getSelectionModel().getSelectedItem();
        if (sel != null) openRestaurantDetails(sel);
    }

    /**
     * Mostra la mappa della posizione del ristorante selezionato.
     */
    @FXML
    private void handleMap() {
        Ristorante sel = restaurantListView.getSelectionModel().getSelectedItem();
        if (sel != null) showOnMap(sel);
    }

    /**
     * Genera un predicato composto che applica i filtri impostati dall'utente.
     *
     * @return il {@link Predicate} di filtraggio da applicare alla lista.
     */
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

        String citta = cittaComboBox.getValue();
        if (citta != null && !citta.isBlank()) {
            String lower = citta.toLowerCase();
            p = p.and(r -> r.getCitta() != null && r.getCitta().toLowerCase().contains(lower));
        }

        String nazione = nazioneComboBox.getValue();
        if (nazione != null && !nazione.isBlank()) {
            String lower = nazione.toLowerCase();
            p = p.and(r -> r.getNazione() != null && r.getNazione().toLowerCase().contains(lower));
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
        if (greenStarCheckBox.isSelected())      p = p.and(Ristorante::isGreenStar);

        return p;
    }

    /**
     * Apre la schermata di dettaglio per il ristorante specificato, impostando l'utente a null
     * in quanto l'utente ospite non è autenticato.
     *
     * @param restaurant il ristorante da visualizzare.
     */
    private void openRestaurantDetails(Ristorante restaurant) {
        try {
            AppNavigator.show("/views/dettaglioRistorante.fxml", (DettaglioRistoranteController controller) -> {
                controller.setRistorante(restaurant);
                controller.setCurrentUser(null);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Mostra la mappa della posizione del ristorante selezionato.
     * Se le coordinate GPS sono 0,0 mostra un alert informativo di errore.
     * Se l'apertura dialog fallisce (es. per problemi FXML), ripiega mostrando le coordinate testuali.
     *
     * @param restaurant il ristorante da visualizzare sulla mappa.
     */
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
            showAlert("Info", "Apri mappa", String.format("Coordinate: %f, %f",
                    restaurant.getLatitudine(), restaurant.getLongitudine()));
        }
    }

    /**
     * Aggiorna le etichette di testo con i conteggi statistici relativi all'elenco dei ristoranti filtrati.
     */
    private void updateStatistics() {
        totalRestaurantsLabel.setText(String.valueOf(filteredRestaurants.size()));
        michelinStarsLabel.setText(String.valueOf(
                filteredRestaurants.stream().filter(r -> r.getStarCount() > 0).count()));
        greenStarsLabel.setText(String.valueOf(
                filteredRestaurants.stream().filter(Ristorante::isGreenStar).count()));
    }

    /**
     * Helper per mostrare una finestra di dialogo di tipo {@link Alert.AlertType#INFORMATION}.
     *
     * @param title   il titolo della finestra.
     * @param header  l'intestazione dell'alert.
     * @param message il messaggio descrittivo da visualizzare.
     */
    private void showAlert(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
