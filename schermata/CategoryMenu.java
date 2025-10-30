package schermata;

import application.Enum.Categoria;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

public class CategoryMenu {
    // Cache delle icone per evitare ricaricamenti multipli
    private static final Map<Categoria, Image> CATEGORY_ICONS = new EnumMap<>(Categoria.class);
    private static final Image ALL_PRODUCTS_ICON = loadIcon("/icons/all.png");
    
    static {
        // Pre-caricamento di tutte le icone all'inizializzazione
        CATEGORY_ICONS.put(Categoria.LIBRI, loadIcon("/icons/books.png"));
        CATEGORY_ICONS.put(Categoria.CASA, loadIcon("/icons/computer.png"));
        CATEGORY_ICONS.put(Categoria.ABBIGLIAMENTO, loadIcon("/icons/shirt.png"));
        CATEGORY_ICONS.put(Categoria.ELETTRONICA, loadIcon("/icons/music.png"));
    }
    
    // Costanti per dimensioni e stili
    private static final int ICON_SIZE = 20;
    private static final int SPACING = 12;
    private static final int CATEGORY_SPACING = 4;
    private static final Insets PADDING = new Insets(16);
    
    // Componenti UI
    private final VBox root = new VBox();
    private final Button allProductsButton = createCategoryButton("Tutti i prodotti", ALL_PRODUCTS_ICON);
    private final Map<Categoria, Button> categoryButtons = new EnumMap<>(Categoria.class);
    
    // Callback e stato
    private Consumer<Categoria> categoryHandler;
    private Categoria selectedCategory;

    /**
     * Carica un'icona in modo sicuro, restituendo null in caso di errore
     * @param path percorso della risorsa
     * @return Image caricata o null
     */
    private static Image loadIcon(String path) {
        try {
            return new Image(CategoryMenu.class.getResourceAsStream(path));
        } catch (Exception e) {
            System.err.println("Errore nel caricamento icona: " + path);
            return null;
        }
    }

    /**
     * Costruttore del menu categorie
     * Inizializza l'interfaccia e configura i pulsanti
     */
    public CategoryMenu() {
        initializeUI();
        setupCategoryButtons();
        applyStyles();
        selectCategory(null); // Selezione iniziale "Tutti i prodotti"
    }

    /**
     * Inizializza l'interfaccia utente principale
     * Configura il layout e il titolo
     */
    private void initializeUI() {
        root.getStyleClass().add("category-menu");
        root.setPadding(PADDING);
        root.setSpacing(SPACING);

        Text title = new Text("Categorie");
        title.getStyleClass().add("menu-title");
        root.getChildren().add(title);
    }

    /**
     * Configura i pulsanti delle categorie
     * Crea un pulsante per ogni categoria disponibile
     */
    private void setupCategoryButtons() {
        VBox categoryList = new VBox(CATEGORY_SPACING);
        categoryList.getStyleClass().add("category-list");

        // Pulsante "Tutti i prodotti"
        allProductsButton.setOnAction(e -> selectCategory(null));
        categoryList.getChildren().add(allProductsButton);

        // Crea pulsanti per ogni categoria
        for (Categoria category : Categoria.values()) {
            Button button = createCategoryButton(
                category.getDisplayName(), 
                CATEGORY_ICONS.get(category)
            );
            button.setOnAction(e -> selectCategory(category));
            categoryButtons.put(category, button);
            categoryList.getChildren().add(button);
        }

        root.getChildren().add(categoryList);
    }

    /**
     * Crea un pulsante di categoria con icona e stile predefiniti
     * @param text testo del pulsante
     * @param icon icona da visualizzare
     * @return Button configurato
     */
    private Button createCategoryButton(String text, Image icon) {
        Button button = new Button(text);
        
        // Configura icona se disponibile
        if (icon != null) {
            ImageView iconView = new ImageView(icon);
            iconView.setFitWidth(ICON_SIZE);
            iconView.setFitHeight(ICON_SIZE);
            button.setGraphic(iconView);
        }
        
        // Applica stili e proprietà
        button.getStyleClass().add("category-button");
        button.setAlignment(Pos.BASELINE_LEFT);
        button.setMaxWidth(Double.MAX_VALUE);
        
        // Tooltip con stesso testo del pulsante
        Tooltip.install(button, new Tooltip(text));
        
        return button;
    }

    /**
     * Seleziona una categoria e aggiorna lo stato dell'interfaccia
     * @param category categoria selezionata (null per "Tutti i prodotti")
     */
    private void selectCategory(Categoria category) {
        selectedCategory = category;
        updateButtonStates();
        
        // Notifica il listener se presente
        if (categoryHandler != null) {
            categoryHandler.accept(category);
        }
    }

    /**
     * Aggiorna lo stato visivo dei pulsanti
     * Rimuove la classe "selected" da tutti i pulsanti e la applica a quello selezionato
     */
    private void updateButtonStates() {
        // Rimuove lo stato selezionato da tutti i pulsanti
        allProductsButton.getStyleClass().remove("selected");
        categoryButtons.values().forEach(btn -> btn.getStyleClass().remove("selected"));

        // Applica lo stato selezionato al pulsante appropriato
        if (selectedCategory == null) {
            allProductsButton.getStyleClass().add("selected");
        } else {
            Button selectedButton = categoryButtons.get(selectedCategory);
            if (selectedButton != null) {
                selectedButton.getStyleClass().add("selected");
            }
        }
    }

    /**
     * Applica le classi di stile CSS ai componenti
     */
    private void applyStyles() {
        root.getStyleClass().add("category-menu");
        allProductsButton.getStyleClass().add("category-button");
        categoryButtons.values().forEach(btn -> btn.getStyleClass().add("category-button"));
    }

    // API PUBBLICA

    /**
     * Restituisce la vista principale del menu categorie
     * @return VBox radice del componente
     */
    public VBox getView() {
        return root;
    }

    /**
     * Imposta il listener per la selezione di categorie
     * @param handler Consumer che riceve la categoria selezionata (null per "Tutti i prodotti")
     */
    public void setOnCategorySelected(Consumer<Categoria> handler) {
        this.categoryHandler = handler;
    }

    /**
     * Restituisce la categoria attualmente selezionata
     * @return Categoria selezionata o null per "Tutti i prodotti"
     */
    public Categoria getSelectedCategory() {
        return selectedCategory;
    }
    
    /**
     * Seleziona programmaticamente una categoria
     * @param category categoria da selezionare (null per "Tutti i prodotti")
     */
    public void setSelectedCategory(Categoria category) {
        selectCategory(category);
    }
}