package application.messagistica;

import application.DB.ConnessioneDB;
import application.DB.MessaggioDAO;
import application.DB.SessionManager;
import application.Classe.utente;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import schermata.FinestraMessaggi;

import java.io.File;
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
        stage.setWidth(450); // Leggermente più largo per le immagini
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
        
        // Lista chat con immagini profilo
        chatListView = new ListView<>();
        chatListView.setCellFactory(param -> new ChatListCell());
        
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

    // Metodo per mostrare timestamp dell'ultimo messaggio
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

    // Classe interna per le celle della lista con immagini profilo
    private class ChatListCell extends ListCell<utente> {
        private final ImageView imageView = new ImageView();
        private final Label nameLabel = new Label();
        private final Label previewLabel = new Label();
        private final Label timeLabel = new Label();
        private final HBox mainHBox = new HBox(10);
        private final VBox contentBox = new VBox(3);
        private final Region spacer = new Region();

        public ChatListCell() {
            super();
            
            // Configurazione ImageView
            imageView.setFitHeight(40);
            imageView.setFitWidth(40);
            imageView.setPreserveRatio(true);
            
            // Rendi l'immagine circolare
            Circle clip = new Circle(20, 20, 20);
            imageView.setClip(clip);
            
            // Stile per l'immagine
            imageView.setStyle("-fx-border-radius: 20px; -fx-border-color: #e0e0e0; -fx-border-width: 1px;");
            
            // Configurazione label
            nameLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            nameLabel.setStyle("-fx-text-fill: #333333;");
            
            previewLabel.setFont(Font.font("Arial", 12));
            previewLabel.setStyle("-fx-text-fill: #666666;");
            previewLabel.setMaxWidth(250);
            previewLabel.setWrapText(true);
            
            timeLabel.setFont(Font.font("Arial", 10));
            timeLabel.setStyle("-fx-text-fill: #999999;");
            
            // Layout
            contentBox.getChildren().addAll(nameLabel, previewLabel);
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
            
            mainHBox.setAlignment(Pos.CENTER_LEFT);
            mainHBox.setPadding(new Insets(8));
            mainHBox.getChildren().addAll(imageView, contentBox, spacer, timeLabel);
        }

        @Override
        protected void updateItem(utente user, boolean empty) {
            super.updateItem(user, empty);
            
            if (empty || user == null) {
                setText(null);
                setGraphic(null);
            } else {
                nameLabel.setText(user.getNome() + " " + user.getCognome());
                
                String preview = getUltimoMessaggioPreview(SessionManager.getCurrentUserId(), user.getId());
                previewLabel.setText(preview);
                
                String timestamp = getUltimoMessaggioTimestamp(SessionManager.getCurrentUserId(), user.getId());
                timeLabel.setText(timestamp);
                
                // Carica l'immagine profilo
                loadProfileImage(user.getFotoProfilo());
                
                setGraphic(mainHBox);
            }
        }

        private void loadProfileImage(String imagePath) {
            try {
                Image image;
                
                if (imagePath == null || imagePath.isEmpty()) {
                    // Nessuna immagine, usa predefinita
                    image = getDefaultProfileImage();
                } else if (imagePath.contains("cloudinary.com") || imagePath.startsWith("http")) {
                    // È un URL Cloudinary - carica direttamente
                    System.out.println("☁️ Caricamento immagine profilo da Cloudinary: " + imagePath);
                    image = new Image(imagePath, 40, 40, true, true, true);
                } else {
                    // È un percorso di file locale - converti in URL file
                    System.out.println("💾 Caricamento immagine profilo da file locale: " + imagePath);
                    File file = new File(imagePath);
                    if (file.exists()) {
                        String fileUrl = file.toURI().toString();
                        image = new Image(fileUrl, 40, 40, true, true, true);
                    } else {
                        // File non trovato, usa immagine predefinita
                        System.err.println("❌ File immagine profilo non trovato: " + imagePath);
                        image = getDefaultProfileImage();
                    }
                }
                
                if (image != null && !image.isError()) {
                    imageView.setImage(image);
                } else {
                    imageView.setImage(getDefaultProfileImage());
                }
                
            } catch (Exception e) {
                System.err.println("❌ Errore nel caricamento immagine profilo: " + e.getMessage());
                imageView.setImage(getDefaultProfileImage());
            }
        }

        private Image getDefaultProfileImage() {
            try {
                // Prova a caricare un'immagine predefinita dalle risorse
                Image defaultImage = new Image(getClass().getResourceAsStream("/images/default_profile.png"));
                if (!defaultImage.isError()) {
                    return defaultImage;
                }
            } catch (Exception e) {
                System.err.println("⚠️ Impossibile caricare l'immagine predefinita dalle risorse: " + e.getMessage());
            }
            
            try {
                // Prova a caricare da file system
                Image defaultImage = new Image("file:default_profile.png");
                if (!defaultImage.isError()) {
                    return defaultImage;
                }
            } catch (Exception e) {
                System.err.println("⚠️ Impossibile caricare l'immagine predefinita da file: " + e.getMessage());
            }
            
            // Crea un'immagine placeholder con un cerchio grigio
            System.out.println("🎨 Usando immagine placeholder predefinita per profilo");
            return createDefaultPlaceholder();
        }

        private Image createDefaultPlaceholder() {
            // Ritorna null per usare il background color del cerchio
            return null;
        }
    }
}