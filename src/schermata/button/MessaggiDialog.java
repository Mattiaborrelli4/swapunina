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
import java.util.List;
import java.util.stream.Collectors;

public class MessaggiDialog extends Dialog<Void> {
    private final Annuncio annuncio;
    private final int currentUserId;
    private final int interlocutoreId;
    private final String interlocutoreNome;
    private final MessaggioDAO messaggioDAO;
    private ListView<String> messaggiList;
    private TextArea rispostaArea;
    private String messaggioIniziale;

    public MessaggiDialog(Annuncio annuncio, int currentUserId) {
        this(annuncio, currentUserId, null);
    }
    
    public MessaggiDialog(Annuncio annuncio, int currentUserId, String messaggioIniziale) {
        this.annuncio = annuncio;
        this.currentUserId = currentUserId;
        this.interlocutoreId = annuncio.getVenditoreId();
        this.interlocutoreNome = annuncio.getNomeUtenteVenditore();
        this.messaggioDAO = new MessaggioDAO();
        this.messaggioIniziale = messaggioIniziale;
        
        initializeUI();
    }
    
    public MessaggiDialog(int currentUserId, int interlocutoreId, String interlocutoreNome) {
        this(currentUserId, interlocutoreId, interlocutoreNome, null);
    }
    
    public MessaggiDialog(int currentUserId, int interlocutoreId, String interlocutoreNome, String messaggioIniziale) {
        this.annuncio = null;
        this.currentUserId = currentUserId;
        this.interlocutoreId = interlocutoreId;
        this.interlocutoreNome = interlocutoreNome;
        this.messaggioDAO = new MessaggioDAO();
        this.messaggioIniziale = messaggioIniziale;
        
        initializeUI();
    }

    private void initializeUI() {
        if (annuncio != null) {
            setTitle("Messaggi con " + interlocutoreNome + " - " + annuncio.getTitolo());
        } else {
            setTitle("Messaggi con " + interlocutoreNome);
        }
        
        // Lista messaggi
        messaggiList = new ListView<>();
        messaggiList.setPrefHeight(300);
        caricaMessaggi();

        // Area risposta
        rispostaArea = new TextArea();
        rispostaArea.setPromptText("Scrivi la tua risposta...");
        rispostaArea.setPrefRowCount(3);
        
        // Imposta il messaggio iniziale se fornito
        if (messaggioIniziale != null) {
            rispostaArea.setText(messaggioIniziale);
        }

        Button inviaBtn = new Button("Invia");
        HBox rispostaBox = new HBox(10, rispostaArea, inviaBtn);
        rispostaBox.setPadding(new Insets(10, 0, 0, 0));
        HBox.setHgrow(rispostaArea, Priority.ALWAYS);

        VBox content = new VBox(10, messaggiList, rispostaBox);
        content.setPadding(new Insets(15));

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        
        // Imposta dimensioni minime sul DialogPane
        getDialogPane().setMinWidth(500);
        getDialogPane().setMinHeight(400);

        // Gestione eventi
        inviaBtn.setOnAction(e -> {
            if (!rispostaArea.getText().isEmpty()) {
                inviaMessaggio(rispostaArea.getText());
                rispostaArea.clear();
                caricaMessaggi(); // Ricarica i messaggi
            }
        });
    }

    private void caricaMessaggi() {
        messaggiList.getItems().clear();
        
        // Recupera i messaggi dal database
        List<Messaggio> messaggi = messaggioDAO.getConversazione(currentUserId, interlocutoreId);
        
        // Filtra i messaggi per annuncio se disponibile
        if (annuncio != null) {
            messaggi = messaggi.stream()
                .filter(msg -> msg.getAnnuncioId() != null && msg.getAnnuncioId().equals(annuncio.getId()))
                .collect(Collectors.toList());
        }
        
        for (Messaggio msg : messaggi) {
            String prefisso = (msg.getMittenteId() == currentUserId) ? "Tu: " : interlocutoreNome + ": ";
            String timestamp = msg.getDataInvio().format(DateTimeFormatter.ofPattern("HH:mm"));
            messaggiList.getItems().add("[" + timestamp + "] " + prefisso + msg.getTesto());
        }
        
        // Scroll automatico all'ultimo messaggio
        if (!messaggiList.getItems().isEmpty()) {
            messaggiList.scrollTo(messaggiList.getItems().size() - 1);
        }
    }

    private void inviaMessaggio(String testo) {
        Integer annuncioId = (annuncio != null) ? annuncio.getId() : null;
        
        Messaggio nuovoMsg = new Messaggio(
            0, // ID verrà generato dal database
            currentUserId,
            interlocutoreId,
            testo,
            LocalDateTime.now(),
            annuncioId
        );
        
        boolean successo = messaggioDAO.inviaMessaggio(nuovoMsg);
        
        if (!successo) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Errore");
            alert.setHeaderText(null);
            alert.setContentText("Impossibile inviare il messaggio. Riprova più tardi.");
            alert.showAndWait();
        } else {
            // Ricarica i messaggi dopo l'invio
            caricaMessaggi();
        }
    }
    
    // Metodo per impostare il messaggio iniziale dopo la creazione
    public void setMessaggioIniziale(String messaggioIniziale) {
        this.messaggioIniziale = messaggioIniziale;
        if (rispostaArea != null) {
            rispostaArea.setText(messaggioIniziale);
        }
    }
}