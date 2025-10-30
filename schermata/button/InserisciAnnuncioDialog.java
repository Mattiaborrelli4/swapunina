package schermata.button;

import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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

import application.servic;
import application.Classe.Annuncio;
import application.Classe.Oggetto;
import application.DB.CloudinaryService;
import application.DB.OggettoDAO;
import application.Enum.Categoria;
import application.Enum.OrigineOggetto;
import application.Enum.Tipologia;
import application.DB.SessionManager;

/**
 * Dialog per l'inserimento di un nuovo annuncio con validazione completa
 * Gestisce la creazione di annunci con upload immagini Cloudinary e validazione in tempo reale
 */
public class InserisciAnnuncioDialog extends Dialog<Annuncio> {

    private String imagePath = null;
    private File file = null;
    private final int venditoreId;

    // Componenti UI
    private TextField titoloField;
    private TextArea descrizioneArea;
    private ComboBox<String> categoriaCombo;
    private ComboBox<String> tipoCombo;
    private ComboBox<String> origineCombo;
    private TextField prezzoField;
    private ComboBox<String> consegnaCombo;
    private File imageFile;
    
    // Sistema di gestione errori
    private final List<Label> errorLabels = new ArrayList<>();
    private Label erroreTitolo;
    private Label erroreDescrizione;
    private Label erroreCategoria;
    private Label erroreTipo;
    private Label erroreOrigine;
    private Label errorePrezzo;
    private Label erroreImmagine;
    private Label erroreConsegna;
    
    // Componenti per l'immagine Cloudinary
    private Button selezioneImmagineButton;
    private ImageView anteprimaImageView;
    private Label nomeFileLabel;
    private File fileImmagineSelezionato;
    private String cloudinaryImageUrl;
    
    // Servizio Cloudinary
    private final servic cloudinaryService;

    // Costanti per configurazione
    private static final int MAX_TITOLO_LENGTH = 100;
    private static final int MAX_DESCRIZIONE_LENGTH = 500;
    private static final String[] ALLOWED_IMAGE_EXTENSIONS = {"*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"};

    /**
     * Costruttore principale del dialog di inserimento annuncio
     * @param venditoreId ID del venditore che sta creando l'annuncio
     */
    public InserisciAnnuncioDialog(int venditoreId) {
        this.venditoreId = venditoreId;
        this.cloudinaryService = new servic();
        this.imageFile = null;
        
        initializeDialog();
        setupUIComponents();
        setupEventHandling();
        setupValidationSystem();
    }

    /**
     * Costruttore alternativo con file immagine pre-selezionato
     * @param venditoreId ID del venditore che sta creando l'annuncio
     * @param selectedFile File immagine pre-selezionato
     */
    public InserisciAnnuncioDialog(int venditoreId, File selectedFile) {
        this.venditoreId = venditoreId;
        this.cloudinaryService = new servic();
        this.imageFile = selectedFile;
        
        initializeDialog();
        setupUIComponents();
        setupEventHandling();
        setupValidationSystem();
    }
   
    /**
     * Inizializza le proprietà base del dialog
     */
    private void initializeDialog() {
        setTitle("Inserisci Nuovo Annuncio");
        setHeaderText("Compila tutti i campi richiesti");
        getDialogPane().setPrefSize(700, 750);
        
        // Aggiunta pulsanti principali
        ButtonType inserisciButtonType = new ButtonType("Inserisci", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(inserisciButtonType, ButtonType.CANCEL);
    }

    /**
     * Configura tutti i componenti dell'interfaccia utente
     */
    private void setupUIComponents() {
        createFormFields();
        setupMainLayout();
        initializeButtonValidation();
    }

    /**
     * Crea e configura i campi di input del form
     */
    private void createFormFields() {
        titoloField = createTextField("Titolo annuncio");
        descrizioneArea = createTextArea();
        categoriaCombo = createCategoriaComboBox();
        tipoCombo = createTipoComboBox();
        origineCombo = createOrigineComboBox();
        prezzoField = createPrezzoField();
        consegnaCombo = createConsegnaComboBox();
        
        initializeErrorLabels();
        initializeImageComponents();
    }

    /**
     * Crea un campo di testo con prompt personalizzato
     */
    private TextField createTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        return field;
    }

