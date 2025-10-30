package schermata.button;

import schermata.button.CarrelloManager;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.math.BigDecimal;

/**
 * Dialog per il checkout che mostra il riepilogo dell'ordine
 * e gestisce la conferma dell'acquisto degli articoli selezionati
 */
public class CheckoutDialog extends Dialog<Boolean> {
    
    private final CarrelloManager carrelloManager;
    
    /**
     * Costruttore che inizializza il dialog di checkout con tutti i componenti UI
     */
    public CheckoutDialog() {
        this.carrelloManager = CarrelloManager.getInstance();
        
        initializeDialogProperties();
        setupButtons();
        setupContent();
    }
    
    /**
     * Inizializza le proprietà base del dialog
     */
    private void initializeDialogProperties() {
        setTitle("Checkout - Conferma Acquisto");
        setHeaderText("Riepilogo del tuo ordine");
        
        // Debug per verificare lo stato della selezione
        carrelloManager.debugSelezione();
    }
    
    /**
     * Configura i pulsanti del dialog
     */
    private void setupButtons() {
        ButtonType confermaButtonType = new ButtonType("Conferma Acquisto", ButtonBar.ButtonData.OK_DONE);
        ButtonType annullaButtonType = new ButtonType("Annulla", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(confermaButtonType, annullaButtonType);
    }
    
    /**
     * Configura il contenuto principale del dialog
     */
    private void setupContent() {
        GridPane grid = createMainGrid();
        addContentToGrid(grid);
        getDialogPane().setContent(grid);
        setupResultConverter();
    }
    
    /**
     * Crea e configura il grid layout principale
     */
    private GridPane createMainGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        return grid;
    }
    
    /**
     * Aggiunge tutti i componenti UI al grid layout
     */
    private void addContentToGrid(GridPane grid) {
        // Titolo principale
        Text titolo = createTitleText();
        grid.add(titolo, 0, 0, 2, 1);
        grid.add(new Separator(), 0, 1, 2, 1);
        
        // Calcola i dati finanziari
        CheckoutData data = calculateCheckoutData();
        
        // Aggiungi i componenti al grid
        addCheckoutInfoToGrid(grid, data);
        addFinancialInfoToGrid(grid, data);
        addStatusMessageToGrid(grid, data);
        
        // Configura il pulsante di conferma
        configureConfirmButton(data);
    }
    
    /**
     * Crea il testo del titolo principale
     */
    private Text createTitleText() {
        Text titolo = new Text("Riepilogo Ordine");
        titolo.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        return titolo;
    }
    
    /**
     * Calcola tutti i dati necessari per il checkout
     */
    private CheckoutData calculateCheckoutData() {
        double totaleSelezionati = carrelloManager.getTotaleSelezionati();
        int numeroArticoliSelezionati = carrelloManager.getNumeroArticoliSelezionati();
        BigDecimal saldoAttuale = carrelloManager.getSaldoUtente();
        BigDecimal saldoDopo = saldoAttuale.subtract(BigDecimal.valueOf(totaleSelezionati));
        boolean puòAcquistare = carrelloManager.puòAcquistareSelezionati();
        int articoliTotali = carrelloManager.getNumeroArticoli();
        
        return new CheckoutData(totaleSelezionati, numeroArticoliSelezionati, 
                              saldoAttuale, saldoDopo, puòAcquistare, articoliTotali);
    }
    
    /**
     * Aggiunge le informazioni sugli articoli al grid
     */
    private void addCheckoutInfoToGrid(GridPane grid, CheckoutData data) {
        // Informazioni quantità articoli
        Label lblNumArticoli = new Label("Numero articoli:");
        Label lblNumArticoliValore = new Label(String.valueOf(data.numeroArticoliSelezionati));
        grid.add(lblNumArticoli, 0, 2);
        grid.add(lblNumArticoliValore, 1, 2);
        
        // Informazioni selezione
        Label lblSelezioneInfo = createSelectionInfoLabel(data);
        grid.add(lblSelezioneInfo, 0, 3, 2, 1);
    }
    
    /**
     * Crea la label che mostra informazioni sulla selezione degli articoli
     */
    private Label createSelectionInfoLabel(CheckoutData data) {
        Label lblSelezioneInfo = new Label();
        
        if (data.numeroArticoliSelezionati < data.articoliTotali) {
            lblSelezioneInfo.setText("(Selezionati " + data.numeroArticoliSelezionati + " di " + data.articoliTotali + " articoli)");
            lblSelezioneInfo.setStyle("-fx-text-fill: #f39c12;");
        } else {
            lblSelezioneInfo.setText("(Tutti gli articoli selezionati)");
            lblSelezioneInfo.setStyle("-fx-text-fill: #27ae60;");
        }
        
        return lblSelezioneInfo;
    }
    
