package schermata.button;

import application.Classe.Annuncio;
import schermata.button.CarrelloManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.List;

public class CarrelloDialog extends Dialog<Void> {
    
    private final CarrelloManager carrelloManager;
    private TableView<CarrelloManager.CarrelloItem> tableView;
    private ObservableList<CarrelloManager.CarrelloItem> carrelloItems;
    private Text txtTotale;
    private Text txtNumeroArticoli;
    private Text txtSaldo;

    public CarrelloDialog() {
        this.carrelloManager = CarrelloManager.getInstance();
        
        setTitle("Carrello Acquisti");
        setHeaderText("I tuoi articoli nel carrello");
        
        // Pulsante di chiusura
        ButtonType chiudiButtonType = new ButtonType("Chiudi", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(chiudiButtonType);
        
        // Creazione interfaccia
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // Tabella carrello
        tableView = new TableView<>();
        setupTable();
        
        // Pulsanti
        HBox pulsantiBox = createPulsantiBox();
        
        // Informazioni totali
        HBox infoBox = createInfoBox();
        
        // Sezione checkout
        HBox checkoutBox = createCheckoutSection();
        
        content.getChildren().addAll(tableView, pulsantiBox, infoBox, checkoutBox);
        getDialogPane().setContent(content);
        
        // Carica dati
        aggiornaDatiCarrello();
    }

    private void setupTable() {
        // Colonna titolo
        TableColumn<CarrelloManager.CarrelloItem, String> colonnaTitolo = new TableColumn<>("Articolo");
        colonnaTitolo.setCellValueFactory(new PropertyValueFactory<>("titolo"));
        colonnaTitolo.setPrefWidth(250);
        
        // Colonna prezzo
        TableColumn<CarrelloManager.CarrelloItem, Double> colonnaPrezzo = new TableColumn<>("Prezzo");
        colonnaPrezzo.setCellValueFactory(new PropertyValueFactory<>("prezzo"));
        colonnaPrezzo.setPrefWidth(100);
        colonnaPrezzo.setCellFactory(tc -> new TableCell<CarrelloManager.CarrelloItem, Double>() {
            @Override
            protected void updateItem(Double prezzo, boolean empty) {
                super.updateItem(prezzo, empty);
                if (empty || prezzo == null) {
                    setText(null);
                } else {
                    setText(String.format("€%.2f", prezzo));
                }
            }
        });
        
        // Colonna quantità
        TableColumn<CarrelloManager.CarrelloItem, Integer> colonnaQuantita = new TableColumn<>("Quantità");
        colonnaQuantita.setCellValueFactory(new PropertyValueFactory<>("quantita"));
        colonnaQuantita.setPrefWidth(80);
        
        // Colonna subtotale
        TableColumn<CarrelloManager.CarrelloItem, Double> colonnaSubtotale = new TableColumn<>("Subtotale");
        colonnaSubtotale.setPrefWidth(100);
        colonnaSubtotale.setCellFactory(tc -> new TableCell<CarrelloManager.CarrelloItem, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    CarrelloManager.CarrelloItem carrelloItem = getTableView().getItems().get(getIndex());
                    double subtotale = carrelloItem.getPrezzo() * carrelloItem.getQuantita();
                    setText(String.format("€%.2f", subtotale));
                }
            }
        });
        
        tableView.getColumns().addAll(colonnaTitolo, colonnaPrezzo, colonnaQuantita, colonnaSubtotale);
        tableView.setPlaceholder(new Label("Il carrello è vuoto"));
    }

    private HBox createPulsantiBox() {
        Button rimuoviBtn = new Button("Rimuovi Selezionato");
        rimuoviBtn.setOnAction(e -> rimuoviArticoloSelezionato());
        
        Button svuotaBtn = new Button("Svuota Carrello");
        svuotaBtn.setOnAction(e -> svuotaCarrello());
        
        HBox pulsantiBox = new HBox(10, rimuoviBtn, svuotaBtn);
        pulsantiBox.setPadding(new Insets(10, 0, 0, 0));
        return pulsantiBox;
    }

    private HBox createInfoBox() {
        txtNumeroArticoli = new Text();
        txtTotale = new Text();
        txtSaldo = new Text();
        
        VBox infoBox = new VBox(5, 
            new Label("Riepilogo:"),
            txtNumeroArticoli,
            txtTotale,
            txtSaldo
        );
        infoBox.setStyle("-fx-border-color: #cccccc; -fx-border-width: 1; -fx-padding: 10;");
        
        HBox container = new HBox(infoBox);
        container.setPadding(new Insets(10, 0, 0, 0));
        HBox.setHgrow(container, Priority.ALWAYS);
        return container;
    }

    private HBox createCheckoutSection() {
        Button checkoutBtn = new Button("🚀 Checkout & Acquista");
        checkoutBtn.setStyle(
            "-fx-background-color: #27ae60; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 14px; " +
            "-fx-padding: 10px 20px;"
        );
        checkoutBtn.setOnAction(e -> {
            CheckoutDialog checkoutDialog = new CheckoutDialog();
            Boolean success = checkoutDialog.showAndWait().orElse(false);
            
            if (success != null && success) {
                // Ricarica il carrello vuoto
                aggiornaDatiCarrello();
                mostraMessaggioSuccesso("Acquisto completato! I fondi sono stati trasferiti ai venditori.");
            }
        });
        
        HBox checkoutBox = new HBox(checkoutBtn);
        checkoutBox.setAlignment(Pos.CENTER_RIGHT);
        checkoutBox.setPadding(new Insets(15, 0, 0, 0));
        
        return checkoutBox;
    }

    private void aggiornaDatiCarrello() {
        List<CarrelloManager.CarrelloItem> items = carrelloManager.getCarrelloItems();
        carrelloItems = FXCollections.observableArrayList(items);
        tableView.setItems(carrelloItems);
        
        // Aggiorna i totali
        int numeroArticoli = carrelloManager.getNumeroArticoli();
        double totale = carrelloManager.getTotale();
        
        txtNumeroArticoli.setText("Numero articoli: " + numeroArticoli);
        txtTotale.setText("Totale: €" + String.format("%.2f", totale));
        txtSaldo.setText("Saldo attuale: " + carrelloManager.getSaldoFormattato());
    }

    private void rimuoviArticoloSelezionato() {
        CarrelloManager.CarrelloItem selected = tableView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            carrelloManager.rimuoviDalCarrello(selected.getAnnuncioId());
            aggiornaDatiCarrello();
        } else {
            mostraMessaggioErrore("Nessun articolo selezionato");
        }
    }

    private void svuotaCarrello() {
        carrelloManager.svuotaCarrello();
        aggiornaDatiCarrello();
    }

    private void mostraMessaggioSuccesso(String messaggio) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Successo");
        alert.setHeaderText(null);
        alert.setContentText(messaggio);
        alert.showAndWait();
    }

    private void mostraMessaggioErrore(String messaggio) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore");
        alert.setHeaderText(null);
        alert.setContentText(messaggio);
        alert.showAndWait();
    }
}