package application.Classe;

import application.Classe.Annuncio;
import application.Enum.Tipologia;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.StringConverter;

public class ModificaAnnuncioDialog extends Dialog<Annuncio> {
    
    private Button eliminaButton;
    private Button concludeButton;
    private Annuncio annuncioOriginale;
    
    public ModificaAnnuncioDialog(Annuncio annuncio) {
        this.annuncioOriginale = annuncio;
        
        setTitle("Modifica Annuncio");
        setHeaderText("Modifica i dettagli del tuo annuncio");
        
        // Pulsanti
        ButtonType salvaButtonType = new ButtonType("Salva", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(salvaButtonType, ButtonType.CANCEL);
        
        // Aggiungi pulsanti personalizzati
        eliminaButton = new Button("Elimina Annuncio");
        eliminaButton.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        
        concludeButton = new Button("Segna come Concluso");
        concludeButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        
        // Layout
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Campi del form
        TextField titoloField = new TextField(annuncio.getOggetto().getNome());
        TextArea descrizioneArea = new TextArea(annuncio.getOggetto().getDescrizione());
        TextField prezzoField = new TextField(String.valueOf(annuncio.getPrezzo()));
        ComboBox<Tipologia> tipologiaCombo = new ComboBox<>();
        tipologiaCombo.getItems().addAll(Tipologia.values());
        tipologiaCombo.setValue(annuncio.getTipologia());
        TextField consegnaField = new TextField(annuncio.getModalitaConsegna());
        
        // Configura combobox
        tipologiaCombo.setConverter(new StringConverter<Tipologia>() {
            @Override
            public String toString(Tipologia tipo) {
                return tipo.getDisplayName();
            }
            
            @Override
            public Tipologia fromString(String string) {
                return Tipologia.fromDisplayName(string);
            }
        });
        
        // Aggiungi campi al grid
        grid.add(new Label("Titolo:"), 0, 0);
        grid.add(titoloField, 1, 0);
        grid.add(new Label("Descrizione:"), 0, 1);
        grid.add(descrizioneArea, 1, 1);
        grid.add(new Label("Prezzo:"), 0, 2);
        grid.add(prezzoField, 1, 2);
        grid.add(new Label("Tipologia:"), 0, 3);
        grid.add(tipologiaCombo, 1, 3);
        grid.add(new Label("Modalità consegna:"), 0, 4);
        grid.add(consegnaField, 1, 4);
        
        // Aggiungi pulsanti personalizzati in fondo
        HBox buttonBox = new HBox(10, eliminaButton, concludeButton);
        buttonBox.setPadding(new Insets(15, 0, 0, 0));
        grid.add(buttonBox, 0, 5, 2, 1);
        
        getDialogPane().setContent(grid);
        
        // Converti il risultato
        setResultConverter(dialogButton -> {
            if (dialogButton == salvaButtonType) {
                // Aggiorna l'oggetto
                annuncio.getOggetto().setNome(titoloField.getText());
                annuncio.getOggetto().setDescrizione(descrizioneArea.getText());
                
                // Aggiorna l'annuncio
                try {
                    annuncio.setPrezzo(Double.parseDouble(prezzoField.getText()));
                } catch (NumberFormatException e) {
                    // Gestisci errore di formato
                    new Alert(Alert.AlertType.ERROR, "Inserisci un prezzo valido").show();
                    return null;
                }
                
                annuncio.setTipologia(tipologiaCombo.getValue());
                annuncio.setModalitaConsegna(consegnaField.getText());
                
                return annuncio;
            }
            return null;
        });
        
        // Configura azioni pulsanti
        setupButtonActions(annuncio);
    }
    
    private void setupButtonActions(Annuncio annuncio) {
        // Azione per eliminare l'annuncio
        eliminaButton.setOnAction(e -> {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Conferma eliminazione");
            confirmAlert.setHeaderText("Sei sicuro di voler eliminare questo annuncio?");
            confirmAlert.setContentText("Questa operazione non può essere annullata.");
            
            confirmAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        application.DB.AnnuncioDAO annuncioDAO = new application.DB.AnnuncioDAO();
                        boolean successo = annuncioDAO.eliminaAnnuncio(annuncio.getId());
                        
                        if (successo) {
                            new Alert(Alert.AlertType.INFORMATION, "Annuncio eliminato con successo").show();
                            close(); // Chiudi la finestra di dialogo
                        } else {
                            new Alert(Alert.AlertType.ERROR, "Errore durante l'eliminazione dell'annuncio").show();
                        }
                    } catch (Exception ex) {
                        new Alert(Alert.AlertType.ERROR, "Errore: " + ex.getMessage()).show();
                    }
                }
            });
        });
        
        // Azione per segnare come concluso
        concludeButton.setOnAction(e -> {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.setTitle("Conferma conclusione");
            confirmAlert.setHeaderText("Segnare questo annuncio come concluso?");
            confirmAlert.setContentText("L'annuncio non sarà più visibile agli altri utenti.");
            
            confirmAlert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        application.DB.AnnuncioDAO annuncioDAO = new application.DB.AnnuncioDAO();
                        boolean successo = annuncioDAO.aggiornaStatoAnnuncio(annuncio.getId(), "VENDUTO");
                        
                        if (successo) {
                            new Alert(Alert.AlertType.INFORMATION, "Annuncio segnato come concluso").show();
                            close(); // Chiudi la finestra di dialogo
                        } else {
                            new Alert(Alert.AlertType.ERROR, "Errore durante l'aggiornamento dell'annuncio").show();
                        }
                    } catch (Exception ex) {
                        new Alert(Alert.AlertType.ERROR, "Errore: " + ex.getMessage()).show();
                    }
                }
            });
        });
    }
    
    // Metodi per ottenere i pulsanti (opzionale, se necessario dall'esterno)
    public Button getEliminaButton() {
        return eliminaButton;
    }
    
    public Button getConcludeButton() {
        return concludeButton;
    }
}