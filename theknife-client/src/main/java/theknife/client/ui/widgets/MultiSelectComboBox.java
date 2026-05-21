package theknife.client.ui.widgets;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableSet;
import javafx.scene.control.Control;
import javafx.scene.control.Skin;
import javafx.util.StringConverter;

import java.util.Set;

/**
 * Controllo personalizzato JavaFX che implementa una ComboBox a selezione multipla con filtro testuale.
 * Permette all'utente di selezionare più elementi contemporaneamente tramite checkbox
 * e visualizza una sintesi degli elementi selezionati.
 *
 * @param <T> il tipo di elementi contenuti nella combo box
 *
 * @author Philip Jon Ji Ciuca, 761446, Sede CO
 * @author Samuele Secchi, 761031, Sede CO
 * @author Flavio Marin, 759910, Sede CO
 * @author Davide Caccia, 760742, Sede CO
 */
public class MultiSelectComboBox<T> extends Control {

    private final ObservableList<T> items = FXCollections.observableArrayList();
    private final ObservableSet<T> selectedItems = FXCollections.observableSet();
    private final ReadOnlyObjectWrapper<Set<T>> selectedItemsWrapper =
            new ReadOnlyObjectWrapper<>(selectedItems);
    private final StringProperty promptText = new SimpleStringProperty("");
    private final StringProperty itemNamePlural = new SimpleStringProperty("elementi");
    private final ObjectProperty<StringConverter<T>> converter =
            new SimpleObjectProperty<>(defaultConverter());

    /** Package-private: usata dalla Skin per implementare clear() sul TextField. */
    final StringProperty filterText = new SimpleStringProperty("");

    /**
     * Costruttore predefinito. Inizializza lo stile CSS e i listener
     * per sincronizzare gli elementi selezionati con la lista generale.
     */
    public MultiSelectComboBox() {
        getStyleClass().add("multi-select-combo-box");
        // Raffinamento 3: quando items cambia, rimuovi dalle selezioni gli item non più presenti
        items.addListener((ListChangeListener<T>) c -> selectedItems.retainAll(items));
    }

    private StringConverter<T> defaultConverter() {
        return new StringConverter<>() {
            @Override public String toString(T object) { return object == null ? "" : object.toString(); }
            @Override public T fromString(String string) { return null; }
        };
    }

    // -------------------------------------------------------------------------
    // Items
    // -------------------------------------------------------------------------

    /**
     * Imposta gli elementi disponibili nella ComboBox.
     *
     * @param newItems la lista di nuovi elementi da impostare
     */
    public void setItems(ObservableList<T> newItems) { items.setAll(newItems); }

    /**
     * Restituisce la lista osservabile degli elementi disponibili.
     *
     * @return la lista osservabile degli elementi
     */
    public ObservableList<T> getItems() { return items; }

    // -------------------------------------------------------------------------
    // Converter
    // -------------------------------------------------------------------------

    /**
     * Imposta il convertitore di stringhe per la rappresentazione testuale degli elementi.
     *
     * @param conv il convertitore da associare
     */
    public void setConverter(StringConverter<T> conv) {
        converter.set(conv != null ? conv : defaultConverter());
    }

    /**
     * Restituisce il convertitore di stringhe correntemente associato.
     *
     * @return il convertitore corrente
     */
    public StringConverter<T> getConverter() { return converter.get(); }

    /**
     * Proprietà associata al convertitore di stringhe degli elementi.
     *
     * @return la proprietà del convertitore
     */
    public ObjectProperty<StringConverter<T>> converterProperty() { return converter; }

    // -------------------------------------------------------------------------
    // Selezioni
    // -------------------------------------------------------------------------

    /**
     * Restituisce il set osservabile contenente gli elementi attualmente selezionati.
     *
     * @return il set degli elementi selezionati
     */
    public ObservableSet<T> getSelectedItems() { return selectedItems; }

    /**
     * Proprietà in sola lettura per monitorare l'insieme degli elementi selezionati.
     *
     * @return la proprietà in sola lettura per gli elementi selezionati
     */
    public ReadOnlyObjectProperty<Set<T>> selectedItemsProperty() {
        return selectedItemsWrapper.getReadOnlyProperty();
    }

    // -------------------------------------------------------------------------
    // PromptText
    // -------------------------------------------------------------------------

    /**
     * Imposta il testo di suggerimento (prompt) visualizzato quando non ci sono selezioni.
     *
     * @param text il testo di prompt da mostrare
     */
    public void setPromptText(String text) { promptText.set(text); }

    /**
     * Restituisce il testo di suggerimento (prompt) corrente.
     *
     * @return il testo di prompt
     */
    public String getPromptText() { return promptText.get(); }

    /**
     * Proprietà associata al testo di prompt del controllo.
     *
     * @return la proprietà del testo di prompt
     */
    public StringProperty promptTextProperty() { return promptText; }

    // -------------------------------------------------------------------------
    // ItemNamePlural
    // -------------------------------------------------------------------------

    /**
     * Imposta il nome al plurale per descrivere la tipologia di elementi controllati
     * (utilizzato nel sommario testuale es. "3 cucine selezionate").
     *
     * @param name il nome plurale degli elementi
     */
    public void setItemNamePlural(String name) { itemNamePlural.set(name); }

    /**
     * Restituisce il nome plurale degli elementi.
     *
     * @return il nome plurale
     */
    public String getItemNamePlural() { return itemNamePlural.get(); }

    /**
     * Proprietà associata al nome plurale utilizzato per la visualizzazione sintetica.
     *
     * @return la proprietà del nome plurale
     */
    public StringProperty itemNamePluralProperty() { return itemNamePlural; }

    // -------------------------------------------------------------------------
    // Azioni
    // -------------------------------------------------------------------------

    /**
     * Svuota tutte le selezioni attive ed azzera il filtro di ricerca testuale.
     */
    public void clear() {
        selectedItems.clear();
        filterText.set("");
    }

    /**
     * Crea e restituisce la skin associata a questo controllo.
     *
     * @return l'istanza della Skin {@link MultiSelectComboBoxSkin}
     */
    @Override
    protected Skin<?> createDefaultSkin() {
        return new MultiSelectComboBoxSkin<>(this);
    }
}
