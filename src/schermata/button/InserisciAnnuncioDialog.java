package schermata.button;

import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.util.Callback;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

import application.Classe.Annuncio;
import application.Classe.Oggetto;
import application.DB.OggettoDAO;
import application.Enum.Categoria;
import application.Enum.OrigineOggetto;

/**
 * Dialog per l'inserimento di un nuovo annuncio
 * Permette all'utente di compilare i dati dell'annuncio e selezionare un'immagine
 */
public class InserisciAnnuncioDialog extends Dialog<Annuncio> {

    private String imagePath = null;  // Percorso dell'immagine da salvare nel DB
    private File file = null;         // File immagine selezionato
    private int venditoreId;          // ID del venditore che sta creando l'annuncio

    // Componenti UI
    private TextField titoloField;
    private TextArea descrizioneArea;
    private ComboBox<String> categoriaCombo;
    private ComboBox<String> tipoCombo;
    private ComboBox<String> origineCombo;
    private TextField prezzoField;
    
    // Label per messaggi di errore
    private Label erroreTitolo;
    private Label erroreDescrizione;
    private Label erroreCategoria;
    private Label erroreTipo;
    private Label erroreOrigine;
    private Label errorePrezzo;
    private Label erroreImmagine;

    /**
     * Costruttore del dialog per l'inserimento annuncio
     * @param venditoreId ID dell'utente che sta creando l'annuncio
     */
    public InserisciAnnuncioDialog(int venditoreId) {
        this.venditoreId = venditoreId;
        
        initializeUI();
        setupEventHandlers();
        setupValidation();
    }

