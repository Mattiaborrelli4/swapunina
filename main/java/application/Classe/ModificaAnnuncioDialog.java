package application.Classe;

import application.Enum.Tipologia;
import application.DB.AnnuncioDAO;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

/**
 * Dialog per la modifica di un annuncio esistente
 * Fornisce interfaccia per modificare titolo, descrizione, prezzo, tipologia e consegna
 */
public class ModificaAnnuncioDialog extends Dialog<Annuncio> {
    
    private Button eliminaButton;
    private Button concludeButton;
    private final Annuncio annuncioOriginale;
    
    // Componenti UI
    private TextField titoloField;
    private TextArea descrizioneArea;
    private TextField prezzoField;
    private ComboBox<Tipologia> tipologiaCombo;
    private ComboBox<String> consegnaCombo;
    
    // Costanti per stili e messaggi
    private static final String STILE_BOTTONE_ELIMINA = "-fx-background-color: #f44336; -fx-text-fill: white;";
    private static final String STILE_BOTTONE_CONCLUDI = "-fx-background-color: #4CAF50; -fx-text-fill: white;";
    private static final int PADDING_GRID = 20;
    private static final int SPACING_ORIZZONTALE = 10;
    private static final int SPACING_VERTICALE = 10;

    public ModificaAnnuncioDialog(Annuncio annuncio) {
        this.annuncioOriginale = annuncio;
        
        // Inizializza i pulsanti prima di qualsiasi altro metodo
        this.eliminaButton = new Button("Elimina Annuncio");
        this.concludeButton = new Button("Segna come Concluso");
        
        inizializzaUI();
        setupButtonActions();
    }

