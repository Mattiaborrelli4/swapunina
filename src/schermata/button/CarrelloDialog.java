package schermata.button;

import application.Main;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class CarrelloDialog extends Stage {

    private TableView<CarrelloManager.CarrelloItem> tableView;
    private ObservableList<CarrelloManager.CarrelloItem> carrelloItems;
    private Label totaleLabel;

    public CarrelloDialog() {
        initModality(Modality.APPLICATION_MODAL);
        setTitle("Carrello - Marketplace Universitario");
        
        // Inizializza la TableView
        tableView = new TableView<>();
        carrelloItems = FXCollections.observableArrayList();
        tableView.setItems(carrelloItems);
        
        // Creazione colonne
        TableColumn<CarrelloManager.CarrelloItem, String> titoloCol = new TableColumn<>("Prodotto");
        titoloCol.setCellValueFactory(new PropertyValueFactory<>("titolo"));
        titoloCol.setPrefWidth(200);
        
        TableColumn<CarrelloManager.CarrelloItem, Double> prezzoCol = new TableColumn<>("Prezzo");
        prezzoCol.setCellValueFactory(new PropertyValueFactory<>("prezzo"));
        prezzoCol.setPrefWidth(100);
        prezzoCol.setCellFactory(col -> new TableCell<CarrelloManager.CarrelloItem, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("€%.2f", item));
                }
            }
        });
        
        TableColumn<CarrelloManager.CarrelloItem, Integer> quantitaCol = new TableColumn<>("Quantità");
        quantitaCol.setCellValueFactory(new PropertyValueFactory<>("quantita"));
        quantitaCol.setPrefWidth(80);
        
        TableColumn<CarrelloManager.CarrelloItem, Double> subtotaleCol = new TableColumn<>("Subtotale");
        subtotaleCol.setCellFactory(col -> new TableCell<CarrelloManager.CarrelloItem, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    CarrelloManager.CarrelloItem carrelloItem = getTableView().getItems().get(getIndex());
                    double subtotale = carrelloItem.getPrezzo() * carrelloItem.getQuantita();
                    setText(String.format("€%.2f", subtotale));
                }
            }
        });
        subtotaleCol.setPrefWidth(100);
        
        TableColumn<CarrelloManager.CarrelloItem, Void> azioniCol = new TableColumn<>("Azioni");
        azioniCol.setPrefWidth(100);
        azioniCol.setCellFactory(col -> new TableCell<CarrelloManager.CarrelloItem, Void>() {
            private final Button rimuoviBtn = new Button("Rimuovi");
            
            {
                rimuoviBtn.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white;");
                rimuoviBtn.setOnAction(e -> {
                    CarrelloManager.CarrelloItem item = getTableView().getItems().get(getIndex());
                    if (item != null) {
                        CarrelloManager.getInstance().rimuoviDalCarrello(item.getAnnuncioId());
                        carrelloItems.remove(item);
                        aggiornaTotale();
                        mostraAlert("Rimosso", "Articolo rimosso dal carrello!");
                    }
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(rimuoviBtn);
                }
            }
        });
        
        tableView.getColumns().addAll(titoloCol, prezzoCol, quantitaCol, subtotaleCol, azioniCol);
        
        // Label per il totale
        totaleLabel = new Label("Totale: €0.00");
        totaleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2e7d32;");
        
        // Pulsanti
        Button chiudiBtn = new Button("Chiudi");
        chiudiBtn.setStyle("-fx-background-color: #757575; -fx-text-fill: white;");
        chiudiBtn.setOnAction(e -> close());
        
        Button svuotaBtn = new Button("Svuota Carrello");
        svuotaBtn.setStyle("-fx-background-color: #ff9800; -fx-text-fill: white;");
        svuotaBtn.setOnAction(e -> {
            if (!carrelloItems.isEmpty()) {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Svuota Carrello");
                confirmAlert.setHeaderText("Sei sicuro di voler svuotare il carrello?");
                confirmAlert.setContentText("Questa operazione non può essere annullata.");
                
                if (confirmAlert.showAndWait().get() == ButtonType.OK) {
                    CarrelloManager.getInstance().svuotaCarrello();
                    carrelloItems.clear();
                    aggiornaTotale();
                    mostraAlert("Carrello svuotato", "Il carrello è stato svuotato con successo!");
                }
            } else {
                mostraAlert("Carrello vuoto", "Il carrello è già vuoto!");
            }
        });
        
        Button checkoutBtn = new Button("Procedi all'acquisto");
        checkoutBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        checkoutBtn.setOnAction(e -> {
            if (carrelloItems.isEmpty()) {
                mostraAlert("Carrello vuoto", "Il carrello è vuoto! Aggiungi alcuni articoli prima di procedere.");
            } else {
                mostraAlert("Checkout", "Funzionalità di checkout in sviluppo!\nTotale: " + totaleLabel.getText());
                // Qui puoi implementare la logica di checkout
            }
        });
        
        HBox pulsantiBox = new HBox(10, chiudiBtn, svuotaBtn, checkoutBtn);
        pulsantiBox.setAlignment(Pos.CENTER_RIGHT);
        pulsantiBox.setPadding(new Insets(15, 0, 0, 0));
        
        // Header con titolo e contatore articoli
        Label titoloLabel = new Label("Il tuo Carrello");
        titoloLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #333;");
        
        Label contatoreLabel = new Label();
        contatoreLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #666;");
        updateContatoreLabel(contatoreLabel);
        
        VBox headerBox = new VBox(5, titoloLabel, contatoreLabel);
        headerBox.setPadding(new Insets(0, 0, 10, 0));
        
        // Layout principale
        VBox contenuto = new VBox(10, headerBox, tableView, totaleLabel, pulsantiBox);
        contenuto.setPadding(new Insets(20));
        contenuto.setPrefSize(700, 500);
        
        Scene scene = new Scene(contenuto);
        setScene(scene);
        
        // Carica i dati del carrello
        caricaCarrello();
    }
    
    private void caricaCarrello() {
        carrelloItems.clear();
        try {
            java.util.List<CarrelloManager.CarrelloItem> items = CarrelloManager.getInstance().getCarrelloItems();
            if (items != null && !items.isEmpty()) {
                carrelloItems.addAll(items);
                System.out.println("✅ Caricati " + items.size() + " articoli nel carrello");
            } else {
                System.out.println("ℹ️  Carrello vuoto");
            }
        } catch (Exception e) {
            System.err.println("❌ Errore nel caricamento del carrello: " + e.getMessage());
            mostraAlert("Errore", "Impossibile caricare il carrello: " + e.getMessage());
        }
        aggiornaTotale();
    }
    
    private void aggiornaTotale() {
        double totale = 0.0;
        for (CarrelloManager.CarrelloItem item : carrelloItems) {
            totale += item.getPrezzo() * item.getQuantita();
        }
        totaleLabel.setText(String.format("Totale: €%.2f", totale));
        
        // Aggiorna anche il contatore
        Label contatoreLabel = (Label) ((VBox) ((VBox) getScene().getRoot()).getChildren().get(0)).getChildren().get(1);
        updateContatoreLabel(contatoreLabel);
    }
    
    private void updateContatoreLabel(Label contatoreLabel) {
        int numArticoli = carrelloItems.size();
        if (numArticoli == 0) {
            contatoreLabel.setText("Nessun articolo nel carrello");
        } else if (numArticoli == 1) {
            contatoreLabel.setText("1 articolo nel carrello");
        } else {
            contatoreLabel.setText(numArticoli + " articoli nel carrello");
        }
    }
    
    private void mostraAlert(String titolo, String messaggio) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titolo);
        alert.setHeaderText(null);
        alert.setContentText(messaggio);
        alert.showAndWait();
    }
    
    /**
     * Metodo per aggiornare il carrello quando vengono apportate modifiche esterne
     */
    public void aggiornaCarrello() {
        caricaCarrello();
    }
}