    /**
     * Aggiunge le informazioni finanziarie al grid
     */
    private void addFinancialInfoToGrid(GridPane grid, CheckoutData data) {
        // Totale ordine
        Label lblTotale = new Label("Totale ordine:");
        Label lblTotaleValore = new Label(carrelloManager.getTotaleSelezionatiFormattato());
        lblTotaleValore.setStyle("-fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        
        // Saldo attuale
        Label lblSaldo = new Label("Il tuo saldo:");
        Label lblSaldoValore = new Label(carrelloManager.getSaldoFormattato());
        
        // Saldo dopo l'acquisto
        Label lblDopoAcquisto = new Label("Saldo dopo l'acquisto:");
        Label lblDopoAcquistoValore = createPostPurchaseLabel(data.saldoDopo);
        
        // Aggiungi al grid
        grid.add(lblTotale, 0, 4);
        grid.add(lblTotaleValore, 1, 4);
        grid.add(lblSaldo, 0, 5);
        grid.add(lblSaldoValore, 1, 5);
        grid.add(lblDopoAcquisto, 0, 6);
        grid.add(lblDopoAcquistoValore, 1, 6);
    }
    
    /**
     * Crea la label per il saldo dopo l'acquisto con styling appropriato
     */
    private Label createPostPurchaseLabel(BigDecimal saldoDopo) {
        Label label = new Label(String.format("€%.2f", saldoDopo));
        String style = saldoDopo.compareTo(BigDecimal.ZERO) >= 0 ? 
            "-fx-text-fill: #27ae60;" : "-fx-text-fill: #e74c3c;";
        label.setStyle(style);
        return label;
    }
    
    /**
     * Aggiunge il messaggio di stato al grid
     */
    private void addStatusMessageToGrid(GridPane grid, CheckoutData data) {
        Text messaggioStato = new Text(carrelloManager.getStatoCheckoutSelezionati());
        String style = data.puòAcquistare ? 
            "-fx-fill: #27ae60; -fx-font-weight: bold;" : 
            "-fx-fill: #e74c3c; -fx-font-weight: bold;";
        messaggioStato.setStyle(style);
        grid.add(messaggioStato, 0, 7, 2, 1);
    }
    
    /**
     * Configura il pulsante di conferma in base allo stato del checkout
     */
    private void configureConfirmButton(CheckoutData data) {
        Button confermaButton = (Button) getDialogPane().lookupButton(
            getDialogPane().getButtonTypes().stream()
                .filter(buttonType -> buttonType.getButtonData() == ButtonBar.ButtonData.OK_DONE)
                .findFirst()
                .orElse(null)
        );
        
        if (confermaButton != null) {
            boolean canProceed = data.puòAcquistare && data.numeroArticoliSelezionati > 0;
            confermaButton.setDisable(!canProceed);
            
            if (!canProceed) {
                setupConfirmButtonTooltip(confermaButton, data);
            }
        }
    }
    
    /**
     * Configura il tooltip per il pulsante di conferma quando disabilitato
     */
    private void setupConfirmButtonTooltip(Button confermaButton, CheckoutData data) {
        String motivo = data.numeroArticoliSelezionati == 0 ? 
            "Nessun articolo selezionato" : 
            "Saldo insufficiente per gli articoli selezionati";
        confermaButton.setTooltip(new Tooltip(motivo));
    }
    
    /**
     * Configura il converter per il risultato del dialog
     */
    private void setupResultConverter() {
        setResultConverter(dialogButton -> {
            if (dialogButton.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                return carrelloManager.checkoutSelezionati();
            }
            return false;
        });
    }
    
    /**
     * Classe interna per incapsulare i dati del checkout
     */
    private static class CheckoutData {
        public final double totaleSelezionati;
        public final int numeroArticoliSelezionati;
        public final BigDecimal saldoAttuale;
        public final BigDecimal saldoDopo;
        public final boolean puòAcquistare;
        public final int articoliTotali;
        
        public CheckoutData(double totaleSelezionati, int numeroArticoliSelezionati,
                          BigDecimal saldoAttuale, BigDecimal saldoDopo,
                          boolean puòAcquistare, int articoliTotali) {
            this.totaleSelezionati = totaleSelezionati;
            this.numeroArticoliSelezionati = numeroArticoliSelezionati;
            this.saldoAttuale = saldoAttuale;
            this.saldoDopo = saldoDopo;
            this.puòAcquistare = puòAcquistare;
            this.articoliTotali = articoliTotali;
        }
    }
}