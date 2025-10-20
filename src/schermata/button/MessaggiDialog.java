package schermata.button;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import application.Classe.Annuncio;
import application.Classe.Messaggio;
import application.DB.MessaggioDAO;
import javafx.geometry.Insets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dialog per la gestione della messaggistica tra utenti
 * Supporta conversazioni sia relative ad annunci che conversazioni dirette
 */
public class MessaggiDialog extends Dialog<Void> {
    private final Annuncio annuncio;
    private final int currentUserId;
    private final int interlocutoreId;
    private final String interlocutoreNome;
    private final MessaggioDAO messaggioDAO;
    private ListView<String> messaggiList;
    private TextArea rispostaArea;
    private String messaggioIniziale;

    // Costanti per configurazione UI
    private static final int DIALOG_MIN_WIDTH = 500;
    private static final int DIALOG_MIN_HEIGHT = 400;
    private static final int LISTVIEW_PREF_HEIGHT = 300;
    private static final int TEXTAREA_PREF_ROWS = 3;
    private static final int PADDING = 15;
    private static final int SPACING = 10;
    private static final String TIME_FORMAT = "HH:mm";
    private static final String DEFAULT_PROMPT = "Scrivi la tua risposta...";

    /**
     * Costruttore per conversazione relativa ad un annuncio
     * @param annuncio Annuncio di riferimento per la conversazione
     * @param currentUserId ID dell'utente corrente
     */
    public MessaggiDialog(Annuncio annuncio, int currentUserId) {
        this(annuncio, currentUserId, null);
    }
    
    /**
     * Costruttore per conversazione relativa ad un annuncio con messaggio iniziale
     * @param annuncio Annuncio di riferimento per la conversazione
     * @param currentUserId ID dell'utente corrente
     * @param messaggioIniziale Messaggio precompilato nell'area di risposta
     */
    public MessaggiDialog(Annuncio annuncio, int currentUserId, String messaggioIniziale) {
        this.annuncio = annuncio;
        this.currentUserId = currentUserId;
        this.interlocutoreId = annuncio.getVenditoreId();
        this.interlocutoreNome = annuncio.getNomeUtenteVenditore();
        this.messaggioDAO = new MessaggioDAO();
        this.messaggioIniziale = messaggioIniziale;
        
        initializeUI();
    }
    
    /**
     * Costruttore per conversazione diretta tra utenti
     * @param currentUserId ID dell'utente corrente
     * @param interlocutoreId ID dell'interlocutore
     * @param interlocutoreNome Nome dell'interlocutore
     */
    public MessaggiDialog(int currentUserId, int interlocutoreId, String interlocutoreNome) {
        this(currentUserId, interlocutoreId, interlocutoreNome, null);
    }
    
    /**
     * Costruttore per conversazione diretta con messaggio iniziale
     * @param currentUserId ID dell'utente corrente
     * @param interlocutoreId ID dell'interlocutore
     * @param interlocutoreNome Nome dell'interlocutore
     * @param messaggioIniziale Messaggio precompilato nell'area di risposta
     */
    public MessaggiDialog(int currentUserId, int interlocutoreId, String interlocutoreNome, String messaggioIniziale) {
        this.annuncio = null;
        this.currentUserId = currentUserId;
        this.interlocutoreId = interlocutoreId;
        this.interlocutoreNome = interlocutoreNome;
        this.messaggioDAO = new MessaggioDAO();
        this.messaggioIniziale = messaggioIniziale;
        
        initializeUI();
    }

    /**
     * Inizializza l'interfaccia utente del dialog
     */
    private void initializeUI() {
        setupDialogProperties();
        setupMessageList();
        setupResponseArea();
        setupMainLayout();
        setupEventHandlers();
    }

    /**
     * Configura le proprietà base del dialog
     */
    private void setupDialogProperties() {
        setTitle(generateDialogTitle());
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        getDialogPane().setMinWidth(DIALOG_MIN_WIDTH);
        getDialogPane().setMinHeight(DIALOG_MIN_HEIGHT);
    }

    /**
     * Genera il titolo del dialog in base al contesto
     */
    private String generateDialogTitle() {
        if (annuncio != null) {
            return "Messaggi con " + interlocutoreNome + " - " + annuncio.getTitolo();
        } else {
            return "Messaggi con " + interlocutoreNome;
        }
    }

    /**
     * Configura la lista dei messaggi
     */
    private void setupMessageList() {
        messaggiList = new ListView<>();
        messaggiList.setPrefHeight(LISTVIEW_PREF_HEIGHT);
        loadMessages();
    }

    /**
     * Configura l'area di risposta
     */
    private void setupResponseArea() {
        rispostaArea = new TextArea();
        rispostaArea.setPromptText(DEFAULT_PROMPT);
        rispostaArea.setPrefRowCount(TEXTAREA_PREF_ROWS);
        
        if (messaggioIniziale != null) {
            rispostaArea.setText(messaggioIniziale);
        }
    }

    /**
     * Configura il layout principale
     */
    private void setupMainLayout() {
        HBox responseBox = createResponseBox();
        VBox content = createContentBox(responseBox);
        getDialogPane().setContent(content);
    }

    /**
     * Crea il box per l'area di risposta e il pulsante invia
     */
    private HBox createResponseBox() {
        Button inviaBtn = new Button("Invia");
        HBox rispostaBox = new HBox(SPACING, rispostaArea, inviaBtn);
        rispostaBox.setPadding(new Insets(10, 0, 0, 0));
        HBox.setHgrow(rispostaArea, Priority.ALWAYS);
        return rispostaBox;
    }

