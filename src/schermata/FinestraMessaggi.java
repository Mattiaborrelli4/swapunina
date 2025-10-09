package schermata;

import application.DB.MessaggioDAO;
import application.Classe.Messaggio;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FinestraMessaggi {
    private Stage stage;
    private int currentUserId;
    private int otherUserId;
    private String otherUserName;
    private MessaggioDAO messaggioDAO;
    
    private ListView<Messaggio> messagesListView;
    private TextField messageField;
    private Button sendButton;

    public FinestraMessaggi(int currentUserId, int otherUserId, String otherUserName) {
        this.currentUserId = currentUserId;
        this.otherUserId = otherUserId;
        this.otherUserName = otherUserName;
        this.messaggioDAO = new MessaggioDAO();
        
        initializeUI();
        loadMessages();
        stage.show();
    }

    private void initializeUI() {
        stage = new Stage();
        stage.setTitle("Chat con " + otherUserName);
        stage.setWidth(500);
        stage.setHeight(600);

        BorderPane root = new BorderPane();

        // Header semplice
        Label headerLabel = new Label("Chat con: " + otherUserName);
        headerLabel.setFont(Font.font("Arial", 16));
        headerLabel.setPadding(new Insets(10));
        root.setTop(headerLabel);

        // Area messaggi
        messagesListView = new ListView<>();
        messagesListView.setCellFactory(param -> new ListCell<Messaggio>() {
            @Override
            protected void updateItem(Messaggio message, boolean empty) {
                super.updateItem(message, empty);
                
                if (empty || message == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox messageBox = new VBox(5);
                    messageBox.setPadding(new Insets(5));
                    
                    HBox contentBox = new HBox();
                    boolean isMyMessage = message.getMittenteId() == currentUserId;
                    contentBox.setAlignment(isMyMessage ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                    
                    VBox textBox = new VBox(2);
                    textBox.setMaxWidth(300);
                    
                    // Testo messaggio
                    Label messageLabel = new Label(message.getTesto());
                    messageLabel.setWrapText(true);
                    messageLabel.setPadding(new Insets(8, 12, 8, 12));
                    messageLabel.setStyle("-fx-background-color: " + 
                                        (isMyMessage ? "#DCF8C6" : "#FFFFFF") + 
                                        "; -fx-background-radius: 10; " +
                                        "-fx-border-color: #E0E0E0; -fx-border-radius: 10;");
                    
                    // Timestamp
                    Label timeLabel = new Label(formatTime(message.getDataInvio()));
                    timeLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 10px;");
                    timeLabel.setAlignment(isMyMessage ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                    
                    textBox.getChildren().addAll(messageLabel, timeLabel);
                    contentBox.getChildren().add(textBox);
                    
                    messageBox.getChildren().add(contentBox);
                    setGraphic(messageBox);
                }
            }
        });

        root.setCenter(messagesListView);

        // Area input
        HBox inputBox = new HBox(10);
        inputBox.setPadding(new Insets(10));
        inputBox.setAlignment(Pos.CENTER);
        
        messageField = new TextField();
        messageField.setPromptText("Scrivi un messaggio...");
        messageField.setPrefWidth(350);
        
        sendButton = new Button("Invia");
        sendButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        
        messageField.setOnAction(e -> sendMessage());
        sendButton.setOnAction(e -> sendMessage());
        
        inputBox.getChildren().addAll(messageField, sendButton);
        root.setBottom(inputBox);

        Scene scene = new Scene(root);
        stage.setScene(scene);
    }

    private void loadMessages() {
        try {
            List<Messaggio> messages = messaggioDAO.getConversazione(currentUserId, otherUserId);
            messagesListView.getItems().setAll(messages);
            
            if (!messages.isEmpty()) {
                messagesListView.scrollTo(messages.size() - 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Errore", "Impossibile caricare i messaggi: " + e.getMessage());
        }
    }

    private void sendMessage() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) return;
        
        try {
            // Messaggio semplice senza riferimento ad annuncio
            Messaggio newMessage = new Messaggio(currentUserId, otherUserId, text, null);
            
            boolean success = messaggioDAO.inviaMessaggio(newMessage);
            if (success) {
                messageField.clear();
                loadMessages();
            } else {
                showAlert("Errore", "Impossibile inviare il messaggio");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Errore", "Impossibile inviare il messaggio: " + e.getMessage());
        }
    }

    private String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
        return dateTime.format(formatter);
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
}