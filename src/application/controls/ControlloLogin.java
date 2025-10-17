package application.controls;

import application.DB.SessionManager;
import application.DB.UtentiDAO;
import application.Classe.utente;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;

public class ControlloLogin extends BorderPane {

    private TextField emailField;
    private PasswordField passwordField;
    private Label emailErrorLabel;
    private Label passwordErrorLabel;
    private Hyperlink registerLink;
    private utente utenteAutenticato;
    private Button loginBtn;

    public ControlloLogin() {
        getStyleClass().add("pannello-radice");
        setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
        initializeUI();
    }

    private void initializeUI() {
        VBox centerWrapper = new VBox();
        centerWrapper.setAlignment(Pos.CENTER);
        centerWrapper.getChildren().add(createLoginContent());
        setCenter(centerWrapper);
    }

    private VBox createLoginContent() {
        VBox mainContainer = new VBox(25);
        mainContainer.setAlignment(Pos.TOP_CENTER);
        mainContainer.setPadding(new Insets(30, 40, 40, 40));
        mainContainer.getStyleClass().add("contenitore-principale");

        VBox formContainer = new VBox(20);
        formContainer.getStyleClass().add("contenitore-modulo");
        formContainer.setEffect(new DropShadow(20, 0, 5, Color.BLACK));
        formContainer.setPadding(new Insets(25, 35, 30, 35));
        formContainer.setMaxWidth(500);

        Text title = new Text("Accesso a SwapUnina");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        title.getStyleClass().add("titolo");

        GridPane formGrid = createFormGrid();
        registerLink = new Hyperlink("Non hai un account? Registrati");
        registerLink.getStyleClass().add("collegamento-login");

        loginBtn = new Button("Accedi"); // Usato il nuovo nome
        loginBtn.getStyleClass().add("bottone-registrati");
        loginBtn.setPrefWidth(220);
        loginBtn.setPrefHeight(45);

        // Configurazione gestione tasto Invio
        configuraGestioneInvio();

        loginBtn.setOnAction(e -> eseguiLogin());
        formContainer.getChildren().addAll(title, formGrid, registerLink, loginBtn);
        mainContainer.getChildren().add(formContainer);

        return mainContainer;
    }

