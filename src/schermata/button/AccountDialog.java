package schermata.button;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.shape.Circle;
import java.io.File;

import application.DB.UtentiDAO;

public class AccountDialog extends Dialog<Void> {
    private ImageView profileImageView;
    private boolean darkMode = false;
    private GridPane grid;
    private String userEmail;
    private Runnable logoutHandler;
    
    public AccountDialog(String nome, String email, String userEmail) {
        this.userEmail = userEmail;
        
        if (userEmail == null || userEmail.isEmpty()) {
            System.out.println("Attenzione: userEmail è null o vuota");
        }
        
        setTitle("Il Tuo Account");
        
        // Imposta dimensioni della finestra
        setWidth(500);
        setHeight(600);
        
        // Pannello principale con layout migliorato
        grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(15);
        grid.setPadding(new Insets(30, 30, 30, 30));
        grid.setAlignment(Pos.CENTER);
        
        // Container per l'immagine profilo (centrato in alto)
        VBox imageContainer = new VBox();
        imageContainer.setAlignment(Pos.CENTER);
        imageContainer.setPadding(new Insets(0, 0, 20, 0));
        
        // Immagine profilo
        profileImageView = createProfileImage();
        imageContainer.getChildren().add(profileImageView);
        
        // Aggiungi container immagine alla griglia
        grid.add(imageContainer, 0, 0, 2, 1);
        
        // Informazioni account con stile migliorato
        Label nomeLabel = new Label("Nome:");
        nomeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label nomeValue = new Label(nome);
        nomeValue.setStyle("-fx-font-size: 14px;");
        
        Label emailLabel = new Label("Email:");
        emailLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label emailValue = new Label(email);
        emailValue.setStyle("-fx-font-size: 14px;");
        
        grid.addRow(1, nomeLabel, nomeValue);
        grid.addRow(2, emailLabel, emailValue);
        
        // Toggle per tema chiaro/scuro
        HBox themeBox = new HBox(10);
        themeBox.setAlignment(Pos.CENTER);
        themeBox.setPadding(new Insets(20, 0, 10, 0));
        
        Label themeLabel = new Label("Tema:");
        themeLabel.setStyle("-fx-font-weight: bold;");
        
        ToggleButton themeToggle = new ToggleButton();
        themeToggle.setPrefSize(60, 30);
        themeToggle.setStyle("-fx-background-radius: 15; -fx-background-color: #cccccc;");
        
        // Cerchio interno del toggle
        Circle toggleCircle = new Circle(12);
        toggleCircle.setTranslateX(-10);
        toggleCircle.setFill(javafx.scene.paint.Color.WHITE);
        toggleCircle.setStroke(javafx.scene.paint.Color.LIGHTGRAY);
        
        // StackPane per contenere il cerchio
        StackPane toggleContainer = new StackPane();
        toggleContainer.getChildren().addAll(themeToggle, toggleCircle);
        
        // Gestione del cambio tema
        themeToggle.selectedProperty().addListener((observable, oldValue, newValue) -> {
            darkMode = newValue;
            applyTheme();
            
            // Animazione del cerchio
            if (newValue) {
                toggleCircle.setTranslateX(10);
                themeToggle.setStyle("-fx-background-radius: 15; -fx-background-color: #2e7d32;");
            } else {
                toggleCircle.setTranslateX(-10);
                themeToggle.setStyle("-fx-background-radius: 15; -fx-background-color: #cccccc;");
            }
        });
        
        themeBox.getChildren().addAll(themeLabel, toggleContainer);
        grid.add(themeBox, 0, 3, 2, 1);
        
        // Container per i pulsanti
        VBox buttonsContainer = new VBox(15);
        buttonsContainer.setAlignment(Pos.CENTER);
        buttonsContainer.setPadding(new Insets(20, 0, 0, 0));
        
        // Pulsante per cambiare immagine
        Button changeImageButton = new Button("🖼️ Cambia Immagine");
        changeImageButton.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10px 20px;");
        changeImageButton.setOnAction(e -> changeProfileImage());
        changeImageButton.setMaxWidth(Double.MAX_VALUE);
        
        // Pulsante cambio password
        Button changePasswordButton = new Button("🔒 Cambia Password");
        changePasswordButton.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10px 20px;");
        changePasswordButton.setOnAction(e -> showChangePasswordDialog());
        changePasswordButton.setMaxWidth(Double.MAX_VALUE);
        
        // Pulsante logout
        Button logoutButton = setupLogoutButton();
        
        buttonsContainer.getChildren().addAll(changeImageButton, changePasswordButton, logoutButton);
        grid.add(buttonsContainer, 0, 4, 2, 1);
        
        // Imposta il contenuto della finestra
        getDialogPane().setContent(grid);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        // Applica stile iniziale
        applyTheme();
        
        // Imposta dimensioni minime
        getDialogPane().setMinWidth(450);
        getDialogPane().setMinHeight(550);
    }
    
