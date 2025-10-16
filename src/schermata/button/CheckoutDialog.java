package schermata.button;

import schermata.button.CarrelloManager;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.math.BigDecimal;

public class CheckoutDialog extends Dialog<Boolean> {
    
    private final CarrelloManager carrelloManager;
    
    public CheckoutDialog() {
        this.carrelloManager = CarrelloManager.getInstance();
        
        setTitle("Checkout - Conferma Acquisto");
        setHeaderText("Riepilogo del tuo ordine");
        
        // ✅ DEBUG: Ora funziona perché il metodo esiste
        carrelloManager.debugSelezione();
        
        // Pulsanti
        ButtonType confermaButtonType = new ButtonType("Conferma Acquisto", ButtonBar.ButtonData.OK_DONE);
        ButtonType annullaButtonType = new ButtonType("Annulla", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(confermaButtonType, annullaButtonType);
        
        // Contenuto
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Informazioni checkout
        Text titolo = new Text("Riepilogo Ordine");
        titolo.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        
        // ✅ CORRETTO: Usa i metodi per gli articoli SELEZIONATI
        double totaleSelezionati = carrelloManager.getTotaleSelezionati();
        int numeroArticoliSelezionati = carrelloManager.getNumeroArticoliSelezionati();
        BigDecimal saldoAttuale = carrelloManager.getSaldoUtente();
        BigDecimal saldoDopo = saldoAttuale.subtract(BigDecimal.valueOf(totaleSelezionati));
        
        Label lblTotale = new Label("Totale ordine:");
        Label lblTotaleValore = new Label(carrelloManager.getTotaleSelezionatiFormattato());
        lblTotaleValore.setStyle("-fx-font-weight: bold; -fx-text-fill: #e74c3c;");
        
        Label lblSaldo = new Label("Il tuo saldo:");
        Label lblSaldoValore = new Label(carrelloManager.getSaldoFormattato());
        
        Label lblDopoAcquisto = new Label("Saldo dopo l'acquisto:");
        Label lblDopoAcquistoValore = new Label(String.format("€%.2f", saldoDopo));
        lblDopoAcquistoValore.setStyle(saldoDopo.compareTo(BigDecimal.ZERO) >= 0 ? 
            "-fx-text-fill: #27ae60;" : "-fx-text-fill: #e74c3c;");
        
        Label lblNumArticoli = new Label("Numero articoli:");
        Label lblNumArticoliValore = new Label(String.valueOf(numeroArticoliSelezionati));
        
        // ✅ AGGIUNTO: Indicatore articoli selezionati vs totali
        Label lblSelezioneInfo = new Label();
        int articoliTotali = carrelloManager.getNumeroArticoli();
        if (numeroArticoliSelezionati < articoliTotali) {
            lblSelezioneInfo.setText("(Selezionati " + numeroArticoliSelezionati + " di " + articoliTotali + " articoli)");
            lblSelezioneInfo.setStyle("-fx-text-fill: #f39c12;");
        } else {
            lblSelezioneInfo.setText("(Tutti gli articoli selezionati)");
            lblSelezioneInfo.setStyle("-fx-text-fill: #27ae60;");
        }
        
        // Aggiungi al grid
        grid.add(titolo, 0, 0, 2, 1);
        grid.add(new Separator(), 0, 1, 2, 1);
        grid.add(lblNumArticoli, 0, 2);
        grid.add(lblNumArticoliValore, 1, 2);
        grid.add(lblSelezioneInfo, 0, 3, 2, 1);
        grid.add(lblTotale, 0, 4);
        grid.add(lblTotaleValore, 1, 4);
        grid.add(lblSaldo, 0, 5);
        grid.add(lblSaldoValore, 1, 5);
        grid.add(lblDopoAcquisto, 0, 6);
        grid.add(lblDopoAcquistoValore, 1, 6);
        
        // Messaggio di stato
        Text messaggioStato = new Text(carrelloManager.getStatoCheckoutSelezionati());
        boolean puòAcquistare = carrelloManager.puòAcquistareSelezionati();
        
        messaggioStato.setStyle(
            puòAcquistare ? 
            "-fx-fill: #27ae60; -fx-font-weight: bold;" : 
            "-fx-fill: #e74c3c; -fx-font-weight: bold;"
        );
        grid.add(messaggioStato, 0, 7, 2, 1);
        
        // ✅ AGGIUNTO: Disabilita il pulsante se non può acquistare
        Button confermaButton = (Button) getDialogPane().lookupButton(confermaButtonType);
        confermaButton.setDisable(!puòAcquistare || numeroArticoliSelezionati == 0);
        
        // ✅ AGGIUNTO: Tooltip per spiegare perché il pulsante è disabilitato
        if (!puòAcquistare || numeroArticoliSelezionati == 0) {
            String motivo = numeroArticoliSelezionati == 0 ? 
                "Nessun articolo selezionato" : 
                "Saldo insufficiente per gli articoli selezionati";
            confermaButton.setTooltip(new Tooltip(motivo));
        }
        
        getDialogPane().setContent(grid);
        
        // Converti il risultato
        setResultConverter(dialogButton -> {
            if (dialogButton == confermaButtonType) {
                // ✅ CORRETTO: Usa checkoutSelezionati invece di checkoutCompleto
                return carrelloManager.checkoutSelezionati();
            }
            return false;
        });
    }
}