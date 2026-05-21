package theknife.controllers;

import javafx.animation.PauseTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.SetChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;
import javafx.geometry.Insets;
import theknife.Main;
import theknife.client.ui.widgets.MultiSelectComboBox;
import theknife.models.Ristorante;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

    /** Filtro multi-selezione per il tipo di cucina. */
    @FXML private MultiSelectComboBox<String> cuisineComboBox;

    /** Filtro multi-selezione per la città. */
    @FXML private MultiSelectComboBox<String> cittaComboBox;

    /** Filtro multi-selezione per la nazione. */
    @FXML private MultiSelectComboBox<String> nazioneComboBox;

    /** Filtro multi-selezione per la fascia di prezzo. */
    @FXML private MultiSelectComboBox<String> priceRangeComboBox;

    /** Filtro multi-selezione per i riconoscimenti Michelin. */
    @FXML private MultiSelectComboBox<String> starsComboBox;

    /** Filtro multi-selezione per i servizi offerti. */
    @FXML private MultiSelectComboBox<String> serviceComboBox;

    /** Checkbox per filtrare solo i ristoranti con servizio a domicilio. */
    @FXML private CheckBox deliveryCheckBox;

    /** Checkbox per filtrare solo i ristoranti che accettano prenotazioni online. */
    @FXML private CheckBox onlineBookingCheckBox;

    /** Checkbox per filtrare solo i ristoranti insigniti di Stella Verde Michelin. */
    @FXML private CheckBox greenStarCheckBox;

    /** Pulsante per ripulire tutti i parametri di filtro. */
    @FXML private Button resetButton;

    /** Componente grafico per la visualizzazione dell'elenco dei ristoranti filtrati (VBox). */
    @FXML private VBox restaurantsContainer;

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

    private static final int ITEMS_PER_PAGE = 20;
    private int currentLimit = ITEMS_PER_PAGE;
    private Ristorante selectedRestaurant = null;
    private VBox selectedVisualCard = null;
    private final List<Ristorante> filteredList = new ArrayList<>();

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
        setupUI();

        priceRangeComboBox.getItems().setAll("€", "€€", "€€€", "€€€€");
        starsComboBox.getItems().setAll(
                "1 Stella Michelin", "2 Stelle Michelin", "3 Stelle Michelin",
                "Bib Gourmand", "Selezionato Michelin");

        loadingLabel.setText("Caricamento ristoranti...");
        loadingLabel.setVisible(true);
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
                    // Cattura selezioni prima di aggiornare le liste (diagnostica Raffinamento C)
                    Set<String> preServizi = new HashSet<>(serviceComboBox.getSelectedItems());
                    Set<String> preCucine  = new HashSet<>(cuisineComboBox.getSelectedItems());
                    Set<String> preCitta   = new HashSet<>(cittaComboBox.getSelectedItems());
                    Set<String> preNazioni = new HashSet<>(nazioneComboBox.getSelectedItems());

                    serviceComboBox.getItems().setAll(servizi);
                    cuisineComboBox.getItems().setAll(cucine);
                    cittaComboBox.getItems().setAll(citta);
                    nazioneComboBox.getItems().setAll(nazioni);

                    logSelezioniPerse("servizi", preServizi, serviceComboBox.getSelectedItems());
                    logSelezioniPerse("cucine",  preCucine,  cuisineComboBox.getSelectedItems());
                    logSelezioniPerse("città",   preCitta,   cittaComboBox.getSelectedItems());
                    logSelezioniPerse("nazioni", preNazioni, nazioneComboBox.getSelectedItems());
                });
                return null;
            }
        };

        initTask.setOnSucceeded(e -> {
            loadingLabel.setVisible(false);
            resetButton.setDisable(false);
            applyFilters();
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
        setupFilterListeners();
    }

    /**
     * Inizializza i controlli e lo stato iniziale per la selezione.
     */
    private void setupUI() {
        selectRestaurant(null, null);
    }

    /**
     * Gestisce graficamente la selezione di una card ristorante e aggiorna lo stato dei bottoni.
     */
    private void selectRestaurant(Ristorante restaurant, VBox card) {
        if (selectedVisualCard != null) {
            selectedVisualCard.getStyleClass().remove("selected-card");
        }
        selectedRestaurant = restaurant;
        selectedVisualCard = card;
        if (selectedVisualCard != null) {
            selectedVisualCard.getStyleClass().add("selected-card");
        }
        boolean hasSelection = selectedRestaurant != null;
        detailsButton.setDisable(!hasSelection);
        mapButton.setDisable(!hasSelection);
    }

    /**
     * Gestisce l'evento di ricerca al click sul bottone Cerca.
     */
    @FXML
    private void handleSearch() {
        applyFilters();
    }

    /**
     * Popola dinamicamente il contenitore VBox dei ristoranti con paginazione locale
     * per evitare lag di rendering con liste estese.
     */
    private void updateRestaurantList() {
        restaurantsContainer.getChildren().clear();
        int limit = Math.min(currentLimit, filteredList.size());
        for (int i = 0; i < limit; i++) {
            Ristorante r = filteredList.get(i);
            VBox card = createRestaurantCard(r);
            
            if (r.equals(selectedRestaurant)) {
                card.getStyleClass().add("selected-card");
                selectedVisualCard = card;
            }
            
            card.setOnMouseClicked(event -> {
                selectRestaurant(r, card);
                if (event.getClickCount() == 2) {
                    openRestaurantDetails(r);
                }
            });
            
            restaurantsContainer.getChildren().add(card);
        }
        
        if (filteredList.size() > limit) {
            int remaining = filteredList.size() - limit;
            Button loadMoreBtn = new Button("Mostra altri ristoranti (" + remaining + " rimanenti)");
            loadMoreBtn.getStyleClass().addAll("button", "outline");
            loadMoreBtn.setMaxWidth(Double.MAX_VALUE);
            loadMoreBtn.setOnAction(e -> {
                currentLimit += ITEMS_PER_PAGE;
                updateRestaurantList();
            });
            VBox.setMargin(loadMoreBtn, new Insets(10, 0, 0, 0));
            restaurantsContainer.getChildren().add(loadMoreBtn);
        }
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
     * Applica tutti i filtri correnti in locale e aggiorna la visualizzazione dei ristoranti.
     * Eseguito sul JavaFX Application Thread.
     */
    private void applyFilters() {
        filteredList.clear();
        filteredList.addAll(allRestaurants.stream().filter(buildPredicate()).collect(Collectors.toList()));
        currentLimit = ITEMS_PER_PAGE;
        selectRestaurant(null, null);
        updateRestaurantList();
        updateStatistics();
    }

    /**
     * Ripristina tutti i filtri di ricerca ai valori originari e ri-applica i filtri
     * (con tutto vuoto mostra tutti i ristoranti).
     */
    @FXML
    private void handleReset() {
        searchField.clear();
        cuisineComboBox.clear();
        cittaComboBox.clear();
        nazioneComboBox.clear();
        priceRangeComboBox.clear();
        starsComboBox.clear();
        serviceComboBox.clear();
        deliveryCheckBox.setSelected(false);
        onlineBookingCheckBox.setSelected(false);
        greenStarCheckBox.setSelected(false);
        applyFilters();
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
            resetButton.setDisable(false);
            applyFilters();
        });

        aggiornaTask.setOnFailed(e -> {
            loadingLabel.setText("Errore aggiornamento");
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
        if (selectedRestaurant != null) {
            openRestaurantDetails(selectedRestaurant);
        }
    }

    /**
     * Mostra la mappa della posizione del ristorante selezionato.
     */
    @FXML
    private void handleMap() {
        if (selectedRestaurant != null) {
            showOnMap(selectedRestaurant);
        }
    }

    /**
     * Genera un predicato composto che applica i filtri impostati dall'utente.
     *
     * @return il {@link Predicate} di filtraggio da applicare alla lista.
     */
    private Predicate<Ristorante> buildPredicate() {
        Predicate<Ristorante> p = r -> true;

        // Ricerca testuale libera: OR su nome, città, cucine (contains, case-insensitive)
        String searchText = searchField.getText() != null ? searchField.getText().trim() : "";
        if (!searchText.isEmpty()) {
            String lower = searchText.toLowerCase();
            p = p.and(r ->
                r.getNome().toLowerCase().contains(lower)
                || (r.getCitta() != null && r.getCitta().toLowerCase().contains(lower))
                || r.getCucine().stream().anyMatch(c -> c.toLowerCase().contains(lower))
            );
        }

        // Cucine: OR — almeno una cucina selezionata deve essere presente nel ristorante
        Set<String> cucineSelezionate = cuisineComboBox.getSelectedItems();
        if (!cucineSelezionate.isEmpty()) {
            p = p.and(r -> cucineSelezionate.stream().anyMatch(sel -> r.getCucine().contains(sel)));
        }

        // Città: OR — corrispondenza esatta
        Set<String> cittaSelezionate = cittaComboBox.getSelectedItems();
        if (!cittaSelezionate.isEmpty()) {
            p = p.and(r -> r.getCitta() != null && cittaSelezionate.contains(r.getCitta()));
        }

        // Nazione: OR — corrispondenza esatta
        Set<String> nazioniSelezionate = nazioneComboBox.getSelectedItems();
        if (!nazioniSelezionate.isEmpty()) {
            p = p.and(r -> r.getNazione() != null && nazioniSelezionate.contains(r.getNazione()));
        }

        // Prezzo: OR sui livelli numerici (lunghezza stringa "€€" = 2)
        Set<String> prezziSelezionati = priceRangeComboBox.getSelectedItems();
        if (!prezziSelezionati.isEmpty()) {
            Set<Integer> livelli = prezziSelezionati.stream()
                    .map(String::length)
                    .collect(Collectors.toSet());
            p = p.and(r -> livelli.contains(r.getPrezzoLivello()));
        }

        // Stelle Michelin: OR — mappa etichette italiane ai valori DB
        Set<String> stelleSelezionate = starsComboBox.getSelectedItems();
        if (!stelleSelezionate.isEmpty()) {
            Set<String> riconoscimenti = stelleSelezionate.stream()
                    .map(s -> switch (s) {
                        case "1 Stella Michelin"    -> "1 Star";
                        case "2 Stelle Michelin"    -> "2 Stars";
                        case "3 Stelle Michelin"    -> "3 Stars";
                        case "Bib Gourmand"         -> "Bib Gourmand";
                        case "Selezionato Michelin" -> "Selected Restaurants";
                        default -> null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!riconoscimenti.isEmpty()) {
                p = p.and(r -> riconoscimenti.contains(r.getRiconoscimento()));
            }
        }

        // Servizi: AND — il ristorante deve offrire TUTTI i servizi selezionati
        Set<String> serviziSelezionati = serviceComboBox.getSelectedItems();
        if (!serviziSelezionati.isEmpty()) {
            p = p.and(r -> serviziSelezionati.stream().allMatch(sel -> r.getServizi().contains(sel)));
        }

        if (deliveryCheckBox.isSelected())       p = p.and(Ristorante::isDelivery);
        if (onlineBookingCheckBox.isSelected())  p = p.and(Ristorante::isPrenotazioneOnline);
        if (greenStarCheckBox.isSelected())      p = p.and(Ristorante::isGreenStar);

        return p;
    }

    private void setupFilterListeners() {
        SetChangeListener<String> widgetListener = change -> applyFilters();
        cuisineComboBox.getSelectedItems().addListener(widgetListener);
        serviceComboBox.getSelectedItems().addListener(widgetListener);
        cittaComboBox.getSelectedItems().addListener(widgetListener);
        nazioneComboBox.getSelectedItems().addListener(widgetListener);
        starsComboBox.getSelectedItems().addListener(widgetListener);
        priceRangeComboBox.getSelectedItems().addListener(widgetListener);

        deliveryCheckBox.selectedProperty().addListener((obs, o, n) -> applyFilters());
        onlineBookingCheckBox.selectedProperty().addListener((obs, o, n) -> applyFilters());
        greenStarCheckBox.selectedProperty().addListener((obs, o, n) -> applyFilters());

        PauseTransition debounce = new PauseTransition(Duration.millis(150));
        debounce.setOnFinished(e -> applyFilters());
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            debounce.stop();
            debounce.playFromStart();
        });
    }

    private void logSelezioniPerse(String filtro, Set<String> prima, Set<String> dopo) {
        Set<String> perse = new HashSet<>(prima);
        perse.removeAll(dopo);
        if (!perse.isEmpty()) {
            System.err.println("[Aggiorna] Selezioni rimosse perché non più disponibili in "
                    + filtro + ": " + perse);
        }
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
        int total = filteredList.size();
        long michelin = filteredList.stream().filter(r -> r.getStarCount() > 0).count();
        long green = filteredList.stream().filter(Ristorante::isGreenStar).count();

        totalRestaurantsLabel.setText(String.valueOf(total));
        michelinStarsLabel.setText(String.valueOf(michelin));
        greenStarsLabel.setText(String.valueOf(green));
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