    /**
     * Inizializza l'interfaccia grafica del dialog
     */
    private void initializeUI() {
        // Configurazione base del dialog
        getDialogPane().getStyleClass().add("root");
        setTitle("Inserisci Nuovo Annuncio");
        setHeaderText("Compila tutti i campi richiesti");
        getDialogPane().setPrefSize(700, 650);

        // Creazione dei componenti UI
        createFormFields();
        setupLayout();
        
        // Aggiunta dei pulsanti
        ButtonType inserisciButtonType = new ButtonType("Inserisci", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(inserisciButtonType, ButtonType.CANCEL);
        
        // Disabilita il pulsante Inserisci inizialmente
        setupButtonValidation();
    }

    /**
     * Crea e configura i campi del form
     */
    private void createFormFields() {
        // Campo titolo dell'annuncio
        titoloField = new TextField();
        titoloField.setPromptText("Titolo annuncio");
        
        // Area descrizione
        descrizioneArea = new TextArea();
        descrizioneArea.setPromptText("Descrizione prodotto (max 500 caratteri)");
        descrizioneArea.setPrefRowCount(3);
        descrizioneArea.getStyleClass().add("text-area");
        
        // Combo box per la categoria
        categoriaCombo = new ComboBox<>();
        categoriaCombo.getItems().addAll(
            "Libri", "Elettronica", "Abbigliamento", "Informatica",
            "Musica", "Casa e arredamento", "Sport", "Giocattoli", "Altro"
        );
        categoriaCombo.setPromptText("Categoria");
        
        // Combo box per la tipologia di annuncio
        tipoCombo = new ComboBox<>();
        tipoCombo.getItems().addAll("Vendita", "Scambio", "Regalo", "Asta");
        tipoCombo.setPromptText("Tipologia");
        
        // Combo box per l'origine dell'oggetto
        origineCombo = new ComboBox<>();
        origineCombo.getItems().addAll(
            "Nuovo", "Usato", "Ricondizionato", 
            "Ricevuto in regalo", "Ottenuto per scambio"
        );
        origineCombo.setValue("Usato");
        origineCombo.setPromptText("Origine oggetto");
        
        // Campo prezzo con validazione numerica
        prezzoField = new TextField();
        prezzoField.setPromptText("Es: 12,99");
        prezzoField.setTextFormatter(createPriceTextFormatter());
        
        // Inizializza label errori
        initializeErrorLabels();
    }

    /**
     * Inizializza le label per i messaggi di errore
     */
    private void initializeErrorLabels() {
        erroreTitolo = createErrorLabel();
        erroreDescrizione = createErrorLabel();
        erroreCategoria = createErrorLabel();
        erroreTipo = createErrorLabel();
        erroreOrigine = createErrorLabel();
        errorePrezzo = createErrorLabel();
        erroreImmagine = createErrorLabel();
    }
    
    private Label createErrorLabel() {
        Label label = new Label();
        label.getStyleClass().add("error-label");
        label.setStyle("-fx-text-fill: red; -fx-font-size: 11px;");
        label.setVisible(false);
        return label;
    }

    /**
     * Crea un TextFormatter per validare l'input del prezzo
     * @return TextFormatter che accetta solo numeri e virgola
     */
    private TextFormatter<String> createPriceTextFormatter() {
        return new TextFormatter<>(change -> {
            if (change.getControlNewText().matches("\\d*[,]?\\d*")) {
                return change;
            }
            return null;
        });
    }

    /**
     * Configura il layout del form
     */
    private void setupLayout() {
        GridPane grid = new GridPane();
        grid.setVgap(10);
        grid.setHgap(15);
        grid.setPadding(new Insets(20));

        // Aggiunta dei campi al grid layout con label errori
        addFieldToGrid(grid, "Titolo annuncio*:", titoloField, erroreTitolo, 0);
        addFieldToGrid(grid, "Descrizione*:", descrizioneArea, erroreDescrizione, 1);
        addFieldToGrid(grid, "Categoria*:", categoriaCombo, erroreCategoria, 2);
        addFieldToGrid(grid, "Tipologia*:", tipoCombo, erroreTipo, 3);
        addFieldToGrid(grid, "Origine*:", origineCombo, erroreOrigine, 4);
        addFieldToGrid(grid, "Prezzo (€):", prezzoField, errorePrezzo, 5);
        
        // Sezione selezione immagine
        VBox imageSection = createImageSelectionSection();
        grid.add(imageSection, 0, 6, 2, 1);

        getDialogPane().setContent(grid);
    }

    /**
     * Aggiunge un campo al grid layout con label errore
     */
    private void addFieldToGrid(GridPane grid, String labelText, Control field, Label errorLabel, int row) {
        grid.add(new Label(labelText), 0, row);
        
        VBox fieldContainer = new VBox(2);
        fieldContainer.getChildren().addAll(field, errorLabel);
        
        grid.add(fieldContainer, 1, row);
    }

    /**
     * Crea la sezione per la selezione dell'immagine
     */
    private VBox createImageSelectionSection() {
        Button selezionaImmagine = new Button("Seleziona Immagine");
        Label immagineSelezionata = new Label("Nessun file selezionato (opzionale)");
        
        // Configurazione del pulsante selezione immagine
        selezionaImmagine.setOnAction(e -> handleImageSelection(immagineSelezionata));
        
        VBox container = new VBox(5, 
            new Label("Immagine:"),
            selezionaImmagine, 
            immagineSelezionata,
            erroreImmagine
        );
        
        return container;
    }

    /**
     * Gestisce la selezione di un'immagine tramite file chooser
     */
    private void handleImageSelection(Label statusLabel) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Immagini", "*.png", "*.jpg", "*.jpeg")
        );
        