    /**
     * Crea l'area di testo per la descrizione
     */
    private TextArea createTextArea() {
        TextArea area = new TextArea();
        area.setPromptText("Descrizione prodotto (max " + MAX_DESCRIZIONE_LENGTH + " caratteri)");
        area.setPrefRowCount(3);
        area.getStyleClass().add("text-area");
        return area;
    }

    /**
     * Crea la combo box per le categorie
     */
    private ComboBox<String> createCategoriaComboBox() {
        ComboBox<String> combo = new ComboBox<>();
        // Usa i display names con emoji dalla enum Categoria
        combo.getItems().addAll(Categoria.getDisplayNamesWithEmojiList());
        combo.setPromptText("Categoria");
        return combo;
    }

    /**
     * Crea la combo box per le tipologie
     */
    private ComboBox<String> createTipoComboBox() {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll("Vendita", "Scambio", "Regalo", "Asta");
        combo.setPromptText("Tipologia");
        return combo;
    }

    /**
     * Crea la combo box per l'origine dell'oggetto
     */
    private ComboBox<String> createOrigineComboBox() {
        ComboBox<String> combo = new ComboBox<>();
        // Usa i display names con emoji dalla enum OrigineOggetto
        combo.getItems().addAll(OrigineOggetto.getDisplayNamesWithEmojiList());
        combo.setValue(OrigineOggetto.USATO.getDisplayNameWithEmoji()); // Imposta "Usato" come default
        combo.setPromptText("Origine oggetto");
        return combo;
    }

    /**
     * Crea il campo prezzo con validazione numerica
     */
    private TextField createPrezzoField() {
        TextField field = new TextField();
        field.setPromptText("Es: 12,99");
        field.setTextFormatter(createPriceTextFormatter());
        return field;
    }

    /**
     * Crea la combo box per la modalità di consegna
     */
    private ComboBox<String> createConsegnaComboBox() {
        ComboBox<String> combo = new ComboBox<>();
        combo.getItems().addAll(
            "Incontro di persona",
            "Spedizione gratuita", 
            "Spedizione a carico acquirente",
            "Ritiro in sede",
            "Standard"
        );
        combo.setValue("Incontro di persona");
        combo.setPromptText("Modalità consegna");
        return combo;
    }

    /**
     * Inizializza i componenti per la gestione delle immagini
     */
    private void initializeImageComponents() {
        selezioneImmagineButton = new Button("📷 Seleziona Immagine");
        anteprimaImageView = new ImageView();
        nomeFileLabel = new Label("Nessun file selezionato (opzionale)");
        
        // Configura l'anteprima
        anteprimaImageView.setFitWidth(200);
        anteprimaImageView.setFitHeight(150);
        anteprimaImageView.setPreserveRatio(true);
        anteprimaImageView.setStyle("-fx-border-color: #ccc; -fx-border-width: 1px;");
        
        // Configura il pulsante
        selezioneImmagineButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        selezioneImmagineButton.setOnAction(e -> handleImageSelection());
    }

