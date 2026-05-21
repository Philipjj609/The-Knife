package theknife.client.ui.widgets;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.StringBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ListView;
import javafx.scene.control.SkinBase;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxListCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

class MultiSelectComboBoxSkin<T> extends SkinBase<MultiSelectComboBox<T>> {

    private final MenuButton menuButton = new MenuButton();
    private final TextField filterField = new TextField();
    private final Button clearFilterButton = new Button("✕");
    private final ListView<T> listView;
    private final FilteredList<T> filteredItems;

    /**
     * Raffinamento 1: cache delle BooleanProperty per item.
     * Garantisce che lo stesso oggetto Property venga restituito al CheckBoxListCell
     * ad ogni scroll/ricostruzione cella, evitando il bug di desync checkbox/selezione.
     */
    private final Map<T, BooleanProperty> propMap = new HashMap<>();

    MultiSelectComboBoxSkin(MultiSelectComboBox<T> control) {
        super(control);

        menuButton.getStyleClass().add("multi-select-menu-button");

        // --- ListView con FilteredList ---
        filteredItems = new FilteredList<>(control.getItems());
        listView = new ListView<>(filteredItems);
        listView.setPrefHeight(200);
        listView.setPrefWidth(230);
        listView.setCellFactory(CheckBoxListCell.forListView(
                item -> getOrCreateProp(item, control),
                control.getConverter()
        ));

        // --- Barra filtro ---
        filterField.setPromptText("Filtra...");
        filterField.textProperty().bindBidirectional(control.filterText);

        clearFilterButton.visibleProperty().bind(filterField.textProperty().isNotEmpty());
        clearFilterButton.setFocusTraversable(false);
        clearFilterButton.setStyle("-fx-padding: 2 6;");
        clearFilterButton.setOnAction(e -> filterField.clear());

        HBox filterBar = new HBox(4, filterField, clearFilterButton);
        HBox.setHgrow(filterField, Priority.ALWAYS);
        filterBar.setAlignment(Pos.CENTER_LEFT);

        // --- Contenuto popup ---
        VBox popupContent = new VBox(8, filterBar, listView);
        popupContent.getStyleClass().add("multi-select-popup-content");
        popupContent.setPadding(new Insets(8));

        CustomMenuItem menuItem = new CustomMenuItem(popupContent, false);
        menuItem.getStyleClass().add("multi-select-custom-menu-item");
        menuButton.getItems().add(menuItem);

        getChildren().add(menuButton);

        // --- Bindings e listener ---
        bindButtonText(control);
        setupFilterListener(control);
        setupSetChangeListener(control);

        // Aggiorna cell factory se converter cambia dopo la costruzione
        control.converterProperty().addListener((obs, oldConv, newConv) ->
                listView.setCellFactory(CheckBoxListCell.forListView(
                        item -> getOrCreateProp(item, control), newConv)));
    }

    // -------------------------------------------------------------------------
    // Raffinamento 1: BooleanProperty con cache e sync bidirezionale
    // -------------------------------------------------------------------------

    private BooleanProperty getOrCreateProp(T item, MultiSelectComboBox<T> control) {
        return propMap.computeIfAbsent(item, k -> {
            BooleanProperty prop = new SimpleBooleanProperty(
                    control.getSelectedItems().contains(k));
            prop.addListener((obs, wasSelected, isSelected) -> {
                if (Boolean.TRUE.equals(isSelected)) {
                    control.getSelectedItems().add(k);
                } else {
                    control.getSelectedItems().remove(k);
                }
            });
            return prop;
        });
    }

    /**
     * Sync inverso: quando selectedItems cambia dall'esterno (es. clear()),
     * aggiorna le BooleanProperty già in cache senza generare cicli
     * (set(true) su prop già true = nessun evento).
     */
    private void setupSetChangeListener(MultiSelectComboBox<T> control) {
        control.getSelectedItems().addListener((SetChangeListener<T>) change -> {
            if (change.wasAdded()) {
                BooleanProperty prop = propMap.get(change.getElementAdded());
                if (prop != null && !prop.get()) prop.set(true);
            }
            if (change.wasRemoved()) {
                BooleanProperty prop = propMap.get(change.getElementRemoved());
                if (prop != null && prop.get()) prop.set(false);
            }
        });
    }

    // -------------------------------------------------------------------------
    // Testo del bottone
    // -------------------------------------------------------------------------

    private void bindButtonText(MultiSelectComboBox<T> control) {
        StringBinding textBinding = Bindings.createStringBinding(() -> {
            ObservableSet<T> sel = control.getSelectedItems();
            int size = sel.size();
            if (size == 0) return control.getPromptText();
            Iterator<T> it = sel.iterator();
            if (size == 1) return control.getConverter().toString(it.next());
            if (size == 2) {
                String a = control.getConverter().toString(it.next());
                String b = control.getConverter().toString(it.next());
                return a + ", " + b;
            }
            return size + " " + control.getItemNamePlural();
        }, control.getSelectedItems(), control.promptTextProperty(),
                control.itemNamePluralProperty(), control.converterProperty());

        menuButton.textProperty().bind(textBinding);
    }

    // -------------------------------------------------------------------------
    // Filtro testuale
    // -------------------------------------------------------------------------

    private void setupFilterListener(MultiSelectComboBox<T> control) {
        filterField.textProperty().addListener((obs, oldText, newText) -> {
            String lower = (newText == null || newText.isEmpty()) ? "" : newText.toLowerCase();
            filteredItems.setPredicate(lower.isEmpty() ? null :
                    item -> control.getConverter().toString(item).toLowerCase().startsWith(lower));
        });
    }

    // -------------------------------------------------------------------------
    // Layout: il menuButton riempie l'area del control
    // -------------------------------------------------------------------------

    @Override
    protected void layoutChildren(double x, double y, double w, double h) {
        menuButton.resizeRelocate(x, y, w, h);
    }

    @Override
    protected double computePrefWidth(double height, double topInset, double rightInset,
                                      double bottomInset, double leftInset) {
        return menuButton.prefWidth(height);
    }

    @Override
    protected double computePrefHeight(double width, double topInset, double rightInset,
                                       double bottomInset, double leftInset) {
        return menuButton.prefHeight(width);
    }
}
