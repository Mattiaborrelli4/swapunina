package application.messagistica;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import schermata.FinestraMessaggi;
import application.Classe.Annuncio;
import application.Classe.utente;
import application.DB.MessaggioDAO;
import application.DB.AnnuncioDAO;
import application.DB.SessionManager;
import java.util.List;

public class ChatListScreen extends Dialog<Void> {

    private MessaggioDAO messaggioDAO;
    private AnnuncioDAO annuncioDAO;

    public ChatListScreen() {
        this.messaggioDAO = new MessaggioDAO();
        this.annuncioDAO = new AnnuncioDAO();
        
        setTitle("Le tue conversazioni");
        
        // Create a list view for conversations
        ListView<String> conversationList = new ListView<>();
        caricaConversazioniReali(conversationList);
        
        conversationList.setCellFactory(lv -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // CORREZIONE: Gestione sicura dello split
                    String[] parts = item.split(" - ", 2); // Limita a 2 parti
                    String userName = parts[0];
                    String productName = parts.length > 1 ? parts[1] : "Conversazione";
                    
                    HBox cellContent = new HBox(10);
                    cellContent.setAlignment(Pos.CENTER_LEFT);
                    
                    Label userLabel = new Label(userName);
                    userLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                    
                    Label productLabel = new Label(productName);
                    productLabel.setFont(Font.font("Arial", 12));
                    
                    // Add a message preview
                    Label previewLabel = new Label("Clicca per vedere la conversazione...");
                    previewLabel.setFont(Font.font("Arial", 10));
                    previewLabel.setStyle("-fx-text-fill: gray;");
                    
                    VBox textContainer = new VBox(2, new HBox(5, userLabel, new Label("-"), productLabel), previewLabel);
                    
                    // Add timestamp
                    Label timeLabel = new Label("Oggi");
                    timeLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 10px;");
                    
                    HBox mainContent = new HBox(10, textContainer, new Region(), timeLabel);
                    HBox.setHgrow(mainContent.getChildren().get(1), Priority.ALWAYS);
                    
                    cellContent.getChildren().addAll(mainContent);
                    setGraphic(cellContent);
                }
            }
        });
        
        // Handle conversation selection
        conversationList.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                String selectedConversation = conversationList.getSelectionModel().getSelectedItem();
                if (selectedConversation != null) {
                    openConversation(selectedConversation);
                }
            }
        });
        
        VBox content = new VBox(10, conversationList);
        content.setPadding(new Insets(15));
        
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        // Set size
        getDialogPane().setPrefSize(500, 400);
    }
    
    private void caricaConversazioniReali(ListView<String> conversationList) {
        int currentUserId = SessionManager.getCurrentUserId();
        
        if (currentUserId == -1) {
            showAlert("Errore", "Devi essere loggato per vedere le conversazioni");
            return;
        }
        
        try {
            // Recupera gli interlocutori reali dal database
            List<utente> interlocutori = messaggioDAO.getInterlocutoriUtenti(currentUserId);
            
            if (interlocutori.isEmpty()) {
                conversationList.getItems().add("Nessuna conversazione trovata");
                return;
            }
            
            for (utente interlocutore : interlocutori) {
                String nomeInterlocutore = interlocutore.getNome() + " " + interlocutore.getCognome();
                String titoloAnnuncio = interlocutore.getTitoloAnnuncio();
                
                // Ora il formato sarà sempre "Nome Cognome - Titolo Annuncio"
                conversationList.getItems().add(nomeInterlocutore + " - " + titoloAnnuncio);
            }
            
        } catch (Exception e) {
            conversationList.getItems().add("Errore nel caricamento conversazioni");
            e.printStackTrace();
        }
    }
    
    private void openConversation(String conversation) {
        // CORREZIONE: Gestione sicura dello split
        String[] parts = conversation.split(" - ", 2);
        String userName = parts[0];
        String productName = parts.length > 1 ? parts[1] : "Conversazione";
        
        int currentUserId = SessionManager.getCurrentUserId();
        
        if (currentUserId == -1) {
            showAlert("Errore", "Devi essere loggato per accedere alle conversazioni");
            return;
        }
        
        showAlert("Info", "Apertura conversazione con: " + userName);
        
        // Per ora apriamo una finestra vuota, implementeremo dopo
        FinestraMessaggi finestra = new FinestraMessaggi(currentUserId, trovaIdUtentePerNome(userName), userName);
    }
    
    private int trovaIdUtentePerNome(String nomeCompleto) {
        // Implementazione semplificata - dovresti cercare nel database
        // Per ora restituiamo un ID fittizio
        return 1;
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}