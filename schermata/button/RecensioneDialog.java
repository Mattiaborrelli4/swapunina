package schermata.button;

import application.Classe.Annuncio;
import application.Classe.Recensioni;
import application.Classe.utente;
import application.DB.RecensioneDAO;
import application.DB.UserDAO;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.List;

/**
 * Dialog per la gestione delle recensioni degli annunci
 * Supporta sia la visualizzazione delle recensioni esistenti che l'inserimento di nuove recensioni
 */
public class RecensioneDialog extends Dialog<Boolean> {
    private final List<Recensioni> recensioni;
    private final double punteggioMedio;
    private final Annuncio annuncio;
    private final utente venditore;
    private final int currentUserId;
    
    private int stelleSelezionate = 0;
    private TextArea commentoArea;

    // Costanti per configurazione
    private static final int MAX_COMMENT_LENGTH = 500;
    private static final int STELLE_TOTALI = 5;
    private static final String FONT_FAMILY = "Arial";
    private static final int DIALOG_WIDTH = 500;
    private static final int DIALOG_HEIGHT_VIEW = 450;
    private static final int DIALOG_HEIGHT_NEW = 400;

    /**
     * Costruttore per visualizzare recensioni esistenti di un annuncio specifico
     * @param annuncio Annuncio di cui visualizzare le recensioni
     * @param recensioni Lista delle recensioni dell'annuncio
     * @param punteggioMedio Punteggio medio delle recensioni
     */
    public RecensioneDialog(Annuncio annuncio, List<Recensioni> recensioni, double punteggioMedio) {
        this.annuncio = annuncio;
        this.recensioni = recensioni;
        this.punteggioMedio = punteggioMedio;
        this.venditore = null;
        this.currentUserId = -1;
        
        initializeDialogProperties();
        initializeReviewViewUI();
    }

    /**
     * Costruttore legacy per compatibilità (visualizzazione senza annuncio specifico)
     * @param recensioni Lista delle recensioni da visualizzare
     * @param punteggioMedio Punteggio medio delle recensioni
     */
    public RecensioneDialog(List<Recensioni> recensioni, double punteggioMedio) {
        this.recensioni = recensioni;
        this.punteggioMedio = punteggioMedio;
        this.annuncio = null;
        this.venditore = null;
        this.currentUserId = -1;
        
        initializeDialogProperties();
        initializeReviewViewUI();
    }

    /**
     * Costruttore per creare una nuova recensione
     * @param annuncio Annuncio da recensire
     * @param venditore Venditore da recensire
     * @param currentUserId ID dell'utente che sta lasciando la recensione
     */
    public RecensioneDialog(Annuncio annuncio, utente venditore, int currentUserId) {
        this.recensioni = null;
        this.punteggioMedio = 0.0;
        this.annuncio = annuncio;
        this.venditore = venditore;
        this.currentUserId = currentUserId;
        
        setTitle("Lascia una Recensione");
        setHeaderText("Recensisci il venditore per: " + getAnnuncioTitolo());
        
        initializeNewReviewUI();
    }

    /**
     * Inizializza le proprietà base del dialog
     */
    private void initializeDialogProperties() {
        setTitle(generateDialogTitle());
        setHeaderText("Punteggio medio: " + String.format("%.1f", punteggioMedio) + " ★");
        getDialogPane().setPrefSize(DIALOG_WIDTH, DIALOG_HEIGHT_VIEW);
    }

    /**
     * Genera il titolo del dialog in base ai dati disponibili
     */
    private String generateDialogTitle() {
        if (annuncio != null && annuncio.getTitolo() != null) {
            return "Recensioni per: " + annuncio.getTitolo();
        }
        
        if (recensioni != null && !recensioni.isEmpty() && recensioni.get(0).getAnnuncio() != null) {
            String titolo = recensioni.get(0).getAnnuncio().getTitolo();
            return "Recensioni per: " + (titolo != null ? titolo : "Annuncio");
        }
        
        return "Recensioni dell'annuncio";
    }

