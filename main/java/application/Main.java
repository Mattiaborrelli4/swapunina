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

/**
 * Classe principale dell'applicazione JavaFX - SwapUnina Marketplace
 * 
 * <p>Questa classe gestisce l'avvio dell'applicazione, la navigazione tra schermate
 * e la coordinazione tra i vari componenti dell'interfaccia utente.</p>
 * 
 * <p><b>Funzionalità principali:</b>
 * <ul>
 *   <li>Inizializzazione database e sistema</li>
 *   <li>Gestione ciclo di vita applicazione</li>
 *   <li>Navigazione tra schermate (login, registrazione, principale)</li>
 *   <li>Gestione sessione utente e immagini profilo</li>
 *   <li>Coordinazione componenti UI</li>
 * </ul>
 * </p>
 */
public class Main extends Application {

    // ========== COMPONENTI APPLICAZIONE ==========
    
    /** Stage principale dell'applicazione */
    private Stage palcoscenicoPrincipale;
    
    /** Barra superiore dell'applicazione */
    private TopBar barraSuperiore;
    
    /** Layout radice dell'applicazione */
    private StackPane layoutRadice;
    
    /** Controller per la gestione del login */
    private ControlloLogin controlloLogin;
    
    /** Controller per la gestione della registrazione */
    private ControlloRegistrazione controlloRegistrazione;

    /** DAO per l'accesso ai dati degli utenti */
    private UtentiDAO utentiDAO = new UtentiDAO();

    // ========== METODI CICLO DI VITA ==========
    
    /**
     * Metodo di inizializzazione dell'applicazione
     * Eseguito prima dello start() principale
     */
    @Override
    public void init() {
        try {
            System.out.println("🚀 Inizializzazione applicazione...");
            
            // Inizializza la connessione al database
            Connection connessione = ConnessioneDB.getConnessione();
            if (connessione != null && !connessione.isClosed()) {
                System.out.println("✅ Connessione al database stabilita con successo");
            } else {
                System.err.println("❌ Impossibile stabilire la connessione al database");
            }
            
            // CORREZIONE TEMPORANEA: TEST AVATAR UNIVOCI
            System.out.println("🎨 Test avatar univoci per tutti gli utenti...");
            utentiDAO.testAvatarPerTuttiUtenti();
            
            // Registra tutti i trigger per la gestione automatica degli annunci
            AnnuncioTrigger.registraTuttiITrigger();
            System.out.println("✅ Trigger annunci registrati");
            
        } catch (Exception e) {
            System.err.println("❌ Errore critico durante l'inizializzazione: " + e.getMessage());
            e.printStackTrace();
            mostraErroreCritico(e);
        }
    }

    /**
     * Metodo principale di avvio dell'applicazione JavaFX
     * 
     * @param stagePrimario Lo stage primario fornito da JavaFX
     */
    @Override
    public void start(Stage stagePrimario) {
        this.palcoscenicoPrincipale = stagePrimario;
        
        try {
            // Inizializzazione componenti UI
            inizializzaComponentiUI();
            
            // Configurazione scena principale
            Scene scenaPrincipale = creaScenaPrincipale();
            configuraStagePrincipale(scenaPrincipale);
            
            // Mostra la schermata iniziale (login)
            mostraSchermataLogin();
            
            // Mostra lo stage
            palcoscenicoPrincipale.show();
            
            System.out.println("✅ Applicazione avviata con successo");
            
        } catch (Exception e) {
            System.err.println("❌ Errore critico durante l'avvio: " + e.getMessage());
            e.printStackTrace();
            mostraErroreCritico(e);
        }
    }

    /**
     * Metodo di pulizia chiamato alla chiusura dell'applicazione
     * Esegue operazioni di cleanup e rilascio risorse
     */
    @Override
    public void stop() {
        try {
            System.out.println("🛑 Applicazione in chiusura...");
            
            // Pulisce le statistiche dei trigger
            AnnuncioTrigger.resettaStatistiche();
            System.out.println("✅ Statistiche trigger pulite");
            
            // Chiude la connessione al database
            ConnessioneDB.chiudiConnessione();
            System.out.println("✅ Connessione database chiusa");
            
            // Pulisce la sessione utente
            SessionManager.logout();
            System.out.println("✅ Sessione utente pulita");
            
        } catch (Exception e) {
            System.err.println("⚠️ Errore durante la chiusura dell'applicazione: " + e.getMessage());
        }
    }

