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
import theknife.Main;
import theknife.client.ui.widgets.MultiSelectComboBox;
import theknife.models.Ristorante;
import theknife.models.Utente;

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
 * Controller JavaFX per la vista di esplorazione e filtraggio dei ristoranti.
 * Fa parte del pattern <b>MVC (Model-View-Controller)</b> nel ruolo di Controller.
 *
 * <p>Gestisce l'interazione utente per la ricerca avanzata e il filtraggio locale dei ristoranti.
 * All'inizializzazione, carica l'elenco completo dei ristoranti e i relativi metadati di filtro
 * (cucine, città, nazioni, servizi) dal server in modo asincrono tramite un thread secondario,
 * prevenendo il blocco del JavaFX Application Thread. Consente poi il filtraggio reattivo in locale
 * attraverso filtri combinati (testo, categorie, checkbox per stelle Michelin, Green Star, ecc.).
 * Supporta inoltre l'aggiunta/rimozione asincrona dei preferiti e l'apertura della visualizzazione
 * su mappa o del dettaglio del ristorante.</p>
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 * @version 3.0
 */
public class EsploraRistorantiController implements Initializable {

    /** Campo di testo per la ricerca testuale libera (nome, città, cucina). */
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

    /** Checkbox per filtrare solo i ristoranti che offrono servizio a domicilio. */
    @FXML private CheckBox deliveryCheckBox;

    /** Checkbox per filtrare solo i ristoranti che offrono la prenotazione online. */
    @FXML private CheckBox onlineBookingCheckBox;

    /** Checkbox per filtrare solo i ristoranti con il riconoscimento Stella Verde Michelin. */
    @FXML private CheckBox greenStarCheckBox;

    /** Pulsante per reimpostare tutti i filtri di ricerca ai valori iniziali. */
    @FXML private Button resetButton;

    /** Componente grafico per la visualizzazione dell'elenco dei ristoranti filtrati. */
    @FXML private ListView<Ristorante> restaurantListView;

    /** Pulsante per visualizzare la scheda di dettaglio del ristorante selezionato. */
    @FXML private Button detailsButton;

    /** Pulsante per aggiungere o rimuovere dai preferiti il ristorante selezionato. */
    @FXML private Button aggiungiPreferitiButton;

    /** Pulsante per mostrare la posizione del ristorante selezionato sulla mappa. */
    @FXML private Button mapButton;

    /** Testo che mostra il numero totale di ristoranti corrispondenti ai filtri correnti. */
    @FXML private Text totalRestaurantsLabel;

    /** Testo che mostra il conteggio dei ristoranti con stelle Michelin correntemente visualizzati. */
    @FXML private Text michelinStarsLabel;

    /** Testo che mostra il conteggio dei ristoranti con stelle verdi Michelin correntemente visualizzati. */
    @FXML private Text greenStarsLabel;

    /** Testo segnaposto/contatore per i preferiti (attualmente non utilizzato direttamente). */
    @FXML private Text favoritesLabel;

    /** Etichetta di stato per indicare il caricamento in corso dei dati dal server. */
    @FXML private Label loadingLabel;

    /** Lista osservabile contenente i ristoranti attualmente filtrati e mostrati nella ListView. */
    private ObservableList<Ristorante> filteredRestaurants;

    /** Lista completa dei ristoranti scaricata dal server, usata come base per i filtri locali. */
    private List<Ristorante> allRestaurants = new ArrayList<>();

    /** Utente attualmente autenticato nel client. */
    private Utente currentUser;

    /** Riferimento al controller della dashboard cliente genitore per notificare aggiornamenti. */
    private DashboardClienteController parentController;