        File selectedFile = fileChooser.showOpenDialog(new Stage());
        if (selectedFile != null) {
            try {
                copyImageToProjectFolder(selectedFile, statusLabel);
                clearError(erroreImmagine);
            } catch (IOException ex) {
                showError(erroreImmagine, "Errore nel salvataggio dell'immagine: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    /**
     * Copia l'immagine selezionata nella cartella del progetto
     */
    private void copyImageToProjectFolder(File selectedFile, Label statusLabel) throws IOException {
        Path destFolder = Paths.get("C:/Users/matti/Desktop/project/application/img");
        
        // Crea la directory se non esiste
        if (!Files.exists(destFolder)) {
            Files.createDirectories(destFolder);
            System.out.println("✅ Cartella creata: " + destFolder);
        }
        
        // Genera un nome file univoco basato sul timestamp
        String fileName = System.currentTimeMillis() + "_" + selectedFile.getName();
        Path destPath = destFolder.resolve(fileName);
        
        // Copia il file
        Files.copy(selectedFile.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);
        
        imagePath = "/application/img/" + fileName;
        file = destPath.toFile();
        
        statusLabel.setText("File selezionato: " + fileName);
        System.out.println("✅ Immagine copiata: " + destPath);
    }

    /**
     * Configura gli event handler per el dialog
     */
    private void setupEventHandlers() {
        setResultConverter(this::handleDialogResult);
    }

    /**
     * Configura la validazione del pulsante Inserisci
     */
    private void setupButtonValidation() {
        // Disabilita il pulsante Inserisci inizialmente
        Button inserisciButton = (Button) getDialogPane().lookupButton(getDialogPane().getButtonTypes().get(0));
        inserisciButton.setDisable(true);
        
        // Aggiungi listener per abilitare/disabilitare il pulsante
        setupValidationListeners();
    }

    /**
     * Configura i listener per la validazione in tempo reale
     */
    private void setupValidation() {
        setupValidationListeners();
    }

    private void setupValidationListeners() {
        // Listener per il titolo
        titoloField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().isEmpty()) {
                showError(erroreTitolo, "Il titolo è obbligatorio");
            } else if (newVal.length() > 100) {
                showError(erroreTitolo, "Il titolo non può superare i 100 caratteri");
            } else {
                clearError(erroreTitolo);
            }
            updateInsertButton();
        });

        // Listener per la descrizione
        descrizioneArea.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().isEmpty()) {
                showError(erroreDescrizione, "La descrizione è obbligatoria");
            } else if (newVal.length() > 500) {
                showError(erroreDescrizione, "La descrizione non può superare i 500 caratteri");
            } else {
                clearError(erroreDescrizione);
            }
            updateInsertButton();
        });

        // Listener per le combo box
        categoriaCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                showError(erroreCategoria, "Seleziona una categoria");
            } else {
                clearError(erroreCategoria);
            }
            updateInsertButton();
        });

        tipoCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                showError(erroreTipo, "Seleziona una tipologia");
            } else {
                clearError(erroreTipo);
                validatePrezzoField(); // Ricontrolla il prezzo quando cambia la tipologia
            }
            updateInsertButton();
        });

        origineCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                showError(erroreOrigine, "Seleziona l'origine dell'oggetto");
            } else {
                clearError(erroreOrigine);
            }
            updateInsertButton();
        });

        // Listener per il prezzo
        prezzoField.textProperty().addListener((obs, oldVal, newVal) -> {
            validatePrezzoField();
            updateInsertButton();
        });
    }

    /**
     * Valida il campo prezzo in base alla tipologia selezionata
     */
    private void validatePrezzoField() {
        String tipologia = tipoCombo.getValue();
        String prezzoText = prezzoField.getText().trim();
        
        if (tipologia == null) return;
        
        if (tipologia.equals("Vendita") || tipologia.equals("Asta")) {
            if (prezzoText.isEmpty()) {
                showError(errorePrezzo, "Il prezzo è obbligatorio per " + tipologia);
            } else {
                try {
                    double prezzo = Double.parseDouble(prezzoText.replace(",", "."));
                    if (prezzo < 0) {
                        showError(errorePrezzo, "Il prezzo non può essere negativo");
                    } else {
                        clearError(errorePrezzo);
                    }
                } catch (NumberFormatException e) {
                    showError(errorePrezzo, "Formato prezzo non valido. Usa: 12,99");
                }
            }
        } else {
            // Per Scambio e Regalo, il prezzo è opzionale
            if (!prezzoText.isEmpty()) {
                try {
                    double prezzo = Double.parseDouble(prezzoText.replace(",", "."));
                    if (prezzo < 0) {
                        showError(errorePrezzo, "Il prezzo non può essere negativo");
                    } else {
                        clearError(errorePrezzo);
                    }
                } catch (NumberFormatException e) {
                    showError(errorePrezzo, "Formato prezzo non valido. Usa: 12,99");
                }
            } else {
                clearError(errorePrezzo);
            }
        }
    }

    /**
     * Aggiorna lo stato del pulsante Inserisci
     */
    private void updateInsertButton() {
        Button inserisciButton = (Button) getDialogPane().lookupButton(getDialogPane().getButtonTypes().get(0));
        
        boolean isValid = isFormValid();
        inserisciButton.setDisable(!isValid);
    }

    /**
     * Verifica se il form è valido
     */
    private boolean isFormValid() {
        // Controlla se ci sono errori visibili
        boolean hasErrors = erroreTitolo.isVisible() || 
                           erroreDescrizione.isVisible() || 
                           erroreCategoria.isVisible() || 
                           erroreTipo.isVisible() || 
                           erroreOrigine.isVisible() || 
                           errorePrezzo.isVisible() ||
                           erroreImmagine.isVisible();
        
        // Controlla che i campi obbligatori siano compilati
        boolean requiredFieldsFilled = !titoloField.getText().trim().isEmpty() &&
                                      !descrizioneArea.getText().trim().isEmpty() &&
                                      categoriaCombo.getValue() != null &&
                                      tipoCombo.getValue() != null &&
                                      origineCombo.getValue() != null;
        
        // Controlla validità prezzo in base alla tipologia
        boolean prezzoValid = true;
        String tipologia = tipoCombo.getValue();
        String prezzoText = prezzoField.getText().trim();
        
        if (tipologia != null && (tipologia.equals("Vendita") || tipologia.equals("Asta"))) {
            if (prezzoText.isEmpty()) {
                prezzoValid = false;
            } else {
                try {
                    double prezzo = Double.parseDouble(prezzoText.replace(",", "."));
                    prezzoValid = prezzo >= 0;
                } catch (NumberFormatException e) {
                    prezzoValid = false;
                }
            }
        }
        
        return !hasErrors && requiredFieldsFilled && prezzoValid;
    }

    /**
     * Mostra un messaggio di errore
     */
    private void showError(Label errorLabel, String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    /**
     * Nasconde un messaggio di errore
     */
    private void clearError(Label errorLabel) {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }

    /**
     * Gestisce il risultato del dialog quando viene premuto un pulsante
     */
    private Annuncio handleDialogResult(ButtonType buttonType) {
        if (buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            // Esegui una validazione finale prima di creare l'annuncio
            if (!performFinalValidation()) {
                return null; // Non chiudere il dialog se ci sono errori
            }
            return createAnnuncioFromForm();
        }
        return null;
    }

    /**
     * Esegue una validazione finale prima dell'invio
     */
    private boolean performFinalValidation() {
        List<String> errors = new ArrayList<>();
        
        // Validazione titolo
        if (titoloField.getText().trim().isEmpty()) {
            errors.add("Il titolo è obbligatorio");
            showError(erroreTitolo, "Il titolo è obbligatorio");
        }
        
        // Validazione descrizione
        if (descrizioneArea.getText().trim().isEmpty()) {
            errors.add("La descrizione è obbligatoria");
            showError(erroreDescrizione, "La descrizione è obbligatoria");
        } else if (descrizioneArea.getText().length() > 500) {
            errors.add("La descrizione non può superare i 500 caratteri");
            showError(erroreDescrizione, "La descrizione non può superare i 500 caratteri");
        }
        
        // Validazione combo box
        if (categoriaCombo.getValue() == null) {
            errors.add("Seleziona una categoria");
            showError(erroreCategoria, "Seleziona una categoria");
        }
        
        if (tipoCombo.getValue() == null) {
            errors.add("Seleziona una tipologia");
            showError(erroreTipo, "Seleziona una tipologia");
        }
        
        if (origineCombo.getValue() == null) {
            errors.add("Seleziona l'origine dell'oggetto");
            showError(erroreOrigine, "Seleziona l'origine dell'oggetto");
        }
        
        // Validazione prezzo finale
        validatePrezzoField();
        if (errorePrezzo.isVisible()) {
            errors.add(errorePrezzo.getText());
        }
        
        if (!errors.isEmpty()) {
            showAlert("Correggi gli errori prima di procedere:\n- " + String.join("\n- ", errors));
            return false;
        }
        
        return true;
    }

    /**
     * Crea un annuncio a partire dai dati del form
     */
    private Annuncio createAnnuncioFromForm() {
        // Validazione e parsing del prezzo
        Double prezzo = validateAndParsePrice();
        if (prezzo == null) {
            return null;
        }
        
        // Creazione dell'oggetto
        Oggetto oggetto = createOggetto();
        if (oggetto == null) {
            return null;
        }
        
        // Creazione dell'annuncio
        return createAnnuncio(oggetto, prezzo);
    }

    /**
     * Valida e parsing del campo prezzo
     */
    private Double validateAndParsePrice() {
        String tipologiaSelezionata = tipoCombo.getValue();
        String prezzoText = prezzoField.getText().trim();
        
        // Se l'utente ha inserito un prezzo, usalo sempre
        if (!prezzoText.isEmpty()) {
            try {
                double prezzo = Double.parseDouble(prezzoText.replace(",", "."));
                if (prezzo < 0) {
                    showAlert("Il prezzo non può essere negativo");
                    return null;
                }
                return prezzo;
            } catch (NumberFormatException e) {
                showAlert("Prezzo non valido. Usa il formato: 12,99");
                return null;
            }
        }
        
        // Se non c'è prezzo inserito
        if (tipologiaSelezionata.equals("Vendita") || tipologiaSelezionata.equals("Asta")) {
            showAlert("Inserisci il prezzo per " + tipologiaSelezionata);
            return null;
        }
        
        // Per Scambio e Regalo, prezzo è 0 se non specificato
        return 0.0;
    }

    /**
     * Crea l'oggetto a partire dai dati del form
     */
    private Oggetto createOggetto() {
        try {
            // Prepara il percorso immagine (opzionale)
            String pathPerDB = imagePath != null ? imagePath : "";
            
            // Parsing dell'origine selezionata
            OrigineOggetto origine = OrigineOggetto.parseOrigine(origineCombo.getValue());
            
            System.out.println("📦 Creazione oggetto:");
            System.out.println("   Descrizione: " + descrizioneArea.getText());
            System.out.println("   Categoria: " + categoriaCombo.getValue());
            System.out.println("   Origine: " + origine);
            System.out.println("   Image URL: " + pathPerDB);
            
            Oggetto oggettoTemporaneo = new Oggetto(
                0, // ID temporaneo
                titoloField.getText(), // Nome dell'oggetto
                descrizioneArea.getText(),
                Categoria.parseCategoria(categoriaCombo.getValue()),
                pathPerDB,
                file,
                origine
            );
            
            // Salva l'oggetto nel database
            System.out.println("💾 Salvataggio oggetto nel database...");
            int oggettoId = OggettoDAO.salvaOggetto(oggettoTemporaneo);
            
            if (oggettoId == -1) {
                showAlert("Errore nel salvataggio dell'oggetto nel database");
                return null;
            }
            
            System.out.println("✅ Oggetto salvato con ID: " + oggettoId);
            
            // Crea l'oggetto finale con ID corretto
            Oggetto oggettoFinale = new Oggetto(
                oggettoId,
                titoloField.getText(),
                descrizioneArea.getText(),
                Categoria.parseCategoria(categoriaCombo.getValue()),
                pathPerDB,
                file,
                origine
            );
            
            // Imposta il file immagine se presente
            if (file != null) {
                oggettoFinale.setImmagine(file);
            }
            
            return oggettoFinale;
            
        } catch (Exception e) {
            System.err.println("❌ Errore nella creazione dell'oggetto: " + e.getMessage());
            e.printStackTrace();
            showAlert("Errore nella creazione dell'oggetto: " + e.getMessage());
            return null;
        }
    }

    /**
     * Crea l'annuncio finale
     */
    private Annuncio createAnnuncio(Oggetto oggetto, double prezzo) {
        try {
            String tipologiaSelezionata = tipoCombo.getValue();
            
            // Crea l'annuncio base
            Annuncio annuncio = new Annuncio(
                oggetto,
                prezzo,
                tipologiaSelezionata,
                "Standard", // Modalità consegna di default
                venditoreId
            );
            
            // Imposta il titolo SOLO sull'annuncio
            annuncio.setTitolo(titoloField.getText().trim());
            
            // Log per debug
            logAnnuncioCreation(annuncio);
            
            return annuncio;
            
        } catch (IllegalArgumentException ex) {
            showAlert("Tipologia non valida: " + ex.getMessage());
            return null;
        } catch (Exception ex) {
            showAlert("Errore nella creazione dell'annuncio: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Log delle informazioni dell'annuncio creato
     */
    private void logAnnuncioCreation(Annuncio annuncio) {
        System.out.println("✅ Annuncio creato con successo:");
        System.out.println("   Titolo: '" + annuncio.getTitolo() + "'");
        System.out.println("   Descrizione oggetto: '" + annuncio.getOggetto().getDescrizione() + "'");
        System.out.println("   Prezzo: €" + annuncio.getPrezzo());
        System.out.println("   Tipologia: " + annuncio.getTipologia());
        System.out.println("   Venditore ID: " + annuncio.getVenditoreId());
    }

    /**
     * Mostra un alert di errore
     */
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}