    // ========== INIZIALIZZAZIONE UI ==========
    
    /**
     * Inizializza i componenti dell'interfaccia utente
     */
    private void inizializzaComponentiUI() {
        // Inizializza la barra superiore
        barraSuperiore = new TopBar();
        configuraGestoriBarraSuperiore();
        
        // Inizializza il layout radice
        layoutRadice = new StackPane();
        layoutRadice.setAlignment(Pos.CENTER);
        layoutRadice.setStyle("-fx-background-color: linear-gradient(to bottom, #0f1423, #1c2439);");
    }
    
    /**
     * Crea la scena principale dell'applicazione
     * 
     * @return La scena configurata
     */
    private Scene creaScenaPrincipale() {
        Scene scena = new Scene(layoutRadice, 550, 780);
        caricaFogliStile(scena);
        return scena;
    }
    
    /**
     * Configura lo stage principale dell'applicazione
     * 
     * @param scena La scena da associare allo stage
     */
    private void configuraStagePrincipale(Scene scena) {
        palcoscenicoPrincipale.setTitle("Università SwapUnina - Marketplace Universitario");
        palcoscenicoPrincipale.setScene(scena);
        palcoscenicoPrincipale.setMinWidth(500);
        palcoscenicoPrincipale.setMinHeight(720);
        
        // Imposta l'icona dell'applicazione (se disponibile)
        // try {
        //     Image icona = new Image(getClass().getResourceAsStream("/images/app-icon.png"));
        //     palcoscenicoPrincipale.getIcons().add(icona);
        // } catch (Exception e) {
        //     System.err.println("⚠️ Impossibile caricare l'icona dell'applicazione");
        // }
    }
    
    /**
     * Carica i fogli di stile CSS per l'applicazione
     * 
     * @param scena La scena a cui applicare gli stili
     */
    private void caricaFogliStile(Scene scena) {
        try {

scena.getStylesheets().add(getClass().getResource("/style/application.css").toExternalForm());
scena.getStylesheets().add(getClass().getResource("/style/principali.css").toExternalForm());

 
            System.out.println("✅ Fogli di stile CSS caricati con successo");
        } catch (NullPointerException e) {
            System.err.println("⚠️ Attenzione: File CSS non trovato! L'applicazione potrebbe non essere stilizzata correttamente.");
        } catch (Exception e) {
            System.err.println("⚠️ Errore nel caricamento CSS: " + e.getMessage());
        }
    }

    // ========== GESTIONE BARRA SUPERIORE ==========
    
    /**
     * Configura gli handler per i pulsanti della barra superiore
     */
    private void configuraGestoriBarraSuperiore() {
        configuraGestoreAccount();
        configuraGestoreCarrello();
        configuraGestoreMessaggi();
        configuraGestoreRicerca();
        configuraGestoreInserisciAnnuncio();
    }
    
    /**
     * Configura il gestore per il pulsante Account con gestione immagini profilo
     */
    private void configuraGestoreAccount() {
        barraSuperiore.setOnAccount(() -> {
            if (SessionManager.getCurrentUser() != null) {
                utente utenteAutenticato = SessionManager.getCurrentUser();
                
                // Recupera l'URL dell'immagine profilo dal database
                String profileImageUrl = utentiDAO.getFotoProfilo(utenteAutenticato.getEmail());
                
                // Crea il dialogo account con i dati utente
                AccountDialog dialogoAccount = new AccountDialog(
                    utenteAutenticato.getNome(), 
                    utenteAutenticato.getEmail(),
                    utenteAutenticato.getEmail()
                );
                
                // Collega l'aggiornamento dell'immagine profilo
                dialogoAccount.setOnProfileImageUpdate(() -> {
                    // Ricarica l'immagine profilo dal database e aggiorna la TopBar
                    String newProfileImageUrl = utentiDAO.getFotoProfilo(utenteAutenticato.getEmail());
                    barraSuperiore.updateProfileImage(newProfileImageUrl);
                });
                
                // Collega il logout
                dialogoAccount.setOnLogout(this::eseguiLogoutCompleto);
                dialogoAccount.showAndWait();
            } else {
                // Dialogo account per utenti non loggati
                AccountDialog dialogoAccount = new AccountDialog("Utente", "email@example.com", null);
                dialogoAccount.showAndWait();
            }
        });
    }
    
