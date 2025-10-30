package schermata;

import application.DB.MessaggioDAO;
import application.DB.UtentiDAO;
import application.Classe.Messaggio;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * FinestraMessaggi - Finestra dedicata per la chat tra due utenti
 * Gestisce la visualizzazione e l'invio di messaggi in tempo reale
 * Con integrazione immagini profilo
 */
public class FinestraMessaggi {
    
    // Componenti principali
    private Stage stage;
    private final int currentUserId;
    private final int otherUserId;
    private final String otherUserName;
    private final MessaggioDAO messaggioDAO;
    private final UtentiDAO utentiDAO;
    
    // Componenti UI
    private ListView<Messaggio> messagesListView;
    private TextField messageField;
    private Button sendButton;
    
    // Immagine profilo dell'interlocutore
    private String otherUserProfileImageUrl;
    
    // Costanti per configurazione
    private static final int WINDOW_WIDTH = 500;
    private static final int WINDOW_HEIGHT = 600;
    private static final int MESSAGE_MAX_WIDTH = 300;
    private static final int INPUT_PADDING = 10;
    private static final int MESSAGE_PADDING = 5;
    private static final int MESSAGE_BUBBLE_PADDING = 8;
    private static final String TIME_FORMAT = "HH:mm";
    private static final String FONT_FAMILY = "Arial";
    private static final int HEADER_FONT_SIZE = 16;
    private static final int TIMESTAMP_FONT_SIZE = 10;

    /**
     * Costruttore principale della finestra messaggi
     * @param currentUserId ID dell'utente corrente
     * @param otherUserId ID dell'altro utente nella conversazione
     * @param otherUserName Nome dell'altro utente
     */
    public FinestraMessaggi(int currentUserId, int otherUserId, String otherUserName) {
        this.currentUserId = currentUserId;
        this.otherUserId = otherUserId;
        this.otherUserName = otherUserName;
        this.messaggioDAO = new MessaggioDAO();
        this.utentiDAO = new UtentiDAO();
        
        // Carica l'immagine profilo dell'interlocutore
        loadOtherUserProfileImage();
        
        initializeUI();
        loadMessages();
        stage.show();
    }

    /**
     * Carica l'immagine profilo dell'interlocutore
     */
    /**
     * Carica l'immagine profilo dell'interlocutore in modo sicuro
     */
    private void loadOtherUserProfileImage() {
        try {
            String userEmail = utentiDAO.getEmailById(otherUserId);
            if (userEmail != null) {
                // Usa il metodo sicuro che verifica l'unicità
                otherUserProfileImageUrl = utentiDAO.getFotoProfilo(userEmail);
                
                if (otherUserProfileImageUrl != null) {
                    System.out.println("🖼️ Immagine profilo univoca caricata per " + otherUserName + ": " + otherUserProfileImageUrl);
                } else {
                    System.out.println("🔄 Usando immagine predefinita per " + otherUserName + " (foto condivisa o non impostata)");
                    otherUserProfileImageUrl = getDefaultProfileImageUrl();
                }
            } else {
                System.out.println("⚠️ Email non trovata per l'utente ID: " + otherUserId);
                otherUserProfileImageUrl = getDefaultProfileImageUrl();
            }
        } catch (Exception e) {
            System.err.println("❌ Errore nel caricamento immagine profilo interlocutore: " + e.getMessage());
            otherUserProfileImageUrl = getDefaultProfileImageUrl();
        }
    }

    /**
     * Restituisce un'immagine predefinita univoca basata sull'ID utente
     */
    private String getDefaultProfileImageUrl() {
        // Crea un'immagine predefinita univoca basata sull'ID
        int uniqueSeed = otherUserId;
        String[] defaultColors = {"#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7", "#DDA0DD", "#98D8C8", "#F7DC6F"};
        String color = defaultColors[Math.abs(uniqueSeed) % defaultColors.length];
        
        // Potresti generare un'immagine SVG univoca basata sull'ID
        return generateDefaultAvatar(otherUserName, color);
    }

    /**
     * Genera un avatar predefinito univoco basato sul nome
     */
    private String generateDefaultAvatar(String userName, String color) {
        // Per ora restituiamo null per usare l'immagine predefinita del sistema
        // In futuro potresti generare un SVG univoco
        return null;
    }