    /**
     * Inizializza l'interfaccia per la visualizzazione delle recensioni
     */
    private void initializeReviewViewUI() {
        VBox content = createReviewViewContent();
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        setResultConverter(buttonType -> false);
    }

    /**
     * Crea il contenuto per la visualizzazione delle recensioni
     */
    private VBox createReviewViewContent() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));

        addAnnuncioInfo(content);
        addPunteggioInfo(content);
        addRecensioniList(content);

        return content;
    }

    /**
     * Aggiunge le informazioni sull'annuncio al contenuto
     */
    private void addAnnuncioInfo(VBox content) {
        if (annuncio != null && annuncio.getTitolo() != null) {
            Label titoloLabel = new Label("Annuncio: " + annuncio.getTitolo());
            titoloLabel.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 14));
            content.getChildren().add(titoloLabel);
        }
    }

    /**
     * Aggiunge le informazioni sul punteggio medio al contenuto
     */
    private void addPunteggioInfo(VBox content) {
        Label punteggioLabel = new Label("Punteggio medio: " + String.format("%.1f", punteggioMedio) + " ★");
        punteggioLabel.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 16));
        content.getChildren().add(punteggioLabel);
    }

    /**
     * Aggiunge la lista delle recensioni al contenuto
     */
    private void addRecensioniList(VBox content) {
        if (recensioni == null || recensioni.isEmpty()) {
            addNoReviewsMessage(content);
        } else {
            addReviewsListView(content);
        }
    }

    /**
     * Aggiunge il messaggio per nessuna recensione disponibile
     */
    private void addNoReviewsMessage(VBox content) {
        Label nessunaRecensioneLabel = new Label("Nessuna recensione disponibile per questo annuncio.");
        nessunaRecensioneLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #666;");
        content.getChildren().add(nessunaRecensioneLabel);
    }

    /**
     * Aggiunge la ListView delle recensioni
     */
    private void addReviewsListView(VBox content) {
        ListView<String> listaRecensioni = createReviewsListView();
        
        Label titoloLista = new Label("Recensioni (" + recensioni.size() + "):");
        titoloLista.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 14));
        
        content.getChildren().addAll(titoloLista, listaRecensioni);
    }

    /**
     * Crea e popola la ListView delle recensioni
     */
    private ListView<String> createReviewsListView() {
        ListView<String> listaRecensioni = new ListView<>();
        
        for (Recensioni recensione : recensioni) {
            listaRecensioni.getItems().add(formatReviewText(recensione));
        }
        
        listaRecensioni.setPrefHeight(300);
        listaRecensioni.setPrefWidth(450);
        
        return listaRecensioni;
    }

    /**
     * Formatta il testo di una singola recensione per la visualizzazione
     */
    private String formatReviewText(Recensioni recensione) {
        String nomeAcquirente = getAcquirenteName(recensione);
        String titoloAnnuncio = getAnnuncioTitolo(recensione);
        int punteggio = recensione.getPunteggio();
        String stelle = generateStars(punteggio);
        String commento = getCommento(recensione);
        String data = formatData(recensione);

        return String.format(
            "👤 %s\n📦 %s\n⭐ %s (%d/5)\n💬 %s\n📅 %s",
            nomeAcquirente, titoloAnnuncio, stelle, punteggio, commento, data
        );
    }

    /**
     * Ottiene il nome dell'acquirente dalla recensione
     */
    private String getAcquirenteName(Recensioni recensione) {
        if (recensione.getAcquirente() == null) {
            return "Utente anonimo";
        }
        
        String nome = recensione.getAcquirente().getNome();
        String cognome = recensione.getAcquirente().getCognome();
        
        if (nome != null && cognome != null) {
            return nome + " " + cognome;
        } else if (nome != null) {
            return nome;
        } else {
            return "Utente anonimo";
        }
    }

    /**
     * Ottiene il titolo dell'annuncio dalla recensione
     */
    private String getAnnuncioTitolo(Recensioni recensione) {
        if (recensione.getAnnuncio() != null && recensione.getAnnuncio().getTitolo() != null) {
            return recensione.getAnnuncio().getTitolo();
        } else if (annuncio != null && annuncio.getTitolo() != null) {
            return annuncio.getTitolo();
        } else {
            return "Annuncio #" + (recensione.getAnnuncio() != null ? recensione.getAnnuncio().getId() : "N/D");
        }
    }

    /**
     * Ottiene il titolo dell'annuncio principale
     */
    private String getAnnuncioTitolo() {
        return annuncio.getTitolo() != null ? annuncio.getTitolo() : "Annuncio";
    }

    /**
     * Genera la stringa delle stelle per il punteggio
     */
    private String generateStars(int punteggio) {
        StringBuilder stelleBuilder = new StringBuilder();
        for (int i = 0; i < STELLE_TOTALI; i++) {
            stelleBuilder.append(i < punteggio ? "★" : "☆");
        }
        return stelleBuilder.toString();
    }

    /**
     * Ottiene il commento della recensione
     */
    private String getCommento(Recensioni recensione) {
        String commento = recensione.getCommento();
        return (commento != null && !commento.isEmpty()) ? commento : "Nessun commento";
    }

    /**
     * Formatta la data della recensione
     */
    private String formatData(Recensioni recensione) {
        return recensione.getDataRecensione().format(
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        );
    }

    /**
     * Inizializza l'interfaccia per la creazione di una nuova recensione
     */
    private void initializeNewReviewUI() {
        VBox content = createNewReviewContent();
        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().setPrefSize(DIALOG_WIDTH, DIALOG_HEIGHT_NEW);
        
        setupNewReviewButtons();
        setupResultConverter();
    }

    /**
     * Crea il contenuto per la nuova recensione
     */
    private VBox createNewReviewContent() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER_LEFT);

        addNewReviewInfo(content);
        addStarSelection(content);
        addCommentSection(content);

        return content;
    }

    /**
     * Aggiunge le informazioni della nuova recensione
     */
    private void addNewReviewInfo(VBox content) {
        Label infoLabel = new Label("Stai recensendo: " + getAnnuncioTitolo());
        infoLabel.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 14));
        
        Label venditoreLabel = new Label("Venditore: " + getVenditoreName());
        venditoreLabel.setFont(Font.font(FONT_FAMILY, 12));

        content.getChildren().addAll(infoLabel, venditoreLabel, new Separator());
    }

    /**
     * Ottiene il nome del venditore
     */
    private String getVenditoreName() {
        if (venditore == null) return "";
        
        String nome = venditore.getNome() != null ? venditore.getNome() : "";
        String cognome = venditore.getCognome() != null ? venditore.getCognome() : "";
        
        return (nome + " " + cognome).trim();
    }

    /**
     * Aggiunge la sezione selezione stelle
     */
    private void addStarSelection(VBox content) {
        Label stelleLabel = new Label("Seleziona il punteggio:*");
        stelleLabel.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 12));
        
        HBox stelleContainer = createStarSelector();
        
        Label infoNote = new Label("* Campo obbligatorio");
        infoNote.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        content.getChildren().addAll(stelleLabel, stelleContainer, infoNote, new Separator());
    }

    /**
     * Aggiunge la sezione commento
     */
    private void addCommentSection(VBox content) {
        Label commentoLabel = new Label("Commento (opzionale):");
        commentoLabel.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 12));
        
        commentoArea = createCommentTextArea();
        
        content.getChildren().addAll(commentoLabel, commentoArea);
    }

    /**
     * Crea l'area di testo per il commento
     */
    private TextArea createCommentTextArea() {
        TextArea area = new TextArea();
        area.setPromptText("Scrivi qui il tuo commento... (massimo " + MAX_COMMENT_LENGTH + " caratteri)");
        area.setPrefRowCount(4);
        area.setWrapText(true);
        
        // Limita la lunghezza del commento
        area.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > MAX_COMMENT_LENGTH) {
                area.setText(oldValue);
            }
        });
        
        return area;
    }

    /**
     * Configura i pulsanti per la nuova recensione
     */
    private void setupNewReviewButtons() {
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);
        okButton.setText("Invia Recensione");
        
        Button cancelButton = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setText("Annulla");
    }

    /**
     * Configura il converter per il risultato del dialog
     */
    private void setupResultConverter() {
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return saveRecensione();
            }
            return false;
        });
    }

    /**
     * Crea il selettore di stelle interattivo
     */
    private HBox createStarSelector() {
        HBox stelleBox = new HBox(5);
        stelleBox.setAlignment(Pos.CENTER_LEFT);
        
        for (int i = 1; i <= STELLE_TOTALI; i++) {
            Label stella = createStarLabel(i);
            stelleBox.getChildren().add(stella);
        }
        
        Label punteggioLabel = createScoreLabel();
        stelleBox.getChildren().add(punteggioLabel);
        
        return stelleBox;
    }

    /**
     * Crea una singola label stella
     */
    private Label createStarLabel(int starValue) {
        Label stella = new Label("☆");
        stella.setFont(Font.font(28));
        stella.setUserData(starValue);
        stella.setStyle("-fx-cursor: hand; -fx-text-fill: #ffd700; -fx-padding: 5px;");
        
        setupStarEventHandlers(stella);
        return stella;
    }

    /**
     * Configura gli event handler per una stella
     */
    private void setupStarEventHandlers(Label stella) {
        stella.setOnMouseEntered(e -> highlightStarsUpTo((int) stella.getUserData(), false));
        stella.setOnMouseExited(e -> highlightStarsUpTo(stelleSelezionate, true));
        stella.setOnMouseClicked(e -> selectStars((int) stella.getUserData()));
    }

    /**
     * Crea la label del punteggio
     */
    private Label createScoreLabel() {
        Label punteggioLabel = new Label("0/5");
        punteggioLabel.setFont(Font.font(FONT_FAMILY, FontWeight.BOLD, 14));
        punteggioLabel.setPadding(new Insets(0, 0, 0, 15));
        punteggioLabel.setStyle("-fx-text-fill: #333;");
        return punteggioLabel;
    }

    /**
     * Seleziona il numero di stelle
     */
    private void selectStars(int numStelle) {
        stelleSelezionate = numStelle;
        highlightStarsUpTo(stelleSelezionate, true);
        updateOkButtonState();
    }

    /**
     * Evidenzia le stelle fino al numero specificato
     */
    private void highlightStarsUpTo(int finoA, boolean permanente) {
        HBox stelleBox = (HBox) getDialogPane().lookup(".hbox");
        if (stelleBox == null) return;
        
        for (int i = 0; i < STELLE_TOTALI; i++) {
            Label stella = (Label) stelleBox.getChildren().get(i);
            if (i < finoA) {
                stella.setText("★");
                applyStarStyle(stella, permanente);
            } else {
                stella.setText("☆");
                stella.setStyle("-fx-cursor: hand; -fx-text-fill: #ffd700; -fx-padding: 5px;");
            }
        }
        
        updateScoreLabel(stelleBox, finoA);
    }

    /**
     * Applica lo stile a una stella
     */
    private void applyStarStyle(Label stella, boolean permanente) {
        String style = "-fx-cursor: hand; -fx-text-fill: " + 
                      (permanente ? "#ffa500; -fx-font-weight: bold;" : "#ffd700;") + 
                      " -fx-padding: 5px;";
        stella.setStyle(style);
    }

    /**
     * Aggiorna la label del punteggio
     */
    private void updateScoreLabel(HBox stelleBox, int punteggio) {
        if (stelleBox.getChildren().size() > STELLE_TOTALI) {
            Label punteggioLabel = (Label) stelleBox.getChildren().get(STELLE_TOTALI);
            punteggioLabel.setText(punteggio + "/5");
            punteggioLabel.setStyle("-fx-text-fill: " + (punteggio > 0 ? "#2E8B57" : "#333") + ";");
        }
    }

    /**
     * Aggiorna lo stato del pulsante OK
     */
    private void updateOkButtonState() {
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.setDisable(stelleSelezionate == 0);
        }
    }

    /**
     * Salva la nuova recensione nel database
     */
    private boolean saveRecensione() {
        try {
            if (!validateReview()) {
                return false;
            }
            
            RecensioneDAO recensioneDAO = new RecensioneDAO();
            
            if (recensioneDAO.haGiaRecensito(currentUserId, annuncio.getId())) {
                showError("Hai già recensito questo annuncio!");
                return false;
            }
            
            utente acquirente = getAcquirente(currentUserId);
            if (acquirente == null) {
                showError("Impossibile recuperare i dati dell'acquirente");
                return false;
            }
            
            Recensioni nuovaRecensione = createRecensione(acquirente);
            boolean successo = recensioneDAO.inserisciRecensione(nuovaRecensione);
            
            return handleSaveResult(successo);
            
        } catch (Exception e) {
            showError("Errore imprevisto: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Valida i dati della recensione prima del salvataggio
     */
    private boolean validateReview() {
        if (stelleSelezionate == 0) {
            showError("Seleziona un punteggio prima di inviare la recensione.");
            return false;
        }
        return true;
    }

    /**
     * Recupera i dati dell'acquirente
     */
    private utente getAcquirente(int userId) {
        try {
            UserDAO userDAO = new UserDAO();
            utente acquirente = userDAO.getUserById(userId);
            if (acquirente != null) {
                return acquirente;
            }
        } catch (Exception e) {
            System.err.println("Errore nel recupero acquirente: " + e.getMessage());
        }
        
        // Fallback: crea un utente base con l'ID
        return createFallbackUser(userId);
    }

    /**
     * Crea un utente fallback se il recupero dal database fallisce
     */
    private utente createFallbackUser(int userId) {
        utente acquirente = new utente();
        acquirente.setId(userId);
        acquirente.setNome("Utente");
        acquirente.setCognome("#" + userId);
        return acquirente;
    }

    /**
     * Crea l'oggetto Recensioni per il salvataggio
     */
    private Recensioni createRecensione(utente acquirente) {
        String commento = getTrimmedComment();
        
        return new Recensioni(
            acquirente,
            venditore,
            annuncio,
            commento,
            stelleSelezionate
        );
    }

    /**
     * Ottiene e tronca il commento alla lunghezza massima
     */
    private String getTrimmedComment() {
        String commento = commentoArea.getText().trim();
        if (commento.length() > MAX_COMMENT_LENGTH) {
            commento = commento.substring(0, MAX_COMMENT_LENGTH);
        }
        return commento;
    }

    /**
     * Gestisce il risultato del salvataggio
     */
    private boolean handleSaveResult(boolean successo) {
        if (successo) {
            showSuccess("Recensione inviata con successo! Grazie per il tuo feedback.");
            return true;
        } else {
            showError("Errore durante il salvataggio della recensione. Riprova più tardi.");
            return false;
        }
    }

    /**
     * Mostra un messaggio di errore
     */
    private void showError(String messaggio) {
        showAlert(Alert.AlertType.ERROR, "Errore", messaggio);
    }

    /**
     * Mostra un messaggio di successo
     */
    private void showSuccess(String messaggio) {
        showAlert(Alert.AlertType.INFORMATION, "Successo", messaggio);
    }

    /**
     * Mostra un alert generico
     */
    private void showAlert(Alert.AlertType type, String titolo, String messaggio) {
        Alert alert = new Alert(type);
        alert.setTitle(titolo);
        alert.setHeaderText(null);
        alert.setContentText(messaggio);
        alert.showAndWait();
    }
}