    /**
     * Inizializza il controller, configurando la ListView dei ristoranti, i filtri fissi
     * e avviando il caricamento asincrono dei dati (ristoranti e metadati di ricerca) dal server.
     *
     * <p>Utilizza un {@link Task} asincrono eseguito su un thread separato per effettuare
     * le chiamate socket bloccanti verso il server. Una volta ottenuti i dati, aggiorna la UI
     * sul JavaFX Application Thread tramite {@link Platform#runLater(Runnable)} e i callback
     * del task ({@code setOnSucceeded} e {@code setOnFailed}).</p>
     *
     * @param location  l'URL utilizzato per risolvere i percorsi relativi dell'oggetto radice, o null.
     * @param resources le risorse utilizzate per localizzare l'oggetto radice, o null.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        filteredRestaurants = FXCollections.observableArrayList();
        restaurantListView.setItems(filteredRestaurants);
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
                System.err.println("[EsploraRistoranti] Task fallito senza eccezione");
            } else {
                System.err.println("[EsploraRistoranti] Errore: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        });

        new Thread(initTask).start();
        setupFilterListeners();
    }

    /**
     * Imposta l'utente correntemente loggato e aggiorna le statistiche della schermata.
     *
     * @param user l'utente autenticato correntemente.
     */
    public void setCurrentUser(Utente user) {
        this.currentUser = user;
        updateStatistics();
    }

    /**
     * Imposta il controller genitore della dashboard cliente per consentire
     * la notifica e l'aggiornamento dei dati a seguito di modifiche.
     *
     * @param parent il controller della dashboard cliente.
     */
    public void setParentController(DashboardClienteController parent) {
        this.parentController = parent;
    }

    /**
     * Configura gli elementi della ListView, impostando la cell factory personalizzata
     * per le card dei ristoranti e gli eventi di selezione e doppio click.
     */
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

    /**
     * Crea graficamente il componente (card) associato a un singolo ristorante.
     * Genera dinamicamente le etichette (badge) per il prezzo, stelle Michelin,
     * stella verde e disponibilità della prenotazione online.
     *
     * @param ristorante il ristorante da rappresentare.
     * @return un {@link VBox} contenente tutti gli elementi grafici del ristorante.
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

    /** Applica tutti i filtri correnti in locale e aggiorna la ListView. Eseguito sul FX thread. */
    private void applyFilters() {
        filteredRestaurants.setAll(allRestaurants.stream().filter(buildPredicate()).toList());
        updateStatistics();
    }

    /**
     * Gestisce l'evento di reset dei filtri. Pulisce tutti i widget e ri-applica i filtri
     * (che con tutto vuoto mostra tutti i ristoranti).
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
     * Esegue l'aggiornamento dell'elenco completo dei ristoranti richiedendoli al server.
     *
     * <p>Questa operazione avviene asincronamente tramite un {@link Task} per non bloccare
     * l'interfaccia utente durante la chiamata di rete (tramite la Facade {@code ClientTK}).
     * In caso di successo, aggiorna l'elenco locale e riapplica i filtri correnti.</p>
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
                System.err.println("[EsploraRistoranti] Aggiorna: task fallito senza eccezione");
            } else {
                System.err.println("[EsploraRistoranti] Aggiorna: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        });

        new Thread(aggiornaTask).start();
    }

    /**
     * Mostra la finestra di dettaglio del ristorante attualmente selezionato.
     */
    @FXML
    private void handleDetails() {
        Ristorante selected = restaurantListView.getSelectionModel().getSelectedItem();
        if (selected != null) openRestaurantDetails(selected);
    }

    /**
     * Aggiunge o rimuove dai preferiti dell'utente il ristorante selezionato.
     * Richiede che ci sia sia un ristorante selezionato sia un utente autenticato.
     */
    @FXML
    private void handleAggiungiPreferiti() {
        Ristorante selected = restaurantListView.getSelectionModel().getSelectedItem();
        if (selected != null && currentUser != null) toggleFavorite(selected);
    }

    /**
     * Mostra la posizione del ristorante selezionato sulla mappa.
     * Se le coordinate GPS sono nulle o pari a zero, mostra un avviso all'utente.
     */
    @FXML
    private void handleMap() {
        Ristorante selected = restaurantListView.getSelectionModel().getSelectedItem();
        if (selected != null) showOnMap(selected);
    }