    /**
     * Inizializza l'interfaccia utente della finestra
     */
    private void initializeUI() {
        createMainStage();
        BorderPane root = createRootLayout();
        setupHeader(root);
        setupMessageArea(root);
        setupInputArea(root);
        applyFinalStyling(root);
    }

    /**
     * Crea lo stage principale della finestra
     */
    private void createMainStage() {
        stage = new Stage();
        stage.setTitle("Chat con " + otherUserName);
        stage.setWidth(WINDOW_WIDTH);
        stage.setHeight(WINDOW_HEIGHT);
    }

    /**
     * Crea il layout root della finestra
     */
    private BorderPane createRootLayout() {
        return new BorderPane();
    }

    /**
     * Configura l'header della finestra con immagine profilo
     */
    private void setupHeader(BorderPane root) {
        HBox headerBox = createHeaderBox();
        root.setTop(headerBox);
    }

    /**
     * Crea l'header con immagine profilo e nome
     */
    private HBox createHeaderBox() {
        HBox headerBox = new HBox(10);
        headerBox.setPadding(new Insets(INPUT_PADDING));
        headerBox.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #e0e0e0; -fx-border-width: 0 0 1 0;");
        headerBox.setAlignment(Pos.CENTER_LEFT);

        // ImageView per l'immagine profilo
        ImageView profileImageView = createProfileImageView(otherUserProfileImageUrl, 40);
        
        // Label per il nome
        Label nameLabel = new Label("Chat con: " + otherUserName);
        nameLabel.setFont(Font.font(FONT_FAMILY, HEADER_FONT_SIZE));
        
        headerBox.getChildren().addAll(profileImageView, nameLabel);
        return headerBox;
    }

    /**
     * Crea un ImageView circolare per l'immagine profilo
     */
    private ImageView createProfileImageView(String imageUrl, double size) {
        ImageView imageView = new ImageView();
        imageView.setFitHeight(size);
        imageView.setFitWidth(size);
        imageView.setPreserveRatio(true);
        
        // Rendi l'immagine circolare
        Circle clip = new Circle(size/2, size/2, size/2);
        imageView.setClip(clip);
        imageView.setStyle("-fx-border-radius: " + (size/2) + "px; -fx-border-color: #e0e0e0; -fx-border-width: 1px;");

        // Carica l'immagine
        loadProfileImage(imageView, imageUrl);

        return imageView;
    }