    // Metodo per impostare l'handler del logout
    public void setOnLogout(Runnable handler) {
        this.logoutHandler = handler;
    }
    
    private Button setupLogoutButton() {
        Button logoutButton = new Button("🚪 Logout");
        logoutButton.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10px 20px;");
        logoutButton.setOnAction(e -> {
            System.out.println("Logout effettuato");
            close(); // Chiude la finestra di dialogo
            
            // Esegui l'handler del logout se impostato
            if (logoutHandler != null) {
                logoutHandler.run();
            }
        });
        logoutButton.setMaxWidth(Double.MAX_VALUE);
        return logoutButton;
    }
    
    private void applyTheme() {
        if (darkMode) {
            // Applica tema scuro
            grid.setStyle("-fx-background-color: #2d2d2d; -fx-text-fill: white;");
            getDialogPane().setStyle("-fx-background-color: #2d2d2d;");
            
            for (javafx.scene.Node node : grid.getChildren()) {
                if (node instanceof Label) {
                    ((Label) node).setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
                }
            }
        } else {
            // Applica tema chiaro
            grid.setStyle("-fx-background-color: white; -fx-text-fill: black;");
            getDialogPane().setStyle("-fx-background-color: white;");
            
            for (javafx.scene.Node node : grid.getChildren()) {
                if (node instanceof Label) {
                    ((Label) node).setStyle("-fx-text-fill: black; -fx-font-weight: bold; -fx-font-size: 14px;");
                }
            }
        }
    }
    
    private ImageView createProfileImage() {
        // Immagine predefinita (puoi sostituirla con un'immagine reale)
        Image image;
        try {
            image = new Image("file:default_profile.png");
        } catch (Exception e) {
            // Se l'immagine non esiste, crea un'immagine placeholder
            image = null;
        }
        
        ImageView imageView = new ImageView(image);
        imageView.setFitHeight(120);
        imageView.setFitWidth(120);
        imageView.setPreserveRatio(true);
        
        // Stile per l'immagine circolare con ombra
        imageView.setStyle("-fx-border-radius: 60px; -fx-border-color: #e0e0e0; -fx-border-width: 3px; " +
                          "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 4);");
        
        // Rendiamo l'immagine circolare
        Circle clip = new Circle(60, 60, 60);
        imageView.setClip(clip);
        
        return imageView;
    }
    