    /**
     * Crea un TextFormatter per validare l'input del prezzo
     */
    private TextFormatter<String> createPriceTextFormatter() {
        return new TextFormatter<>(change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*[,.]?\\d*")) {
                return change;
            }
            return null;
        });
    }

    /**
     * Inizializza il sistema de label per gli errori
     */
    private void initializeErrorLabels() {
        erroreTitolo = createErrorLabel();
        erroreDescrizione = createErrorLabel();
        erroreCategoria = createErrorLabel();
        erroreTipo = createErrorLabel();
        erroreOrigine = createErrorLabel();
        errorePrezzo = createErrorLabel();
        erroreImmagine = createErrorLabel();
        erroreConsegna = createErrorLabel();
        
        // Aggiungi tutte le label alla lista per gestione centralizzata
        errorLabels.addAll(List.of(
            erroreTitolo, erroreDescrizione, erroreCategoria,
            erroreTipo, erroreOrigine, errorePrezzo, erroreImmagine, erroreConsegna
        ));
    }

    /**
     * Crea una label di errore con stile standardizzato
     */
    private Label createErrorLabel() {
        Label label = new Label();
        label.getStyleClass().add("error-label");
        label.setStyle("-fx-text-fill: red; -fx-font-size: 11px;");
        label.setVisible(false);
        return label;
    }

    /**
     * Configura il layout principale del dialog
     */
    private void setupMainLayout() {
        GridPane grid = createMainGrid();
        addFormFieldsToGrid(grid);
        getDialogPane().setContent(grid);
    }

    /**
     * Crea il grid pane principale con configurazione standard
     */
    private GridPane createMainGrid() {
        GridPane grid = new GridPane();
        grid.setVgap(10);
        grid.setHgap(15);
        grid.setPadding(new Insets(20));
        return grid;
    }

    /**
     * Aggiunge tutti i campi del form al grid layout
     */
    private void addFormFieldsToGrid(GridPane grid) {
        int row = 0;
        
        // Titolo
        addFieldToGrid(grid, "Titolo annuncio*:", titoloField, erroreTitolo, row++);
        
        // Descrizione
        addFieldToGrid(grid, "Descrizione*:", descrizioneArea, erroreDescrizione, row++);
        
        // Categoria
        addFieldToGrid(grid, "Categoria*:", categoriaCombo, erroreCategoria, row++);
        
        // Tipologia
        addFieldToGrid(grid, "Tipologia*:", tipoCombo, erroreTipo, row++);
        
        // Origine
        addFieldToGrid(grid, "Origine*:", origineCombo, erroreOrigine, row++);
        
        // Prezzo
        addFieldToGrid(grid, "Prezzo (€):", prezzoField, errorePrezzo, row++);
        
        // Modalità consegna
        addFieldToGrid(grid, "Modalità consegna*:", consegnaCombo, erroreConsegna, row++);
        
        // Sezione immagine Cloudinary
        addImageSectionToGrid(grid, row);
    }

    /**
     * Aggiunge un singolo campo al grid layout
     */
    private void addFieldToGrid(GridPane grid, String labelText, Control field, Label errorLabel, int row) {
        grid.add(new Label(labelText), 0, row);
        
        VBox fieldContainer = new VBox(2);
        fieldContainer.getChildren().addAll(field, errorLabel);
        grid.add(fieldContainer, 1, row);
    }

    /**
     * Aggiunge la sezione selezione immagine al grid
     */
    private void addImageSectionToGrid(GridPane grid, int row) {
        VBox imageSection = createImageSelectionSection();
        grid.add(imageSection, 0, row, 2, 1);
    }

    /**
     * Crea la sezione per la selezione dell'immagine con Cloudinary
     */
    private VBox createImageSelectionSection() {
        VBox imageSection = new VBox(10);
        imageSection.setStyle("-fx-border-color: #ddd; -fx-border-radius: 5; -fx-padding: 10;");
        
        Label titoloSezione = new Label("Immagine dell'articolo:");
        titoloSezione.setStyle("-fx-font-weight: bold;");
        
        VBox anteprimaContainer = new VBox(5);
        anteprimaContainer.getChildren().addAll(anteprimaImageView, nomeFileLabel);
        
        imageSection.getChildren().addAll(
            titoloSezione,
            new Label("Seleziona un'immagine per il tuo annuncio (verrà caricata su Cloudinary)"),
            selezioneImmagineButton,
            anteprimaContainer,
            erroreImmagine
        );
        
        return imageSection;
    }

    /**
     * Configura gli event handler per i componenti UI
     */
    private void setupEventHandling() {
        setResultConverter(this::handleDialogResult);
    }

    /**
     * Configura il sistema de validazione del form
     */
    private void setupValidationSystem() {
        setupFieldValidators();
        setupRealTimeValidation();
    }

    /**
     * Configura i validatori per ogni campo
     */
    private void setupFieldValidators() {
        setupTitoloValidator();
        setupDescrizioneValidator();
        setupComboBoxValidators();
        setupPrezzoValidator();
    }

    /**
     * Configura la validazione in tempo reale
     */
    private void setupRealTimeValidation() {
        // Aggiorna lo stato del pulsante quando qualsiasi campo cambia
        titoloField.textProperty().addListener((obs, oldVal, newVal) -> updateInsertButton());
        descrizioneArea.textProperty().addListener((obs, oldVal, newVal) -> updateInsertButton());
        categoriaCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateInsertButton());
        tipoCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateInsertButton());
        origineCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateInsertButton());
        prezzoField.textProperty().addListener((obs, oldVal, newVal) -> updateInsertButton());
        consegnaCombo.valueProperty().addListener((obs, oldVal, newVal) -> updateInsertButton());
    }

    /**
     * Configura il validatore per il campo titolo
     */
    private void setupTitoloValidator() {
        titoloField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().isEmpty()) {
                showError(erroreTitolo, "Il titolo è obbligatorio");
            } else if (newVal.length() > MAX_TITOLO_LENGTH) {
                showError(erroreTitolo, "Il titolo non può superare i " + MAX_TITOLO_LENGTH + " caratteri");
            } else {
                clearError(erroreTitolo);
            }
        });
    }

    /**
     * Configura il validatore per il campo descrizione
     */
    private void setupDescrizioneValidator() {
        descrizioneArea.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.trim().isEmpty()) {
                showError(erroreDescrizione, "La descrizione è obbligatoria");
            } else if (newVal.length() > MAX_DESCRIZIONE_LENGTH) {
                showError(erroreDescrizione, "La descrizione non può superare i " + MAX_DESCRIZIONE_LENGTH + " caratteri");
            } else {
                clearError(erroreDescrizione);
            }
        });
    }

    /**
     * Configura i validatori per le combo box
     */
    private void setupComboBoxValidators() {
        categoriaCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                showError(erroreCategoria, "Seleziona una categoria");
            } else {
                clearError(erroreCategoria);
            }
        });

        tipoCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                showError(erroreTipo, "Seleziona una tipologia");
            } else {
                clearError(erroreTipo);
                validatePrezzoField(); // Ricontrolla il prezzo quando cambia la tipologia
            }
        });

        origineCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                showError(erroreOrigine, "Seleziona l'origine dell'oggetto");
            } else {
                clearError(erroreOrigine);
            }
        });

        consegnaCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) {
                showError(erroreConsegna, "Seleziona una modalità di consegna");
            } else {
                clearError(erroreConsegna);
            }
        });
    }

    /**
     * Configura il validatore per il campo prezzo
     */
    private void setupPrezzoValidator() {
        prezzoField.textProperty().addListener((obs, oldVal, newVal) -> {
            validatePrezzoField();
        });
    }

    /**
     * Inizializza la validazione del pulsante Inserisci
     */
    private void initializeButtonValidation() {
        Button inserisciButton = getInserisciButton();
        if (inserisciButton != null) {
            inserisciButton.setDisable(true);
        }
    }

    /**
     * Ottiene il riferimento al pulsante Inserisci
     */
    private Button getInserisciButton() {
        return (Button) getDialogPane().lookupButton(
            getDialogPane().getButtonTypes().stream()
                .filter(buttonType -> buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                .findFirst()
                .orElse(null)
        );
    }

    /**
     * Gestisce la selezione dell'immagine tramite file chooser
     */
    private void handleImageSelection() {
        File selectedFile = showImageFileChooser();
        if (selectedFile != null) {
            processSelectedImage(selectedFile);
        }
    }

    /**
     * Mostra il file chooser per la selezione immagini
     */
    private File showImageFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleziona Immagine Annuncio");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Immagini", ALLOWED_IMAGE_EXTENSIONS)
        );
        return fileChooser.showOpenDialog(new Stage());
    }

    /**
     * Processa l'immagine selezionata dall'utente e carica su Cloudinary
     */
    private void processSelectedImage(File selectedFile) {
        try {
            // Verifica dimensione file (max 10MB)
            long fileSizeMB = selectedFile.length() / (1024 * 1024);
            if (fileSizeMB > 10) {
                showError(erroreImmagine, "L'immagine è troppo grande (" + fileSizeMB + "MB). Max 10MB consentiti.");
                resetImageSelection();
                return;
            }

            // Mostra anteprima
            showImagePreview(selectedFile);
            
            // Salva riferimento al file
            fileImmagineSelezionato = selectedFile;
            clearError(erroreImmagine);
            
        } catch (Exception ex) {
            showError(erroreImmagine, "Errore nel caricamento dell'anteprima: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Mostra l'anteprima dell'immagine selezionata
     */
    private void showImagePreview(File imageFile) {
        try {
            Image image = new Image(imageFile.toURI().toString());
            anteprimaImageView.setImage(image);
            nomeFileLabel.setText("File selezionato: " + imageFile.getName());
        } catch (Exception e) {
            throw new RuntimeException("Impossibile caricare l'anteprima dell'immagine", e);
        }
    }

    /**
     * Resetta la selezione dell'immagine
     */
    private void resetImageSelection() {
        fileImmagineSelezionato = null;
        cloudinaryImageUrl = null;
        anteprimaImageView.setImage(null);
        nomeFileLabel.setText("Nessun file selezionato (opzionale)");
    }

    /**
     * Carica l'immagine su Cloudinary
     */
    private String uploadImageToCloudinary(File imageFile) {
        try {
            CloudinaryService cloudinaryService = new CloudinaryService();
            
            if (!cloudinaryService.isEnabled()) {
                System.out.println("⚠️ Cloudinary disabilitato - uso percorso locale");
                return "file:" + imageFile.getAbsolutePath();
            }
            
            String publicId = "annuncio_" + System.currentTimeMillis() + "_" + SessionManager.getCurrentUserId();
            String cloudinaryUrl = cloudinaryService.uploadImage(imageFile.getAbsolutePath(), publicId);
            
            if (cloudinaryUrl != null && !cloudinaryUrl.equals(imageFile.getAbsolutePath())) {
                System.out.println("✅ Immagine caricata su Cloudinary: " + cloudinaryUrl);
                return cloudinaryUrl;
            } else {
                System.out.println("⚠️ Fallback a percorso locale per immagine");
                return "file:" + imageFile.getAbsolutePath();
            }
            
        } catch (Exception e) {
            System.err.println("❌ Errore upload Cloudinary: " + e.getMessage());
            return "file:" + imageFile.getAbsolutePath();
        }
    }

    /**
     * Valida il campo prezzo in base alla tipologia selezionata
     */
    private void validatePrezzoField() {
        String tipologia = tipoCombo.getValue();
        String prezzoText = prezzoField.getText().trim();
        
        if (tipologia == null) return;
        
        boolean isPrezzoRichiesto = tipologia.equals("Vendita") || tipologia.equals("Asta");
        
        if (isPrezzoRichiesto && prezzoText.isEmpty()) {
            showError(errorePrezzo, "Il prezzo è obbligatorio per " + tipologia);
        } else if (!prezzoText.isEmpty()) {
            validatePrezzoFormat(prezzoText);
        } else {
            clearError(errorePrezzo);
        }
    }

    /**
     * Valida il formato del prezzo inserito
     */
    private void validatePrezzoFormat(String prezzoText) {
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

    /**
     * Aggiorna lo stato del pulsante Inserisci basato sulla validità del form
     */
    private void updateInsertButton() {
        Button inserisciButton = getInserisciButton();
        if (inserisciButton != null) {
            inserisciButton.setDisable(!isFormValid());
        }
    }

    /**
     * Verifica se il form è completamente valido
     */
    private boolean isFormValid() {
        return !hasValidationErrors() && 
               areRequiredFieldsFilled() && 
               isPrezzoValidForTipologia();
    }

    /**
     * Verifica se ci sono errori di validazione visibili
     */
    private boolean hasValidationErrors() {
        return errorLabels.stream().anyMatch(Label::isVisible);
    }

    /**
     * Verifica che tutti i campi obbligatori siano compilati
     */
    private boolean areRequiredFieldsFilled() {
        return !titoloField.getText().trim().isEmpty() &&
               !descrizioneArea.getText().trim().isEmpty() &&
               categoriaCombo.getValue() != null &&
               tipoCombo.getValue() != null &&
               origineCombo.getValue() != null &&
               consegnaCombo.getValue() != null;
    }

    /**
     * Verifica la validità del prezzo in base alla tipologia
     */
    private boolean isPrezzoValidForTipologia() {
        String tipologia = tipoCombo.getValue();
        String prezzoText = prezzoField.getText().trim();
        
        if (tipologia == null) return false;
        
        if (tipologia.equals("Vendita") || tipologia.equals("Asta")) {
            if (prezzoText.isEmpty()) return false;
            try {
                double prezzo = Double.parseDouble(prezzoText.replace(",", "."));
                return prezzo >= 0;
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true; // Per Scambio e Regalo, il prezzo è opzionale
    }

    /**
     * Mostra un messaggio di errore in una label
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
            return performFinalValidationAndCreate();
        }
        return null;
    }

    /**
     * Esegue la validazione finale e crea l'annuncio
     */
    private Annuncio performFinalValidationAndCreate() {
        if (!performFinalValidation()) {
            return null;
        }
        return createAnnuncioFromForm();
    }

    /**
     * Esegue una validazione finale completa prima dell'invio
     */
    private boolean performFinalValidation() {
        List<String> errors = collectValidationErrors();
        
        if (!errors.isEmpty()) {
            showValidationAlert(errors);
            return false;
        }
        
        return true;
    }

    /**
     * Raccoglie tutti gli errori di validazione
     */
    private List<String> collectValidationErrors() {
        List<String> errors = new ArrayList<>();
        
        validateTitolo(errors);
        validateDescrizione(errors);
        validateComboBoxes(errors);
        validatePrezzoFinale(errors);
        
        return errors;
    }

    /**
     * Valida il campo titolo per la validazione finale
     */
    private void validateTitolo(List<String> errors) {
        String titolo = titoloField.getText().trim();
        if (titolo.isEmpty()) {
            errors.add("Il titolo è obbligatorio");
            showError(erroreTitolo, "Il titolo è obbligatorio");
        } else if (titolo.length() > MAX_TITOLO_LENGTH) {
            errors.add("Il titolo non può superare i " + MAX_TITOLO_LENGTH + " caratteri");
            showError(erroreTitolo, "Il titolo non può superare i " + MAX_TITOLO_LENGTH + " caratteri");
        }
    }

    /**
     * Valida el campo descrizione per la validazione finale
     */
    private void validateDescrizione(List<String> errors) {
        String descrizione = descrizioneArea.getText().trim();
        if (descrizione.isEmpty()) {
            errors.add("La descrizione è obbligatoria");
            showError(erroreDescrizione, "La descrizione è obbligatoria");
        } else if (descrizione.length() > MAX_DESCRIZIONE_LENGTH) {
            errors.add("La descrizione non può superare i " + MAX_DESCRIZIONE_LENGTH + " caratteri");
            showError(erroreDescrizione, "La descrizione non può superare i " + MAX_DESCRIZIONE_LENGTH + " caratteri");
        }
    }

    /**
     * Valida le combo box per la validazione finale
     */
    private void validateComboBoxes(List<String> errors) {
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
        
        if (consegnaCombo.getValue() == null) {
            errors.add("Seleziona una modalità di consegna");
            showError(erroreConsegna, "Seleziona una modalità di consegna");
        }
    }

    /**
     * Valida il prezzo per la validazione finale
     */
    private void validatePrezzoFinale(List<String> errors) {
        validatePrezzoField();
        if (errorePrezzo.isVisible()) {
            errors.add(errorePrezzo.getText());
        }
    }

    /**
     * Mostra un alert con tutti gli errori di validazione
     */
    private void showValidationAlert(List<String> errors) {
        String errorMessage = "Correggi gli errori prima di procedere:\n- " + 
                            String.join("\n- ", errors);
        showAlert("Errore di Validazione", errorMessage);
    }

    /**
     * Crea un annuncio a partire dai dati del form validati
     */
    private Annuncio createAnnuncioFromForm() {
        // 1. Gestione immagine
        String imageUrl = "";
        if (fileImmagineSelezionato != null) {
            imageUrl = uploadImageToCloudinary(fileImmagineSelezionato);
            if (imageUrl == null || imageUrl.isEmpty()) {
                showAlert("Errore Caricamento Immagine", "Impossibile caricare l'immagine. Riprova.");
                return null;
            }
        }

        // 2. Parsing del prezzo
        Double prezzo = validateAndParsePrice();
        if (prezzo == null) return null;
        
        // 3. Creazione oggetto
        Oggetto oggetto = createOggetto(imageUrl);
        if (oggetto == null) return null;
        
        // 4. Creazione annuncio finale
        return createAnnuncio(oggetto, prezzo);
    }

    /**
     * Valida e parsing del campo prezzo
     */
    private Double validateAndParsePrice() {
        String tipologiaSelezionata = tipoCombo.getValue();
        String prezzoText = prezzoField.getText().trim();
        
        if (!prezzoText.isEmpty()) {
            return parsePrezzo(prezzoText);
        }
        
        // Gestione prezzo per diverse tipologie
        if (tipologiaSelezionata.equals("Vendita") || tipologiaSelezionata.equals("Asta")) {
            showAlert("Prezzo Richiesto", "Inserisci il prezzo per " + tipologiaSelezionata);
            return null;
        }
        
        return 0.0; // Prezzo default per Scambio e Regalo
    }

    /**
     * Parsing del testo del prezzo in double
     */
    private Double parsePrezzo(String prezzoText) {
        try {
            double prezzo = Double.parseDouble(prezzoText.replace(",", "."));
            if (prezzo < 0) {
                showAlert("Prezzo Non Valido", "Il prezzo non può essere negativo");
                return null;
            }
            return prezzo;
        } catch (NumberFormatException e) {
            showAlert("Formato Prezzo Non Valido", "Usa il formato: 12,99");
            return null;
        }
    }

    /**
     * Crea l'oggetto a partire dai dati del form
     */
    private Oggetto createOggetto(String imageUrl) {
        try {
            String categoriaSelezionata = categoriaCombo.getValue();
            String origineSelezionata = origineCombo.getValue();
            
            // Usa i metodi di parsing delle enum
            Categoria categoria = parseCategoria(categoriaSelezionata);
            OrigineOggetto origine = parseOrigine(origineSelezionata);
            
            System.out.println("🎯 Categoria selezionata: " + categoria.toDebugString());
            System.out.println("🎯 Origine selezionata: " + origine.toDebugString());
            
            // Crea oggetto temporaneo
            Oggetto oggettoTemporaneo = new Oggetto(
                0, // ID temporaneo
                titoloField.getText().trim(),
                descrizioneArea.getText().trim(),
                categoria,
                imageUrl,
                fileImmagineSelezionato,
                origine
            );
            
            // Salva nel database
            int oggettoId = saveOggettoToDatabase(oggettoTemporaneo);
            
            if (oggettoId == -1) {
                showAlert("Errore Database", "Errore nel salvataggio dell'oggetto nel database");
                return null;
            }
            
            // Crea oggetto finale con ID
            Oggetto oggettoFinale = new Oggetto(
                oggettoId,
                titoloField.getText().trim(),
                descrizioneArea.getText().trim(),
                categoria,
                imageUrl,
                fileImmagineSelezionato,
                origine
            );
            
            // Log informazioni utili
            logOggettoCreation(oggettoFinale, categoria, origine);
            
            return oggettoFinale;
            
        } catch (Exception e) {
            handleOggettoCreationError(e);
            return null;
        }
    }

    /**
     * Converte la stringa categoria in enum Categoria
     * Usa il metodo statico della enum Categoria
     */
    private Categoria parseCategoria(String categoriaString) {
        if (categoriaString == null || categoriaString.trim().isEmpty()) {
            return Categoria.ALTRO;
        }
        
        // Rimuovi l'emoji se presente per il parsing
        String cleanedString = categoriaString.replaceAll("[\\p{So}\\p{Cn}]", "").trim();
        
        return Categoria.parseCategoria(cleanedString);
    }

    /**
     * Converte la stringa origine in enum OrigineOggetto
     * Usa il metodo statico della enum OrigineOggetto
     */
    private OrigineOggetto parseOrigine(String origineString) {
        if (origineString == null || origineString.trim().isEmpty()) {
            return OrigineOggetto.USATO;
        }
        
        // Rimuovi l'emoji se presente per il parsing
        String cleanedString = origineString.replaceAll("[\\p{So}\\p{Cn}]", "").trim();
        
        return OrigineOggetto.parseOrigine(cleanedString);
    }

    /**
     * Log delle informazioni dell'oggetto creato
     */
    private void logOggettoCreation(Oggetto oggetto, Categoria categoria, OrigineOggetto origine) {
        System.out.println("✅ Oggetto creato con successo:");
        System.out.println("   Nome: '" + oggetto.getNome() + "'");
        System.out.println("   Categoria: " + categoria.getDisplayNameWithEmoji());
        System.out.println("   Origine: " + origine.getDisplayNameWithEmoji());
        System.out.println("   Colore badge: " + origine.getColor());
        System.out.println("   Vendibile: " + origine.isVendibile());
        System.out.println("   Suggerimento prezzo: " + origine.getSuggerimentoPrezzo());
    }

    /**
     * Salva l'oggetto nel database
     */
    private int saveOggettoToDatabase(Oggetto oggetto) {
        System.out.println("💾 Salvataggio oggetto nel database...");
        int oggettoId = OggettoDAO.salvaOggetto(oggetto);
        System.out.println("✅ Oggetto salvato con ID: " + oggettoId);
        return oggettoId;
    }

    /**
     * Gestisce gli errori durante la creazione dell'oggetto
     */
    private void handleOggettoCreationError(Exception e) {
        System.err.println("❌ Errore nella creazione dell'oggetto: " + e.getMessage());
        e.printStackTrace();
        showAlert("Errore Creazione Oggetto", "Errore nella creazione dell'oggetto: " + e.getMessage());
    }

    /**
     * Crea l'annuncio finale
     */
    private Annuncio createAnnuncio(Oggetto oggetto, double prezzo) {
        try {
            String tipologiaSelezionata = tipoCombo.getValue();
            String consegnaSelezionata = consegnaCombo.getValue();
            
            // Converti la stringa della tipologia in enum
            Tipologia tipologiaEnum = convertiTipologia(tipologiaSelezionata);
            
            Annuncio annuncio = new Annuncio(
                oggetto,
                prezzo,
                tipologiaEnum,
                consegnaSelezionata,
                venditoreId
            );
            
            annuncio.setTitolo(titoloField.getText().trim());
            annuncio.setDescrizione(descrizioneArea.getText().trim());
            logAnnuncioCreation(annuncio);
            
            return annuncio;
            
        } catch (IllegalArgumentException ex) {
            showAlert("Tipologia Non Valida", "Tipologia non valida: " + ex.getMessage());
            return null;
        } catch (Exception ex) {
            showAlert("Errore Creazione Annuncio", "Errore nella creazione dell'annuncio: " + ex.getMessage());
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * Converte la stringa della tipologia in enum Tipologia
     */
    private Tipologia convertiTipologia(String tipologiaString) {
        switch (tipologiaString) {
            case "Vendita": return Tipologia.VENDITA;
            case "Scambio": return Tipologia.SCAMBIO;
            case "Regalo": return Tipologia.REGALO;
            case "Asta": return Tipologia.ASTA;
            default: throw new IllegalArgumentException("Tipologia non supportata: " + tipologiaString);
        }
    }

    /**
     * Log delle informazioni dell'annuncio creato
     */
    private void logAnnuncioCreation(Annuncio annuncio) {
        System.out.println("✅ Annuncio creato con successo:");
        System.out.println("   Titolo: '" + annuncio.getTitolo() + "'");
        System.out.println("   Descrizione: '" + annuncio.getDescrizione() + "'");
        System.out.println("   Prezzo: €" + annuncio.getPrezzo());
        System.out.println("   Tipologia: " + annuncio.getTipologia());
        System.out.println("   Consegna: " + annuncio.getModalitaConsegna());
        System.out.println("   Venditore ID: " + annuncio.getVenditoreId());
        System.out.println("   Immagine Cloudinary: " + (fileImmagineSelezionato != null ? "Sì" : "No"));
    }

    /**
     * Mostra un alert di errore
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Mostra un alert di informazione
     */
    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}