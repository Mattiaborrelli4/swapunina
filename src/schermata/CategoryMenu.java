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
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class CategoryMenu {
    private static final Map<Categoria, String> CATEGORY_ICONS = new HashMap<>();
    static {
        CATEGORY_ICONS.put(Categoria.LIBRI, "/icons/books.png");
        CATEGORY_ICONS.put(Categoria.CASA, "/icons/computer.png");
        CATEGORY_ICONS.put(Categoria.ABBIGLIAMENTO, "/icons/shirt.png");
        CATEGORY_ICONS.put(Categoria.ELETTRONICA, "/icons/music.png");
    }

    private final VBox root = new VBox();
    private final Button allProductsButton = createCategoryButton("Tutti i prodotti", "/icons/all.png");
    private final Map<Categoria, Button> categoryButtons = new HashMap<>();
    private Consumer<Categoria> categoryHandler;
    private Categoria selectedCategory;

    public CategoryMenu() {
        initializeUI();
        setupCategoryButtons();
        applyStyles();
    }

    private void initializeUI() {
        root.getStyleClass().add("category-menu");
        root.setPadding(new Insets(16));
        root.setSpacing(12);

        Text title = new Text("Categorie");
        title.getStyleClass().add("menu-title");
        root.getChildren().add(title);
    }

    private void setupCategoryButtons() {
        VBox categoryList = new VBox(4);
        categoryList.getStyleClass().add("category-list");

        // Pulsante "Tutti i prodotti"
        allProductsButton.setOnAction(e -> selectCategory(null));
        categoryList.getChildren().add(allProductsButton);

        // Pulsanti per ogni categoria
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

    private Button createCategoryButton(String text, String iconPath) {
        Button button = new Button(text);
        try {
            ImageView icon = new ImageView(new Image(getClass().getResourceAsStream(iconPath)));
            icon.setFitWidth(20);
            icon.setFitHeight(20);
            button.setGraphic(icon);
        } catch (Exception e) {
            System.err.println("Errore nel caricamento dell'icona: " + iconPath);
        }
        button.getStyleClass().add("category-button");
        button.setAlignment(Pos.BASELINE_LEFT);
        button.setMaxWidth(Double.MAX_VALUE);
        Tooltip.install(button, new Tooltip(text));
        return button;
    }

    private void selectCategory(Categoria category) {
        selectedCategory = category;
        updateButtonStates();
        if (categoryHandler != null) {
            categoryHandler.accept(category);
        }
    }

    private void updateButtonStates() {
        // Reset all buttons
        allProductsButton.getStyleClass().remove("selected");
        categoryButtons.values().forEach(btn -> btn.getStyleClass().remove("selected"));

        // Set selected state
        if (selectedCategory == null) {
            allProductsButton.getStyleClass().add("selected");
        } else {
            Button selectedBtn = categoryButtons.get(selectedCategory);
            if (selectedBtn != null) {
                selectedBtn.getStyleClass().add("selected");
            }
        }
    }

    private void applyStyles() {
        root.getStyleClass().add("category-menu");
        allProductsButton.getStyleClass().add("category-button");
        categoryButtons.values().forEach(btn -> btn.getStyleClass().add("category-button"));
    }

    public VBox getView() {
        return root;
    }

    public void setOnCategorySelected(Consumer<Categoria> handler) {
        this.categoryHandler = handler;
    }

    public Categoria getSelectedCategory() {
        return selectedCategory;
    }
}