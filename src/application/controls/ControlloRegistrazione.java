package application.controls;

import application.Classe.utente;
import application.DB.SessionManager;
import application.DB.UtentiDAO;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import java.util.Optional;

public class ControlloRegistrazione extends BorderPane {
    private TextField campoMatricola;
    private TextField campoEmail;
    private TextField campoNome;
    private TextField campoCognome;
    private PasswordField campoPassword;
    private PasswordField campoConfermaPassword;
    
    private Label erroreMatricola;
    private Label erroreEmail;
    private Label erroreNome;
    private Label erroreCognome;
    private Label errorePassword;
    private Label erroreConfermaPassword;
    
    private Hyperlink collegamentoLogin;
    private Button bottoneRegistrati;

    public ControlloRegistrazione() {
        getStyleClass().add("pannello-radice");
        inizializzaInterfaccia();
    }

    private void inizializzaInterfaccia() {
        VBox contenitoreCentrale = new VBox();
        contenitoreCentrale.setAlignment(Pos.CENTER);
        contenitoreCentrale.getChildren().add(creaContenutoRegistrazione());
        this.setCenter(contenitoreCentrale);
        this.setPadding(new Insets(0, 0, 20, 0));
    }

    private VBox creaContenutoRegistrazione() {
        VBox contenitorePrincipale = new VBox(25);
        contenitorePrincipale.setAlignment(Pos.TOP_CENTER);
        contenitorePrincipale.setPadding(new Insets(30, 40, 40, 40));
        contenitorePrincipale.getStyleClass().add("contenitore-principale");

        VBox contenitoreModulo = new VBox(20);
        contenitoreModulo.getStyleClass().add("contenitore-modulo");
        contenitoreModulo.setEffect(new DropShadow(20, 0, 5, Color.BLACK));
        contenitoreModulo.setPadding(new Insets(25, 35, 30, 35));
        contenitoreModulo.setMaxWidth(500);

        Text titolo = new Text("Registrazione Studente");
        titolo.setFont(Font.font("Segoe UI", FontWeight.BOLD, 32));
        titolo.getStyleClass().add("titolo");

        GridPane grigliaModulo = creaGrigliaModulo();
        collegamentoLogin = new Hyperlink("Hai già un account? Accedi");
        collegamentoLogin.getStyleClass().add("collegamento-login");

        bottoneRegistrati = new Button("Registrati");
        bottoneRegistrati.getStyleClass().add("bottone-registrati");
        bottoneRegistrati.setPrefWidth(220);
        bottoneRegistrati.setPrefHeight(45);
        
        configuraGestioneInvio();
        
        bottoneRegistrati.setOnAction(e -> eseguiRegistrazione());
        contenitoreModulo.getChildren().addAll(titolo, grigliaModulo, collegamentoLogin, bottoneRegistrati);
        contenitorePrincipale.getChildren().add(contenitoreModulo);
        
        return contenitorePrincipale;
    }

    private void configuraGestioneInvio() {
        // Lista di tutti i campi
        TextInputControl[] campi = {
            campoMatricola, campoEmail, campoNome, campoCognome,
            campoPassword, campoConfermaPassword
        };
        
        // Aggiungi gestore per tasto Invio a tutti i campi
        for(TextInputControl campo : campi) {
            if (campo != null) {
                campo.setOnKeyPressed(event -> {
                    if(event.getCode() == KeyCode.ENTER) {
                        eseguiRegistrazione();
                    }
                });
            }
        }
    }

    private void eseguiRegistrazione() {
        pulisciErrori();
        convalidaCampi();
        
        if (tuttiCampiValidi()) {
            utente nuovoUtente = new utente(
                campoMatricola.getText(),
                campoNome.getText(),
                campoCognome.getText(),
                campoEmail.getText(),
                campoPassword.getText()
            );
            
            eseguiRegistrazioneAsync(nuovoUtente);
        }
    }