    /**
     * Configura il gestore per il pulsante Carrello
     */
    private void configuraGestoreCarrello() {
        barraSuperiore.setOnCart(() -> {
            if (!isUtenteAutenticato()) {
                mostraAlert("Accesso richiesto", "Devi effettuare l'accesso per visualizzare il carrello");
                return;
            }
            
            List<Annuncio> carrelloUtente = ottieniCarrelloUtente();
            CarrelloDialog dialogoCarrello = new CarrelloDialog();
            dialogoCarrello.showAndWait();
        });
    }
    
    /**
     * Configura il gestore per il pulsante Messaggi
     */
    private void configuraGestoreMessaggi() {
        barraSuperiore.setOnMessages(() -> {
            if (!isUtenteAutenticato()) {
                mostraAlert("Accesso richiesto", "Devi effettuare l'accesso per visualizzare le chat");
                return;
            }
            
            if (haChatDisponibili()) {
                apriListaChat();
            } else {
                mostraAlert("Nessuna chat", 
                    "Non hai ancora nessuna conversazione. " +
                    "Contatta un venditore per iniziare una chat!");
            }
        });
    }
    
    /**
     * Configura il gestore per la barra di ricerca
     */
    private void configuraGestoreRicerca() {
        barraSuperiore.setOnSearch(query -> {
            if (isUtenteAutenticato()) {
                System.out.println("🔍 Ricerca effettuata: " + query);
                // TODO: Implementare la logica di ricerca effettiva
            } else {
                mostraAlert("Accesso richiesto", "Devi effettuare l'accesso per effettuare ricerche");
            }
        });
    }
    
    /**
     * Configura il gestore per il pulsante Inserisci Annuncio
     */
    private void configuraGestoreInserisciAnnuncio() {
        barraSuperiore.setOnInserisciAnnuncio(() -> {
            if (!isUtenteAutenticato()) {
                mostraAlert("Accesso richiesto", "Devi effettuare l'accesso per inserire un annuncio");
                return;
            }
            // TODO: Implementare l'apertura del dialogo inserimento annuncio
            System.out.println("📝 Apertura dialogo inserimento annuncio");
        });
    }

    // ========== GESTIONE NAVIGAZIONE SCHERMATE ==========
    
    /**
     * Mostra la schermata di login
     */
    private void mostraSchermataLogin() {
        if (layoutRadice == null) return;
        
        layoutRadice.getChildren().clear();
        controlloLogin = new ControlloLogin();
        
        // Configurazione azioni del controller login
        controlloLogin.setOnRegisterAction(this::mostraSchermataRegistrazione);
        controlloLogin.setOnLoginSuccess(this::gestisciLoginSuccesso);

        layoutRadice.getChildren().add(controlloLogin);
    }
    
    /**
     * Gestisce il login avvenuto con successo
     * Aggiorna la barra superiore con i dati utente e l'immagine profilo
     */
    private void gestisciLoginSuccesso() {
        utente utenteAutenticato = SessionManager.getCurrentUser();
        if (utenteAutenticato != null) {
            // Recupera l'URL dell'immagine profilo dal database
            String profileImageUrl = utentiDAO.getFotoProfilo(utenteAutenticato.getEmail());
            
            // Aggiorna la barra superiore con i dati utente e immagine profilo
            barraSuperiore.setDatiUtente(
                utenteAutenticato.getNome(), 
                utenteAutenticato.getEmail(),
                profileImageUrl  // Terzo parametro aggiunto per l'immagine profilo
            );
            mostraSchermataPrincipale(utenteAutenticato.getMatricola());
        } else {
            mostraAlert("Errore", "Accesso riuscito ma utente non trovato in sessione");
        }
    }

