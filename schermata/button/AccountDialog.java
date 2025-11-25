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
import javafx.util.Pair;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

import application.Classe.Annuncio;
import application.DB.AnnuncioDAO;
import application.DB.UtentiDAO;
import application.DB.CloudinaryService;
import application.DB.CodiceDAO;
import application.DB.SessionManager;
import application.DB.ConnessioneDB;

/**
 * Dialog per la gestione dell'account utente
 * 
 * <p>Questa classe gestisce tutte le funzionalità relative all'account utente:
 * - Visualizzazione e modifica immagine profilo
 * - Gestione saldo e ricariche
 * - Cambio password
 * - Logout
 * - Gestione codici di sicurezza per acquisti</p>
 */
public class AccountDialog extends Dialog<Void> {
    
    // ========== COMPONENTI UI ==========
    private ImageView profileImageView;
    private GridPane grid;
    private Label saldoLabel;
    private ScrollPane scrollPane;
    
    // ========== GESTIONE STATO ==========
    private String userEmail;
    private String nomeUtente; // Aggiunto per memorizzare il nome
    
    // ========== HANDLER PER EVENTI ESTERNI ==========
    private Runnable logoutHandler;
    private Runnable profileImageUpdateHandler;
    
    // ========== GESTIONE DATI ==========
    private CarrelloManager carrelloManager;
    private UtentiDAO utentiDAO;
    private CloudinaryService cloudinaryService;
    private CodiceDAO codiceDAO;
    
    /**
     * Costruttore del dialogo account
     * 
     * @param nome Nome dell'utente
     * @param email Email dell'utente
     * @param userEmail Email per identificare l'utente (chiave primaria)
     */
    public AccountDialog(String nome, String email, String userEmail) {
        this.userEmail = userEmail;
        this.nomeUtente = nome; // Memorizza il nome
        this.carrelloManager = CarrelloManager.getInstance();
        this.utentiDAO = new UtentiDAO();
        this.cloudinaryService = new CloudinaryService();
        this.codiceDAO = new CodiceDAO();
        
        if (userEmail == null || userEmail.isEmpty()) {
            System.out.println("Attenzione: userEmail è null o vuota");
        }
        
        inizializzaUI(nome, email);
        caricaFotoProfilo();
    }
    
    private void inizializzaUI(String nome, String email) {
        setTitle("Il Tuo Account");

        // Crea il contenuto principale
        VBox mainContent = createMainContent(nome, email);

        // Configura ScrollPane (NON TRASPARENTE!)
        scrollPane = new ScrollPane();
        scrollPane.setContent(mainContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(600);

        // Sfondo bianco sullo scrollPane e sul contenuto
        scrollPane.setStyle("-fx-background-color: white;");
        mainContent.setStyle("-fx-background-color: white; -fx-text-fill: black;");

        // Imposta il contenuto della finestra
        getDialogPane().setContent(scrollPane);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // Imposta dimensioni minime
        getDialogPane().setMinWidth(450);
        getDialogPane().setMinHeight(600);

        // Aggiorna il saldo all'apertura
        aggiornaSaldoDisplay();
    }
    
    // ========== METODI PER GLI HANDLER ESTERNI ==========
    
    /**
     * Imposta l'handler per il logout
     * 
     * @param handler Runnable da eseguire al logout
     */
    public void setOnLogout(Runnable handler) {
        this.logoutHandler = handler;
    }
    
    /**
     * Imposta l'handler per l'aggiornamento dell'immagine profilo
     * 
     * @param handler Runnable da eseguire quando l'immagine profilo viene aggiornata
     */
    public void setOnProfileImageUpdate(Runnable handler) {
        this.profileImageUpdateHandler = handler;
    }
    
    // ========== CREAZIONE COMPONENTI UI ==========
    
    /**
     * Crea il contenuto principale del dialogo
     */
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
        VBox imageContainer = createImageContainer();
        
        // Informazioni account
        aggiungiInformazioniAccount(nome, email);
        
        // Sezione Conto
        VBox contoSection = createContoSection();
        grid.add(contoSection, 0, 3, 2, 1);
        
        // Sezione Codici di Sicurezza (NUOVA SEZIONE AGGIUNTA)
        VBox codiciSection = createCodiciSection();
        grid.add(codiciSection, 0, 4, 2, 1);
        
        // Container per i pulsanti
        VBox buttonsContainer = createButtonsContainer();
        grid.add(buttonsContainer, 0, 5, 2, 1);
        
        // Aggiungi la griglia al contenuto principale
        mainContent.getChildren().add(grid);
        
        return mainContent;
    }
    
