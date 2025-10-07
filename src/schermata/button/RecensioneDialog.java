package schermata.button;

import application.Classe.Annuncio;
import application.Classe.Recensioni;
import application.Classe.utente;
import application.DB.RecensioneDAO;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.time.LocalDateTime;
import java.util.List;

public class RecensioneDialog extends Dialog<Boolean> {
    private final List<Recensioni> recensioni;
    private final double punteggioMedio;
    private final Annuncio annuncio;
    private final utente venditore;
    private final int currentUserId;
    
    private int stelleSelezionate = 0;
    private TextArea commentoArea;

    // MODIFICA: Nuovo costruttore per visualizzare recensioni con annuncio specifico
    public RecensioneDialog(Annuncio annuncio, List<Recensioni> recensioni, double punteggioMedio) {
        this.annuncio = annuncio;
        this.recensioni = recensioni;
        this.punteggioMedio = punteggioMedio;
        this.venditore = null;
        this.currentUserId = -1;
        
        // MODIFICA: Usa il titolo dell'annuncio dal database
        String titoloAnnuncio = annuncio.getTitolo() != null ? annuncio.getTitolo() : "Annuncio #" + annuncio.getId();
        setTitle("Recensioni per: " + titoloAnnuncio);
        setHeaderText("Punteggio medio: " + String.format("%.1f", punteggioMedio) + " ★");
        
        initializeUI();
    }

    // Costruttore legacy per compatibilità (senza annuncio)
    public RecensioneDialog(List<Recensioni> recensioni, double punteggioMedio) {
        this.recensioni = recensioni;
        this.punteggioMedio = punteggioMedio;
        this.annuncio = null;
        this.venditore = null;
        this.currentUserId = -1;
        
        // MODIFICA: Titolo più specifico
        if (recensioni != null && !recensioni.isEmpty()) {
            String titoloAnnuncio = "Annuncio";
            if (recensioni.get(0).getAnnuncio() != null && recensioni.get(0).getAnnuncio().getTitolo() != null) {
                titoloAnnuncio = recensioni.get(0).getAnnuncio().getTitolo();
            }
            setTitle("Recensioni dell'annuncio: " + titoloAnnuncio);
        } else {
            setTitle("Recensioni dell'annuncio");
        }
        
        setHeaderText("Punteggio medio: " + String.format("%.1f", punteggioMedio) + " ★");
        
        initializeUI();
    }

    // Costruttore per lasciare una nuova recensione
    public RecensioneDialog(Annuncio annuncio, utente venditore, int currentUserId) {
        this.recensioni = null;
        this.punteggioMedio = 0.0;
        this.annuncio = annuncio;
        this.venditore = venditore;
        this.currentUserId = currentUserId;
        
        setTitle("Lascia una Recensione");
        setHeaderText("Recensisci il venditore per: " + (annuncio.getTitolo() != null ? annuncio.getTitolo() : "Annuncio"));
        
        initializeUIForNewReview();
    }

