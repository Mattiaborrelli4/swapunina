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
    // Cache delle costanti per migliorare le performance
    private static final String[] TIPOLOGIE = {"Tutti", "Vendita", "Scambio", "Regalo"};
    private static final String[] ORDINAMENTI = {"Più recenti", "Prezzo crescente", "Prezzo decrescente"};
    private static final String STYLE_FILTER_BAR = "filter-bar";
    private static final String STYLE_FILTER_COMBO = "filter-combo";
    private static final String STYLE_COUNT_TEXT = "count-text";
    
    // Componenti UI
    private final HBox root = new HBox();
    private final ComboBox<String> typeCombo = new ComboBox<>();
    private final ComboBox<String> sortCombo = new ComboBox<>();
    private final Text countText = new Text("0 prodotti");
    
    // Callbacks - utilizzati per notificare i cambiamenti
    private Consumer<Tipologia> typeHandler;
    private Consumer<String> sortHandler;

    /**
     * Costruttore della barra di filtri
     * Inizializza il layout e configura tutti i componenti
     */
    public FilterBar() {
        configureLayout();
        setupTypeFilter();
        setupSortFilter();
        applyStyles();
        setupTooltips();
    }
    
    /**
     * Configura il layout principale della barra filtri
     * Organizza le sezioni con spaziature appropriate
     */
    private void configureLayout() {
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPadding(new Insets(16));
        root.setSpacing(24);
        
        // Sezione filtro per tipologia
        HBox typeSection = createTypeSection();
        
        // Spacer flessibile per separare le sezioni
        Region spacer = createSpacer();
        
        // Sezione ordinamento e conteggio
        HBox sortSection = createSortSection();
        
        root.getChildren().addAll(typeSection, spacer, sortSection);
    }
    
    /**
     * Crea la sezione per il filtro per tipologia
     * @return HBox configurata con label e combobox
     */
    private HBox createTypeSection() {
        HBox typeSection = new HBox(12);
        typeSection.setAlignment(Pos.CENTER_LEFT);
        typeSection.getChildren().addAll(
            new Label("Filtra:"),
            typeCombo
        );
        return typeSection;
    }
    
    /**
     * Crea la sezione per l'ordinamento e visualizzazione conteggio
     * @return HBox configurata con testo conteggio e combobox ordinamento
     */
    private HBox createSortSection() {
        HBox sortSection = new HBox(12);
        sortSection.setAlignment(Pos.CENTER_RIGHT);
        sortSection.getChildren().addAll(
            countText,
            new Label("Ordina:"),
            sortCombo
        );
        return sortSection;
    }
    
    /**
     * Crea uno spacer flessibile per separare le sezioni
     * @return Region configurata come spacer
     */
    private Region createSpacer() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }
    
    /**
     * Configura il combobox per il filtro per tipologia
     * Popola le opzioni e imposta il listener per i cambiamenti
     */
    private void setupTypeFilter() {
        // Utilizza addAll invece di cicli separati per migliorare le performance
        typeCombo.getItems().addAll(TIPOLOGIE);
        typeCombo.setValue(TIPOLOGIE[0]);
        typeCombo.setOnAction(e -> handleTypeChange());
    }
    
    /**
     * Configura il combobox per l'ordinamento
     * Popola le opzioni e imposta il listener per i cambiamenti
     */
    private void setupSortFilter() {
        sortCombo.getItems().addAll(ORDINAMENTI);
        sortCombo.setValue(ORDINAMENTI[0]);
        sortCombo.setOnAction(e -> handleSortChange());
    }
    
    /**
     * Gestisce il cambiamento del filtro per tipologia
     * Converte la selezione in enum Tipologia e notifica il listener
     */
    private void handleTypeChange() {
        if (typeHandler == null) return;
        
        // Utilizzo di switch expression per codice più conciso
        Tipologia tipologia = switch (typeCombo.getValue()) {
            case "Vendita" -> Tipologia.VENDITA;
            case "Scambio" -> Tipologia.SCAMBIO;
            case "Regalo" -> Tipologia.REGALO;
            default -> null;
        };
        
        typeHandler.accept(tipologia);
    }
    
    /**
     * Gestisce il cambiamento del criterio di ordinamento
     * Converte la selezione in stringa identificativa e notifica il listener
     */
    private void handleSortChange() {
        if (sortHandler == null) return;
        
        // Utilizzo di switch expression per codice più conciso
        String sortKey = switch (sortCombo.getValue()) {
            case "Prezzo crescente" -> "price_asc";
            case "Prezzo decrescente" -> "price_desc";
            default -> "recent";
        };
        
        sortHandler.accept(sortKey);
    }
    
    /**
     * Applica le classi di stile CSS ai componenti
     * Utilizza costanti per evitare stringhe hardcoded
     */
    private void applyStyles() {
        root.getStyleClass().add(STYLE_FILTER_BAR);
        typeCombo.getStyleClass().add(STYLE_FILTER_COMBO);
        sortCombo.getStyleClass().add(STYLE_FILTER_COMBO);
        countText.getStyleClass().add(STYLE_COUNT_TEXT);
    }
    
    /**
     * Configura i tooltip per i componenti interattivi
     * Fornisce spiegazioni contestuali all'utente
     */
    private void setupTooltips() {
        Tooltip.install(typeCombo, new Tooltip("Filtra per tipologia di annuncio"));
        Tooltip.install(sortCombo, new Tooltip("Ordina i risultati"));
    }
    
    // API PUBBLICA
    
    /**
     * Restituisce il nodo radice della barra filtri
     * @return HBox contenente tutti i componenti della barra filtri
     */
    public HBox getRoot() {
        return root;
    }
    
    /**
     * Aggiorna il conteggio dei prodotti visualizzati
     * @param count numero di prodotti da visualizzare
     */
    public void updateCount(int count) {
        // Utilizzo di operatore ternario per gestire singolare/plurale
        countText.setText(count + (count == 1 ? " prodotto" : " prodotti"));
    }
    
    /**
     * Imposta il listener per i cambiamenti del filtro tipologia
     * @param handler Consumer che riceve la Tipologia selezionata (null per "Tutti")
     */
    public void setOnTypeChange(Consumer<Tipologia> handler) {
        this.typeHandler = handler;
    }
    
    /**
     * Imposta il listener per i cambiamenti dell'ordinamento
     * @param handler Consumer che riceve la chiave di ordinamento
     */
    public void setOnSortChange(Consumer<String> handler) {
        this.sortHandler = handler;
    }
    
    /**
     * Reimposta i filtri ai valori predefiniti
     * "Tutti" per la tipologia e "Più recenti" per l'ordinamento
     */
    public void resetFilters() {
        typeCombo.setValue(TIPOLOGIE[0]);
        sortCombo.setValue(ORDINAMENTI[0]);
    }
}