    /**
     * Crea la sezione per la gestione dei codici di sicurezza (NUOVA SEZIONE)
     */
    private VBox createCodiciSection() {
        VBox codiciSection = new VBox(10);
        codiciSection.setAlignment(Pos.CENTER);
        codiciSection.setPadding(new Insets(15));
        codiciSection.setStyle("-fx-background-color: #fff3cd; -fx-border-radius: 8; -fx-border-color: #ffeaa7;");

        Label titoloCodici = new Label("🔐 Codici di Sicurezza");
        titoloCodici.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #856404;");

        Label descrizione = new Label("Gestisci i codici per i tuoi acquisti e vendite");
        descrizione.setStyle("-fx-font-size: 12px; -fx-text-fill: #856404;");

        HBox pulsantiCodici = new HBox(10);
        pulsantiCodici.setAlignment(Pos.CENTER);

        // Bottone per visualizzare codici attivi (ACQUIRENTE)
        Button visualizzaCodiciButton = new Button("👁️ Visualizza Codici Attivi");
        visualizzaCodiciButton.setStyle("-fx-background-color: #17a2b8; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 12px;");
        visualizzaCodiciButton.setOnAction(e -> mostraCodiciAttivi());

        // Bottone per verificare codice (VENDITORE)
        Button verificaCodiceButton = new Button("✓ Verifica Codice");
        verificaCodiceButton.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8px 12px;");
        verificaCodiceButton.setOnAction(e -> mostraVerificaCodice());

        pulsantiCodici.getChildren().addAll(visualizzaCodiciButton, verificaCodiceButton);

        codiciSection.getChildren().addAll(titoloCodici, descrizione, pulsantiCodici);

        return codiciSection;
    }
    
