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

    /**
     * Costruttore del dialog per l'inserimento annuncio
     * @param venditoreId ID dell'utente che sta creando l'annuncio
     */
    public InserisciAnnuncioDialog(int venditoreId) {
        this.venditoreId = venditoreId;
        
        initializeUI();
        setupEventHandlers();
    }

    /**
     * Inizializza l'interfaccia grafica del dialog
     */
    private void initializeUI() {
        // Configurazione base del dialog
        getDialogPane().getStyleClass().add("root");
        setTitle("Inserisci Nuovo Annuncio");
        setHeaderText("Compila tutti i campi richiesti");
        getDialogPane().setPrefSize(700, 600);

        // Creazione dei componenti UI
        createFormFields();
        setupLayout();
        
        // Aggiunta dei pulsanti
        ButtonType inserisciButtonType = new ButtonType("Inserisci", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(inserisciButtonType, ButtonType.CANCEL);
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
        grid.setVgap(15);
        grid.setHgap(15);
        grid.setPadding(new Insets(20));

        // Aggiunta dei campi al grid layout
        addFieldToGrid(grid, "Titolo annuncio:", titoloField, 0);
        addFieldToGrid(grid, "Descrizione:", descrizioneArea, 1);
        addFieldToGrid(grid, "Categoria:", categoriaCombo, 2);
        addFieldToGrid(grid, "Tipologia:", tipoCombo, 3);
        addFieldToGrid(grid, "Origine:", origineCombo, 4);
        addFieldToGrid(grid, "Prezzo (€):", prezzoField, 5);
        
        // Sezione selezione immagine
        VBox imageSection = createImageSelectionSection();
        grid.add(imageSection, 0, 6, 2, 1);

        getDialogPane().setContent(grid);
    }

    /**
     * Aggiunge un campo al grid layout
     * @param grid GridPane a cui aggiungere il campo
     * @param labelText Testo della label
     * @param field Componente del campo
     * @param row Riga in cui posizionare il campo
     */
    private void addFieldToGrid(GridPane grid, String labelText, Control field, int row) {
        grid.add(new Label(labelText), 0, row);
        grid.add(field, 1, row);
    }

    /**
     * Crea la sezione per la selezione dell'immagine
     * @return VBox contenente i componenti per la selezione immagine
     */
    private VBox createImageSelectionSection() {
        Button selezionaImmagine = new Button("Seleziona Immagine");
        Label immagineSelezionata = new Label("Nessun file selezionato (opzionale)");
        
        // Configurazione del pulsante selezione immagine
        selezionaImmagine.setOnAction(e -> handleImageSelection(immagineSelezionata));
        
        return new VBox(5, selezionaImmagine, immagineSelezionata);
    }

    /**
     * Gestisce la selezione di un'immagine tramite file chooser
     * @param statusLabel Label per mostrare lo stato della selezione
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
            } catch (IOException ex) {
                showAlert("Errore nel salvataggio dell'immagine.");
                ex.printStackTrace();
            }
        }
    }

    /**
     * Copia l'immagine selezionata nella cartella del progetto
     * @param selectedFile File immagine selezionato
     * @param statusLabel Label per mostrare lo stato
     */
    private void copyImageToProjectFolder(File selectedFile, Label statusLabel) throws IOException {
        // 🔥 MODIFICA: Percorso corretto senza "src"
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
        
        // 🔥 MODIFICA: Percorso corretto per il database
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
     * Gestisce il risultato del dialog quando viene premuto un pulsante
     * @param buttonType Tipo di pulsante premuto
     * @return Annuncio creato o null se annullato
     */
    private Annuncio handleDialogResult(ButtonType buttonType) {
        if (buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            return createAnnuncioFromForm();
        }
        return null;
    }

    /**
     * Crea un annuncio a partire dai dati del form
     * @return Annuncio creato o null se ci sono errori
     */
    private Annuncio createAnnuncioFromForm() {
        // Validazione dei campi obbligatori
        if (!validateRequiredFields()) {
            return null;
        }
        
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
     * Valida i campi obbligatori del form
     * @return true se tutti i campi sono validi, false altrimenti
     */
    private boolean validateRequiredFields() {
        if (titoloField.getText().isEmpty() ||
            descrizioneArea.getText().isEmpty() ||
            categoriaCombo.getValue() == null ||
            tipoCombo.getValue() == null ||
            origineCombo.getValue() == null) {
            
            showAlert("Compila tutti i campi obbligatori");
            return false;
        }
        return true;
    }

    /**
     * Valida e parsing del campo prezzo
     * @return Prezzo parsato o null se invalido
     */
    private Double validateAndParsePrice() {
        String tipologiaSelezionata = tipoCombo.getValue();
        
        // Verifica se la tipologia richiede il prezzo
        if (tipologiaSelezionata.equals("Vendita") || tipologiaSelezionata.equals("Asta")) {
            if (prezzoField.getText().isEmpty()) {
                showAlert("Inserisci il prezzo per " + tipologiaSelezionata);
                return null;
            }
            
            try {
                double prezzo = Double.parseDouble(prezzoField.getText().replace(",", "."));
                if (prezzo <= 0) {
                    showAlert("Il prezzo deve essere maggiore di zero");
                    return null;
                }
                return prezzo;
            } catch (NumberFormatException e) {
                showAlert("Prezzo non valido");
                return null;
            }
        }
        
        return 0.0; // Prezzo zero per regalo/scambio
    }

    /**
     * Crea l'oggetto a partire dai dati del form
     * @return Oggetto creato o null se errore
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
            
            // 🔥 MODIFICA: Crea l'oggetto SENZA nome (solo descrizione)
            Oggetto oggettoTemporaneo = new Oggetto(
            	    0, // ID temporaneo
            	    "Inserisci il nome qui", // 🔥 Nuovo parametro obbligatorio
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
            	    titoloField.getText(), // 🔥 AGGIUNGI QUESTO - il nome dell'oggetto
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
     * @param oggetto Oggetto associato all'annuncio
     * @param prezzo Prezzo dell'annuncio
     * @return Annuncio creato o null se errore
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
            
            // ✅ IMPORTANTE: Imposta il titolo SOLO sull'annuncio
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
     * Log delle informazioni dell'annuncio creato (per debug)
     * @param annuncio Annuncio creato
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
     * @param message Messaggio da visualizzare
     */
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}