    /**
     * Inizializza l'interfaccia utente del dialog
     */
    private void inizializzaUI() {
        setTitle("Modifica Annuncio");
        setHeaderText("Modifica i dettagli del tuo annuncio");
        
        // Pulsanti standard del dialog
        ButtonType salvaButtonType = new ButtonType("Salva Modifiche", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(salvaButtonType, ButtonType.CANCEL);
        
        // Crea e configura i campi del form
        GridPane grid = creaGridPane();
        creaCampiForm();
        
        // Aggiungi campi al grid
        aggiungiCampiAlGrid(grid);
        
        // Configura i pulsanti personalizzati
        configuraPulsantiPersonalizzati();
        HBox buttonBox = creaContainerPulsanti();
        grid.add(buttonBox, 0, 6, 2, 1); // Posiziona in fondo
        
        getDialogPane().setContent(grid);
        
        // Configura il converter per il risultato
        configuraResultConverter();
    }

    /**
     * Crea e configura i campi del form
     */
    private void creaCampiForm() {
        titoloField = new TextField(annuncioOriginale.getTitolo());
        
        // CORREZIONE: Gestione sicura del TextArea per evitare NullPointerException
        String descrizione = annuncioOriginale.getDescrizione();
        descrizioneArea = new TextArea(descrizione != null ? descrizione : "");
        
        prezzoField = new TextField(String.format("%.2f", annuncioOriginale.getPrezzo()));
        tipologiaCombo = creaComboBoxTipologia();
        consegnaCombo = creaComboBoxConsegna();
    }

    /**
     * Crea e configura il GridPane principale
     */
    private GridPane creaGridPane() {
        GridPane grid = new GridPane();
        grid.setHgap(SPACING_ORIZZONTALE);
        grid.setVgap(SPACING_VERTICALE);
        grid.setPadding(new Insets(PADDING_GRID, 150, 10, 10));
        return grid;
    }

    /**
     * Crea e configura la ComboBox per le tipologie
     */
    private ComboBox<Tipologia> creaComboBoxTipologia() {
        ComboBox<Tipologia> tipologiaCombo = new ComboBox<>();
        tipologiaCombo.getItems().addAll(Tipologia.values());
        tipologiaCombo.setValue(annuncioOriginale.getTipologia());
        
        tipologiaCombo.setConverter(new StringConverter<Tipologia>() {
            @Override
            public String toString(Tipologia tipo) {
                return tipo != null ? tipo.getDisplayName() : "";
            }
            
            @Override
            public Tipologia fromString(String string) {
                return Tipologia.fromDisplayName(string);
            }
        });
        
        return tipologiaCombo;
    }

    /**
     * Crea e configura la ComboBox per la modalità di consegna
     */
    private ComboBox<String> creaComboBoxConsegna() {
        ComboBox<String> consegnaCombo = new ComboBox<>();
        consegnaCombo.getItems().addAll(
            "Incontro di persona",
            "Spedizione gratuita", 
            "Spedizione a carico acquirente",
            "Ritiro in sede",
            "Standard",
            "free"
        );
        
        // Imposta il valore corrente o un default
        String modalitaCorrente = annuncioOriginale.getModalitaConsegna();
        if (modalitaCorrente != null && !modalitaCorrente.isEmpty()) {
            consegnaCombo.setValue(modalitaCorrente);
        } else {
            consegnaCombo.setValue("Incontro di persona");
        }
        
        return consegnaCombo;
    }

    /**
     * Aggiunge i campi al GridPane
     */
    private void aggiungiCampiAlGrid(GridPane grid) {
        // Configura dimensioni campi
        descrizioneArea.setPrefRowCount(4);
        descrizioneArea.setWrapText(true);
        
        int row = 0;
        grid.add(new Label("Titolo*:"), 0, row);
        grid.add(titoloField, 1, row++);
        
        grid.add(new Label("Descrizione:"), 0, row);
        grid.add(descrizioneArea, 1, row++);
        
        grid.add(new Label("Prezzo* (€):"), 0, row);
        grid.add(prezzoField, 1, row++);
        
        grid.add(new Label("Tipologia*:"), 0, row);
        grid.add(tipologiaCombo, 1, row++);
        
        grid.add(new Label("Modalità consegna*:"), 0, row);
        grid.add(consegnaCombo, 1, row++);
        
        // Aggiungi note sui campi obbligatori
        Label noteLabel = new Label("* Campi obbligatori");
        noteLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
        grid.add(noteLabel, 0, row, 2, 1);
    }

    /**
     * Configura i pulsanti personalizzati
     */
    private void configuraPulsantiPersonalizzati() {
        eliminaButton.setStyle(STILE_BOTTONE_ELIMINA);
        concludeButton.setStyle(STILE_BOTTONE_CONCLUDI);
    }

    /**
     * Crea il container per i pulsanti personalizzati
     */
    private HBox creaContainerPulsanti() {
        HBox buttonBox = new HBox(10, eliminaButton, concludeButton);
        buttonBox.setPadding(new Insets(15, 0, 0, 0));
        return buttonBox;
    }

    /**
     * Configura il result converter per il dialog
     */
    private void configuraResultConverter() {
        setResultConverter(dialogButton -> {
            if (dialogButton.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                // Validazione campi obbligatori
                if (!validaCampi()) {
                    return null;
                }
                
                // Crea una copia dell'annuncio originale per le modifiche
                Annuncio annuncioModificato = creaAnnuncioModificato();
                
                return annuncioModificato;
            }
            return null;
        });
    }

    /**
     * Valida i campi obbligatori del form
     */
    private boolean validaCampi() {
        // Valida titolo
        if (titoloField.getText() == null || titoloField.getText().trim().isEmpty()) {
            mostraErrore("Errore di validazione", "Il titolo è obbligatorio");
            return false;
        }
        
        // Valida prezzo - CORREZIONE: Gestione più robusta
        String prezzoText = prezzoField.getText().trim();
        if (prezzoText.isEmpty()) {
            mostraErrore("Errore di validazione", "Il prezzo è obbligatorio");
            return false;
        }
        
        try {
            // Rimuovi eventuali caratteri non numerici tranne punto e virgola
            prezzoText = prezzoText.replace(",", ".");
            
            // Rimuovi spazi e caratteri non numerici
            prezzoText = prezzoText.replaceAll("[^\\d.]", "");
            
            double prezzo = Double.parseDouble(prezzoText);
            if (prezzo < 0) {
                mostraErrore("Errore di validazione", "Il prezzo non può essere negativo");
                return false;
            }
            
            // Aggiorna il campo con il valore normalizzato
            prezzoField.setText(String.format("%.2f", prezzo));
            
        } catch (NumberFormatException e) {
            mostraErrore("Errore di validazione", "Inserisci un prezzo valido (es. 25.50 o 25,50)");
            return false;
        }
        
        // Valida tipologia
        if (tipologiaCombo.getValue() == null) {
            mostraErrore("Errore di validazione", "Seleziona una tipologia");
            return false;
        }
        
        // Valida modalità consegna
        if (consegnaCombo.getValue() == null) {
            mostraErrore("Errore di validazione", "Seleziona una modalità di consegna");
            return false;
        }
        
        return true;
    }

    /**
     * Crea un annuncio modificato con i nuovi valori
     */
    private Annuncio creaAnnuncioModificato() {
        Annuncio annuncioModificato = new Annuncio();
        annuncioModificato.setId(annuncioOriginale.getId());
        
        // CORREZIONE: Gestione sicura dei campi per evitare NullPointerException
        annuncioModificato.setTitolo(titoloField.getText() != null ? titoloField.getText().trim() : "");
        
        String descrizione = descrizioneArea.getText();
        annuncioModificato.setDescrizione(descrizione != null ? descrizione.trim() : "");
        
        // CORREZIONE: Parsing robusto del prezzo
        String prezzoText = prezzoField.getText().trim().replace(",", ".");
        prezzoText = prezzoText.replaceAll("[^\\d.]", "");
        annuncioModificato.setPrezzo(Double.parseDouble(prezzoText));
        
        annuncioModificato.setTipologia(tipologiaCombo.getValue());
        annuncioModificato.setModalitaConsegna(consegnaCombo.getValue());
        annuncioModificato.setVenditoreId(annuncioOriginale.getVenditoreId());
        
        // Copia l'oggetto originale
        if (annuncioOriginale.getOggetto() != null) {
            annuncioModificato.setOggetto(annuncioOriginale.getOggetto().clone());
            annuncioModificato.getOggetto().setNome(titoloField.getText() != null ? titoloField.getText().trim() : "");
            
            String descrizioneOggetto = descrizioneArea.getText();
            annuncioModificato.getOggetto().setDescrizione(descrizioneOggetto != null ? descrizioneOggetto.trim() : "");
        }
        
        return annuncioModificato;
    }


    private void setupButtonActions() {
        configuraAzioneElimina();
        configuraAzioneConcludi();
    }

    private void configuraAzioneElimina() {
        eliminaButton.setOnAction(e -> {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Conferma eliminazione");
            confirmAlert.setHeaderText("Sei sicuro di voler eliminare questo annuncio?");
            confirmAlert.setContentText("Questa operazione non può essere annullata.");
            
            confirmAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    eseguiEliminazioneAnnuncio();
                }
            });
        });
    }

    private void configuraAzioneConcludi() {
        concludeButton.setOnAction(e -> {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Conferma conclusione");
            confirmAlert.setHeaderText("Segnare questo annuncio come concluso?");
            confirmAlert.setContentText("L'annuncio non sarà più visibile agli altri utenti.");
            
            confirmAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    eseguiConclusioneAnnuncio();
                }
            });
        });
    }

    private void eseguiEliminazioneAnnuncio() {
        try {
            AnnuncioDAO annuncioDAO = new AnnuncioDAO();
            boolean successo = annuncioDAO.eliminaAnnuncio(annuncioOriginale.getId());
            
            if (successo) {
                mostraSuccesso("Annuncio eliminato con successo");
                close();
            } else {
                mostraErrore("Errore", "Impossibile eliminare l'annuncio");
            }
        } catch (Exception ex) {
            mostraErrore("Errore", "Errore durante l'eliminazione: " + ex.getMessage());
        }
    }

    private void eseguiConclusioneAnnuncio() {
        try {
            AnnuncioDAO annuncioDAO = new AnnuncioDAO();
            boolean successo = annuncioDAO.aggiornaStatoAnnuncio(annuncioOriginale.getId(), "VENDUTO");
            
            if (successo) {
                mostraSuccesso("Annuncio segnato come concluso");
                close();
            } else {
                mostraErrore("Errore", "Impossibile aggiornare lo stato dell'annuncio");
            }
        } catch (Exception ex) {
            mostraErrore("Errore", "Errore durante l'aggiornamento: " + ex.getMessage());
        }
    }

    private void mostraErrore(String titolo, String messaggio) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titolo);
        alert.setHeaderText(null);
        alert.setContentText(messaggio);
        alert.showAndWait();
    }

    private void mostraSuccesso(String messaggio) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Operazione completata");
        alert.setHeaderText(null);
        alert.setContentText(messaggio);
        alert.showAndWait();
    }

    public Button getEliminaButton() {
        return eliminaButton;
    }
    
    public Button getConcludeButton() {
        return concludeButton;
    }
}