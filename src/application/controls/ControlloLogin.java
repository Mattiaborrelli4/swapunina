package application.controls;

import application.DB.SessionManager;
import application.DB.UtentiDAO;

import java.util.Optional;

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

/**
 * Componente di controllo per il login utente
 * Gestisce l'interfaccia e la logica di autenticazione
 */
public class ControlloLogin extends BorderPane {

    private TextField emailField;
    private PasswordField passwordField;
    private Label emailErrorLabel;
    private Label passwordErrorLabel;
    private Hyperlink registerLink;
    private utente utenteAutenticato;
    private Button loginBtn;
    private Runnable onLoginSuccess;

    // Regex per validazione email istituzionale
    private static final String EMAIL_REGEX = "^[\\w-\\.]+@studenti\\.unina\\.it$";
    
    // Costanti per stili CSS
    private static final String STYLE_CAMPO_TESTO = "campo-testo";
    private static final String STYLE_CAMPO_ERRORE = "campo-errore";
    private static final String STYLE_CAMPO_SUCCESSO = "campo-successo";
    private static final String STYLE_ETICHETTA_ERRORE = "etichetta-errore";
    private static final String STYLE_ETICHETTA_CAMPO = "etichetta-campo";
    private static final String STYLE_BOTTONE_REGISTRATI = "bottone-registrati";
    private static final String STYLE_COLLEGAMENTO_LOGIN = "collegamento-login";
    private static final String STYLE_CONTENITORE_MODULO = "contenitore-modulo";
    private static final String STYLE_CONTENITORE_PRINCIPALE = "contenitore-principale";
    private static final String STYLE_PANNELLO_RADICE = "pannello-radice";

    /**
     * Costruttore - inizializza l'interfaccia utente
     */
    public ControlloLogin() {
        getStyleClass().add(STYLE_PANNELLO_RADICE);
        setPrefSize(Double.MAX_VALUE, Double.MAX_VALUE);
        initializeUI();
    }

    /**
     * Inizializza l'interfaccia utente principale
     */
    private void initializeUI() {
        VBox centerWrapper = new VBox();
        centerWrapper.setAlignment(Pos.CENTER);
        centerWrapper.getChildren().add(createLoginContent());
        setCenter(centerWrapper);
    }

    /**
     * Crea il contenuto principale del pannello di login
     */
    private VBox createLoginContent() {
        VBox mainContainer = new VBox(25);
        mainContainer.setAlignment(Pos.TOP_CENTER);
        mainContainer.setPadding(new Insets(30, 40, 40, 40));
        mainContainer.getStyleClass().add(STYLE_CONTENITORE_PRINCIPALE);

        VBox formContainer = new VBox(20);
        formContainer.getStyleClass().add(STYLE_CONTENITORE_MODULO);
        formContainer.setEffect(new DropShadow(20, 0, 5, Color.BLACK));
        formContainer.setPadding(new Insets(25, 35, 30, 35));
        formContainer.setMaxWidth(500);

        // Titolo
        Text title = createTitle();
        
        // Form
        GridPane formGrid = createFormGrid();
        registerLink = createRegisterLink();
        loginBtn = createLoginButton();

        // Configura gestione eventi
        configureEventHandlers();

        formContainer.getChildren().addAll(title, formGrid, registerLink, loginBtn);
        mainContainer.getChildren().add(formContainer);

        return mainContainer;
    }

    /**
     * Crea il titolo del form
     */
    private Text createTitle() {
        Text title = new Text("Accesso a SwapUnina");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        title.getStyleClass().add("titolo");
        return title;
    }

    /**
     * Crea il link per la registrazione
     */
    private Hyperlink createRegisterLink() {
        Hyperlink link = new Hyperlink("Non hai un account? Registrati");
        link.getStyleClass().add(STYLE_COLLEGAMENTO_LOGIN);
        return link;
    }

    /**
     * Crea il bottone di login
     */
    private Button createLoginButton() {
        Button button = new Button("Accedi");
        button.getStyleClass().add(STYLE_BOTTONE_REGISTRATI);
        button.setPrefWidth(220);
        button.setPrefHeight(45);
        return button;
    }