    private void changeProfileImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleziona Immagine Profilo");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Immagini", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        
        File selectedFile = fileChooser.showOpenDialog((Stage) getDialogPane().getScene().getWindow());
        if (selectedFile != null) {
            try {
                Image newImage = new Image(selectedFile.toURI().toString());
                profileImageView.setImage(newImage);
                System.out.println("Immagine profilo cambiata: " + selectedFile.getName());
                
                // Mostra conferma
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Successo");
                successAlert.setHeaderText(null);
                successAlert.setContentText("Immagine profilo aggiornata con successo!");
                successAlert.showAndWait();
                
            } catch (Exception e) {
                showAlert("Errore", "Impossibile caricare l'immagine selezionata: " + e.getMessage());
            }
        }
    }
    
    private void showChangePasswordDialog() {
        Dialog<Void> passwordDialog = new Dialog<>();
        passwordDialog.setTitle("Cambia Password");
        passwordDialog.setHeaderText("Inserisci le informazioni per cambiare la password");
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(25, 25, 25, 25));
        grid.setAlignment(Pos.CENTER);
        
        // Campo password attuale
        PasswordField currentPasswordField = new PasswordField();
        currentPasswordField.setPromptText("Password attuale");
        currentPasswordField.setPrefWidth(250);
        currentPasswordField.setStyle("-fx-font-size: 14px;");
        
        // Campo nuova password
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Nuova password (min. 6 caratteri)");
        newPasswordField.setPrefWidth(250);
        newPasswordField.setStyle("-fx-font-size: 14px;");
        
        // Campo conferma password
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Conferma nuova password");
        confirmPasswordField.setPrefWidth(250);
        confirmPasswordField.setStyle("-fx-font-size: 14px;");
        
        // Etichette di errore
        Label currentPasswordError = new Label();
        currentPasswordError.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        Label newPasswordError = new Label();
        newPasswordError.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        Label confirmPasswordError = new Label();
        confirmPasswordError.setStyle("-fx-text-fill: red; -fx-font-size: 12px;");
        
        // Layout della griglia
        grid.add(new Label("Password attuale:"), 0, 0);
        grid.add(currentPasswordField, 1, 0);
        grid.add(currentPasswordError, 1, 1);
        GridPane.setColumnSpan(currentPasswordError, 2);
        
        grid.add(new Label("Nuova password:"), 0, 2);
        grid.add(newPasswordField, 1, 2);
        grid.add(newPasswordError, 1, 3);
        GridPane.setColumnSpan(newPasswordError, 2);
        
        grid.add(new Label("Conferma password:"), 0, 4);
        grid.add(confirmPasswordField, 1, 4);
        grid.add(confirmPasswordError, 1, 5);
        GridPane.setColumnSpan(confirmPasswordError, 2);
        
        ButtonType confirmButtonType = new ButtonType("Conferma", ButtonBar.ButtonData.OK_DONE);
        passwordDialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);
        
        passwordDialog.getDialogPane().setContent(grid);
        
        // Validazione in tempo reale
        newPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > 0 && newVal.length() < 6) {
                newPasswordError.setText("La password deve essere di almeno 6 caratteri");
            } else {
                newPasswordError.setText("");
            }
            
            // Aggiorna anche la conferma password
            if (!confirmPasswordField.getText().isEmpty() && 
                !confirmPasswordField.getText().equals(newVal)) {
                confirmPasswordError.setText("Le password non coincidono");
            } else {
                confirmPasswordError.setText("");
            }
        });
        
        confirmPasswordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.equals(newPasswordField.getText())) {
                confirmPasswordError.setText("Le password non coincidono");
            } else {
                confirmPasswordError.setText("");
            }
        });
        
        // Validazione finale al click del pulsante Conferma
        Button confirmButton = (Button) passwordDialog.getDialogPane().lookupButton(confirmButtonType);
        confirmButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            boolean isValid = true;
            
            // Reset errori
            currentPasswordError.setText("");
            newPasswordError.setText("");
            confirmPasswordError.setText("");
            
            // Validazione campo password attuale
            if (currentPasswordField.getText().isEmpty()) {
                currentPasswordError.setText("Inserisci la password attuale");
                isValid = false;
            }
            
            // Validazione campo nuova password
            if (newPasswordField.getText().isEmpty()) {
                newPasswordError.setText("Inserisci la nuova password");
                isValid = false;
            } else if (newPasswordField.getText().length() < 6) {
                newPasswordError.setText("La password deve essere di almeno 6 caratteri");
                isValid = false;
            }
            
            // Validazione campo conferma password
            if (confirmPasswordField.getText().isEmpty()) {
                confirmPasswordError.setText("Conferma la nuova password");
                isValid = false;
            } else if (!confirmPasswordField.getText().equals(newPasswordField.getText())) {
                confirmPasswordError.setText("Le password non coincidono");
                isValid = false;
            }
            
            // Controllo che la nuova password non sia uguale alla vecchia
            if (isValid && currentPasswordField.getText().equals(newPasswordField.getText())) {
                newPasswordError.setText("La nuova password non può essere uguale alla attuale");
                isValid = false;
            }
            
            if (!isValid) {
                event.consume(); // Blocca la chiusura del dialogo
            } else {
                // Verifica che userEmail non sia null o vuota
                if (userEmail == null || userEmail.isEmpty()) {
                    showAlert("Errore", "Email utente non disponibile. Effettua nuovamente il login.");
                    event.consume();
                    return;
                }
                
                try {
                    // Qui chiami il DAO per aggiornare la password
                    UtentiDAO utentiDAO = new UtentiDAO();
                    boolean success = utentiDAO.aggiornaPassword(
                        userEmail,
                        currentPasswordField.getText(),
                        newPasswordField.getText()
                    );
                    
                    if (success) {
                        showAlert("Successo", "Password cambiata con successo!");
                        // Pulisci i campi dopo il successo
                        currentPasswordField.clear();
                        newPasswordField.clear();
                        confirmPasswordField.clear();
                    } else {
                        currentPasswordError.setText("Password attuale errata");
                        event.consume();
                    }
                } catch (Exception e) {
                    showAlert("Errore", "Si è verificato un errore durante il cambio password: " + e.getMessage());
                    event.consume();
                }
            }
        });
        
        // Focus sul primo campo all'apertura
        Platform.runLater(() -> currentPasswordField.requestFocus());
        
        passwordDialog.showAndWait();
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}