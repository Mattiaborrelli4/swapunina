package application.messagistica;

import application.DB.ConnessioneDB;
import application.DB.MessaggioDAO;
import application.DB.SessionManager;
import application.Classe.utente;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import schermata.FinestraMessaggi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ChatListDialog {
    private Stage stage;
    private ListView<utente> chatListView;
    private MessaggioDAO messaggioDAO;

    public ChatListDialog() {
        stage = new Stage();
        stage.setTitle("Le tue conversazioni");
        stage.setWidth(400);
        stage.setHeight(500);
        
        messaggioDAO = new MessaggioDAO();
        initializeUI();
    }

    private void initializeUI() {
        BorderPane root = new BorderPane();
        
        // Titolo
        Label titleLabel = new Label("Le tue conversazioni");
        titleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        titleLabel.setPadding(new Insets(10));
        
        // Lista chat
        chatListView = new ListView<>();
        chatListView.setCellFactory(param -> new ListCell<utente>() {
            @Override
            protected void updateItem(utente user, boolean empty) {
                super.updateItem(user, empty);
                
                if (empty || user == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(10);
                    hbox.setAlignment(Pos.CENTER_LEFT);
                    hbox.setPadding(new Insets(5));
                    
                    // Icona utente semplificata (cerchio grigio)
                    Region iconPlaceholder = new Region();
                    iconPlaceholder.setPrefSize(30, 30);
                    iconPlaceholder.setStyle("-fx-background-color: #e0e0e0; -fx-background-radius: 15;");
                    
                    // Info utente con preview ultimo messaggio
                    VBox vbox = new VBox(2);
                    Label nameLabel = new Label(user.getNome() + " " + user.getCognome());
                    nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                    
                    // Preview ultimo messaggio
                    String ultimoMessaggio = getUltimoMessaggioPreview(SessionManager.getCurrentUserId(), user.getId());
                    Label previewLabel = new Label(ultimoMessaggio);
                    previewLabel.setFont(Font.font("Arial", 12));
                    previewLabel.setStyle("-fx-text-fill: gray;");
                    previewLabel.setMaxWidth(300);
                    previewLabel.setWrapText(true);
                    
                    vbox.getChildren().addAll(nameLabel, previewLabel);
                    hbox.getChildren().addAll(iconPlaceholder, vbox);
                    
                    setGraphic(hbox);
                }
            }
        });
        
        // Gestione click su chat
        chatListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                utente selectedUser = chatListView.getSelectionModel().getSelectedItem();
                if (selectedUser != null) {
                    openChat(selectedUser);
                }
            }
        });
        
        root.setTop(titleLabel);
        root.setCenter(chatListView);
        
        Scene scene = new Scene(root);
        stage.setScene(scene);
        
        // Carica le chat
        getChatsForUser();
    }

    private void getChatsForUser() {
        try {
            int currentUserId = SessionManager.getCurrentUserId();
            if (currentUserId == -1) {
                showAlert("Errore", "Utente non autenticato");
                return;
            }
            
            List<utente> interlocutori = messaggioDAO.getInterlocutoriUtenti(currentUserId);
            chatListView.getItems().setAll(interlocutori);
            
            if (interlocutori.isEmpty()) {
                showAlert("Nessuna conversazione", "Non hai ancora conversazioni attive.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Errore", "Impossibile caricare le conversazioni: " + e.getMessage());
        }
    }

    private void openChat(utente otherUser) {
        try {
            int currentUserId = SessionManager.getCurrentUserId();
            int otherUserId = otherUser.getId();
            String otherUserName = otherUser.getNome() + " " + otherUser.getCognome();
            
            // Apri FinestraMessaggi con chat semplice
            FinestraMessaggi finestra = new FinestraMessaggi(currentUserId, otherUserId, otherUserName);
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Errore", "Impossibile aprire la chat: " + e.getMessage());
        }
    }

    // Metodo per ottenere l'anteprima dell'ultimo messaggio
    private String getUltimoMessaggioPreview(int currentUserId, int otherUserId) {
        String query = """
            SELECT testo_plaintext_backup 
            FROM messaggio 
            WHERE (mittente_id = ? AND destinatario_id = ?) 
               OR (mittente_id = ? AND destinatario_id = ?) 
            ORDER BY data_invio DESC 
            LIMIT 1
            """;
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, currentUserId);
            stmt.setInt(2, otherUserId);
            stmt.setInt(3, otherUserId);
            stmt.setInt(4, currentUserId);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String messaggio = rs.getString("testo_plaintext_backup");
                if (messaggio != null) {
                    // Accorcia il messaggio se troppo lungo
                    return messaggio.length() > 50 ? 
                           messaggio.substring(0, 47) + "..." : messaggio;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return "Nessun messaggio ancora";
    }

    // Metodo per mostrare timestamp dell'ultimo messaggio (opzionale)
    private String getUltimoMessaggioTimestamp(int currentUserId, int otherUserId) {
        String query = """
            SELECT data_invio 
            FROM messaggio 
            WHERE (mittente_id = ? AND destinatario_id = ?) 
               OR (mittente_id = ? AND destinatario_id = ?) 
            ORDER BY data_invio DESC 
            LIMIT 1
            """;
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, currentUserId);
            stmt.setInt(2, otherUserId);
            stmt.setInt(3, otherUserId);
            stmt.setInt(4, currentUserId);
            
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                java.sql.Timestamp timestamp = rs.getTimestamp("data_invio");
                if (timestamp != null) {
                    return formatTimestamp(timestamp);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return "";
    }

    private String formatTimestamp(java.sql.Timestamp timestamp) {
        java.time.LocalDateTime dateTime = timestamp.toLocalDateTime();
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        
        if (dateTime.toLocalDate().equals(now.toLocalDate())) {
            // Oggi: mostra solo l'orario
            return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
        } else if (dateTime.toLocalDate().equals(now.toLocalDate().minusDays(1))) {
            // Ieri
            return "Ieri";
        } else {
            // Altri giorni: mostra la data
            return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM"));
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(okButton);
        alert.showAndWait();
    }

    public void show() {
        stage.show();
        stage.toFront();
    }

    // Versione avanzata del ListCell con timestamp (opzionale)
    private class ChatListCell extends ListCell<utente> {
        @Override
        protected void updateItem(utente user, boolean empty) {
            super.updateItem(user, empty);
            
            if (empty || user == null) {
                setText(null);
                setGraphic(null);
            } else {
                HBox mainHBox = new HBox(10);
                mainHBox.setAlignment(Pos.CENTER_LEFT);
                mainHBox.setPadding(new Insets(8));
                
                // Icona utente
                Region icon = new Region();
                icon.setPrefSize(40, 40);
                icon.setStyle("-fx-background-color: #4CAF50; -fx-background-radius: 20;");
                
                // Contenuto principale
                VBox contentBox = new VBox(3);
                
                // Nome utente
                Label nameLabel = new Label(user.getNome() + " " + user.getCognome());
                nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
                nameLabel.setStyle("-fx-text-fill: #333333;");
                
                // Anteprima ultimo messaggio
                String preview = getUltimoMessaggioPreview(SessionManager.getCurrentUserId(), user.getId());
                Label previewLabel = new Label(preview);
                previewLabel.setFont(Font.font("Arial", 12));
                previewLabel.setStyle("-fx-text-fill: #666666;");
                previewLabel.setMaxWidth(250);
                previewLabel.setWrapText(true);
                
                contentBox.getChildren().addAll(nameLabel, previewLabel);
                
                // Spacer
                Region spacer = new Region();
                HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
                
                // Timestamp
                String timestamp = getUltimoMessaggioTimestamp(SessionManager.getCurrentUserId(), user.getId());
                Label timeLabel = new Label(timestamp);
                timeLabel.setFont(Font.font("Arial", 10));
                timeLabel.setStyle("-fx-text-fill: #999999;");
                
                mainHBox.getChildren().addAll(icon, contentBox, spacer, timeLabel);
                setGraphic(mainHBox);
            }
        }
    }
}