    /**
     * Mostra i codici attivi dell'utente (per ACQUIRENTE)
     */
    // Sostituisci i metodi relativi ai codici nell'AccountDialog con questi:

/**
 * Mostra i codici attivi dell'utente (per ACQUIRENTE) recuperati dal database
 */
  /**
 * Mostra i codici attivi dell'utente (per ACQUIRENTE) recuperati dal database
 */
private void mostraCodiciAttivi() {
    if (userEmail == null || userEmail.isEmpty()) {
        showAlert("Errore", "Utente non trovato. Rieffettua il login.");
        return;
    }

    try {
        // MODIFICA: usa getIdByEmail invece di getUserIdByEmail
        int userId = utentiDAO.getIdByEmail(userEmail);
        if (userId == -1) {
            showAlert("Errore", "Utente non trovato nel database.");
            return;
        }

        // Recupera i codici attivi dal database
        List<Map<String, String>> codiciAttivi = codiceDAO.getCodiciAttiviPerUtente(userId);

        if (codiciAttivi == null || codiciAttivi.isEmpty()) {
            // MODIFICA: Aggiornato il messaggio per riflettere la durata di 2 settimane
            showAlert("Codici Attivi", "Non hai codici attivi al momento.\n\nI codici compaiono qui dopo aver effettuato un acquisto e rimangono validi per 2 settimane.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("I Tuoi Codici Attivi");
        alert.setHeaderText("📦 Codici per i tuoi acquisti in corso");

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        content.setStyle("-fx-background-color: white;");

        Label titolo = new Label("Ecco i tuoi codici di ritiro:");
        titolo.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");

        content.getChildren().add(titolo);

        // Crea una lista ordinata dei codici
        for (int i = 0; i < codiciAttivi.size(); i++) {
            Map<String, String> codice = codiciAttivi.get(i);
            
            VBox codiceBox = createCodiceBox(codice, i + 1);
            content.getChildren().add(codiceBox);
            
            // Aggiungi separatore tra i codici (tranche per l'ultimo)
            if (i < codiciAttivi.size() - 1) {
                Separator separator = new Separator();
                separator.setPadding(new Insets(8, 0, 8, 0));
                content.getChildren().add(separator);
            }
        }

        // Aggiungi istruzioni
        VBox istruzioniBox = new VBox(5);
        istruzioniBox.setPadding(new Insets(15, 0, 0, 0));
        istruzioniBox.setStyle("-fx-background-color: #e8f5e8; -fx-padding: 10; -fx-border-radius: 5;");

        Label istruzione1 = new Label("💡 Mostra il codice al venditore quando ritiri il prodotto");
        istruzione1.setStyle("-fx-font-size: 12px; -fx-text-fill: #27ae60;");

        // MODIFICA: Aggiornata la durata a 2 settimane
        Label istruzione2 = new Label("🔒 Ogni codice è valido per 2 settimane");
        istruzione2.setStyle("-fx-font-size: 12px; -fx-text-fill: #27ae60;");

        Label istruzione3 = new Label("⏰ Il codice scade automaticamente dopo l'uso");
        istruzione3.setStyle("-fx-font-size: 12px; -fx-text-fill: #27ae60;");

        istruzioniBox.getChildren().addAll(istruzione1, istruzione2, istruzione3);
        content.getChildren().add(istruzioniBox);

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(400);
        scrollPane.setStyle("-fx-background-color: white;");

        alert.getDialogPane().setContent(scrollPane);
        alert.getDialogPane().setPrefSize(500, 450);
        alert.showAndWait();

    } catch (Exception e) {
        showAlert("Errore", "Errore nel caricamento dei codici: " + e.getMessage());
        e.printStackTrace();
    }
}


/**
 * Crea un box visivamente accattivante per ogni codice
 */
private VBox createCodiceBox(Map<String, String> codice, int numero) {
        VBox codiceBox = new VBox(8);
        codiceBox.setPadding(new Insets(12));
        codiceBox.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #e9ecef; -fx-border-radius: 8; -fx-border-width: 1;");

        // Header con numero e titolo
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);

        Label numeroLabel = new Label(numero + ".");
        numeroLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #6c757d;");

        String titoloProdotto = codice.get("titolo") != null ? codice.get("titolo") : "Prodotto";
        Label titoloLabel = new Label(titoloProdotto);
        titoloLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2c3e50;");
        titoloLabel.setWrapText(true);

        headerBox.getChildren().addAll(numeroLabel, titoloLabel);

        // Codice di sicurezza
        HBox codiceBoxInner = new HBox(10);
        codiceBoxInner.setAlignment(Pos.CENTER_LEFT);

        Label codiceLabel = new Label("Codice:");
        codiceLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #495057;");

        String codiceValueStr = codice.get("codice") != null ? codice.get("codice") : "N/A";
        Label codiceValue = new Label(codiceValueStr);
        codiceValue.setStyle("-fx-font-family: 'Courier New'; -fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #e74c3c; -fx-background-color: #fff; -fx-padding: 5 10; -fx-border-color: #ddd; -fx-border-radius: 4;");

        codiceBoxInner.getChildren().addAll(codiceLabel, codiceValue);

        // Info aggiuntive
        Label infoLabel = new Label("📋 Presenta questo codice al venditore per ritirare: " + titoloProdotto);
        infoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d; -fx-wrap-text: true;");

        codiceBox.getChildren().addAll(headerBox, codiceBoxInner, infoLabel);

        return codiceBox;
    }


/**
 * Verifica il codice nel database con controllo di sicurezza
 */
private void verificaCodiceConDatabase(int annuncioId, String codice) {
    try {
        // Usa SessionManager per ottenere l'ID utente corrente
        int currentUserId = SessionManager.getCurrentUserId();
        
        if (currentUserId == -1) {
            showAlert("Errore", "Utente non autenticato. Effettua il login.");
            return;
        }

        //Passa tutti e tre i parametri richiesti
        boolean codiceValido = codiceDAO.verificaCodicePerAnnuncio(annuncioId, codice, currentUserId);
        
        if (codiceValido) {
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("✅ Codice Verificato");
            successAlert.setHeaderText("Consegna completata!");
            successAlert.setContentText("La transazione è stata confermata con successo.\n\n" +
                "• L'annuncio è stato segnato come CONSEGNATO\n" +
                "• Verrà rimosso dalla piattaforma\n" +
                "• Il codice è stato invalidato automaticamente\n\n" +
                "Il prodotto è stato consegnato all'acquirente.");
            successAlert.showAndWait();
        } else {
            Alert errorAlert = new Alert(Alert.AlertType.ERROR);
            errorAlert.setTitle("❌ Codice Non Valido");
            errorAlert.setHeaderText("Codice errato o accesso negato");
            errorAlert.setContentText("Impossibile verificare il codice.\n\nPossibili motivi:\n" +
                "• Codice errato\n" +
                "• Codice scaduto (2 settimane)\n" +
                "• Troppi tentativi errati\n" +
                "• Non sei il venditore di questo annuncio\n" +
                "• Annuncio non trovato\n\n" +
                "Verifica con l'acquirente e riprova.");
            errorAlert.showAndWait();
        }
        
    } catch (Exception e) {
        showAlert("Errore", "Si è verificato un errore durante la verifica: " + e.getMessage());
    }
}



/**
 * Mostra il dialogo per verificare un codice (per VENDITORE)
 * Ora include anche l'ID annuncio
 */
/**
 * Mostra il dialogo per verificare un codice (per VENDITORE)
 * Ora mostra solo gli annunci del venditore corrente che hanno codici pendenti
 */
private void mostraVerificaCodice() {
    // Recupera l'ID dell'utente corrente (venditore)
    int currentUserId = SessionManager.getCurrentUserId();
    if (currentUserId == -1) {
        showAlert("Errore", "Utente non autenticato. Effettua il login.");
        return;
    }

    // Recupera gli annunci del venditore che hanno codici pendenti
    List<Annuncio> annunciConCodici = getAnnunciConCodiciPendenti(currentUserId);
    
    if (annunciConCodici == null || annunciConCodici.isEmpty()) {
        showAlert("Nessun codice pendente", 
            "Non hai annunci con codici di sicurezza in attesa di verifica.\n\n" +
            "I codici compaiono qui quando un acquirente effettua un acquisto dai tuoi annunci.");
        return;
    }

    Dialog<Pair<Integer, String>> dialog = new Dialog<>();
    dialog.setTitle("Verifica Codice di Sicurezza");
    dialog.setHeaderText("Seleziona l'annuncio e inserisci il codice");

    // Setup dei bottoni
    ButtonType verificaButtonType = new ButtonType("Verifica", ButtonBar.ButtonData.OK_DONE);
    dialog.getDialogPane().getButtonTypes().addAll(verificaButtonType, ButtonType.CANCEL);

    // Creazione del contenuto
    VBox content = new VBox(15);
    content.setPadding(new Insets(20));
    content.setStyle("-fx-background-color: white;");

    // Istruzioni
    Label labelIstruzioni = new Label("Seleziona l'annuncio per cui vuoi verificare un codice:");
    labelIstruzioni.setStyle("-fx-font-size: 14px; -fx-text-fill: #2c3e50; -fx-wrap-text: true;");

    // ComboBox per selezionare l'annuncio
    VBox annuncioBox = new VBox(5);
    Label labelAnnuncio = new Label("I tuoi annunci con codici pendenti:");
    labelAnnuncio.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

    ComboBox<String> annuncioComboBox = new ComboBox<>();
    
    // Mappa per associare la selezione all'ID annuncio
    Map<String, Integer> annuncioMap = new java.util.HashMap<>();
    
    for (Annuncio annuncio : annunciConCodici) {
        String displayText = String.format("%s (ID: %d) - €%.2f", 
            annuncio.getTitolo(), annuncio.getId(), annuncio.getPrezzo());
        annuncioComboBox.getItems().add(displayText);
        annuncioMap.put(displayText, annuncio.getId());
    }
    
    annuncioComboBox.setPromptText("Seleziona un annuncio");
    annuncioComboBox.setStyle("-fx-font-size: 14px;");

    annuncioBox.getChildren().addAll(labelAnnuncio, annuncioComboBox);

    // Campo Codice
    VBox codiceBox = new VBox(5);
    Label labelCodice = new Label("Codice di sicurezza (6 caratteri):");
    labelCodice.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

    TextField codiceField = new TextField();
    codiceField.setPromptText("Es: A1B2C3");
    codiceField.setStyle("-fx-font-size: 16px; -fx-alignment: center; -fx-font-family: 'Courier New';");
    codiceField.setMaxWidth(200);

    // Limita l'input a 6 caratteri e converte in maiuscolo
    codiceField.textProperty().addListener((observable, oldValue, newValue) -> {
        if (newValue.length() > 6) {
            codiceField.setText(oldValue);
        }
        if (!newValue.toUpperCase().equals(newValue)) {
            codiceField.setText(newValue.toUpperCase());
        }
    });

    codiceBox.getChildren().addAll(labelCodice, codiceField);

    content.getChildren().addAll(labelIstruzioni, annuncioBox, codiceBox);
    dialog.getDialogPane().setContent(content);

    // Converti il risultato quando viene premuto il bottone Verifica
    dialog.setResultConverter(dialogButton -> {
        if (dialogButton == verificaButtonType) {
            String annuncioSelezionato = annuncioComboBox.getValue();
            if (annuncioSelezionato == null) {
                showAlert("Errore", "Seleziona un annuncio dalla lista.");
                return null;
            }

            // Recupera l'ID annuncio dalla mappa
            int annuncioId = annuncioMap.get(annuncioSelezionato);

            String codice = codiceField.getText().trim().toUpperCase();
            if (codice.length() != 6) {
                showAlert("Errore", "Il codice deve essere esattamente di 6 caratteri.");
                return null;
            }
            
            if (!codice.matches("[A-Z0-9]{6}")) {
                showAlert("Errore", "Il codice deve contenere solo lettere maiuscole e numeri.");
                return null;
            }
            
            return new Pair<>(annuncioId, codice);
        }
        return null;
    });

    // Mostra la finestra e processa il risultato
    Optional<Pair<Integer, String>> result = dialog.showAndWait();
    if (result.isPresent()) {
        int annuncioId = result.get().getKey();
        String codice = result.get().getValue();
        verificaCodiceConDatabase(annuncioId, codice);
    }
}

/**
 * Recupera gli annunci del venditore che hanno codici di sicurezza pendenti
 */
private List<Annuncio> getAnnunciConCodiciPendenti(int venditoreId) {
    List<Annuncio> annunciConCodici = new java.util.ArrayList<>();
    
    try {
        String sql = "SELECT DISTINCT a.* " +
                    "FROM annuncio a " +
                    "JOIN codice_conferma cc ON a.id = cc.annuncio_id " +
                    "WHERE a.venditore_id = ? " +
                    "AND cc.data_creazione > CURRENT_TIMESTAMP - INTERVAL '14 days' " +
                    "AND a.stato = 'ATTIVO'";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, venditoreId);
            ResultSet rs = stmt.executeQuery();
            
            AnnuncioDAO annuncioDAO = new AnnuncioDAO();
            while (rs.next()) {
                int annuncioId = rs.getInt("id");
                Annuncio annuncio = annuncioDAO.getAnnuncioById(annuncioId);
                if (annuncio != null) {
                    annunciConCodici.add(annuncio);
                }
            }
        }
    } catch (SQLException e) {
        System.err.println("Errore nel recupero annunci con codici pendenti: " + e.getMessage());
        e.printStackTrace();
    }
    