    private void configuraGestioneInvio() {
        // Gestione tasto Invio per campo email
        emailField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                eseguiLogin();
            }
        });

        // Gestione tasto Invio per campo password
        passwordField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                eseguiLogin();
            }
        });
    }

    private void eseguiLogin() {
        clearErrors();
        validateFields();

        if (allFieldsValid()) {
            String email = emailField.getText().trim().toLowerCase();
            String password = passwordField.getText();
            
            new Thread(() -> {
                try {
                    UtentiDAO gestore = new UtentiDAO();
                    
                    if (!gestore.emailEsiste(email)) {
                        Platform.runLater(() -> {
                            emailErrorLabel.setText("Email non registrata");
                            applyErrorStyle(emailField);
                        });
                        return;
                    }
                    
                    if (!gestore.verificaCredenziali(email, password)) {
                        Platform.runLater(() -> {
                            passwordErrorLabel.setText("Password errata");
                            applyErrorStyle(passwordField);
                        });
                        return;
                    }
                    
                    utenteAutenticato = gestore.getUtenteByEmail(email);
                    
                    // SALVA L'UTENTE NEL SESSIONMANAGER DOPO IL LOGIN RIUSCITO
                    if (utenteAutenticato != null) {
                        SessionManager.setCurrentUser(utenteAutenticato);
                        System.out.println("[DEBUG] Utente loggato e salvato in SessionManager: " + 
                                          utenteAutenticato.getEmail() + " (ID: " + utenteAutenticato.getId() + ")");
                    }
                    
                    Platform.runLater(() -> {
                        if (onLoginSuccess != null) onLoginSuccess.run();
                    });
                    
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Errore di accesso");
                        alert.setHeaderText(null);
                        alert.setContentText("Si è verificato un errore durante l'accesso. Riprova.");
                        alert.showAndWait();
                        ex.printStackTrace();
                    });
                }
            }).start();
        }
    }

    private GridPane createFormGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(8);
        grid.setPadding(new Insets(15, 0, 0, 0));

        ColumnConstraints col1 = new ColumnConstraints(150);
        ColumnConstraints col2 = new ColumnConstraints(250);
        grid.getColumnConstraints().addAll(col1, col2);

        // Campo Email
        Label emailLabel = new Label("Email:");
        emailLabel.getStyleClass().add("etichetta-campo");
        grid.add(emailLabel, 0, 0);

        emailField = new TextField();
        emailField.getStyleClass().add("campo-testo");
        emailField.setPrefWidth(220);
        emailField.setPromptText("nome.cognome@studenti.unina.it");
        
        emailField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                emailField.setText(newValue.toLowerCase());
            }
        });
        
        HBox emailBox = new HBox(10, emailField);
        emailBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(emailBox, 1, 0);

        emailErrorLabel = new Label();
        emailErrorLabel.getStyleClass().add("etichetta-errore");
        grid.add(emailErrorLabel, 1, 1);
        GridPane.setColumnSpan(emailErrorLabel, 2);

        // Campo Password
        Label passwordLabel = new Label("Password:");
        passwordLabel.getStyleClass().add("etichetta-campo");
        grid.add(passwordLabel, 0, 2);

        passwordField = new PasswordField();
        passwordField.getStyleClass().add("campo-testo");
        passwordField.setPrefWidth(220);
        passwordField.setPromptText("Inserisci la tua password");
        HBox passwordBox = new HBox(10, passwordField);
        passwordBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(passwordBox, 1, 2);

        passwordErrorLabel = new Label();
        passwordErrorLabel.getStyleClass().add("etichetta-errore");
        grid.add(passwordErrorLabel, 1, 3);
        GridPane.setColumnSpan(passwordErrorLabel, 2);

        return grid;
    }

    private void clearErrors() {
        clearFieldStyle(emailField);
        clearFieldStyle(passwordField);
        emailErrorLabel.setText("");
        passwordErrorLabel.setText("");
    }

    private void validateFields() {
        validateEmailField();
        validatePasswordField();
    }

    private void validateEmailField() {
        String email = emailField.getText();
        
        if (email.isEmpty()) {
            emailErrorLabel.setText("Email obbligatoria");
            applyErrorStyle(emailField);
        } else if (!email.contains("@")) {
            emailErrorLabel.setText("Manca il simbolo @");
            applyErrorStyle(emailField);
        } else if (!email.matches("^[\\w-\\.]+@studenti\\.unina\\.it$")) {
            emailErrorLabel.setText("Deve essere un'email @studenti.unina.it");
            applyErrorStyle(emailField);
        } else {
            applySuccessStyle(emailField);
            emailErrorLabel.setText("");
        }
    }

    private void validatePasswordField() {
        String password = passwordField.getText();
        
        if (password.isEmpty()) {
            passwordErrorLabel.setText("Password obbligatoria");
            applyErrorStyle(passwordField);
        } else {
            applySuccessStyle(passwordField);
            passwordErrorLabel.setText("");
        }
    }

    private void applyErrorStyle(TextField field) {
        field.getStyleClass().remove("campo-successo");
        field.getStyleClass().add("campo-errore");
    }

    private void applySuccessStyle(TextField field) {
        field.getStyleClass().remove("campo-errore");
        field.getStyleClass().add("campo-successo");
    }

    private void clearFieldStyle(TextField field) {
        field.getStyleClass().remove("campo-errore");
        field.getStyleClass().remove("campo-successo");
    }

    private boolean allFieldsValid() {
        return emailErrorLabel.getText().isEmpty() && 
               passwordErrorLabel.getText().isEmpty();
    }

    public void setOnRegisterAction(Runnable action) {
        registerLink.setOnAction(e -> action.run());
    }
    
    private Runnable onLoginSuccess;

    public void setOnLoginSuccess(Runnable action) {
        this.onLoginSuccess = action;
    }

    public TextField getEmailField() {
        return this.emailField;
    }
    
    public utente getUtenteAutenticato() {
        return this.utenteAutenticato;
    }
    
    public void setEmail(String email) {
        this.emailField.setText(email);
        this.passwordField.requestFocus();
    }
    public PasswordField getPasswordField() {
        return this.passwordField;
    }
}