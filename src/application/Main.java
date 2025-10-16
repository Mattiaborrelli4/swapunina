package application;

import application.controls.ControlloLogin;
import application.controls.ControlloRegistrazione;
import application.Classe.*;
import application.DB.*;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import schermata.SchermataPrincipale;
import schermata.TopBar;
import schermata.button.AccountDialog;
import schermata.button.CarrelloDialog;
import application.messagistica.ChatListDialog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Main extends Application {

    private Stage primaryStage;
    private TopBar topBar;
    private StackPane radice;
    private ControlloRegistrazione controlloRegistrazione;
    private ControlloLogin controlloLogin;
    private Stage palcoscenicoPrincipale;

    // Metodo di inizializzazione dell'applicazione, eseguito prima dello start
    @Override
    public void init() {
        
        try {
            // Inizializza il database
            ConnessioneDB.getConnessione();
            
            // Registra tutti i trigger
            AnnuncioTrigger.registraTuttiITrigger();
            
        } catch (Exception e) {
            System.err.println("❌ Errore durante l'inizializzazione: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Metodo principale di avvio dell'applicazione JavaFX
    @Override
    public void start(Stage primaryStage) {
        this.palcoscenicoPrincipale = primaryStage;
        
        topBar = new TopBar();
        setupTopBarHandlers();
        
        // Inizializzazione radice
        radice = new StackPane();
        radice.setAlignment(Pos.CENTER);
        radice.setStyle("-fx-background-color: linear-gradient(to bottom, #0f1423, #1c2439);");

        // Carica prima la schermata di login
        mostraSchermataLogin();

        // Configurazione della scena principale
        Scene scene = new Scene(radice, 550, 780);
        try {
            scene.getStylesheets().add(getClass().getResource("/application/application.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/schermata/principali.css").toExternalForm());
        } catch (NullPointerException e) {
            System.err.println("Attenzione: File CSS non trovato!");
        }

        // Configurazione dello stage
        primaryStage.setTitle("Università SwapUnina");
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(500);
        primaryStage.setMinHeight(720);
        primaryStage.show();
    }

    // Metodo di pulizia chiamato alla chiusura dell'applicazione
    @Override
    public void stop() {
        // Pulizia quando l'applicazione chiude
        System.out.println("🛑 Applicazione in chiusura...");
        AnnuncioTrigger.rimuoviTuttiITrigger();
    }
    
    // Configura gli handler per i pulsanti della TopBar
    private void setupTopBarHandlers() {
        // Handler per il pulsante Account (esistente)
        topBar.setOnAccount(() -> {
            if (controlloLogin != null && controlloLogin.getUtenteAutenticato() != null) {
                utente utenteAutenticato = controlloLogin.getUtenteAutenticato();
                AccountDialog accountDialog = new AccountDialog(
                    utenteAutenticato.getNome(), 
                    utenteAutenticato.getEmail(),
                    utenteAutenticato.getEmail()
                );
                
                accountDialog.setOnLogout(() -> {
                    eseguiLogoutCompleto();
                });
                
                accountDialog.showAndWait();
            } else {
                AccountDialog accountDialog = new AccountDialog("Utente", "email@example.com", null);
                accountDialog.showAndWait();
            }
        });
        
        // Handler per il pulsante Carrello
        topBar.setOnCart(() -> {
            // Recupera il carrello dell'utente (dovrai implementare questa logica)
            List<Annuncio> carrelloUtente = getCarrelloUtente();
            
            CarrelloDialog carrelloDialog = new CarrelloDialog();
            carrelloDialog.showAndWait();
        });
        
        // Handler per il pulsante Messaggi (nuovo)
        topBar.setOnMessages(() -> {
            if (!isUtenteLoggato()) {
                mostraAlert("Accesso richiesto", "Devi effettuare l'accesso per visualizzare le chat");
                return;
            }
            
            // Verifica se l'utente ha effettivamente delle chat
            if (haChatDisponibili()) {
                apriListaChat();
            } else {
                mostraAlert("Nessuna chat", "Non hai ancora nessuna conversazione. Contatta un venditore per iniziare una chat!");
            }
        });
        
        // Altri handler esistenti
        topBar.setOnSearch(query -> {
            // Implementa la logica di ricerca
        });
        
        topBar.setOnInserisciAnnuncio(() -> {
            // Implementa l'inserimento annuncio
        });
    }

    // Metodo per aprire la lista delle chat
    private void apriListaChat() {
        try {
            ChatListDialog chatListDialog = new ChatListDialog();
            chatListDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
            mostraAlert("Errore", "Impossibile aprire la lista delle chat: " + e.getMessage());
        }
    }

    // Metodo para verificare se l'utente è loggato
    private boolean isUtenteLoggato() {
        return controlloLogin != null && controlloLogin.getUtenteAutenticato() != null;
    }

    // Metodo para mostrare un alert
    private void mostraAlert(String titolo, String messaggio) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titolo);
        alert.setHeaderText(null);
        alert.setContentText(messaggio);
        alert.showAndWait();
    }

    // Metodo para recuperare il carrello
    private List<Annuncio> getCarrelloUtente() {
        return new ArrayList<>();
    }
    
    // Mostra la schermata di login
    private void mostraSchermataLogin() {
        if (radice == null) return;
        
        radice.getChildren().clear();
        controlloLogin = new ControlloLogin();
        
        // Configurazione azioni
        controlloLogin.setOnRegisterAction(this::mostraSchermataRegistrazione);
        
        controlloLogin.setOnLoginSuccess(() -> {
            utente utenteAutenticato = controlloLogin.getUtenteAutenticato();
            if (utenteAutenticato != null) {
                // Imposta i dati utente nella TopBar
                topBar.setDatiUtente(
                    utenteAutenticato.getNome(), 
                    utenteAutenticato.getEmail()
                );
                mostraSchermataPrincipale(utenteAutenticato.getMatricola());
            }
        });

        radice.getChildren().add(controlloLogin);
    }

 // Mostra la schermata di registrazione
    private void mostraSchermataRegistrazione() {
        if (radice == null) return;
        
        radice.getChildren().clear();
        controlloRegistrazione = new ControlloRegistrazione();
        
        // Configurazione azioni
        controlloRegistrazione.setOnLoginAction(this::mostraSchermataLogin);
        controlloRegistrazione.setOnRegistrazioneSuccess(() -> {
            // DOPO REGISTRAZIONE SUCCESSO, VAI DIRETTAMENTE ALL'APPLICAZIONE PRINCIPALE
            utente utenteRegistrato = SessionManager.getCurrentUser();
            if (utenteRegistrato != null) {
                // Imposta i dati utente nella TopBar
                topBar.setDatiUtente(
                    utenteRegistrato.getNome(), 
                    utenteRegistrato.getEmail()
                );
                mostraSchermataPrincipale(utenteRegistrato.getMatricola());
                
            } else {
                // Fallback: se per qualche motivo SessionManager non ha l'utente
                String emailRegistrata = controlloRegistrazione.getEmail();
                mostraSchermataLogin();
                if (controlloLogin != null && emailRegistrata != null) {
                    controlloLogin.setEmail(emailRegistrata);
                    mostraTooltipFeedback();
                }
            }
        });
        
        radice.getChildren().add(controlloRegistrazione);
    }

    // Mostra un tooltip di feedback dopo la registrazione
    private void mostraTooltipFeedback() {
        try {
            Tooltip tooltip = new Tooltip("Email precaricata\nInserisci la password");
            tooltip.setStyle("-fx-font-size: 12; -fx-text-fill: white;");
            tooltip.setAutoHide(true);
            
            // Mostra il tooltip e richiedi il focus al campo password
            javafx.application.Platform.runLater(() -> {
                controlloLogin.getEmailField().requestFocus();
                Tooltip.install(controlloLogin.getEmailField(), tooltip);
                tooltip.show(
                    controlloLogin.getEmailField(), 
                    controlloLogin.getEmailField().localToScreen(
                        controlloLogin.getEmailField().getWidth() + 10, 
                        0
                    ).getX(),
                    controlloLogin.getEmailField().localToScreen(0, 0).getY()
                );
            });
            
            // Rimuovi il tooltip dopo 3 secondi
            new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                     javafx.application.Platform.runLater(() -> {
                            Tooltip.uninstall(controlloLogin.getEmailField(), tooltip);
                            controlloLogin.getPasswordField().requestFocus();
                        });
                    }
                }, 
                3000
            );
        } catch (Exception e) {
            System.err.println("Errore nel tooltip: " + e.getMessage());
        }
    }

    // Mostra la schermata principale dell'applicazione
    private void mostraSchermataPrincipale(String matricolaUtente) {
        if (radice == null || matricolaUtente == null) return;

        radice.getChildren().clear();

        BorderPane mainLayout = new BorderPane();

        // La TopBar la gestisci solo qui
        mainLayout.setTop(topBar.getRoot());

        // Passa la TopBar esistente a SchermataPrincipale
        SchermataPrincipale schermataPrincipale = new SchermataPrincipale(matricolaUtente, palcoscenicoPrincipale, topBar);
        mainLayout.setCenter(schermataPrincipale);

        radice.getChildren().add(mainLayout);
    }

    // Aggiorna i dati utente visualizzati
    private void aggiornaDatiUtente() {
        // Qui dovresti recuperare i dati reali dell'utente dal database
        // Per ora usiamo dati di esempio
        utente utenteCorrente = controlloLogin.getUtenteAutenticato();
        if (utenteCorrente != null) {
            // Dovresti aggiungere un metodo in TopBar per impostare i dati utente
            // topBar.setDatiUtente(utenteCorrente.getNome(), utenteCorrente.getEmail());
        }
    }
    
    // Esegue il logout completo dall'applicazione
    private void eseguiLogoutCompleto() {
        // Resetta lo stato dell'applicazione
        controlloLogin = null;
        
        // Pulisci eventuali dati nella top bar
        topBar.setDatiUtente("", "");
        
        // Torna alla schermata di login
        mostraSchermataLogin();
    }
    
    // Metodo main - punto di ingresso dell'applicazione
    public static void main(String[] args) {
        try {
            launch(args);
        } catch (Exception e) {
            System.err.println("❌ Errore nell'avvio dell'applicazione:");
            e.printStackTrace();
        }
    }
    
    // Verifica se l'utente ha chat disponibili
    private boolean haChatDisponibili() {
        try (Connection connection = ConnessioneDB.getConnessione()) {
            String query = "SELECT COUNT(*) FROM messaggio WHERE mittente_id = ? OR destinatario_id = ?";
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, SessionManager.getCurrentUserId());
            stmt.setInt(2, SessionManager.getCurrentUserId());
            ResultSet rs = stmt.executeQuery();
            
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}