    /**
     * Mostra la schermata di registrazione
     */
    private void mostraSchermataRegistrazione() {
        if (layoutRadice == null) return;
        
        layoutRadice.getChildren().clear();
        controlloRegistrazione = new ControlloRegistrazione();
        
        // Configurazione azioni del controller registrazione
        controlloRegistrazione.setOnLoginAction(this::mostraSchermataLogin);
        controlloRegistrazione.setOnRegistrazioneSuccess(this::gestisciRegistrazioneSuccesso);
        
        layoutRadice.getChildren().add(controlloRegistrazione);
    }
    
    /**
     * Gestisce la registrazione avvenuta con successo
     * Aggiorna la barra superiore con i dati del nuovo utente
     */
    private void gestisciRegistrazioneSuccesso() {
        utente utenteRegistrato = SessionManager.getCurrentUser();
        if (utenteRegistrato != null) {
            // Recupera l'URL dell'immagine profilo (potrebbe essere null per un nuovo utente)
            String profileImageUrl = utentiDAO.getFotoProfilo(utenteRegistrato.getEmail());
            
            // Aggiorna la barra superiore e mostra schermata principale
            barraSuperiore.setDatiUtente(
                utenteRegistrato.getNome(), 
                utenteRegistrato.getEmail(),
                profileImageUrl  // Terzo parametro aggiunto per l'immagine profilo
            );
            mostraSchermataPrincipale(utenteRegistrato.getMatricola());
        } else {
            // Fallback: torna al login con email precompilata
            String emailRegistrata = controlloRegistrazione.getEmail();
            mostraSchermataLogin();
            if (controlloLogin != null && emailRegistrata != null && !emailRegistrata.isEmpty()) {
                controlloLogin.setEmail(emailRegistrata);
                mostraTooltipFeedback();
            }
        }
    }

    /**
     * Mostra la schermata principale dell'applicazione
     * 
     * @param matricolaUtente La matricola dell'utente autenticato
     */
    private void mostraSchermataPrincipale(String matricolaUtente) {
        if (layoutRadice == null || matricolaUtente == null) return;

        layoutRadice.getChildren().clear();

        BorderPane layoutPrincipale = new BorderPane();

        // Configura il layout con barra superiore e contenuto centrale
        layoutPrincipale.setTop(barraSuperiore.getRoot());
        
        SchermataPrincipale schermataPrincipale = new SchermataPrincipale(
            matricolaUtente, 
            palcoscenicoPrincipale, 
            barraSuperiore
        );
        layoutPrincipale.setCenter(schermataPrincipale);

        layoutRadice.getChildren().add(layoutPrincipale);
    }

    // ========== GESTIONE UTENTE E SESSIONE ==========
    
    /**
     * Esegue il logout completo dall'applicazione
     * Pulisce la sessione e resetta la barra superiore
     */
    private void eseguiLogoutCompleto() {
        System.out.println("👋 Logout utente in corso...");
        
        // Pulisce la sessione
        SessionManager.logout();
        
        // Resetta la barra superiore con tutti e tre i parametri
        barraSuperiore.setDatiUtente("", "", "");
        
        // Torna alla schermata di login
        mostraSchermataLogin();
        
        System.out.println("✅ Logout completato");
    }
    
    /**
     * Verifica se c'è un utente autenticato
     * 
     * @return true se l'utente è loggato, false altrimenti
     */
    private boolean isUtenteAutenticato() {
        return SessionManager.getCurrentUser() != null;
    }
    
    /**
     * Aggiorna i dati utente visualizzati nell'interfaccia
     * Inclusa l'immagine profilo
     */
    private void aggiornaDatiUtente() {
        utente utenteCorrente = SessionManager.getCurrentUser();
        if (utenteCorrente != null) {
            // Recupera l'URL dell'immagine profilo
            String profileImageUrl = utentiDAO.getFotoProfilo(utenteCorrente.getEmail());
            
            barraSuperiore.setDatiUtente(
                utenteCorrente.getNome(), 
                utenteCorrente.getEmail(),
                profileImageUrl  // Terzo parametro aggiunto per l'immagine profilo
            );
        }
    }

    // ========== GESTIONE MESSAGGI E CHAT ==========
    
    /**
     * Apre la lista delle chat dell'utente
     */
    private void apriListaChat() {
        try {
            ChatListDialog dialogoChat = new ChatListDialog();
            dialogoChat.show();
        } catch (Exception e) {
            System.err.println("❌ Errore nell'apertura lista chat: " + e.getMessage());
            mostraAlert("Errore", "Impossibile aprire la lista delle chat: " + e.getMessage());
        }
    }
    
