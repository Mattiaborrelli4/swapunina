package application.Classe;


import application.servic;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

/**
 * Dialog per il caricamento di immagini su Cloudinary per gli annunci
 */
public class CaricaImmagineDialog extends Dialog<String> {
    
    private File fileSelezionato;
    private ImageView anteprimaImageView;
    private Label nomeFileLabel;
    private final servic cloudinaryService;
    private final int annuncioId;

    public CaricaImmagineDialog(int annuncioId) {
        this.annuncioId = annuncioId;
        this.cloudinaryService = new servic();
        
        setTitle("Carica Immagine");
        setHeaderText("Seleziona un'immagine per il tuo annuncio");
        
        inizializzaUI();
        setupPulsanti();
    }

    private void inizializzaUI() {
        // Container principale
        VBox content = new VBox(15);
        content.setPadding(new javafx.geometry.Insets(20));
        
        // Pulsante selezione file
        Button selezioneButton = new Button("Seleziona Immagine");
        selezioneButton.setOnAction(e -> selezionaFile());
        
        // Anteprima immagine
        anteprimaImageView = new ImageView();
        anteprimaImageView.setFitWidth(300);
        anteprimaImageView.setFitHeight(200);
        anteprimaImageView.setPreserveRatio(true);
        anteprimaImageView.setStyle("-fx-border-color: #ccc; -fx-border-width: 1px;");
        
        // Label nome file
        nomeFileLabel = new Label("Nessun file selezionato");
        nomeFileLabel.setStyle("-fx-text-fill: #666;");
        
        content.getChildren().addAll(
            new Label("Formati supportati: JPG, PNG, GIF"),
            selezioneButton,
            nomeFileLabel,
            anteprimaImageView
        );
        
        getDialogPane().setContent(content);
    }

    private void setupPulsanti() {
        ButtonType caricaButtonType = new ButtonType("Carica", ButtonBar.ButtonData.OK_DONE);
        ButtonType annullaButtonType = new ButtonType("Annulla", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        getDialogPane().getButtonTypes().addAll(caricaButtonType, annullaButtonType);
        
        // Disabilita il pulsante Carica inizialmente
        Button caricaButton = (Button) getDialogPane().lookupButton(caricaButtonType);
        caricaButton.setDisable(true);
        
        setResultConverter(dialogButton -> {
            if (dialogButton == caricaButtonType) {
                return caricaImmagine();
            }
            return null;
        });
    }

    private void selezionaFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleziona Immagine");
        
        // Filtri per i file
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
            "File immagine", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.bmp"
        );
        fileChooser.getExtensionFilters().add(extFilter);
        
        Stage stage = (Stage) getDialogPane().getScene().getWindow();
        fileSelezionato = fileChooser.showOpenDialog(stage);
        
        if (fileSelezionato != null) {
            mostraAnteprima();
            abilitaPulsanteCarica();
        }
    }

    private void mostraAnteprima() {
        try {
            Image image = new Image(fileSelezionato.toURI().toString());
            anteprimaImageView.setImage(image);
            nomeFileLabel.setText(fileSelezionato.getName());
        } catch (Exception e) {
            mostraErrore("Errore", "Impossibile caricare l'anteprima dell'immagine");
        }
    }

    private void abilitaPulsanteCarica() {
        Button caricaButton = (Button) getDialogPane().lookupButton(
            getDialogPane().getButtonTypes().stream()
                .filter(bt -> bt.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                .findFirst().orElse(null)
        );
        if (caricaButton != null) {
            caricaButton.setDisable(false);
        }
    }

    private String caricaImmagine() {
        if (fileSelezionato == null) {
            mostraErrore("Errore", "Nessun file selezionato");
            return null;
        }

        try {
            // Mostra progresso
            Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
            progressAlert.setTitle("Caricamento");
            progressAlert.setHeaderText("Caricamento immagine in corso...");
            progressAlert.setContentText("Attendere prego");
            progressAlert.show();

            // Carica su Cloudinary
            String imageUrl = cloudinaryService.uploadImmagineAnnuncio(fileSelezionato, annuncioId);
            
            progressAlert.close();
            mostraSuccesso("Immagine caricata con successo!");
            
            return imageUrl;
            
        } catch (Exception e) {
            mostraErrore("Errore di caricamento", "Impossibile caricare l'immagine: " + e.getMessage());
            return null;
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
        alert.setTitle("Successo");
        alert.setHeaderText(null);
        alert.setContentText(messaggio);
        alert.showAndWait();
    }
}