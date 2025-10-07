package schermata;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.util.function.Consumer;

import application.DB.ConnessioneDB;
import application.DB.SessionManager;
import application.messagistica.ChatListDialog;
import application.messagistica.ChatListScreen;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class TopBar {
    // Componenti UI
	private String nomeUtente;
	private String emailUtente;
    private final HBox root = new HBox();
    private final TextField searchField = new TextField();
    private final Button searchButton = new Button("🔍");
    private final Button accountButton = new Button("👤");
    private final Button cartButton = new Button("🛒");
    private final Button messagesButton = new Button("💬");
    private final Button inserisciAnnuncioButton = new Button("➕ Inserisci Annuncio");
    
    // Handler fields
    private Consumer<String> searchHandler;
    private Runnable accountHandler;
    private Runnable cartHandler;
    private Runnable messagesHandler;
    private Runnable inserisciAnnuncioHandler;
    
    public TopBar() {
        configureRoot();
        setupSearchSection();
        setupActionButtons();
        applyStyles();
    }
    
    private void configureRoot() {
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(10, 24, 10, 24));
        root.setSpacing(16);
        root.getStyleClass().add("top-bar");

        // Logo applicazione - emoji come fallback
        Region logo = createLogo();
        
        // Spacer per centratura
        Region leftSpacer = new Region();
        Region rightSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        root.getChildren().addAll(logo, leftSpacer, createSearchContainer(), rightSpacer, createActionsContainer());
    }
    
    private Region createLogo() {
        // Prova a caricare l'immagine del logo
        try {
            InputStream logoStream = getClass().getResourceAsStream("/application/icons/logo.png");
            if (logoStream != null) {
                ImageView logoImage = new ImageView(new Image(logoStream));
                logoImage.setFitHeight(36);
                logoImage.setPreserveRatio(true);
                
                // Creiamo un contenitore Region per l'immagine
                StackPane container = new StackPane(logoImage);
                container.setPrefSize(36, 36);
                return container;
            }
        } catch (Exception e) {
            System.err.println("Error loading logo: " + e.getMessage());
        }
        
        // Fallback con emoji
        Text emojiLogo = new Text("📦");
        emojiLogo.setFont(Font.font(24));
        
        HBox fallbackContainer = new HBox(emojiLogo);
        fallbackContainer.setPrefSize(36, 36);
        fallbackContainer.setAlignment(Pos.CENTER);
        return fallbackContainer;
    }
    
    
    private HBox createSearchContainer() {
        HBox container = new HBox();
        container.setAlignment(Pos.CENTER);
        container.setMaxWidth(700);
        
        // Configura campo ricerca
        searchField.setPromptText("Cerca prodotti...");
        searchField.setOnAction(e -> handleSearch());
        searchField.setPrefHeight(44);
        
        // Configura pulsante ricerca
        searchButton.setOnAction(e -> handleSearch());
        searchButton.setPrefHeight(44);
        searchButton.setPrefWidth(44);
        searchButton.setFont(Font.font(18));
        
        container.getChildren().addAll(searchField, searchButton);
        return container;
    }

    private HBox createActionsContainer() {
        HBox container = new HBox();
        container.setSpacing(12);
        container.setAlignment(Pos.CENTER_RIGHT);
        
        // Configura pulsanti con emoji
        configureEmojiButton(messagesButton, "💬", 24, e -> handleMessages());
        configureEmojiButton(cartButton, "🛒", 24, e -> handleCart());
        configureEmojiButton(accountButton, "👤", 24, e -> handleAccount());
        
        // Configura pulsante "Inserisci Annuncio"
        configureEmojiButton(inserisciAnnuncioButton, "➕", 18, e -> handleInserisciAnnuncio());
        
        // Stile aggiuntivo per pulsanti
        String iconButtonStyle = "-fx-background-color: transparent; -fx-padding: 10px;";
        messagesButton.setStyle(iconButtonStyle);
        cartButton.setStyle(iconButtonStyle);
        accountButton.setStyle(iconButtonStyle);
        
        // Stile speciale per il pulsante di inserimento
        inserisciAnnuncioButton.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; "
                + "-fx-padding: 8px 16px; -fx-background-radius: 8px;");
        
        container.getChildren().addAll(inserisciAnnuncioButton, messagesButton, cartButton, accountButton);
        return container;
    }
    
    private void configureEmojiButton(Button button, String emoji, int fontSize, javafx.event.EventHandler<javafx.event.ActionEvent> handler) {
        button.setText(emoji);
        button.setFont(Font.font(fontSize));
        button.setOnAction(handler);
    }
    
    private void setupSearchSection() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.isEmpty() && searchHandler != null) {
                searchHandler.accept("");
            }
        });
    }
    
    private void setupActionButtons() {
        // Tooltip per accessibilità
        Tooltip.install(searchButton, new Tooltip("Cerca"));
        Tooltip.install(messagesButton, new Tooltip("Messaggi"));
        Tooltip.install(cartButton, new Tooltip("Carrello"));
        Tooltip.install(accountButton, new Tooltip("Account"));
        Tooltip.install(inserisciAnnuncioButton, new Tooltip("Inserisci nuovo annuncio"));
    }
    
    private void applyStyles() {
        // Classi CSS
        root.getStyleClass().add("top-bar");
        searchField.getStyleClass().add("search-field");
        searchButton.getStyleClass().add("search-button");
        accountButton.getStyleClass().add("action-button");
        cartButton.getStyleClass().add("action-button");
        messagesButton.getStyleClass().add("action-button");
        inserisciAnnuncioButton.getStyleClass().add("inserisci-annuncio-button");
        
        // Stili aggiuntivi
        searchButton.setStyle("-fx-min-width: 44px; -fx-min-height: 44px;");
    }
    
    // Gestori eventi
    private void handleSearch() {
        if (searchHandler != null) {
            searchHandler.accept(searchField.getText().trim());
        }
    }
    
    private void handleAccount() {
        if (accountHandler != null) {
            accountHandler.run();
        }
    }
    
    private void handleCart() {
        if (cartHandler != null) {
            cartHandler.run();
        }
    }
    
 // In the TopBar class, update the handleMessages method:
    private void handleMessages() {
        try {
            if (!isUtenteLoggato()) {
                showAlert("Accesso richiesto", "Devi effettuare l'accesso per visualizzare le chat");
                return;
            }
            
            if (haChatDisponibili()) {
                ChatListDialog chatListDialog = new ChatListDialog();
                chatListDialog.show();
            } else {
                showAlert("Nessuna chat", "Non hai ancora nessuna conversazione. Contatta un venditore per iniziare una chat!");
            }
        } catch (Exception e) {
            showError("Impossibile aprire le chat: " + e.getMessage());
        }
    }

    // Metodo di supporto per verificare se l'utente è loggato
    private boolean isUtenteLoggato() {
        return SessionManager.getCurrentUserId() != -1;
    }

    // Metodo di supporto per verificare se ci sono chat disponibili
    private boolean haChatDisponibili() {
        try (Connection connection = ConnessioneDB.getConnessione()) {
            String query = "SELECT COUNT(*) FROM messaggio WHERE mittente_id = ? OR destinatario_id = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, SessionManager.getCurrentUserId());
            stmt.setInt(2, SessionManager.getCurrentUserId());
            ResultSet rs = stmt.executeQuery();
            
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }


    // Also update the setOnMessages method documentation to reflect the new behavior
    
    private void handleInserisciAnnuncio() {
        if (inserisciAnnuncioHandler != null) {
            inserisciAnnuncioHandler.run();
        }
    }
    
    // API pubblica
    public HBox getRoot() {
        return root;
    }
    
    public void setOnSearch(Consumer<String> handler) {
        this.searchHandler = handler;
    }
    
    public void setOnAccount(Runnable handler) {
        this.accountHandler = handler;
    }
    
    public void setOnCart(Runnable handler) {
        this.cartHandler = handler;
    }
    
    public void setOnMessages(Runnable handler) {
        this.messagesHandler = handler;
    }
    
    public void setOnInserisciAnnuncio(Runnable handler) {
        this.inserisciAnnuncioHandler = handler;
    }
    
    public void clearSearch() {
        searchField.clear();
    }

    public void setDatiUtente(String nome, String email) {
        this.nomeUtente = nome;
        this.emailUtente = email;
    }
    public String getNomeUtente() {
        return nomeUtente;
    }
    
    public String getEmailUtente() {
        return emailUtente;
    }
    
    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            
            // Applica stili consistenti con il tema dell'applicazione
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStyleClass().add("custom-alert");
            
            alert.showAndWait();
        });
    }
    
    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Errore");
            alert.setHeaderText(null);
            alert.setContentText(message);
            
            // Applica stili consistenti
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStyleClass().add("custom-alert");
            
            alert.showAndWait();
        });
    }
}