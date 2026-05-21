package theknife.controllers;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import theknife.Main;
import theknife.models.Recensione;
import theknife.models.Risposta;
import theknife.models.Ristorante;
import theknife.models.Utente;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Controller JavaFX per la dashboard riservata all'utente con ruolo "Ristoratore".
 * Fa parte del pattern <b>MVC (Model-View-Controller)</b> nel ruolo di Controller.
 *
 * <p>Gestisce la visualizzazione dei ristoranti di proprietà del ristoratore loggato
 * e di tutte le recensioni ad essi collegate. Fornisce statistiche aggregate (numero ristoranti,
 * totale recensioni ricevute, media stelle generale, risposte ancora da inviare), consente
 * di filtrare le recensioni per singolo ristorante e permette di rispondere direttamente alle recensioni
 * o inserire un nuovo ristorante. Le chiamate di rete al server avvengono tramite la Facade {@link theknife.client.ClientTK}
 * in modo asincrono su un thread secondario, aggiornando successivamente la UI nel JavaFX Application Thread.</p>
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 * @version 2.0
 */
public class DashboardRistoratoreController implements Initializable {

    /**
     * Costruttore di default per {@link DashboardRistoratoreController}.
     * Necessario per l'inizializzazione tramite FXML loader.
     */
    public DashboardRistoratoreController() {}

    /** Testo di benvenuto personalizzato con il nome del ristoratore. */
    @FXML private Text benvenutoLabel;

    /** Contatore dei ristoranti posseduti dal ristoratore. */
    @FXML private Text numRistorantiLabel;

    /** Contatore complessivo delle recensioni ricevute da tutti i ristoranti del ristoratore. */
    @FXML private Text totalRecensioniLabel;

    /** Valutazione media complessiva di tutti i ristoranti del ristoratore, con rappresentazione grafica a stelle. */
    @FXML private Text mediaGeneraleLabel;

    /** Contatore delle recensioni che non hanno ancora ricevuto una risposta. */
    @FXML private Text risposteDaInviareLabel;

    /** ListView per mostrare l'elenco dei ristoranti posseduti. */
    @FXML private ListView<Ristorante> ristorantiListView;

    /** ListView per mostrare l'elenco delle recensioni ricevute. */
    @FXML private ListView<Recensione> recensioniListView;

    /** ComboBox per filtrare le recensioni in base ad un ristorante specifico. */
    @FXML private ComboBox<String> filtroRistoranteCombo;

    /** Label mostrata qualora l'elenco dei ristoranti sia vuoto. */
    @FXML private Label nessunRistoranteLabel;

    /** Label mostrata qualora l'elenco delle recensioni sia vuoto. */
    @FXML private Label nessueRecensioniRistoratoreLabel;

    /** Pannello a schede (TabPane) per navigare tra ristoranti e recensioni. */
    @FXML private TabPane tabPane;

    /** L'utente ristoratore correntemente autenticato. */
    private Utente currentUser;

    /** Lista locale dei ristoranti associati al ristoratore. */
    private List<Ristorante> ristorantiUtente;

    /** Lista locale di tutte le recensioni ricevute per tutti i ristoranti del ristoratore. */
    private List<Recensione> tutteRecensioni;