    /**
     * Configura gli handler per gli eventi
     */
    private void configureEventHandlers() {
        // Gestione tasto Invio
        emailField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                eseguiLogin();
            }
        });

        passwordField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                eseguiLogin();
            }
        });

        // Listener per validazione real-time
        emailField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty()) validateEmailField();
        });

        passwordField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.isEmpty()) validatePasswordField();
        });

        // Action handler per il bottone login
        loginBtn.setOnAction(e -> eseguiLogin());
    }

    /**
     * Esegue il processo di login con validazione
     */
    private void eseguiLogin() {
        clearErrors();
        validateFields();

        if (allFieldsValid()) {
            String email = emailField.getText().trim().toLowerCase();
            String password = passwordField.getText();
            
            eseguiLoginAsync(email, password);
        }
    }

    /**
     * Esegue il login in un thread separato per non bloccare l'UI
     */
    private void eseguiLoginAsync(String email, String password) {
        disableForm(true);
        
        new Thread(() -> {
            try {
                UtentiDAO gestore = new UtentiDAO();
                
                // Verifica esistenza email
                if (!gestore.emailEsiste(email)) {
                    Platform.runLater(() -> {
                        mostraErroreEmail("Email non registrata");
                    });
                    return;
                }
                
                // Verifica credenziali
                if (!gestore.verificaCredenziali(email, password)) {
                    Platform.runLater(() -> {
                        mostraErrorePassword("Password errata");
                    });
                    return;
                }
                
                // Login riuscito - recupera utente
                Optional<utente> utenteOpt = gestore.getUtenteByEmail(email);
                
                if (utenteOpt.isPresent()) {
                    utenteAutenticato = utenteOpt.get();
                    SessionManager.setCurrentUser(utenteAutenticato);
                    System.out.println("[DEBUG] Utente loggato: " + 
                                      utenteAutenticato.getEmail() + " (ID: " + utenteAutenticato.getId() + ")");
                    
                    Platform.runLater(() -> {
                        if (onLoginSuccess != null) {
                            onLoginSuccess.run();
                        }
                    });
                } else {
                    // Caso inaspettato: credenziali valide ma utente non trovato
                    Platform.runLater(() -> {
                        mostraErroreGenerico("Errore interno: utente non trovato dopo login riuscito.");
                    });
                }
                
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    mostraErroreGenerico("Si è verificato un errore durante l'accesso. Riprova.");
                    ex.printStackTrace();
                });
            } finally {
                Platform.runLater(() -> disableForm(false));
            }
        }).start();
    }

    /**
     * Crea la griglia del form con tutti i campi
     */
    private GridPane createFormGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(8);
        grid.setPadding(new Insets(15, 0, 0, 0));

        // Configura colonne
        ColumnConstraints col1 = new ColumnConstraints(150);
        ColumnConstraints col2 = new ColumnConstraints(250);
        grid.getColumnConstraints().addAll(col1, col2);

        // Campo Email
        addEmailField(grid);
        
        // Campo Password
        addPasswordField(grid);

        return grid;
    }

    /**
     * Aggiunge il campo email alla griglia
     */
    private void addEmailField(GridPane grid) {
        Label emailLabel = new Label("Email:");
        emailLabel.getStyleClass().add(STYLE_ETICHETTA_CAMPO);
        grid.add(emailLabel, 0, 0);

        emailField = new TextField();
        emailField.getStyleClass().add(STYLE_CAMPO_TESTO);
        emailField.setPrefWidth(220);
        emailField.setPromptText("nome.cognome@studenti.unina.it");
        
        // Auto lowercase per email
        emailField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                emailField.setText(newValue.toLowerCase());
            }
        });
        
        HBox emailBox = new HBox(10, emailField);
        emailBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(emailBox, 1, 0);

        emailErrorLabel = new Label();
        emailErrorLabel.getStyleClass().add(STYLE_ETICHETTA_ERRORE);
        grid.add(emailErrorLabel, 1, 1);
        GridPane.setColumnSpan(emailErrorLabel, 2);
    }

    /**
     * Aggiunge il campo password alla griglia
     */
    private void addPasswordField(GridPane grid) {
        Label passwordLabel = new Label("Password:");
        passwordLabel.getStyleClass().add(STYLE_ETICHETTA_CAMPO);
        grid.add(passwordLabel, 0, 2);

        passwordField = new PasswordField();
        passwordField.getStyleClass().add(STYLE_CAMPO_TESTO);
        passwordField.setPrefWidth(220);
        passwordField.setPromptText("Inserisci la tua password");
        
        HBox passwordBox = new HBox(10, passwordField);
        passwordBox.setAlignment(Pos.CENTER_LEFT);
        grid.add(passwordBox, 1, 2);

        passwordErrorLabel = new Label();
        passwordErrorLabel.getStyleClass().add(STYLE_ETICHETTA_ERRORE);
        grid.add(passwordErrorLabel, 1, 3);
        GridPane.setColumnSpan(passwordErrorLabel, 2);
    }

    // ========== METODI DI VALIDAZIONE ==========

    /**
     * Pulisce tutti gli errori e stili
     */
    private void clearErrors() {
        clearFieldStyle(emailField);
        clearFieldStyle(passwordField);
        emailErrorLabel.setText("");
        passwordErrorLabel.setText("");
    }

    /**
     * Valida tutti i campi del form
     */
    private void validateFields() {
        validateEmailField();
        validatePasswordField();
    }

    /**
     * Valida il campo email
     */
    private void validateEmailField() {
        String email = emailField.getText().trim();
        
        if (email.isEmpty()) {
            mostraErroreEmail("Email obbligatoria");
        } else if (!email.contains("@")) {
            mostraErroreEmail("Manca il simbolo @");
        } else if (!email.matches(EMAIL_REGEX)) {
            mostraErroreEmail("Deve essere un'email @studenti.unina.it");
        } else {
            applicaStileSuccesso(emailField);
            emailErrorLabel.setText("");
        }
    }

    /**
     * Valida il campo password
     */
    private void validatePasswordField() {
        String password = passwordField.getText();
        
        if (password.isEmpty()) {
            mostraErrorePassword("Password obbligatoria");
        } else {
            applicaStileSuccesso(passwordField);
            passwordErrorLabel.setText("");
        }
    }

    /**
     * Verifica se tutti i campi sono validi
     */
    private boolean allFieldsValid() {
        return emailErrorLabel.getText().isEmpty() && 
               passwordErrorLabel.getText().isEmpty();
    }

    // ========== METODI DI GESTIONE ERRORI ==========

    private void mostraErroreEmail(String messaggio) {
        emailErrorLabel.setText(messaggio);
        applicaStileErrore(emailField);
    }

    private void mostraErrorePassword(String messaggio) {
        passwordErrorLabel.setText(messaggio);
        applicaStileErrore(passwordField);
    }

    private void mostraErroreGenerico(String messaggio) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore di accesso");
        alert.setHeaderText(null);
        alert.setContentText(messaggio);
        alert.showAndWait();
    }

    private void applicaStileErrore(TextField field) {
        field.getStyleClass().remove(STYLE_CAMPO_SUCCESSO);
        if (!field.getStyleClass().contains(STYLE_CAMPO_ERRORE)) {
            field.getStyleClass().add(STYLE_CAMPO_ERRORE);
        }
    }

    private void applicaStileSuccesso(TextField field) {
        field.getStyleClass().remove(STYLE_CAMPO_ERRORE);
        if (!field.getStyleClass().contains(STYLE_CAMPO_SUCCESSO)) {
            field.getStyleClass().add(STYLE_CAMPO_SUCCESSO);
        }
    }

    private void clearFieldStyle(TextField field) {
        field.getStyleClass().removeAll(STYLE_CAMPO_ERRORE, STYLE_CAMPO_SUCCESSO);
        field.getStyleClass().add(STYLE_CAMPO_TESTO);
    }

    /**
     * Abilita/disabilita il form durante le operazioni
     */
    private void disableForm(boolean disable) {
        emailField.setDisable(disable);
        passwordField.setDisable(disable);
        loginBtn.setDisable(disable);
        registerLink.setDisable(disable);
        
        if (disable) {
            loginBtn.setText("Accesso in corso...");
        } else {
            loginBtn.setText("Accedi");
        }
    }

    // ========== GETTER E SETTER PUBBLICI ==========

    public void setOnRegisterAction(Runnable action) {
        registerLink.setOnAction(e -> action.run());
    }
    
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