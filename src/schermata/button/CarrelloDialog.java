package schermata.button;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import java.util.List;
import application.Classe.Annuncio;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.cell.PropertyValueFactory;
import schermata.button.CarrelloManager;

public class CarrelloDialog extends Dialog<Void> {

    private TableView<CarrelloManager.CarrelloItem> tableView;
    private Label totaleLabel;
    private CarrelloManager carrelloManager;
    
    // Definizione dei ButtonType
    private final ButtonType rimuoviButtonType = new ButtonType("Rimuovi", ButtonBar.ButtonData.OTHER);
    private final ButtonType svuotaButtonType = new ButtonType("Svuota", ButtonBar.ButtonData.OTHER);
    private final ButtonType checkoutButtonType = new ButtonType("Checkout", ButtonBar.ButtonData.OK_DONE);

    public CarrelloDialog() {
        this.carrelloManager = CarrelloManager.getInstance();
        
        setTitle("🛒 Il Tuo Carrello");
        setHeaderText("Gestisci i tuoi articoli");
        
        // Imposta dimensioni
        setWidth(700);
        setHeight(500);

        // Aggiungi i ButtonType al DialogPane
        getDialogPane().getButtonTypes().addAll(rimuoviButtonType, svuotaButtonType, checkoutButtonType, ButtonType.CLOSE);

        initializeUI();
        aggiornaContenuto();
    }

    private void initializeUI() {
        // Tabella degli articoli
        tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<CarrelloManager.CarrelloItem, String> nomeCol = new TableColumn<>("Articolo");
        nomeCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getAnnuncio().getOggetto().getNome()));

        TableColumn<CarrelloManager.CarrelloItem, String> venditoreCol = new TableColumn<>("Venditore");
        venditoreCol.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getAnnuncio().getNomeUtenteVenditore())); // Modificato

        TableColumn<CarrelloManager.CarrelloItem, Double> prezzoCol = new TableColumn<>("Prezzo Unitario");
        prezzoCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getAnnuncio().getPrezzo()).asObject());

        TableColumn<CarrelloManager.CarrelloItem, Integer> quantitaCol = new TableColumn<>("Quantità");
        quantitaCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getQuantita()).asObject());
        quantitaCol.setStyle("-fx-alignment: CENTER;");

        TableColumn<CarrelloManager.CarrelloItem, Double> subtotaleCol = new TableColumn<>("Subtotale");
        subtotaleCol.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleDoubleProperty(cellData.getValue().getSubtotale()).asObject());
        subtotaleCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        tableView.getColumns().addAll(nomeCol, venditoreCol, prezzoCol, quantitaCol, subtotaleCol);

        // Etichetta totale
        totaleLabel = new Label();
        totaleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        VBox content = new VBox(10, tableView, totaleLabel);
        content.setPadding(new Insets(15));
        VBox.setVgrow(tableView, Priority.ALWAYS);

        getDialogPane().setContent(content);

        // Setup degli event handlers
        setupEventHandlers();
    }

    private void setupEventHandlers() {
        // Ottieni i pulsanti usando i ButtonType corretti
        Button rimuoviBtn = (Button) getDialogPane().lookupButton(rimuoviButtonType);
        Button svuotaBtn = (Button) getDialogPane().lookupButton(svuotaButtonType);
        Button checkoutBtn = (Button) getDialogPane().lookupButton(checkoutButtonType);
        
        // Stile dei pulsanti
        if (rimuoviBtn != null) {
            rimuoviBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
            rimuoviBtn.setText("❌ Rimuovi");
        }
        
        if (svuotaBtn != null) {
            svuotaBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white;");
            svuotaBtn.setText("🗑️ Svuota Carrello");
        }
        
        if (checkoutBtn != null) {
            checkoutBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
            checkoutBtn.setText("💰 Checkout");
        }

        // Rimozione articolo
        if (rimuoviBtn != null) {
            rimuoviBtn.setOnAction(e -> {
                CarrelloManager.CarrelloItem selected = tableView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    carrelloManager.rimuoviDalCarrello(selected.getAnnuncio());
                    aggiornaContenuto();
                    showAlert("Rimosso", "Articolo rimosso dal carrello!");
                } else {
                    showAlert("Selezione", "Seleziona un articolo da rimuovere.");
                }
            });
        }

        // Svuota carrello
        if (svuotaBtn != null) {
            svuotaBtn.setOnAction(e -> {
                if (!carrelloManager.getCarrello().isEmpty()) {
                    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmAlert.setTitle("Conferma Svuotamento");
                    confirmAlert.setHeaderText("Vuoi svuotare tutto il carrello?");
                    confirmAlert.setContentText("Questa operazione non può essere annullata.");
                    
                    confirmAlert.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            carrelloManager.svuotaCarrello();
                            aggiornaContenuto();
                            showAlert("Carrello", "Carrello svuotato con successo!");
                        }
                    });
                } else {
                    showAlert("Carrello", "Il carrello è già vuoto!");
                }
            });
        }

        // Checkout
        if (checkoutBtn != null) {
            checkoutBtn.setOnAction(e -> {
                if (carrelloManager.getCarrello().isEmpty()) {
                    showAlert("Carrello vuoto", "Aggiungi articoli al carrello prima di procedere.");
                } else {
                    Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                    confirmAlert.setTitle("Conferma Acquisto");
                    confirmAlert.setHeaderText("Confermi l'acquisto?");
                    confirmAlert.setContentText(String.format("Totale: €%.2f", carrelloManager.getTotale()));
                    
                    confirmAlert.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            simulateCheckout();
                        }
                    });
                }
            });
        }
    }

    private void aggiornaContenuto() {
        // Aggiorna la tabella
        tableView.getItems().setAll(carrelloManager.getCarrelloConQuantita());
        
        // Aggiorna il totale
        double totale = carrelloManager.getTotale();
        totaleLabel.setText(String.format("Totale: €%.2f", totale));
        
        // Disabilita pulsanti se il carrello è vuoto
        boolean carrelloVuoto = carrelloManager.getCarrello().isEmpty();
        
        Button rimuoviBtn = (Button) getDialogPane().lookupButton(rimuoviButtonType);
        Button svuotaBtn = (Button) getDialogPane().lookupButton(svuotaButtonType);
        Button checkoutBtn = (Button) getDialogPane().lookupButton(checkoutButtonType);
        
        if (rimuoviBtn != null) rimuoviBtn.setDisable(carrelloVuoto);
        if (svuotaBtn != null) svuotaBtn.setDisable(carrelloVuoto);
        if (checkoutBtn != null) checkoutBtn.setDisable(carrelloVuoto);
    }

    private void simulateCheckout() {
        try {
            showAlert("Processing", "Elaborazione pagamento in corso...");
            Thread.sleep(1500);
            
            Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
            successAlert.setTitle("Acquisto Completato");
            successAlert.setHeaderText("✅ Pagamento effettuato con successo!");
            successAlert.setContentText(String.format(
                "Grazie per l'acquisto!\nTotale: €%.2f\nGli articoli verranno spediti a breve.",
                carrelloManager.getTotale()
            ));
            
            successAlert.showAndWait();
            
            carrelloManager.svuotaCarrello();
            aggiornaContenuto();
            
        } catch (InterruptedException e) {
            showAlert("Errore", "Si è verificato un errore durante il pagamento.");
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}