    /**
     * Verifica se l'utente ha chat disponibili
     * 
     * @return true se ci sono chat, false altrimenti
     */
    private boolean haChatDisponibili() {
        try (Connection connection = ConnessioneDB.getConnessione()) {
            String query = "SELECT COUNT(*) FROM messaggio WHERE mittente_id = ? OR destinatario_id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            
            utente utenteCorrente = SessionManager.getCurrentUser();
            if (utenteCorrente == null) {
                return false;
            }
            
            statement.setInt(1, utenteCorrente.getId());
            statement.setInt(2, utenteCorrente.getId());
            ResultSet risultato = statement.executeQuery();
            
            return risultato.next() && risultato.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("❌ Errore SQL nel controllo chat: " + e.getMessage());
            return false;
        }
    }

    // ========== GESTIONE CARRELLO ==========
    
    /**
     * Recupera il carrello dell'utente dal database
     * 
     * @return Lista di annunci nel carrello
     */
    private List<Annuncio> ottieniCarrelloUtente() {
        // TODO: Implementare il recupero effettivo del carrello dal database
        List<Annuncio> carrello = new ArrayList<>();
        
        try {
            utente utenteCorrente = SessionManager.getCurrentUser();
            if (utenteCorrente != null) {
                // Implementazione placeholder
                System.out.println("🛒 Recupero carrello per utente: " + utenteCorrente.getEmail());
            }
        } catch (Exception e) {
            System.err.println("❌ Errore nel recupero carrello: " + e.getMessage());
        }
        
        return carrello;
    }

    // ========== UTILITY E FEEDBACK ==========
    
    /**
     * Mostra un alert informativo all'utente
     * 
     * @param titolo Il titolo dell'alert
     * @param messaggio Il messaggio da visualizzare
     */
    private void mostraAlert(String titolo, String messaggio) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titolo);
        alert.setHeaderText(null);
        alert.setContentText(messaggio);
        alert.showAndWait();
    }
    
    /**
     * Mostra un alert di errore critico
     * 
     * @param eccezione L'eccezione che ha causato l'errore
     */
    private void mostraErroreCritico(Exception eccezione) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore Critico");
        alert.setHeaderText("Errore nell'avvio dell'applicazione");
        alert.setContentText("Si è verificato un errore critico: " + eccezione.getMessage());
        alert.showAndWait();
    }
    
    /**
     * Mostra un tooltip di feedback dopo la registrazione
     * Indica all'utente che l'email è stata precaricata
     */
    private void mostraTooltipFeedback() {
        try {
            Tooltip tooltip = new Tooltip("Email precaricata\nInserisci la password");
            tooltip.setStyle("-fx-font-size: 12; -fx-text-fill: white;");
            tooltip.setAutoHide(true);
            
            javafx.application.Platform.runLater(() -> {
                if (controlloLogin.getEmailField() != null) {
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
                }
            });
            
            // Rimuovi il tooltip dopo 3 secondi
            new java.util.Timer().schedule(
                new java.util.TimerTask() {
                    @Override
                    public void run() {
                        javafx.application.Platform.runLater(() -> {
                            if (controlloLogin.getEmailField() != null) {
                                Tooltip.uninstall(controlloLogin.getEmailField(), tooltip);
                            }
                            if (controlloLogin.getPasswordField() != null) {
                                controlloLogin.getPasswordField().requestFocus();
                            }
                        });
                    }
                }, 
                3000
            );
        } catch (Exception e) {
            System.err.println("⚠️ Errore nel tooltip: " + e.getMessage());
        }
    }

    // ========== METODO MAIN ==========
    
    /**
     * Metodo main - punto di ingresso dell'applicazione
     * 
     * @param args Argomenti da riga di comando
     */
    public static void main(String[] args) {
        try {
            System.out.println("🎯 Avvio applicazione SwapUnina...");
            launch(args);
        } catch (Exception e) {
            System.err.println("💥 Errore fatale nell'avvio dell'applicazione:");
            e.printStackTrace();
            System.exit(1);
        }
    }
}