    /**
     * Carica un'immagine profilo in un ImageView
     */
    private void loadProfileImage(ImageView imageView, String imagePath) {
        try {
            Image image;
            
            if (imagePath == null || imagePath.isEmpty()) {
                image = getDefaultProfileImage();
            } else if (imagePath.contains("cloudinary.com") || imagePath.startsWith("http")) {
                // URL Cloudinary
                image = new Image(imagePath, imageView.getFitWidth(), imageView.getFitHeight(), true, true, true);
            } else {
                // Percorso locale
                File file = new File(imagePath);
                if (file.exists()) {
                    image = new Image(file.toURI().toString(), imageView.getFitWidth(), imageView.getFitHeight(), true, true, true);
                } else {
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

    /**
     * Restituisce l'immagine profilo predefinita
     */
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
        
        // Ritorna null se non ci sono immagini predefinite
        return null;
    }

    /**
     * Configura l'area dei messaggi
     */
    private void setupMessageArea(BorderPane root) {
        messagesListView = createMessagesListView();
        root.setCenter(messagesListView);
    }

    /**
     * Crea la ListView per i messaggi con cell factory personalizzata
     */
    private ListView<Messaggio> createMessagesListView() {
        ListView<Messaggio> listView = new ListView<>();
        listView.setCellFactory(param -> new MessageListCell());
        listView.setStyle("-fx-background-color: #fafafa;");
        return listView;
    }

    /**
     * Configura l'area di input messaggi
     */
    private void setupInputArea(BorderPane root) {
        HBox inputBox = createInputBox();
        root.setBottom(inputBox);
    }

    /**
     * Crea il box di input per i messaggi
     */
    private HBox createInputBox() {
        HBox inputBox = new HBox(10);
        inputBox.setPadding(new Insets(INPUT_PADDING));
        inputBox.setAlignment(Pos.CENTER);
        inputBox.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #e0e0e0; -fx-border-width: 1 0 0 0;");
        
        setupMessageField();
        setupSendButton();
        
        inputBox.getChildren().addAll(messageField, sendButton);
        return inputBox;
    }

    /**
     * Configura il campo di input del messaggio
     */
    private void setupMessageField() {
        messageField = new TextField();
        messageField.setPromptText("Scrivi un messaggio...");
        messageField.setPrefWidth(350);
        messageField.setOnAction(e -> sendMessage());
    }

    /**
     * Configura il pulsante di invio
     */
    private void setupSendButton() {
        sendButton = new Button("Invia");
        sendButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        sendButton.setOnAction(e -> sendMessage());
    }

    /**
     * Applica la stilizzazione finale alla finestra
     */
    private void applyFinalStyling(BorderPane root) {
        Scene scene = new Scene(root);
        stage.setScene(scene);
        
        // Focus sul campo messaggio all'apertura
        messageField.requestFocus();
    }

    /**
     * Classe interna per la visualizzazione personalizzata dei messaggi con immagini profilo
     */
    private class MessageListCell extends ListCell<Messaggio> {
        @Override
        protected void updateItem(Messaggio message, boolean empty) {
            super.updateItem(message, empty);
            
            if (empty || message == null) {
                setText(null);
                setGraphic(null);
            } else {
                setGraphic(createMessageBubble(message));
            }
        }

        /**
         * Crea la bubble del messaggio con immagine profilo
         */
        private HBox createMessageBubble(Messaggio message) {
            boolean isMyMessage = message.getMittenteId() == currentUserId;
            HBox messageBubble = new HBox(8);
            messageBubble.setPadding(new Insets(MESSAGE_PADDING));
            messageBubble.setAlignment(isMyMessage ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            
            VBox messageBox = createMessageBox(message, isMyMessage);
            
            if (!isMyMessage) {
                // Messaggio ricevuto: immagine profilo a sinistra
                ImageView profileImageView = createProfileImageView(otherUserProfileImageUrl, 32);
                messageBubble.getChildren().addAll(profileImageView, messageBox);
            } else {
                // Messaggio inviato: solo il messaggio (allineato a destra)
                messageBubble.getChildren().add(messageBox);
            }
            
            return messageBubble;
        }

        /**
         * Crea il box del contenuto del messaggio
         */
        private VBox createMessageBox(Messaggio message, boolean isMyMessage) {
            VBox messageBox = new VBox(2);
            messageBox.setMaxWidth(MESSAGE_MAX_WIDTH);
            messageBox.setAlignment(isMyMessage ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            
            Label messageLabel = createMessageLabel(message, isMyMessage);
            Label timeLabel = createTimeLabel(message, isMyMessage);
            
            messageBox.getChildren().addAll(messageLabel, timeLabel);
            return messageBox;
        }

        /**
         * Crea la label del messaggio
         */
        private Label createMessageLabel(Messaggio message, boolean isMyMessage) {
            Label messageLabel = new Label(message.getTesto());
            messageLabel.setWrapText(true);
            messageLabel.setPadding(new Insets(MESSAGE_BUBBLE_PADDING, 12, MESSAGE_BUBBLE_PADDING, 12));
            applyMessageBubbleStyle(messageLabel, isMyMessage);
            return messageLabel;
        }

        /**
         * Applica lo stile alla bubble del messaggio
         */
        private void applyMessageBubbleStyle(Label messageLabel, boolean isMyMessage) {
            String backgroundColor = isMyMessage ? "#DCF8C6" : "#FFFFFF";
            String textColor = isMyMessage ? "#000000" : "#333333";
            String borderColor = isMyMessage ? "#B2DFDB" : "#E0E0E0";
            
            messageLabel.setStyle(
                "-fx-background-color: " + backgroundColor + ";" +
                "-fx-background-radius: 15;" +
                "-fx-border-color: " + borderColor + ";" +
                "-fx-border-radius: 15;" +
                "-fx-text-fill: " + textColor + ";" +
                "-fx-font-family: " + FONT_FAMILY + ";" +
                "-fx-font-size: 14px;"
            );
        }

        /**
         * Crea la label del timestamp
         */
        private Label createTimeLabel(Messaggio message, boolean isMyMessage) {
            Label timeLabel = new Label(formatTime(message.getDataInvio()));
            timeLabel.setStyle("-fx-text-fill: gray; -fx-font-size: " + TIMESTAMP_FONT_SIZE + "px;");
            timeLabel.setAlignment(isMyMessage ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
            return timeLabel;
        }
    }

    /**
     * Carica i messaggi della conversazione
     */
    private void loadMessages() {
        try {
            List<Messaggio> messages = messaggioDAO.getConversazione(currentUserId, otherUserId);
            messagesListView.getItems().setAll(messages);
            
            scrollToLatestMessage();
        } catch (Exception e) {
            handleLoadMessagesError(e);
        }
    }

    /**
     * Scorri all'ultimo messaggio
     */
    private void scrollToLatestMessage() {
        if (!messagesListView.getItems().isEmpty()) {
            messagesListView.scrollTo(messagesListView.getItems().size() - 1);
        }
    }

    /**
     * Gestisce gli errori nel caricamento dei messaggi
     */
    private void handleLoadMessagesError(Exception e) {
        e.printStackTrace();
        showAlert("Errore", "Impossibile caricare i messaggi: " + e.getMessage());
    }

    /**
     * Invia un nuovo messaggio
     */
    private void sendMessage() {
        String text = messageField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        
        try {
            Messaggio newMessage = createNewMessage(text);
            boolean success = messaggioDAO.inviaMessaggio(newMessage);
            
            handleSendMessageResult(success);
        } catch (Exception e) {
            handleSendMessageError(e);
        }
    }

    /**
     * Crea un nuovo oggetto messaggio
     */
    private Messaggio createNewMessage(String text) {
        return new Messaggio(0, currentUserId, otherUserId, text, LocalDateTime.now(), null);
    }

    /**
     * Gestisce il risultato dell'invio del messaggio
     */
    private void handleSendMessageResult(boolean success) {
        if (success) {
            messageField.clear();
            loadMessages(); // Ricarica per mostrare il nuovo messaggio
        } else {
            showAlert("Errore", "Impossibile inviare il messaggio");
        }
    }

    /**
     * Gestisce gli errori nell'invio del messaggio
     */
    private void handleSendMessageError(Exception e) {
        e.printStackTrace();
        showAlert("Errore", "Impossibile inviare il messaggio: " + e.getMessage());
    }

    /**
     * Formatta il timestamp del messaggio
     */
    private String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(TIME_FORMAT);
        return dateTime.format(formatter);
    }

    /**
     * Mostra un alert all'utente
     */
    private void showAlert(String title, String message) {
        Alert alert = createAlert(title, message);
        alert.showAndWait();
    }

    /**
     * Crea un alert configurato
     */
    private Alert createAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // Personalizza il pulsante
        ButtonType okButton = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        alert.getButtonTypes().setAll(okButton);
        
        return alert;
    }

    // === METODI PUBBLICI PER GESTIONE ESTERNA ===

    /**
     * Chiude la finestra dei messaggi
     */
    public void close() {
        if (stage != null) {
            stage.close();
        }
    }

    /**
     * Mostra la finestra dei messaggi
     */
    public void show() {
        if (stage != null) {
            stage.show();
            stage.toFront(); // Porta in primo piano
        }
    }

    /**
     * Verifica se la finestra è aperta
     */
    public boolean isShowing() {
        return stage != null && stage.isShowing();
    }

    /**
     * Aggiorna la conversazione ricaricando i messaggi
     */
    public void refresh() {
        loadMessages();
    }

    /**
     * Imposta il focus sul campo di input del messaggio
     */
    public void focusMessageField() {
        if (messageField != null) {
            messageField.requestFocus();
        }
    }

    /**
     * Restituisce il numero di messaggi attualmente visualizzati
     */
    public int getMessageCount() {
        return messagesListView != null ? messagesListView.getItems().size() : 0;
    }

    /**
     * Imposta un messaggio precompilato nel campo di input
     */
    public void setPredefinedMessage(String message) {
        if (messageField != null) {
            messageField.setText(message);
            messageField.positionCaret(message.length());
        }
    }

    /**
     * Aggiunge un listener per la chiusura della finestra
     */
    public void setOnCloseListener(Runnable listener) {
        if (stage != null) {
            stage.setOnHidden(e -> listener.run());
        }
    }

    /**
     * Restituisce l'ID dell'utente corrente
     */
    public int getCurrentUserId() {
        return currentUserId;
    }

    /**
     * Restituisce l'ID dell'interlocutore
     */
    public int getOtherUserId() {
        return otherUserId;
    }

    /**
     * Restituisce il nome dell'interlocutore
     */
    public String getOtherUserName() {
        return otherUserName;
    }
    
    
    
}