    /**
     * Inizializza il controller JavaFX. Associa i custom cell factory per le ListView dei ristoranti
     * e delle recensioni per visualizzarli come schede (card) personalizzate.
     * Metodo richiamato automaticamente dopo il caricamento del file FXML.
     *
     * @param location l'URL di localizzazione del file FXML
     * @param resources il bundle di risorse localizzate
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupRistorantiListView();
        setupRecensioniListView();
        setupFiltroRistoranteCombo();
    }

    private void setupFiltroRistoranteCombo() {
        filtroRistoranteCombo.setButtonCell(new ListCell<>() {
            /**
             * Aggiorna l'elemento grafico della cella associando il nome del ristorante o il prompt text.
             *
             * @param item la stringa
             * @param empty true se la cella è vuota
             */
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null || item.isBlank()
                        ? filtroRistoranteCombo.getPromptText()
                        : item);
            }
        });
    }

    /**
     * Associa l'utente ristoratore loggato ed avvia il caricamento asincrono dei dati.
     *
     * @param user l'utente ristoratore loggato
     */
    public void setCurrentUser(Utente user) {
        this.currentUser = user;
        loadUserData();
    }

    /**
     * Imposta il cell factory per la ListView dei ristoranti, delegando la creazione grafica alla card.
     */
    private void setupRistorantiListView() {
        ristorantiListView.setCellFactory(lv -> new ListCell<>() {
            /**
             * Aggiorna l'elemento grafico della cella associando la card del ristorante.
             *
             * @param r l'oggetto ristorante
             * @param empty true se la cella è vuota
             */
            @Override
            protected void updateItem(Ristorante r, boolean empty) {
                super.updateItem(r, empty);
                setGraphic(empty || r == null ? null : createRistoranteCard(r));
            }
        });
    }

    /**
     * Imposta il cell factory per la ListView delle recensioni, delegando la creazione grafica alla card del ristoratore.
     */
    private void setupRecensioniListView() {
        recensioniListView.setCellFactory(lv -> new ListCell<>() {
            /**
             * Aggiorna l'elemento grafico della cella associando la card della recensione per il ristoratore.
             *
             * @param r l'oggetto recensione
             * @param empty true se la cella è vuota
             */
            @Override
            protected void updateItem(Recensione r, boolean empty) {
                super.updateItem(r, empty);
                setGraphic(empty || r == null ? null : createRecensioneCardRistoratore(r));
            }
        });
    }

    /**
     * Avvia un thread in background che richiede asincronamente al server i ristoranti di proprietà dell'utente
     * e tutte le recensioni a loro collegate tramite {@link theknife.client.ClientTK}. Al successo, inserisce gli elementi
     * nelle relative liste e aggiorna le statistiche aggregate e i filtri di selezione nella UI.
     */
    private void loadUserData() {
        if (currentUser == null) return;

        benvenutoLabel.setText("Benvenuto, " + currentUser.getNome() + "!");

        Task<Void> task = new Task<>() {
            /**
             * Esegue il caricamento asincrono dei ristoranti e delle recensioni associate dal server.
             *
             * @return null
             */
            @Override
            protected Void call() {
                ristorantiUtente = Main.getClient().getRistorantiProprietario(currentUser.getId());

                List<Long> ristIds = ristorantiUtente.stream()
                        .map(Ristorante::getId)
                        .collect(Collectors.toList());

                tutteRecensioni = ristIds.isEmpty()
                        ? List.of()
                        : Main.getClient().getRecensioniRistoratori(ristIds);
                return null;
            }

            /**
             * Aggiorna le liste grafiche e le statistiche aggregate al completamento del caricamento dati.
             */
            @Override
            protected void succeeded() {
                ristorantiListView.setItems(FXCollections.observableArrayList(ristorantiUtente));
                recensioniListView.setItems(FXCollections.observableArrayList(tutteRecensioni));

                List<String> nomi = ristorantiUtente.stream()
                        .map(Ristorante::getNome)
                        .collect(Collectors.toList());
                filtroRistoranteCombo.setItems(FXCollections.observableArrayList(nomi));

                updateStatistiche(ristorantiUtente, tutteRecensioni);
                nessunRistoranteLabel.setVisible(ristorantiUtente.isEmpty());
                nessueRecensioniRistoratoreLabel.setVisible(tutteRecensioni.isEmpty());
            }
        };
        new Thread(task).start();
    }

    /**
     * Calcola le statistiche riassuntive del ristoratore (numero ristoranti, recensioni totali, media stelle,
     * e risposte in attesa) impostando i relativi testi nella UI.
     *
     * @param ristoranti la lista dei ristoranti del ristoratore
     * @param recensioni la lista delle recensioni collegate
     */
    private void updateStatistiche(List<Ristorante> ristoranti, List<Recensione> recensioni) {
        numRistorantiLabel.setText(String.valueOf(ristoranti.size()));
        updateStatisticheRecensioni(recensioni);
    }

    /**
     * Aggiorna le statistiche basate sulle recensioni attualmente visualizzate.
     *
     * @param recensioni la lista di recensioni da rappresentare nelle statistiche
     */
    private void updateStatisticheRecensioni(List<Recensione> recensioni) {
        totalRecensioniLabel.setText(String.valueOf(recensioni.size()));

        if (recensioni.isEmpty()) {
            mediaGeneraleLabel.setText("N/A");
        } else {
            double media = recensioni.stream().mapToInt(Recensione::getValutazione).average().orElse(0.0);
            String stelle = "★".repeat((int) Math.round(media)) + "☆".repeat(5 - (int) Math.round(media));
            mediaGeneraleLabel.setText(String.format("%.1f %s", media, stelle));
        }

        long risposteDaInviare = recensioni.stream().filter(r -> r.getRisposta() == null).count();
        risposteDaInviareLabel.setText(String.valueOf(risposteDaInviare));
    }

    /**
     * Crea graficamente una card (VBox) che rappresenta un ristorante del ristoratore.
     * Mostra dettagli quali nome, cucine, città, telefono e riconoscimenti Michelin. Fornisce inoltre
     * pulsanti per visualizzarne il dettaglio o per passare direttamente al tab recensioni pre-filtrato
     * per quel ristorante.
     *
     * @param ristorante l'oggetto {@link Ristorante} da mostrare
     * @return la VBox contenente la scheda grafica del ristorante
     */
    private VBox createRistoranteCard(Ristorante ristorante) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 8; " +
                "-fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2);");

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Text nome = new Text(ristorante.getNome());
        nome.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        Text cucina = new Text(String.join(", ", ristorante.getCucine()));
        cucina.setStyle("-fx-font-size: 12; -fx-fill: #6c757d;");

        header.getChildren().addAll(nome, cucina);

        Text localita = new Text("📍 " + ristorante.getCitta());
        localita.setStyle("-fx-font-size: 12; -fx-fill: #495057;");

        Text telefono = new Text("📞 " + (ristorante.getTelefono() != null ? ristorante.getTelefono() : "N/A"));
        telefono.setStyle("-fx-font-size: 12; -fx-fill: #495057;");

        HBox stelleBox = new HBox(10);
        if (ristorante.getStarCount() > 0) {
            Text s = new Text("★".repeat(ristorante.getStarCount()) + " Michelin");
            s.setStyle("-fx-font-size: 12; -fx-fill: #f39c12; -fx-font-weight: bold;");
            stelleBox.getChildren().add(s);
        }
        if (ristorante.isGreenStar()) {
            Text gs = new Text("🌟 Green Star");
            gs.setStyle("-fx-font-size: 12; -fx-fill: #27ae60; -fx-font-weight: bold;");
            stelleBox.getChildren().add(gs);
        }

        HBox buttonsBox = new HBox(10);
        Button visualizzaBtn = new Button("Visualizza Dettagli");
        visualizzaBtn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-size: 10;");
        visualizzaBtn.setOnAction(event -> apriDettaglioRistorante(ristorante));

        Button recensioniBtn = new Button("Vedi Recensioni");
        recensioniBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-size: 10;");
        recensioniBtn.setOnAction(event -> {
            tabPane.getSelectionModel().select(1);
            filtroRistoranteCombo.setValue(ristorante.getNome());
            handleFiltroRistorante();
        });

        buttonsBox.getChildren().addAll(visualizzaBtn, recensioniBtn);
        card.getChildren().addAll(header, localita, telefono, stelleBox, buttonsBox);
        card.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                apriDettaglioRistorante(ristorante);
            }
        });
        return card;
    }

    /**
     * Crea graficamente una card (VBox) che rappresenta una recensione ricevuta da uno dei ristoranti.
     * Visualizza stelle, nome del ristorante recensito, autore, data, titolo e commento.
     * Mostra inoltre l'eventuale risposta già fornita o presenta un pulsante "Rispondi alla Recensione"
     * per aprirne la finestra di inserimento risposta.
     *
     * @param recensione l'oggetto {@link Recensione} ricevuto
     * @return la VBox contenente la scheda grafica della recensione
     */
    private VBox createRecensioneCardRistoratore(Recensione recensione) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-border-color: #dee2e6; -fx-border-radius: 8; " +
                "-fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2);");

        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Text ristorante = new Text(recensione.getNomeRistorante());
        ristorante.setStyle("-fx-font-size: 16; -fx-font-weight: bold;");

        Text stelle = new Text(recensione.getStelle());
        stelle.setStyle("-fx-font-size: 14; -fx-fill: #f39c12;");

        Text cliente = new Text("di " + recensione.getUsernameCliente());
        cliente.setStyle("-fx-font-size: 12; -fx-fill: #6c757d;");

        Text data = new Text(recensione.getDataRecensioneFormatted());
        data.setStyle("-fx-font-size: 10; -fx-fill: #6c757d;");

        header.getChildren().addAll(ristorante, stelle, cliente, data);

        Text titolo = new Text(recensione.getTitolo());
        titolo.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        Text commento = new Text(recensione.getCommento());
        commento.setStyle("-fx-font-size: 12; -fx-fill: #495057;");
        commento.setWrappingWidth(500);

        card.getChildren().addAll(header, titolo, commento);

        if (recensione.getRisposta() != null) {
            VBox rispostaBox = new VBox(5);
            rispostaBox.setStyle("-fx-background-color: #e8f5e8; -fx-padding: 10; -fx-border-radius: 5;");

            Text rispostaHeader = new Text("✅ La tua risposta (" +
                    recensione.getRisposta().getDataRispostaFormatted() + "):");
            rispostaHeader.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-fill: #2e7d32;");

            Text rispostaTesto = new Text(recensione.getRisposta().getTesto());
            rispostaTesto.setStyle("-fx-font-size: 11; -fx-fill: #2e7d32;");
            rispostaTesto.setWrappingWidth(480);

            HBox rispostaActions = new HBox(10);
            Button modificaRispostaBtn = new Button("Modifica risposta");
            modificaRispostaBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-size: 11;");
            modificaRispostaBtn.setOnAction(event -> apriModificaRisposta(recensione));

            Button eliminaRispostaBtn = new Button("Elimina risposta");
            eliminaRispostaBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-font-size: 11;");
            eliminaRispostaBtn.setOnAction(event -> confermaEliminaRisposta(recensione));

            rispostaActions.getChildren().addAll(modificaRispostaBtn, eliminaRispostaBtn);
            rispostaBox.getChildren().addAll(rispostaHeader, rispostaTesto, rispostaActions);
            card.getChildren().add(rispostaBox);
        } else {
            Button rispondiBtn = new Button("💬 Rispondi alla Recensione");
            rispondiBtn.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-size: 11;");
            rispondiBtn.setOnAction(event -> apriFinstraRisposta(recensione));
            card.getChildren().add(rispondiBtn);
        }

        return card;
    }

    /**
     * Gestisce l'azione di click sul pulsante Aggiungi Ristorante, mostrando la schermata di creazione ristorante.
     */
    @FXML
    private void handleAggiungiRistorante() {
        try {
            AppNavigator.show("/views/aggiungiRistorante.fxml", (AggiungiRistoranteController controller) -> {
                controller.setCurrentUser(currentUser);
                controller.setParentController(this);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gestisce il filtro sulle recensioni in base al ristorante selezionato nella ComboBox.
     */
    @FXML
    private void handleFiltroRistorante() {
        String ristoranteSelezionato = filtroRistoranteCombo.getValue();
        if (ristoranteSelezionato == null || ristoranteSelezionato.isEmpty()) return;

        List<Recensione> filtrate = tutteRecensioni.stream()
                .filter(r -> r.getNomeRistorante().equals(ristoranteSelezionato))
                .collect(Collectors.toList());
        recensioniListView.setItems(FXCollections.observableArrayList(filtrate));
        updateStatisticheRecensioni(filtrate);
        nessueRecensioniRistoratoreLabel.setVisible(filtrate.isEmpty());
    }

    /**
     * Rimuove il filtro di selezione ristoranti e mostra tutte le recensioni collegate.
     */
    @FXML
    private void handleMostraTutte() {
        filtroRistoranteCombo.getSelectionModel().clearSelection();
        filtroRistoranteCombo.setValue(null);
        filtroRistoranteCombo.setPromptText("Filtra per ristorante");
        recensioniListView.setItems(FXCollections.observableArrayList(tutteRecensioni));
        updateStatisticheRecensioni(tutteRecensioni);
        nessueRecensioniRistoratoreLabel.setVisible(tutteRecensioni.isEmpty());
    }

    /**
     * Apre la schermata di dettaglio di un ristorante specifico.
     *
     * @param ristorante il ristorante {@link Ristorante} da visualizzare
     */
    private void apriDettaglioRistorante(Ristorante ristorante) {
        try {
            AppNavigator.show("/views/dettaglioRistorante.fxml", (DettaglioRistoranteController controller) -> {
                controller.setRistorante(ristorante);
                controller.setCurrentUser(currentUser.getUsername());
                controller.setIsProprietario(true);
                controller.setDashboardRistoratoreParentController(this);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Apre la finestra per inviare o modificare una risposta ad una determinata recensione.
     *
     * @param recensione la recensione {@link Recensione} a cui rispondere
     */
    private void apriFinstraRisposta(Recensione recensione) {
        try {
            AppNavigator.show("/views/rispondiRecensione.fxml", (RispondiRecensioneController controller) -> {
                controller.setRecensione(recensione);
                controller.setCurrentUser(currentUser.getUsername());
                controller.setParentController(this);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void apriModificaRisposta(Recensione recensione) {
        Risposta risposta = recensione.getRisposta();
        if (risposta == null) return;

        try {
            AppNavigator.show("/views/rispondiRecensione.fxml", (RispondiRecensioneController controller) -> {
                controller.setRecensione(recensione);
                controller.setCurrentUser(currentUser.getUsername());
                controller.setParentController(this);
                controller.setRispostaEsistente(risposta);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void confermaEliminaRisposta(Recensione recensione) {
        Risposta risposta = recensione.getRisposta();
        if (risposta == null) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Elimina risposta");
        confirm.setHeaderText("Eliminare la risposta a questa recensione?");
        confirm.setContentText("La recensione restera visibile, ma la tua risposta verra rimossa.");
        confirm.showAndWait()
                .filter(button -> button == ButtonType.OK)
                .ifPresent(button -> eliminaRisposta(risposta.getId()));
    }

    private void eliminaRisposta(long rispostaId) {
        Task<Boolean> task = new Task<>() {
            /**
             * Esegue la richiesta asincrona di eliminazione della risposta sul server.
             *
             * @return true se l'operazione è completata
             */
            @Override
            protected Boolean call() {
                return Main.getClient().eliminaRisposta(rispostaId);
            }
        };

        task.setOnSucceeded(event -> refreshData());
        task.setOnFailed(event -> showAlert("Errore", "Impossibile eliminare la risposta",
                task.getException().getMessage()));

        new Thread(task).start();
    }

    /**
     * Helper per mostrare messaggi informativi a schermo tramite finestre di dialogo JavaFX.
     *
     * @param title titolo del dialogo
     * @param header testata del dialogo
     * @param message corpo del messaggio
     */
    private void showAlert(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Aggiorna e ricarica i dati del ristoratore effettuando una nuova chiamata al server.
     */
    public void refreshData() {
        if (currentUser != null) loadUserData();
    }
}
