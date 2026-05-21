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

    public void setItems(ObservableList<T> newItems) { items.setAll(newItems); }
    public ObservableList<T> getItems() { return items; }

    // -------------------------------------------------------------------------
    // Converter
    // -------------------------------------------------------------------------

    public void setConverter(StringConverter<T> conv) {
        converter.set(conv != null ? conv : defaultConverter());
    }
    public StringConverter<T> getConverter() { return converter.get(); }
    public ObjectProperty<StringConverter<T>> converterProperty() { return converter; }

    // -------------------------------------------------------------------------
    // Selezioni
    // -------------------------------------------------------------------------

    public ObservableSet<T> getSelectedItems() { return selectedItems; }
    public ReadOnlyObjectProperty<Set<T>> selectedItemsProperty() {
        return selectedItemsWrapper.getReadOnlyProperty();
    }

    // -------------------------------------------------------------------------
    // PromptText
    // -------------------------------------------------------------------------

    public void setPromptText(String text) { promptText.set(text); }
    public String getPromptText() { return promptText.get(); }
    public StringProperty promptTextProperty() { return promptText; }

    // -------------------------------------------------------------------------
    // ItemNamePlural
    // -------------------------------------------------------------------------

    public void setItemNamePlural(String name) { itemNamePlural.set(name); }
    public String getItemNamePlural() { return itemNamePlural.get(); }
    public StringProperty itemNamePluralProperty() { return itemNamePlural; }

    // -------------------------------------------------------------------------
    // Azioni
    // -------------------------------------------------------------------------

    /** Svuota selezioni e testo di filtro. */
    public void clear() {
        selectedItems.clear();
        filterText.set("");
    }

    @Override
    protected Skin<?> createDefaultSkin() {
        return new MultiSelectComboBoxSkin<>(this);
    }
}