    /**
     * Crea il box contenitore principale
     */
    private VBox createContentBox(HBox responseBox) {
        VBox content = new VBox(SPACING, messaggiList, responseBox);
        content.setPadding(new Insets(PADDING));
        return content;
    }

    /**
     * Configura gli event handler per i componenti UI
     */
    private void setupEventHandlers() {
        setupSendButtonHandler();
        setupEnterKeyHandler();
    }

    /**
     * Configura l'handler per il pulsante invia
     */
    private void setupSendButtonHandler() {
        Button inviaBtn = (Button) getDialogPane().lookup(".button");
        if (inviaBtn != null) {
            inviaBtn.setOnAction(e -> handleSendMessage());
        }
    }

    /**
     * Configura l'handler per il tasto Enter nell'area di testo
     */
    private void setupEnterKeyHandler() {
        rispostaArea.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case ENTER:
                    if (event.isShiftDown()) {
                        // Shift+Enter: nuova riga
                        rispostaArea.appendText("\n");
                    } else {
                        // Enter: invia messaggio
                        event.consume();
                        handleSendMessage();
                    }
                    break;
                default:
                    break;
            }
        });
    }

    /**
     * Gestisce l'invio del messaggio
     */
    private void handleSendMessage() {
        String testo = rispostaArea.getText().trim();
        if (!testo.isEmpty()) {
            sendMessage(testo);
            rispostaArea.clear();
            loadMessages(); // Ricarica i messaggi
        }
    }

    /**
     * Carica i messaggi della conversazione
     */
    private void loadMessages() {
        messaggiList.getItems().clear();
        
        List<Messaggio> messaggi = getFilteredMessages();
        
        for (Messaggio msg : messaggi) {
            String formattedMessage = formatMessage(msg);
            messaggiList.getItems().add(formattedMessage);
        }
        
        scrollToLatestMessage();
    }

    /**
     * Ottiene e filtra i messaggi in base al contesto
     */
    private List<Messaggio> getFilteredMessages() {
        if (annuncio != null) {
            // Usa il metodo specifico per gli annunci
            return messaggioDAO.getConversazionePerAnnuncio(currentUserId, interlocutoreId, annuncio.getId());
        } else {
            // Conversazione diretta
            return messaggioDAO.getConversazione(currentUserId, interlocutoreId);
        }
    }

    /**
     * Filtra i messaggi per annuncio specifico (metodo alternativo)
     */
    private List<Messaggio> filterMessagesByAnnuncio(List<Messaggio> messaggi) {
        List<Messaggio> filtered = new ArrayList<>();
        for (Messaggio msg : messaggi) {
            if (msg.getAnnuncioId() == null) {
                // Includi messaggi senza annuncio (potrebbero essere vecchi)
                filtered.add(msg);
            } else if (msg.getAnnuncioId().equals(annuncio.getId())) {
                // Includi messaggi con l'annuncio corretto
                filtered.add(msg);
            }
        }
        return filtered;
    }

    /**
     * Formatta un singolo messaggio per la visualizzazione
     */
    private String formatMessage(Messaggio msg) {
        String prefisso = getMessagePrefix(msg);
        String timestamp = formatTimestamp(msg);
        return "[" + timestamp + "] " + prefisso + msg.getTesto();
    }

    /**
     * Determina il prefisso del messaggio in base al mittente
     */
    private String getMessagePrefix(Messaggio msg) {
        return (msg.getMittenteId() == currentUserId) ? "Tu: " : interlocutoreNome + ": ";
    }

    /**
     * Formatta il timestamp del messaggio
     */
    private String formatTimestamp(Messaggio msg) {
        return msg.getDataInvio().format(DateTimeFormatter.ofPattern(TIME_FORMAT));
    }

    /**
     * Scorri automaticamente all'ultimo messaggio
     */
    private void scrollToLatestMessage() {
        if (!messaggiList.getItems().isEmpty()) {
            messaggiList.scrollTo(messaggiList.getItems().size() - 1);
        }
    }

    /**
     * Invia un nuovo messaggio
     */
    private void sendMessage(String testo) {
        Integer annuncioId = (annuncio != null) ? annuncio.getId() : null;
        
        Messaggio nuovoMsg = createMessage(testo, annuncioId);
        boolean successo = messaggioDAO.inviaMessaggio(nuovoMsg);
        
        if (!successo) {
            showErrorMessage();
        }
    }

    /**
     * Crea un nuovo oggetto Messaggio
     */
    private Messaggio createMessage(String testo, Integer annuncioId) {
        return new Messaggio(
            0, // ID verrà generato dal database
            currentUserId,
            interlocutoreId,
            testo,
            LocalDateTime.now(),
            annuncioId
        );
    }

    /**
     * Mostra un messaggio di errore in caso di fallimento nell'invio
     */
    private void showErrorMessage() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore");
        alert.setHeaderText(null);
        alert.setContentText("Impossibile inviare il messaggio. Riprova più tardi.");
        alert.showAndWait();
    }
    
    /**
     * Imposta il messaggio iniziale nell'area di risposta
     * @param messaggioIniziale Il messaggio da precompilare
     */
    public void setMessaggioIniziale(String messaggioIniziale) {
        this.messaggioIniziale = messaggioIniziale;
        if (rispostaArea != null) {
            rispostaArea.setText(messaggioIniziale);
        }
    }
}