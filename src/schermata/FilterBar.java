package schermata;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import java.util.function.Consumer;
import application.Enum.Tipologia;


public class FilterBar {
    // Costanti
    private static final String[] TIPOLOGIE = {"Tutti", "Vendita", "Scambio", "Regalo"};
    private static final String[] ORDINAMENTI = {"Più recenti", "Prezzo crescente", "Prezzo decrescente"};
    
    // Componenti UI
    private final HBox root = new HBox();
    private final ComboBox<String> typeCombo = new ComboBox<>();
    private final ComboBox<String> sortCombo = new ComboBox<>();
    private final Text countText = new Text("0 prodotti");
    
    // Callbacks
    private Consumer<Tipologia> typeHandler;
    private Consumer<String> sortHandler;
    
    public FilterBar() {
        configureLayout();
        setupTypeFilter();
        setupSortFilter();
        applyStyles();
        setupTooltips();
    }
    
    private void configureLayout() {
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(16));
        root.setSpacing(24);
        
        // Sezione filtro tipologia
        HBox typeSection = new HBox(12);
        typeSection.setAlignment(Pos.CENTER_LEFT);
        typeSection.getChildren().addAll(
            new Label("Filtra:"),
            typeCombo
        );
        
        // Spacer
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        // Sezione ordinamento
        HBox sortSection = new HBox(12);
        sortSection.setAlignment(Pos.CENTER_RIGHT);
        sortSection.getChildren().addAll(
            countText,
            new Label("Ordina:"),
            sortCombo
        );
        
        root.getChildren().addAll(typeSection, spacer, sortSection);
    }
    
    private void setupTypeFilter() {
        typeCombo.getItems().addAll(TIPOLOGIE);
        typeCombo.setValue(TIPOLOGIE[0]);
        typeCombo.setOnAction(e -> handleTypeChange());
    }
    
    private void setupSortFilter() {
        sortCombo.getItems().addAll(ORDINAMENTI);
        sortCombo.setValue(ORDINAMENTI[0]);
        sortCombo.setOnAction(e -> handleSortChange());
    }
    
    private void handleTypeChange() {
        if (typeHandler == null) return;
        
        switch (typeCombo.getValue()) {
            case "Vendita":
                typeHandler.accept(Tipologia.VENDITA);
                break;
            case "Scambio":
                typeHandler.accept(Tipologia.SCAMBIO);
                break;
            case "Regalo":
                typeHandler.accept(Tipologia.REGALO);
                break;
            default:
                typeHandler.accept(null);
        }
    }
    
    private void handleSortChange() {
        if (sortHandler == null) return;
        
        switch (sortCombo.getValue()) {
            case "Prezzo crescente":
                sortHandler.accept("price_asc");
                break;
            case "Prezzo decrescente":
                sortHandler.accept("price_desc");
                break;
            default:
                sortHandler.accept("recent");
        }
    }
    
    private void applyStyles() {
        root.getStyleClass().add("filter-bar");
        typeCombo.getStyleClass().add("filter-combo");
        sortCombo.getStyleClass().add("filter-combo");
        countText.getStyleClass().add("count-text");
    }
    
    private void setupTooltips() {
        Tooltip.install(typeCombo, new Tooltip("Filtra per tipologia di annuncio"));
        Tooltip.install(sortCombo, new Tooltip("Ordina i risultati"));
    }
    
    // API pubblica
    public HBox getRoot() {
        return root;
    }
    
    public void updateCount(int count) {
        countText.setText(count + (count == 1 ? " prodotto" : " prodotti"));
    }
    
    public void setOnTypeChange(Consumer<Tipologia> handler) {
        this.typeHandler = handler;
    }
    
    public void setOnSortChange(Consumer<String> handler) {
        this.sortHandler = handler;
    }
    
    public void resetFilters() {
        typeCombo.setValue(TIPOLOGIE[0]);
        sortCombo.setValue(ORDINAMENTI[0]);
    }
}