    return annunciConCodici;
}

    
    /**
     * Simula la verifica del codice (da sostituire con chiamata reale al database)
     */
    private boolean verificaCodiceSimulato(String codice) {
        // ESEMPIO: Logica di verifica simulata
        // In produzione, questo dovrebbe usare CodiceDAO.verificaCodiceConferma()
        
        // Verifica che il codice sia di 6 caratteri alfanumerici
        if (!codice.matches("^[A-Z0-9]{6}$")) {
            return false;
        }
        
        // Simula una verifica nel database
        // I codici "validi" per la demo sono quelli che iniziano con A, B o C
        return codice.startsWith("A") || codice.startsWith("B") || codice.startsWith("C");
    }
    
    // ========== METODI ESISTENTI (MANTENUTI DAL CODICE ORIGINALE) ==========
    
    /**
     * Crea il container per l'immagine profilo
     */
    private VBox createImageContainer() {
        VBox imageContainer = new VBox();
        imageContainer.setAlignment(Pos.CENTER);
        imageContainer.setPadding(new Insets(0, 0, 20, 0));
        
        // Immagine profilo
        profileImageView = createProfileImage();
        imageContainer.getChildren().add(profileImageView);
        
        // Aggiungi container immagine alla griglia
        grid.add(imageContainer, 0, 0, 2, 1);
        
        return imageContainer;
    }
    
    /**
     * Aggiunge le informazioni dell'account alla griglia
     */
    private void aggiungiInformazioniAccount(String nome, String email) {
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
    }
    
    private VBox createContoSection() {
        VBox contoSection = new VBox(10);
        contoSection.setAlignment(Pos.CENTER);
        contoSection.setPadding(new Insets(15));
        contoSection.setStyle("-fx-background-color: #f8f9fa; -fx-border-radius: 8; -fx-border-color: #e9ecef;");

        Label titoloConto = new Label("💳 Il Mio Conto");
        titoloConto.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: black;");

        HBox saldoBox = new HBox(10);
        saldoBox.setAlignment(Pos.CENTER);

        Label saldoTitolo = new Label("Saldo Attuale:");
        saldoTitolo.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: black;");

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
    
    /**
     * Crea il container con tutti i pulsanti di azione
     */
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
    
    /**
     * Crea l'immagine profilo con stile circolare
     */
    private ImageView createProfileImage() {
        ImageView imageView = new ImageView();
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
    
    // ========== GESTIONE IMMAGINE PROFILO ==========
    
    /**
     * Carica la foto profilo dall'URL salvato nel database
     */
    private void caricaFotoProfilo() {
        if (userEmail == null || userEmail.isEmpty()) {
            System.out.println("⚠️  Impossibile caricare foto profilo: userEmail non disponibile");
            return;
        }
        
        try {
            String fotoPath = utentiDAO.getFotoProfilo(userEmail);
            System.out.println("🔍 Percorso immagine profilo dal DB: " + 
                (fotoPath != null && fotoPath.length() > 100 ? 
                 fotoPath.substring(0, 100) + "..." : fotoPath));
            
            caricaImmagineProfilo(fotoPath);
            
        } catch (Exception e) {
            System.err.println("❌ Errore nel caricamento foto profilo: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Carica un'immagine profilo da URL (Cloudinary, locale o avatar SVG)
     */
    private void caricaImmagineProfilo(String imagePath) {
        try {
            Image image;
            
            if (imagePath == null || imagePath.isEmpty()) {
                // Nessuna immagine, usa predefinita
                image = getDefaultProfileImage();
                System.out.println("🎨 Usando immagine predefinita");
            } else if (imagePath.startsWith("data:image/svg+xml")) {
                // È un avatar SVG generato - carica come data URL
                System.out.println("🎨 Caricamento avatar SVG univoco");
                image = new Image(imagePath, 120, 120, true, true, true);
            } else if (imagePath.contains("cloudinary.com") || imagePath.startsWith("http")) {
                // È un URL Cloudinary - carica direttamente
                System.out.println("☁️  Caricamento da Cloudinary: " + imagePath);
                image = new Image(imagePath, 120, 120, true, true, true);
            } else {
                // È un percorso di file locale - converti in URL file
                System.out.println("💾 Caricamento da file locale: " + imagePath);
                File file = new File(imagePath);
                if (file.exists()) {
                    String fileUrl = file.toURI().toString();
                    image = new Image(fileUrl, 120, 120, true, true, true);
                } else {
                    // File non trovato, usa immagine predefinita
                    System.err.println("❌ File non trovato: " + imagePath);
                    image = getDefaultProfileImage();
                }
            }
            
            if (image != null && !image.isError()) {
                profileImageView.setImage(image);
                System.out.println("✅ Immagine profilo caricata con successo");
            } else {
                System.err.println("❌ Errore nel caricamento dell'immagine");
                profileImageView.setImage(getDefaultProfileImage());
            }
            
        } catch (Exception e) {
            System.err.println("💥 Errore critico nel caricamento immagine profilo: " + e.getMessage());
            e.printStackTrace();
            profileImageView.setImage(getDefaultProfileImage());
        }
    }
    
    /**
     * Gestisce il cambio dell'immagine profilo con upload su Cloudinary
     */
    private void changeProfileImage() {
        if (userEmail == null || userEmail.isEmpty()) {
            showAlert("Errore", "Impossibile cambiare l'immagine: email utente non disponibile");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleziona Immagine Profilo");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Immagini", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp")
        );
        
        File selectedFile = fileChooser.showOpenDialog((Stage) getDialogPane().getScene().getWindow());
        if (selectedFile != null) {
            try {
                System.out.println("🔄 Inizio upload immagine su Cloudinary...");
                
                // Usa UtentiDAO per caricare su Cloudinary e aggiornare il database
                boolean success = utentiDAO.aggiornaFotoProfilo(userEmail, selectedFile.getAbsolutePath());
                
                if (success) {
                    // Ricarica l'immagine profilo dal database
                    caricaFotoProfilo();
                    
                    // Notifica l'aggiornamento alla TopBar
                    notificaCambiamentoFotoProfilo();
                    
                    showAlert("Successo", "Immagine profilo aggiornata con successo!");
                    System.out.println("✅ Immagine profilo aggiornata con successo");
                } else {
                    showAlert("Errore", "Impossibile aggiornare l'immagine profilo nel database");
                }
                
            } catch (Exception e) {
                System.err.println("❌ Errore durante l'upload: " + e.getMessage());
                e.printStackTrace();
                showAlert("Errore", "Errore durante l'upload: " + e.getMessage());
            }
        }
    }
    
    /**
     * Notifica il cambiamento della foto profilo alla TopBar
     */
    private void notificaCambiamentoFotoProfilo() {
        if (profileImageUpdateHandler != null) {
            System.out.println("🔔 Notifica cambio immagine profilo alla TopBar");
            profileImageUpdateHandler.run();
        } else {
            System.err.println("⚠️  Nessun handler per l'aggiornamento dell'immagine profilo");
        }
    }
    
    // ========== GESTIONE CONTO E SALDO ==========
    
    /**
     * Mostra il dialogo per la ricarica del conto
     */
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
    
    /**
     * Esegue la ricarica del conto
     */
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
    
    /**
     * Aggiorna la visualizzazione del saldo
     */
    private void aggiornaSaldoDisplay() {
        BigDecimal saldo = carrelloManager.getSaldoUtente();
        saldoLabel.setText(String.format("€%.2f", saldo));
    }
    
    /**
     * Mostra lo storico dei movimenti del conto
     */
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
    
    // ========== GESTIONE PASSWORD ==========
    
    /**
     * Mostra il dialogo per il cambio password
     */
    private void showChangePasswordDialog() {
        Dialog<Void> passwordDialog = new Dialog<>();
        passwordDialog.setTitle("Cambia Password");
        passwordDialog.setHeaderText("Inserisci le informazioni per cambiare la password");
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(25, 25, 25, 25));
        grid.setAlignment(Pos.CENTER);
        
        // Campi per le password
        PasswordField currentPasswordField = new PasswordField();
        currentPasswordField.setPromptText("Password attuale");
        currentPasswordField.setPrefWidth(250);
        currentPasswordField.setStyle("-fx-font-size: 14px;");
        
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Nuova password (min. 6 caratteri)");
        newPasswordField.setPrefWidth(250);
        newPasswordField.setStyle("-fx-font-size: 14px;");
        
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
        configuraValidazionePassword(newPasswordField, confirmPasswordField, newPasswordError, confirmPasswordError);
        
        // Validazione finale al click del pulsante Conferma
        configuraValidazioneFinale(passwordDialog, confirmButtonType, currentPasswordField, 
                                 newPasswordField, confirmPasswordField, currentPasswordError, 
                                 newPasswordError, confirmPasswordError);
        
        // Focus sul primo campo all'apertura
        Platform.runLater(() -> currentPasswordField.requestFocus());
        
        passwordDialog.showAndWait();
    }
    
    /**
     * Configura la validazione in tempo reale per i campi password
     */
    private void configuraValidazionePassword(PasswordField newPasswordField, PasswordField confirmPasswordField,
                                            Label newPasswordError, Label confirmPasswordError) {
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
    }
    
    /**
     * Configura la validazione finale per il cambio password
     */
    private void configuraValidazioneFinale(Dialog<Void> passwordDialog, ButtonType confirmButtonType,
                                          PasswordField currentPasswordField, PasswordField newPasswordField,
                                          PasswordField confirmPasswordField, Label currentPasswordError,
                                          Label newPasswordError, Label confirmPasswordError) {
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
                eseguiCambioPassword(currentPasswordField, newPasswordField, confirmPasswordField,
                                   currentPasswordError, event);
            }
        });
    }
    
    /**
     * Esegue il cambio password nel database
     */
    private void eseguiCambioPassword(PasswordField currentPasswordField, PasswordField newPasswordField,
                                    PasswordField confirmPasswordField, Label currentPasswordError,
                                    javafx.event.ActionEvent event) {
        // Verifica che userEmail non sia null o vuota
        if (userEmail == null || userEmail.isEmpty()) {
            showAlert("Errore", "Email utente non disponibile. Effettua nuovamente il login.");
            event.consume();
            return;
        }
        
        try {
            // Chiama il DAO per aggiornare la password
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
    
    // ========== GESTIONE LOGOUT ==========
    
    /**
     * Configura il pulsante di logout
     */
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
     
    // ========== UTILITY METHODS ==========
    
    /**
     * Mostra un alert informativo
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Restituisce l'immagine profilo predefinita
     */
    private Image getDefaultProfileImage() {
        try {
            // Prova a caricare un'immagine predefinita
            Image defaultImage = new Image("file:default_profile.png", 120, 120, true, true, true);
            if (!defaultImage.isError()) {
                return defaultImage;
            }
        } catch (Exception e) {
            System.err.println("⚠️  Impossibile caricare l'immagine predefinita: " + e.getMessage());
        }
        
        // Fallback: crea un'immagine placeholder con un cerchio colorato
        System.out.println("🎨 Usando immagine placeholder predefinita");
        return null; // JavaFX gestirà l'assenza di immagine
    }
}