    private void initializeUI() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));

        // MODIFICA: Header migliorato con titolo annuncio
        if (annuncio != null && annuncio.getTitolo() != null) {
            Label titoloLabel = new Label("Annuncio: " + annuncio.getTitolo());
            titoloLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            content.getChildren().add(titoloLabel);
        }
        
        Label punteggioLabel = new Label("Punteggio medio: " + String.format("%.1f", punteggioMedio) + " ★");
        punteggioLabel.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        content.getChildren().add(punteggioLabel);

        // Lista recensioni
        if (recensioni == null || recensioni.isEmpty()) {
            Label nessunaRecensioneLabel = new Label("Nessuna recensione disponibile per questo annuncio.");
            nessunaRecensioneLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #666;");
            content.getChildren().add(nessunaRecensioneLabel);
        } else {
            ListView<String> listaRecensioni = new ListView<>();
            
            for (Recensioni recensione : recensioni) {
                // MODIFICA: Gestione migliorata del titolo annuncio
                String titoloAnnuncio = "Annuncio";
                if (recensione.getAnnuncio() != null && recensione.getAnnuncio().getTitolo() != null) {
                    titoloAnnuncio = recensione.getAnnuncio().getTitolo();
                } else if (annuncio != null && annuncio.getTitolo() != null) {
                    titoloAnnuncio = annuncio.getTitolo(); // Fallback all'annuncio principale
                } else {
                    titoloAnnuncio = "Annuncio #" + (recensione.getAnnuncio() != null ? recensione.getAnnuncio().getId() : "N/D");
                }
                
                // Gestione sicura dell'acquirente
                String nomeAcquirente = "Utente anonimo";
                if (recensione.getAcquirente() != null) {
                    if (recensione.getAcquirente().getNome() != null && recensione.getAcquirente().getCognome() != null) {
                        nomeAcquirente = recensione.getAcquirente().getNome() + " " + recensione.getAcquirente().getCognome();
                    } else if (recensione.getAcquirente().getNome() != null) {
                        nomeAcquirente = recensione.getAcquirente().getNome();
                    }
                }
                
                // CORREZIONE: Usa getPunteggio() invece di getPunteggioStelle()
                int punteggio = recensione.getPunteggio();
                
                // Crea le stelle visualizzate
                StringBuilder stelleBuilder = new StringBuilder();
                for (int i = 0; i < 5; i++) {
                    if (i < punteggio) {
                        stelleBuilder.append("★");
                    } else {
                        stelleBuilder.append("☆");
                    }
                }
                String stelle = stelleBuilder.toString();
                
                // MODIFICA: Formattazione migliorata
                String recensioneText = String.format(
                    "👤 %s\n📦 %s\n⭐ %s (%d/5)\n💬 %s\n📅 %s",
                    nomeAcquirente,
                    titoloAnnuncio,
                    stelle,
                    punteggio,
                    recensione.getCommento() != null && !recensione.getCommento().isEmpty() ? 
                        recensione.getCommento() : "Nessun commento",
                    recensione.getDataRecensione().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                );
                listaRecensioni.getItems().add(recensioneText);
            }
            
            listaRecensioni.setPrefHeight(300);
            listaRecensioni.setPrefWidth(450);
            
            Label titoloLista = new Label("Recensioni (" + recensioni.size() + "):");
            titoloLista.setFont(Font.font("Arial", FontWeight.BOLD, 14));
            
            content.getChildren().addAll(titoloLista, listaRecensioni);
        }

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        getDialogPane().setPrefSize(500, 450);
        
        setResultConverter(buttonType -> false);
    }

    private void initializeUIForNewReview() {
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER_LEFT);

        // Informazioni annuncio
        Label infoLabel = new Label("Stai recensendo: " + (annuncio.getTitolo() != null ? annuncio.getTitolo() : "Annuncio"));
        infoLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        
        Label venditoreLabel = new Label("Venditore: " + 
            (venditore.getNome() != null ? venditore.getNome() : "") + " " + 
            (venditore.getCognome() != null ? venditore.getCognome() : ""));
        venditoreLabel.setFont(Font.font("Arial", 12));

        // Selezione stelle
        Label stelleLabel = new Label("Seleziona il punteggio:*");
        stelleLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        HBox stelleContainer = createStelleSelector();
        
        // Commento
        Label commentoLabel = new Label("Commento (opzionale):");
        commentoLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        
        commentoArea = new TextArea();
        commentoArea.setPromptText("Scrivi qui il tuo commento... (massimo 500 caratteri)");
        commentoArea.setPrefRowCount(4);
        commentoArea.setWrapText(true);
        
        // Limita la lunghezza del commento
        commentoArea.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.length() > 500) {
                commentoArea.setText(oldValue);
            }
        });

        // Label informativo
        Label infoNote = new Label("* Campo obbligatorio");
        infoNote.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");

        content.getChildren().addAll(
            infoLabel, venditoreLabel, 
            new Separator(),
            stelleLabel, stelleContainer, infoNote,
            new Separator(),
            commentoLabel, commentoArea
        );

        getDialogPane().setContent(content);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        getDialogPane().setPrefSize(500, 400);
        
        // Disabilita il pulsante OK finché non sono selezionate almeno 1 stella
        Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);
        okButton.setText("Invia Recensione");
        
        // MODIFICA: Rinomina il pulsante Cancel
        Button cancelButton = (Button) getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setText("Annulla");
        
        // Gestione del risultato
        setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                return salvaRecensione();
            }
            return false;
        });
    }

    private HBox createStelleSelector() {
        HBox stelleBox = new HBox(5);
        stelleBox.setAlignment(Pos.CENTER_LEFT);
        
        for (int i = 1; i <= 5; i++) {
            Label stella = new Label("☆");
            stella.setFont(Font.font(28));
            stella.setUserData(i);
            stella.setStyle("-fx-cursor: hand; -fx-text-fill: #ffd700; -fx-padding: 5px;");
            
            stella.setOnMouseEntered(e -> highlightStelle(stelleBox, (int) stella.getUserData(), false));
            stella.setOnMouseExited(e -> highlightStelle(stelleBox, stelleSelezionate, true));
            stella.setOnMouseClicked(e -> {
                stelleSelezionate = (int) stella.getUserData();
                highlightStelle(stelleBox, stelleSelezionate, true);
                
                // Abilita il pulsante OK quando sono selezionate le stelle
                Button okButton = (Button) getDialogPane().lookupButton(ButtonType.OK);
                okButton.setDisable(stelleSelezionate == 0);
            });
            
            stelleBox.getChildren().add(stella);
        }
        
        // Label per visualizzare il punteggio
        Label punteggioLabel = new Label("0/5");
        punteggioLabel.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        punteggioLabel.setPadding(new Insets(0, 0, 0, 15));
        punteggioLabel.setStyle("-fx-text-fill: #333;");
        stelleBox.getChildren().add(punteggioLabel);
        
        return stelleBox;
    }

    private void highlightStelle(HBox stelleBox, int finoA, boolean permanente) {
        for (int i = 0; i < 5; i++) {
            Label stella = (Label) stelleBox.getChildren().get(i);
            if (i < finoA) {
                stella.setText("★");
                if (permanente) {
                    stella.setStyle("-fx-cursor: hand; -fx-text-fill: #ffa500; -fx-font-weight: bold; -fx-padding: 5px;");
                } else {
                    stella.setStyle("-fx-cursor: hand; -fx-text-fill: #ffd700; -fx-padding: 5px;");
                }
            } else {
                stella.setText("☆");
                stella.setStyle("-fx-cursor: hand; -fx-text-fill: #ffd700; -fx-padding: 5px;");
            }
        }
        
        // Aggiorna il label del punteggio
        if (stelleBox.getChildren().size() > 5) {
            Label punteggioLabel = (Label) stelleBox.getChildren().get(5);
            punteggioLabel.setText(finoA + "/5");
            punteggioLabel.setStyle("-fx-text-fill: " + (finoA > 0 ? "#2E8B57" : "#333") + ";");
        }
    }

    private boolean salvaRecensione() {
        try {
            // Validazione
            if (stelleSelezionate == 0) {
                mostraErrore("Seleziona un punteggio prima di inviare la recensione.");
                return false;
            }
            
            RecensioneDAO recensioneDAO = new RecensioneDAO();
            
            if (recensioneDAO.haGiaRecensito(currentUserId, annuncio.getId())) {
                mostraErrore("Hai già recensito questo annuncio!");
                return false;
            }
            
            utente acquirente = recuperaAcquirente(currentUserId);
            
            if (acquirente == null) {
                mostraErrore("Impossibile recuperare i dati dell'acquirente");
                return false;
            }
            
            // Prepara il commento
            String commento = commentoArea.getText().trim();
            if (commento.length() > 500) {
                commento = commento.substring(0, 500);
            }
            
            // Crea la nuova recensione
            Recensioni nuovaRecensione = new Recensioni(
                acquirente,
                venditore,
                annuncio,
                commento,
                stelleSelezionate
            );
            
            boolean successo = recensioneDAO.inserisciRecensione(nuovaRecensione);
            
            if (successo) {
                mostraSuccesso("Recensione inviata con successo! Grazie per il tuo feedback.");
                return true;
            } else {
                mostraErrore("Errore durante il salvataggio della recensione. Riprova più tardi.");
                return false;
            }
            
        } catch (Exception e) {
            mostraErrore("Errore imprevisto: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private utente recuperaAcquirente(int userId) {
        try {
            // MODIFICA: Prova a recuperare l'utente dal database
            application.DB.UserDAO userDAO = new application.DB.UserDAO();
            utente acquirente = userDAO.getUserById(userId);
            if (acquirente != null) {
                return acquirente;
            }
        } catch (Exception e) {
            System.err.println("Errore nel recupero acquirente: " + e.getMessage());
        }
        
        // Fallback: crea un utente base con l'ID
        utente acquirente = new utente();
        acquirente.setId(userId);
        acquirente.setNome("Utente");
        acquirente.setCognome("#" + userId);
        return acquirente;
    }

    private void mostraErrore(String messaggio) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore");
        alert.setHeaderText(null);
        alert.setContentText(messaggio);
        alert.showAndWait();
    }

    private void mostraSuccesso(String messaggio) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Successo");
        alert.setHeaderText(null);
        alert.setContentText(messaggio);
        alert.showAndWait();
    }
}