    /**
     * Esegue la registrazione in un thread separato
     */
    private void eseguiRegistrazioneAsync(utente nuovoUtente) {
        disabilitaForm(true);
        
        new Thread(() -> {
            try {
                UtentiDAO gestore = new UtentiDAO();
                
                // Verifica preventiva matricola esistente
                if (gestore.matricolaEsiste(nuovoUtente.getMatricola())) {
                    Platform.runLater(() -> {
                        erroreMatricola.setText("Matricola già registrata");
                        applicaStileErrore(campoMatricola);
                    });
                    return;
                }
                
                // Verifica preventiva email esistente
                if (gestore.emailEsiste(nuovoUtente.getEmail())) {
                    Platform.runLater(() -> {
                        erroreEmail.setText("Email già registrata");
                        applicaStileErrore(campoEmail);
                    });
                    return;
                }
                
                // Effettua la registrazione
                boolean registrato = gestore.registraUtente(nuovoUtente);
                
                if (registrato) {
                    // Ottieni l'utente completo dal database (con ID) - gestione Optional
                    Optional<utente> utenteOpt = gestore.getUtenteByEmail(nuovoUtente.getEmail());
                    
                    if (utenteOpt.isPresent()) {
                        utente utenteRegistrato = utenteOpt.get();
                        SessionManager.setCurrentUser(utenteRegistrato);
                        System.out.println("[DEBUG] Utente registrato e salvato in SessionManager: " + 
                                          utenteRegistrato.getEmail() + " (ID: " + utenteRegistrato.getId() + ")");
                        
                        Platform.runLater(() -> {
                            mostraConfermaRegistrazione();
                            if (onRegistrazioneSuccess != null) onRegistrazioneSuccess.run();
                        });
                    } else {
                        Platform.runLater(() -> {
                            mostraErroreRegistrazione("Registrazione completata ma utente non trovato. Effettua il login manualmente.");
                        });
                    }
                } else {
                    Platform.runLater(() -> {
                        mostraErroreRegistrazione("Registrazione fallita per un errore sconosciuto");
                    });
                }
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    mostraErroreRegistrazione("Errore di connessione al database: " + ex.getMessage());
                    ex.printStackTrace();
                });
            } finally {
                Platform.runLater(() -> disabilitaForm(false));
            }
        }).start();
    }

    private void mostraConfermaRegistrazione() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Registrazione Completata");
        alert.setHeaderText(null);
        alert.setContentText("Registrazione completata con successo! Benvenuto in SwapUnina.");
        alert.showAndWait();
    }

    private void mostraErroreRegistrazione(String messaggio) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore di Registrazione");
        alert.setHeaderText(null);
        alert.setContentText(messaggio);
        alert.showAndWait();
    }

    private void pulisciErrori() {
        pulisciStileCampo(campoMatricola);
        pulisciStileCampo(campoEmail);
        pulisciStileCampo(campoNome);
        pulisciStileCampo(campoCognome);
        pulisciStileCampo(campoPassword);
        pulisciStileCampo(campoConfermaPassword);
        
        erroreMatricola.setText("");
        erroreEmail.setText("");
        erroreNome.setText("");
        erroreCognome.setText("");
        errorePassword.setText("");
        erroreConfermaPassword.setText("");
    }

    private void convalidaCampi() {
        convalidaCampoMatricola();
        convalidaCampoEmail();
        convalidaCampoNome();
        convalidaCampoCognome();
        convalidaCampoPassword();
        convalidaCampoConfermaPassword();
    }

    private void convalidaCampoMatricola() {
        String valore = campoMatricola.getText().trim();
        
        if (valore.isEmpty()) {
            erroreMatricola.setText("Matricola obbligatoria");
            applicaStileErrore(campoMatricola);
        } else if (!valore.matches("^[A-Za-z]\\d{8}$")) {
            erroreMatricola.setText("Formato: lettera + 8 cifre (es. A12345678)");
            applicaStileErrore(campoMatricola);
        } else {
            applicaStileSuccesso(campoMatricola);
            erroreMatricola.setText("");
        }
    }

    private void convalidaCampoEmail() {
        String valore = campoEmail.getText().trim().toLowerCase();
        
        if (valore.isEmpty()) {
            erroreEmail.setText("Email obbligatoria");
            applicaStileErrore(campoEmail);
        } else if (!valore.contains("@")) {
            erroreEmail.setText("Manca il simbolo @");
            applicaStileErrore(campoEmail);
        } else if (!valore.matches("^[\\w-\\.]+@studenti\\.unina\\.it$")) {
            erroreEmail.setText("Deve essere un'email @studenti.unina.it");
            applicaStileErrore(campoEmail);
        } else {
            applicaStileSuccesso(campoEmail);
            erroreEmail.setText("");
        }
    }

    private void convalidaCampoNome() {
        String valore = campoNome.getText().trim();
        
        if (valore.isEmpty()) {
            erroreNome.setText("Nome obbligatorio");
            applicaStileErrore(campoNome);
        } else if (valore.length() < 2) {
            erroreNome.setText("Nome troppo breve (min 2 caratteri)");
            applicaStileErrore(campoNome);
        } else if (valore.length() > 30) {
            erroreNome.setText("Nome troppo lungo (max 30 caratteri)");
            applicaStileErrore(campoNome);
        } else if (!valore.matches("^[a-zA-Z\\sàèéìòù''-]+$")) {
            erroreNome.setText("Caratteri non validi (solo lettere)");
            applicaStileErrore(campoNome);
        } else {
            applicaStileSuccesso(campoNome);
            erroreNome.setText("");
        }
    }

    private void convalidaCampoCognome() {
        String valore = campoCognome.getText().trim();
        
        if (valore.isEmpty()) {
            erroreCognome.setText("Cognome obbligatorio");
            applicaStileErrore(campoCognome);
        } else if (valore.length() < 2) {
            erroreCognome.setText("Cognome troppo breve (min 2 caratteri)");
            applicaStileErrore(campoCognome);
        } else if (valore.length() > 30) {
            erroreCognome.setText("Cognome troppo lungo (max 30 caratteri)");
            applicaStileErrore(campoCognome);
        } else if (!valore.matches("^[a-zA-Z\\sàèéìòù''-]+$")) {
            erroreCognome.setText("Caratteri non validi (solo lettere)");
            applicaStileErrore(campoCognome);
        } else {
            applicaStileSuccesso(campoCognome);
            erroreCognome.setText("");
        }
    }

    private void convalidaCampoPassword() {
        String password = campoPassword.getText();
        
        if (password.isEmpty()) {
            errorePassword.setText("Password obbligatoria");
            applicaStileErrore(campoPassword);
        } else if (password.length() < 8) {
            errorePassword.setText("Password troppo corta (min 8 caratteri)");
            applicaStileErrore(campoPassword);
        } else if (password.length() > 20) {
            errorePassword.setText("Password troppo lunga (max 20 caratteri)");
            applicaStileErrore(campoPassword);
        } else if (!password.matches(".*[A-Z].*")) {
            errorePassword.setText("Manca una lettera maiuscola");
            applicaStileErrore(campoPassword);
        } else if (!password.matches(".*\\d.*")) {
            errorePassword.setText("Manca un numero");
            applicaStileErrore(campoPassword);
        } else if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*")) {
            errorePassword.setText("Manca un carattere speciale");
            applicaStileErrore(campoPassword);
        } else {
            applicaStileSuccesso(campoPassword);
            errorePassword.setText("");
        }
    }

    private void convalidaCampoConfermaPassword() {
        String conferma = campoConfermaPassword.getText();
        String password = campoPassword.getText();
        
        if (conferma.isEmpty()) {
            erroreConfermaPassword.setText("Conferma password obbligatoria");
            applicaStileErrore(campoConfermaPassword);
        } else if (!conferma.equals(password)) {
            erroreConfermaPassword.setText("Le password non coincidono");
            applicaStileErrore(campoConfermaPassword);
        } else {
            applicaStileSuccesso(campoConfermaPassword);
            erroreConfermaPassword.setText("");
        }
    }

    private void applicaStileErrore(Control campo) {
        if (campo != null) {
            campo.getStyleClass().remove("campo-successo");
            if (!campo.getStyleClass().contains("campo-errore")) {
                campo.getStyleClass().add("campo-errore");
            }
        }
    }

    private void applicaStileSuccesso(Control campo) {
        if (campo != null) {
            campo.getStyleClass().remove("campo-errore");
            if (!campo.getStyleClass().contains("campo-successo")) {
                campo.getStyleClass().add("campo-successo");
            }
        }
    }

    private void pulisciStileCampo(Control campo) {
        if (campo != null) {
            campo.getStyleClass().removeAll("campo-errore", "campo-successo");
        }
    }

    private GridPane creaGrigliaModulo() {
        GridPane griglia = new GridPane();
        griglia.setHgap(15);
        griglia.setVgap(8);
        griglia.setPadding(new Insets(15, 0, 0, 0));

        ColumnConstraints col1 = new ColumnConstraints(150);
        ColumnConstraints col2 = new ColumnConstraints(250);
        griglia.getColumnConstraints().addAll(col1, col2);
        
        // Inizializza le etichette di errore
        erroreMatricola = new Label();
        erroreMatricola.getStyleClass().add("etichetta-errore");
        erroreEmail = new Label();
        erroreEmail.getStyleClass().add("etichetta-errore");
        erroreNome = new Label();
        erroreNome.getStyleClass().add("etichetta-errore");
        erroreCognome = new Label();
        erroreCognome.getStyleClass().add("etichetta-errore");
        errorePassword = new Label();
        errorePassword.getStyleClass().add("etichetta-errore");
        erroreConfermaPassword = new Label();
        erroreConfermaPassword.getStyleClass().add("etichetta-errore");

        // Campo Matricola
        Label matricolaLabel = new Label("Matricola:");
        matricolaLabel.getStyleClass().add("etichetta-campo");
        campoMatricola = new TextField();
        campoMatricola.getStyleClass().add("campo-testo");
        campoMatricola.setPromptText("A12345678");
        griglia.add(matricolaLabel, 0, 0);
        griglia.add(campoMatricola, 1, 0);
        griglia.add(erroreMatricola, 1, 1);
        GridPane.setColumnSpan(erroreMatricola, 2);

        // Campo Email
        Label emailLabel = new Label("Email:");
        emailLabel.getStyleClass().add("etichetta-campo");
        campoEmail = new TextField();
        campoEmail.getStyleClass().add("campo-testo");
        campoEmail.setPromptText("nome.cognome@studenti.unina.it");
        
        // Auto lowercase per email
        campoEmail.textProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue.isEmpty()) {
                campoEmail.setText(newValue.toLowerCase());
            }
        });
        
        griglia.add(emailLabel, 0, 2);
        griglia.add(campoEmail, 1, 2);
        griglia.add(erroreEmail, 1, 3);
        GridPane.setColumnSpan(erroreEmail, 2);

        // Campo Nome
        Label nomeLabel = new Label("Nome:");
        nomeLabel.getStyleClass().add("etichetta-campo");
        campoNome = new TextField();
        campoNome.getStyleClass().add("campo-testo");
        campoNome.setPromptText("Mario");
        griglia.add(nomeLabel, 0, 4);
        griglia.add(campoNome, 1, 4);
        griglia.add(erroreNome, 1, 5);
        GridPane.setColumnSpan(erroreNome, 2);

        // Campo Cognome
        Label cognomeLabel = new Label("Cognome:");
        cognomeLabel.getStyleClass().add("etichetta-campo");
        campoCognome = new TextField();
        campoCognome.getStyleClass().add("campo-testo");
        campoCognome.setPromptText("Rossi");
        griglia.add(cognomeLabel, 0, 6);
        griglia.add(campoCognome, 1, 6);
        griglia.add(erroreCognome, 1, 7);
        GridPane.setColumnSpan(erroreCognome, 2);

        // Campo Password
        Label passwordLabel = new Label("Password:");
        passwordLabel.getStyleClass().add("etichetta-campo");
        campoPassword = new PasswordField();
        campoPassword.getStyleClass().add("campo-testo");
        campoPassword.setPromptText("Min. 8 caratteri con maiuscola, numero e speciale");
        griglia.add(passwordLabel, 0, 8);
        griglia.add(campoPassword, 1, 8);
        griglia.add(errorePassword, 1, 9);
        GridPane.setColumnSpan(errorePassword, 2);

        // Campo Conferma Password
        Label confermaPasswordLabel = new Label("Conferma Password:");
        confermaPasswordLabel.getStyleClass().add("etichetta-campo");
        campoConfermaPassword = new PasswordField();
        campoConfermaPassword.getStyleClass().add("campo-testo");
        campoConfermaPassword.setPromptText("Reinserisci la password");
        griglia.add(confermaPasswordLabel, 0, 10);
        griglia.add(campoConfermaPassword, 1, 10);
        griglia.add(erroreConfermaPassword, 1, 11);
        GridPane.setColumnSpan(erroreConfermaPassword, 2);

        return griglia;
    }
    
    /**
     * Abilita/disabilita il form durante la registrazione
     */
    private void disabilitaForm(boolean disabilita) {
        campoMatricola.setDisable(disabilita);
        campoEmail.setDisable(disabilita);
        campoNome.setDisable(disabilita);
        campoCognome.setDisable(disabilita);
        campoPassword.setDisable(disabilita);
        campoConfermaPassword.setDisable(disabilita);
        bottoneRegistrati.setDisable(disabilita);
        collegamentoLogin.setDisable(disabilita);
        
        if (disabilita) {
            bottoneRegistrati.setText("Registrazione in corso...");
        } else {
            bottoneRegistrati.setText("Registrati");
        }
    }
    
    private boolean tuttiCampiValidi() {
        return erroreMatricola.getText().isEmpty() &&
               erroreEmail.getText().isEmpty() &&
               erroreNome.getText().isEmpty() &&
               erroreCognome.getText().isEmpty() &&
               errorePassword.getText().isEmpty() &&
               erroreConfermaPassword.getText().isEmpty();
    }

    public void setOnLoginAction(Runnable azione) {
        collegamentoLogin.setOnAction(e -> azione.run());
    }

    private Runnable onRegistrazioneSuccess;

    public void setOnRegistrazioneSuccess(Runnable azione) {
        this.onRegistrazioneSuccess = azione;
    }
    
    public String getEmail() {
        if (campoEmail != null) {
            return campoEmail.getText().trim().toLowerCase();
        }
        return "";
    }
}