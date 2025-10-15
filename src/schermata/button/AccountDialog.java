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
import javafx.util.StringConverter;
import java.io.File;
import java.math.BigDecimal;
import java.util.Optional;

import application.DB.UtentiDAO;

public class AccountDialog extends Dialog<Void> {
    private ImageView profileImageView;
    private boolean darkMode = false;
    private GridPane grid;
    private String userEmail;
    private Runnable logoutHandler;
    private CarrelloManager carrelloManager;
    private Label saldoLabel;
    private ScrollPane scrollPane;
    
    public AccountDialog(String nome, String email, String userEmail) {
        this.userEmail = userEmail;
        this.carrelloManager = CarrelloManager.getInstance();
        
        if (userEmail == null || userEmail.isEmpty()) {
            System.out.println("Attenzione: userEmail è null o vuota");
        }
        
        setTitle("Il Tuo Account");
        
        // Imposta dimensioni della finestra
        setWidth(500);
        setHeight(650);
        
        // Crea il contenuto principale
        VBox mainContent = createMainContent(nome, email);
        
        // Crea ScrollPane per permettere lo scorrimento
        scrollPane = new ScrollPane();
        scrollPane.setContent(mainContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(600);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        
        // Imposta il contenuto della finestra
        getDialogPane().setContent(scrollPane);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        // Applica stile iniziale
        applyTheme();
        
        // Imposta dimensioni minime
        getDialogPane().setMinWidth(450);
        getDialogPane().setMinHeight(600);
        
        // Aggiorna il saldo all'apertura
        aggiornaSaldoDisplay();
    }
    
    // NUOVO METODO: Crea il contenuto principale dentro lo ScrollPane
    private VBox createMainContent(String nome, String email) {
        VBox mainContent = new VBox();
        mainContent.setSpacing(20);
        mainContent.setPadding(new Insets(20));
        mainContent.setStyle("-fx-background-color: white;");
        
        // Pannello principale con layout migliorato
        grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(15);
        grid.setPadding(new Insets(10));
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
        
        // Sezione Conto
        VBox contoSection = createContoSection();
        grid.add(contoSection, 0, 3, 2, 1);
        
        // Toggle per tema chiaro/scuro
        HBox themeBox = createThemeBox();
        grid.add(themeBox, 0, 4, 2, 1);
        
        // Container per i pulsanti
        VBox buttonsContainer = createButtonsContainer();
        grid.add(buttonsContainer, 0, 5, 2, 1);
        
        // Aggiungi la griglia al contenuto principale
        mainContent.getChildren().add(grid);
        
        return mainContent;
    }
    
    // NUOVO METODO: Crea la sezione tema
    private HBox createThemeBox() {
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
        return themeBox;
    }
    
    // NUOVO METODO: Crea il container dei pulsanti
    private VBox createButtonsContainer() {
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
        
        // Pulsante storico movimenti
        Button storicoButton = new Button("📊 Storico Movimenti");
        storicoButton.setStyle("-fx-background-color: #8b5cf6; -fx-text-fill: white; -fx-font-size: 14px; -fx-padding: 10px 20px;");
        storicoButton.setOnAction(e -> mostraStoricoMovimenti());
        storicoButton.setMaxWidth(Double.MAX_VALUE);
        
        // Pulsante logout
        Button logoutButton = setupLogoutButton();
        
        buttonsContainer.getChildren().addAll(changeImageButton, changePasswordButton, storicoButton, logoutButton);
        return buttonsContainer;
    }

    // NUOVO METODO: Crea la sezione conto
    private VBox createContoSection() {
        VBox contoSection = new VBox(10);
        contoSection.setAlignment(Pos.CENTER);
        contoSection.setPadding(new Insets(15));
        contoSection.setStyle("-fx-background-color: #f8f9fa; -fx-border-radius: 8; -fx-border-color: #e9ecef;");
        
        Label titoloConto = new Label("💳 Il Mio Conto");
        titoloConto.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        
        HBox saldoBox = new HBox(10);
        saldoBox.setAlignment(Pos.CENTER);
        
        Label saldoTitolo = new Label("Saldo Attuale:");
        saldoTitolo.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        saldoLabel = new Label("€0,00");
        saldoLabel.setStyle("-fx-font-size: 18px; -fx-text-fill: #2e7d32; -fx-font-weight: bold;");
        
        saldoBox.getChildren().addAll(saldoTitolo, saldoLabel);
        
        Button ricaricaButton = new Button("💰 Ricarica");
        ricaricaButton.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 16px;");
        ricaricaButton.setOnAction(e -> mostraDialogRicarica());
        
        HBox pulsantiBox = new HBox(10);
        pulsantiBox.setAlignment(Pos.CENTER);
        
        Button aggiornaButton = new Button("🔄 Aggiorna");
        aggiornaButton.setStyle("-fx-background-color: #6b7280; -fx-text-fill: white; -fx-padding: 8px 16px;");
        aggiornaButton.setOnAction(e -> aggiornaSaldoDisplay());
        
        pulsantiBox.getChildren().addAll(ricaricaButton, aggiornaButton);
        
        contoSection.getChildren().addAll(titoloConto, saldoBox, pulsantiBox);
        
        return contoSection;
    }
    
    // METODO: Mostra dialog per ricarica
    private void mostraDialogRicarica() {
        Dialog<Double> dialog = new Dialog<>();
        dialog.setTitle("Ricarica Conto");
        dialog.setHeaderText("Inserisci l'importo da caricare");

        // Setup dei bottoni
        ButtonType ricaricaButtonType = new ButtonType("Ricarica", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ricaricaButtonType, ButtonType.CANCEL);

        // Creazione del contenuto
        VBox content = new VBox();
        content.setSpacing(15);
        content.setPadding(new Insets(20));

        Label labelImporto = new Label("Importo (€):");
        Spinner<Double> spinnerImporto = new Spinner<>(1.0, 1000.0, 10.0, 5.0);
        spinnerImporto.setEditable(true);
        
        // Formattazione dello spinner
        spinnerImporto.getValueFactory().setConverter(new StringConverter<Double>() {
            @Override
            public String toString(Double value) {
                return String.format("%.2f", value);
            }

            @Override
            public Double fromString(String string) {
                try {
                    return Double.parseDouble(string.replace(",", "."));
                } catch (NumberFormatException e) {
                    return 10.0;
                }
            }
        });

        Label labelMetodo = new Label("Metodo di pagamento:");
        ComboBox<String> comboMetodo = new ComboBox<>();
        comboMetodo.getItems().addAll(
            "Carta di Credito",
            "PayPal", 
            "Bonifico Bancario",
            "Carta Prepagata"
        );
        comboMetodo.setValue("Carta di Credito");

        content.getChildren().addAll(labelImporto, spinnerImporto, labelMetodo, comboMetodo);
        dialog.getDialogPane().setContent(content);

        // Converti il risultato quando viene premuto il bottone Ricarica
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ricaricaButtonType) {
                return spinnerImporto.getValue();
            }
            return null;
        });

        // Mostra la finestra e processa il risultato
        Optional<Double> result = dialog.showAndWait();
        if (result.isPresent()) {
            double importo = result.get();
            eseguiRicarica(importo, comboMetodo.getValue());
        }
    }
    
    // METODO: Esegue la ricarica
    private void eseguiRicarica(double importo, String metodoPagamento) {
        try {
            boolean successo = carrelloManager.ricaricaConto(importo, metodoPagamento);
            
            if (successo) {
                // Aggiorna la visualizzazione del saldo
                aggiornaSaldoDisplay();
                
                // Mostra conferma
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Ricarica Completata");
                alert.setHeaderText(null);
                alert.setContentText(String.format(
                    "Ricarica di €%.2f effettuata con successo!\nMetodo: %s\nNuovo saldo: %s",
                    importo, metodoPagamento, carrelloManager.getSaldoFormattato()
                ));
                alert.showAndWait();
            } else {
                showAlert("Errore", "Errore durante la ricarica. Riprova.");
            }
        } catch (Exception e) {
            showAlert("Errore", "Si è verificato un errore: " + e.getMessage());
        }
    }
    
    // METODO: Aggiorna la visualizzazione del saldo
    private void aggiornaSaldoDisplay() {
        BigDecimal saldo = carrelloManager.getSaldoUtente();
        saldoLabel.setText(String.format("€%.2f", saldo));
    }
    
    // METODO: Mostra storico movimenti
    private void mostraStoricoMovimenti() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Storico Movimenti");
        alert.setHeaderText("Ultimi movimenti del conto");
        
        StringBuilder movimentiText = new StringBuilder();
        var movimenti = carrelloManager.getMovimentiConto();
        
        if (movimenti.isEmpty()) {
            movimentiText.append("Nessun movimento trovato.");
        } else {
            for (var movimento : movimenti) {
                String tipo = "";
                String segno = "";
                
                switch (movimento.getTipo()) {
                    case RICARICA:
                        tipo = "💰 Ricarica";
                        segno = "+";
                        break;
                    case ACQUISTO:
                        tipo = "🛒 Acquisto";
                        segno = "-";
                        break;
                    case ACCREDITO:
                        tipo = "💳 Accredito";
                        segno = "+";
                        break;
                    case ADDEBITO:
                        tipo = "💸 Addebito";
                        segno = "-";
                        break;
                }
                
                movimentiText.append(String.format("%s | %s€%.2f | %s\n",
                    movimento.getData().toLocalDate(),
                    segno,
                    movimento.getImporto(),
                    movimento.getDescrizione()
                ));
            }
        }
        
        TextArea textArea = new TextArea(movimentiText.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        
        alert.getDialogPane().setContent(textArea);
        alert.getDialogPane().setPrefSize(500, 400);
        alert.showAndWait();
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
        String backgroundColor = darkMode ? "#2d2d2d" : "white";
        String textColor = darkMode ? "white" : "black";
        
        // Applica il tema allo ScrollPane e al contenuto
        scrollPane.setStyle("-fx-background: " + backgroundColor + "; -fx-background-color: " + backgroundColor + ";");
        
        // Trova il VBox principale dentro lo ScrollPane
        if (scrollPane.getContent() instanceof VBox) {
            VBox mainContent = (VBox) scrollPane.getContent();
            mainContent.setStyle("-fx-background-color: " + backgroundColor + ";");
            
            // Applica il tema a tutti i nodi figli
            applyThemeToNode(mainContent, textColor);
        }
        
        getDialogPane().setStyle("-fx-background-color: " + backgroundColor + ";");
    }
    
    // NUOVO METODO: Applica il tema ricorsivamente a tutti i nodi
    private void applyThemeToNode(javafx.scene.Node node, String textColor) {
        if (node instanceof Label) {
            ((Label) node).setStyle("-fx-text-fill: " + textColor + ";");
        } else if (node instanceof VBox || node instanceof HBox || node instanceof GridPane) {
            // Se è un container, applica il tema a tutti i figli
            if (node instanceof Pane) {
                for (javafx.scene.Node child : ((Pane) node).getChildren()) {
                    applyThemeToNode(child, textColor);
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
                    showAlert("Errore", "Si è verificato un errore durante el cambio password: " + e.getMessage());
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