    /**
     * Costruisce un predicato combinando tutti i filtri inseriti dall'utente.
     * Consente la ricerca parziale sul nome, sulla città e sulla cucina,
     * oltre al filtraggio esatto per nazione, fascia di prezzo, riconoscimenti Michelin,
     * servizi specifici e caratteristiche speciali (domicilio, prenotazione online, stella verde).
     *
     * @return un {@link Predicate} di Ristorante da applicare allo stream dei ristoranti.
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

        // Città: OR — corrispondenza esatta (i valori del widget provengono dal server)
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

    /**
     * Apre la scena di dettaglio per il ristorante specificato, passando
     * i riferimenti necessari e configurando il controller destinazione.
     *
     * @param restaurant il ristorante di cui mostrare i dettagli.
     */
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

    /**
     * Aggiunge o rimuove asincronamente un ristorante dai preferiti dell'utente.
     * Interroga prima il server per verificare lo stato corrente del preferito,
     * dopodiché invia la richiesta di aggiunta o rimozione.
     * Al completamento, aggiorna la vista corrente e la dashboard cliente genitore
     * sul thread grafico.
     *
     * @param restaurant il ristorante da inserire o rimuovere.
     */
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

    /**
     * Mostra la mappa geografica con la posizione del ristorante.
     * Se non è possibile caricare la finestra di dialogo FXML della mappa,
     * tenta di mostrare un alert informativo contenente il link di Google Maps
     * come meccanismo di fallback.
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
            String mapUrl = String.format("https://www.google.com/maps?q=%f,%f",
                    restaurant.getLatitudine(), restaurant.getLongitudine());
            showAlert("Info", "Apri mappa", "URL: " + mapUrl);
        }
    }

    /**
     * Ritorna alla dashboard o chiude la schermata corrente.
     */
    @FXML
    private void tornaDashboard() {
        AppNavigator.goBackOrClose(searchField);
    }

    /**
     * Ricalcola le statistiche riassuntive sui ristoranti correntemente
     * filtrati (numero totale, ristoranti stellati, stella verde) e aggiorna
     * le relative etichette testuali.
     */
    private void updateStatistics() {
        int total = filteredRestaurants.size();
        long michelin = filteredRestaurants.stream().filter(r -> r.getStarCount() > 0).count();
        long green = filteredRestaurants.stream().filter(Ristorante::isGreenStar).count();

        totalRestaurantsLabel.setText(String.valueOf(total));
        michelinStarsLabel.setText(String.valueOf(michelin));
        greenStarsLabel.setText(String.valueOf(green));
        favoritesLabel.setText("0");
    }

    /**
     * Visualizza un dialogo di tipo {@link Alert.AlertType#INFORMATION} all'utente.
     *
     * @param title   il titolo della finestra di dialogo.
     * @param header  l'intestazione dell'alert.
     * @param message il messaggio descrittivo da mostrare.
     */
    private void showAlert(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Rinfresca graficamente la ListView dei ristoranti e aggiorna
     * i contatori statistici in base ai filtri attuali.
     */
    public void refreshView() {
        restaurantListView.refresh();
        updateStatistics();
    }

    /**
     * Registra i listener reattivi su tutti i widget di filtro.
     * I MultiSelectComboBox notificano tramite SetChangeListener, i CheckBox tramite selectedProperty,
     * la searchField con un debounce di 150 ms per non filtrare a ogni singolo keystroke.
     */
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

    /**
     * Logga le selezioni rimosse automaticamente quando la lista di un widget viene aggiornata
     * (Raffinamento 3: retainAll). Utile per diagnosticare perdite di selezione dopo un aggiorna.
     */
    private void logSelezioniPerse(String filtro, Set<String> prima, Set<String> dopo) {
        Set<String> perse = new HashSet<>(prima);
        perse.removeAll(dopo);
        if (!perse.isEmpty()) {
            System.err.println("[Aggiorna] Selezioni rimosse perché non più disponibili in "
                    + filtro + ": " + perse);
        }
    }
}
