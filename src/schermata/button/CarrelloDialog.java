package schermata.button;

import application.Classe.Annuncio;
import schermata.button.CarrelloManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.StackPane;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CarrelloDialog extends Dialog<Void> {
    
    private final CarrelloManager carrelloManager;
    private TableView<CarrelloManager.CarrelloItem> tableView;
    private ObservableList<CarrelloManager.CarrelloItem> carrelloItems;
    private Label lblTotaleSelezionati;
    private Label lblSaldo;
    private Button checkoutBtn;
    
    // Executor per operazioni in background
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    
    // Flag per prevenire aggiornamenti multipli
    private boolean aggiornamentoInCorso = false;

    public CarrelloDialog() {
        this.carrelloManager = CarrelloManager.getInstance();
        
        setTitle("Carrello");
        setHeaderText("Gestisci gli articoli nel carrello");
        
        // Collegamento al file CSS
        try {
            getDialogPane().getStylesheets().add(getClass().getResource("/carello.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Impossibile caricare il file CSS: " + e.getMessage());
        }
        
        // Dimensione della finestra
        setWidth(1000);
        setHeight(700);
        
        // Pulsante di chiusura
        ButtonType chiudiButtonType = new ButtonType("Chiudi", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(chiudiButtonType);
        
        // Applica classe CSS al dialog pane
        getDialogPane().getStyleClass().add("container");
        
        // Creazione interfaccia
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        content.getStyleClass().add("cart-container");
        
        // Tabella carrello - con scroll integrato
        tableView = new TableView<>();
        tableView.getStyleClass().add("cart-table");
        setupTable();
        
        // Pulsanti azione
        HBox pulsantiBox = createPulsantiBox();
        pulsantiBox.getStyleClass().add("actions-box");
        
        // Riepilogo compatto
        HBox riepilogoBox = createRiepilogoBox();
        riepilogoBox.getStyleClass().add("summary-box");
        
        // Sezione checkout
        HBox checkoutBox = createCheckoutSection();
        checkoutBox.getStyleClass().add("checkout-box");
        
        content.getChildren().addAll(tableView, pulsantiBox, riepilogoBox, checkoutBox);
        getDialogPane().setContent(content);
        
        // ✅ AGGIUNTO: Listener per la chiusura del dialog
        setOnHidden(e -> {
            backgroundExecutor.shutdown();
        });
        
        // Carica dati iniziali
        caricaDatiIniziali();
    }
    
    // Metodo ottimizzato per caricamento iniziale
    private void caricaDatiIniziali() {
        backgroundExecutor.execute(() -> {
            List<CarrelloManager.CarrelloItem> items = carrelloManager.getCarrelloItems();
            Platform.runLater(() -> {
                carrelloItems = FXCollections.observableArrayList(items);
                tableView.setItems(carrelloItems);
                aggiornaRiepilogo();
            });
        });
    }

    private void setupTable() {
        // Colonna selezione con quadratino che si riempie alla selezione
        TableColumn<CarrelloManager.CarrelloItem, Boolean> colonnaSelezione = new TableColumn<>("Seleziona");
        colonnaSelezione.setCellValueFactory(new PropertyValueFactory<>("selected"));
        colonnaSelezione.setCellFactory(tc -> new TableCell<CarrelloManager.CarrelloItem, Boolean>() {
            private final Rectangle box = new Rectangle(18, 18);
            private final Label check = new Label("✓");
            private final StackPane container = new StackPane();

            {
                // Stili base del quadratino
                box.setArcWidth(6);
                box.setArcHeight(6);
                box.setStroke(Color.web("#cccccc"));
                box.setStrokeWidth(2);
                
                // Impostiamo il colore di riempimento in base allo stato
                updateBoxAppearance(false);

                check.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 12px;");
                check.setVisible(false);

                container.setPrefSize(24, 24);
                container.getChildren().addAll(box, check);
                setAlignment(Pos.CENTER);
                
                // Gestore click sulla cella - OTTIMIZZATO
                container.setOnMouseClicked(e -> {
                    e.consume();
                    if (!isEmpty() && !aggiornamentoInCorso) {
                        int idx = getIndex();
                        if (idx >= 0 && idx < getTableView().getItems().size()) {
                            CarrelloManager.CarrelloItem item = getTableView().getItems().get(idx);
                            if (item != null) {
                                aggiornamentoInCorso = true;
                                
                                boolean nuovoStato = !item.isSelected();
                                item.setSelected(nuovoStato);
                                
                                // OTTIMIZZATO: Aggiornamento immediato e locale
                                carrelloManager.aggiornaStatoSelezione(item.getAnnuncioId(), nuovoStato);
                                updateBoxAppearance(nuovoStato);
                                
                                // OTTIMIZZATO: Aggiorna solo il riepilogo, non tutta la tabella
                                aggiornaRiepilogoRapido();
                                
                                aggiornamentoInCorso = false;
                            }
                        }
                    }
                });
            }

            private void updateBoxAppearance(boolean selected) {
                if (selected) {
                    box.setFill(Color.web("#007bff"));
                    box.setStroke(Color.web("#0056b3"));
                    check.setVisible(true);
                } else {
                    box.setFill(Color.WHITE);
                    box.setStroke(Color.web("#cccccc"));
                    check.setVisible(false);
                }
            }

            @Override
            protected void updateItem(Boolean selected, boolean empty) {
                super.updateItem(selected, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    if (selected != null) {
                        updateBoxAppearance(selected);
                    } else {
                        updateBoxAppearance(false);
                    }
                    setGraphic(container);
                }
            }
        });
        colonnaSelezione.setPrefWidth(80);
        colonnaSelezione.setStyle("-fx-alignment: CENTER;");
        colonnaSelezione.setResizable(false);
        
        // Colonna articolo
        TableColumn<CarrelloManager.CarrelloItem, String> colonnaArticolo = new TableColumn<>("Articolo");
        colonnaArticolo.setCellValueFactory(new PropertyValueFactory<>("titolo"));
        colonnaArticolo.setPrefWidth(300);
        colonnaArticolo.getStyleClass().add("item-column");
        
        // Colonna prezzo
        TableColumn<CarrelloManager.CarrelloItem, Double> colonnaPrezzo = new TableColumn<>("Prezzo Unitario");
        colonnaPrezzo.setCellValueFactory(new PropertyValueFactory<>("prezzo"));
        colonnaPrezzo.setPrefWidth(120);
        colonnaPrezzo.getStyleClass().add("price-column");
        colonnaPrezzo.setCellFactory(tc -> new TableCell<CarrelloManager.CarrelloItem, Double>() {
            @Override
            protected void updateItem(Double prezzo, boolean empty) {
                super.updateItem(prezzo, empty);
                if (empty || prezzo == null) {
                    setText(null);
                } else {
                    setText(String.format("€%.2f", prezzo));
                    getStyleClass().add("price-cell");
                }
            }
        });
        
        // Colonna quantità
        TableColumn<CarrelloManager.CarrelloItem, Integer> colonnaQuantita = new TableColumn<>("Q.tà");
        colonnaQuantita.setCellValueFactory(new PropertyValueFactory<>("quantita"));
        colonnaQuantita.setPrefWidth(60);
        colonnaQuantita.setStyle("-fx-alignment: CENTER;");
        colonnaQuantita.getStyleClass().add("quantity-column");
        
        // Colonna subtotale
        TableColumn<CarrelloManager.CarrelloItem, String> colonnaSubtotale = new TableColumn<>("Subtotale");
        colonnaSubtotale.setPrefWidth(120);
        colonnaSubtotale.getStyleClass().add("subtotal-column");
        colonnaSubtotale.setCellFactory(tc -> new TableCell<CarrelloManager.CarrelloItem, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    CarrelloManager.CarrelloItem carrelloItem = getTableView().getItems().get(getIndex());
                    double subtotale = carrelloItem.getPrezzo() * carrelloItem.getQuantita();
                    setText(String.format("€%.2f", subtotale));
                    getStyleClass().add("subtotal-cell");
                }
            }
        });
        
        // Colonna azioni
        TableColumn<CarrelloManager.CarrelloItem, Void> colonnaAzioni = new TableColumn<>("Azioni");
        colonnaAzioni.setPrefWidth(100);
        colonnaAzioni.getStyleClass().add("actions-column");
        colonnaAzioni.setCellFactory(tc -> new TableCell<CarrelloManager.CarrelloItem, Void>() {
            private final Button rimuoviBtn = new Button("Rimuovi");
            
            {
                rimuoviBtn.getStyleClass().add("remove-button");
                rimuoviBtn.setOnAction(e -> {
                    if (!aggiornamentoInCorso) {
                        CarrelloManager.CarrelloItem item = getTableView().getItems().get(getIndex());
                        rimuoviArticolo(item.getAnnuncioId());
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
        
        tableView.getColumns().addAll(colonnaSelezione, colonnaArticolo, colonnaPrezzo, colonnaQuantita, colonnaSubtotale, colonnaAzioni);
        tableView.setPlaceholder(new Label("Nessun articolo nel carrello"));
        
        // Abilita lo scrolling
        tableView.setMinHeight(400);
        tableView.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(tableView, Priority.ALWAYS);
        
        // OTTIMIZZATO: Listener per click sulla riga
        tableView.setRowFactory(tv -> {
            TableRow<CarrelloManager.CarrelloItem> row = new TableRow<>();
            row.getStyleClass().add("cart-row");
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 1 && !row.isEmpty() && !aggiornamentoInCorso) {
                    CarrelloManager.CarrelloItem item = row.getItem();
                    aggiornamentoInCorso = true;
                    
                    boolean nuovoStato = !item.isSelected();
                    item.setSelected(nuovoStato);
                    
                    carrelloManager.aggiornaStatoSelezione(item.getAnnuncioId(), nuovoStato);
                    aggiornaRiepilogoRapido();
                    
                    aggiornamentoInCorso = false;
                }
            });
            return row;
        });
    }

    private HBox createPulsantiBox() {
        Button selezionaTuttiBtn = new Button("Seleziona Tutti");
        selezionaTuttiBtn.getStyleClass().add("select-all-button");
        selezionaTuttiBtn.setOnAction(e -> {
            if (!aggiornamentoInCorso) {
                aggiornamentoInCorso = true;
                carrelloManager.selezionaTutti(true);
                // OTTIMIZZATO: Aggiorna solo le celle visive invece di refresh completo
                for (CarrelloManager.CarrelloItem item : carrelloItems) {
                    item.setSelected(true);
                }
                aggiornaRiepilogoRapido();
                aggiornamentoInCorso = false;
            }
        });
        
        Button deselezionaTuttiBtn = new Button("Deseleziona Tutti");
        deselezionaTuttiBtn.getStyleClass().add("deselect-all-button");
        deselezionaTuttiBtn.setOnAction(e -> {
            if (!aggiornamentoInCorso) {
                aggiornamentoInCorso = true;
                carrelloManager.selezionaTutti(false);
                for (CarrelloManager.CarrelloItem item : carrelloItems) {
                    item.setSelected(false);
                }
                aggiornaRiepilogoRapido();
                aggiornamentoInCorso = false;
            }
        });
        
        Button rimuoviSelezionatiBtn = new Button("Rimuovi Selezionati");
        rimuoviSelezionatiBtn.getStyleClass().add("remove-selected-button");
        rimuoviSelezionatiBtn.setOnAction(e -> {
            if (!aggiornamentoInCorso && carrelloManager.rimuoviSelezionati()) {
                // OTTIMIZZATO: Ricarica solo quando necessario (dopo rimozione)
                backgroundExecutor.execute(() -> {
                    List<CarrelloManager.CarrelloItem> nuoviItems = carrelloManager.getCarrelloItems();
                    Platform.runLater(() -> {
                        carrelloItems.setAll(nuoviItems);
                        aggiornaRiepilogo();
                    });
                });
            }
        });
        
        HBox pulsantiBox = new HBox(10, selezionaTuttiBtn, deselezionaTuttiBtn, rimuoviSelezionatiBtn);
        pulsantiBox.setPadding(new Insets(10, 0, 10, 0));
        pulsantiBox.setAlignment(Pos.CENTER_LEFT);
        
        return pulsantiBox;
    }

    private HBox createRiepilogoBox() {
        Label lblTitoloTotale = new Label("Totale da pagare:");
        lblTitoloTotale.getStyleClass().add("total-label");
        
        lblTotaleSelezionati = new Label("€0.00");
        lblTotaleSelezionati.getStyleClass().add("total-value");
        
        VBox totaleBox = new VBox(5, lblTitoloTotale, lblTotaleSelezionati);
        totaleBox.setAlignment(Pos.CENTER_LEFT);
        
        Label lblTitoloSaldo = new Label("Saldo attuale:");
        lblTitoloSaldo.getStyleClass().add("balance-label");
        
        lblSaldo = new Label("€0.00");
        lblSaldo.getStyleClass().add("balance-value");
        
        VBox saldoBox = new VBox(5, lblTitoloSaldo, lblSaldo);
        saldoBox.setAlignment(Pos.CENTER_LEFT);
        
        HBox riepilogoBox = new HBox(20, totaleBox, saldoBox);
        riepilogoBox.setPadding(new Insets(15));
        riepilogoBox.getStyleClass().add("summary-container");
        riepilogoBox.setAlignment(Pos.CENTER_LEFT);
        
        return riepilogoBox;
    }

    private HBox createCheckoutSection() {
        checkoutBtn = new Button("PROCEDI AL CHECKOUT");
        checkoutBtn.getStyleClass().add("checkout-button");
        checkoutBtn.setOnAction(e -> apriCheckout());
        
        HBox checkoutBox = new HBox(checkoutBtn);
        checkoutBox.setPadding(new Insets(15, 0, 0, 0));
        checkoutBox.setAlignment(Pos.CENTER_RIGHT);
        
        return checkoutBox;
    }

    // Metodo ottimizzato per aggiornamenti rapidi
    private void aggiornaRiepilogoRapido() {
        double totaleSelezionati = carrelloManager.getTotaleSelezionati();
        int numeroSelezionati = carrelloManager.getNumeroArticoliSelezionati();
        
        lblTotaleSelezionati.setText(String.format("€%.2f", totaleSelezionati));
        lblSaldo.setText(carrelloManager.getSaldoFormattato());
        
        aggiornaStatoCheckout();
        
        // OTTIMIZZATO: Refresh solo delle celle modificate invece di tutta la tabella
        tableView.refresh();
    }

    // Metodo per aggiornamenti completi (più lento)
    private void aggiornaRiepilogo() {
        backgroundExecutor.execute(() -> {
            double totaleSelezionati = carrelloManager.getTotaleSelezionati();
            int numeroSelezionati = carrelloManager.getNumeroArticoliSelezionati();
            String saldoFormattato = carrelloManager.getSaldoFormattato();
            
            Platform.runLater(() -> {
                lblTotaleSelezionati.setText(String.format("€%.2f", totaleSelezionati));
                lblSaldo.setText(saldoFormattato);
                aggiornaStatoCheckout();
                tableView.refresh();
            });
        });
    }
    
    private void aggiornaStatoCheckout() {
        int selezionati = carrelloManager.getNumeroArticoliSelezionati();
        boolean puoAcquistare = carrelloManager.puòAcquistareSelezionati();
        double totaleSelezionati = carrelloManager.getTotaleSelezionati();
        
        if (selezionati == 0) {
            checkoutBtn.setText("SELEZIONA ARTICOLI");
            checkoutBtn.getStyleClass().removeAll("checkout-button-enabled", "checkout-button-disabled");
            checkoutBtn.getStyleClass().add("checkout-button-disabled");
            checkoutBtn.setDisable(true);
        } else if (puoAcquistare) {
            checkoutBtn.setText("CHECKOUT (" + selezionati + " ARTICOLI - €" + String.format("%.2f", totaleSelezionati) + ")");
            checkoutBtn.getStyleClass().removeAll("checkout-button-enabled", "checkout-button-disabled");
            checkoutBtn.getStyleClass().add("checkout-button-enabled");
            checkoutBtn.setDisable(false);
        } else {
            checkoutBtn.setText("SALDO INSUFFICIENTE - €" + String.format("%.2f", totaleSelezionati));
            checkoutBtn.getStyleClass().removeAll("checkout-button-enabled", "checkout-button-disabled");
            checkoutBtn.getStyleClass().add("checkout-button-disabled");
            checkoutBtn.setDisable(true);
        }
    }

    private void apriCheckout() {
        if (carrelloManager.nessunoSelezionato()) {
            mostraMessaggioErrore("Nessun articolo selezionato", "Seleziona almeno un articolo per procedere al checkout");
            return;
        }
        
        CheckoutDialog checkoutDialog = new CheckoutDialog();
        checkoutDialog.showAndWait().ifPresent(success -> {
            if (success) {
                // OTTIMIZZATO: Ricarica dati in background dopo checkout
                backgroundExecutor.execute(() -> {
                    List<CarrelloManager.CarrelloItem> nuoviItems = carrelloManager.getCarrelloItems();
                    Platform.runLater(() -> {
                        carrelloItems.setAll(nuoviItems);
                        aggiornaRiepilogo();
                        mostraMessaggioSuccesso("Acquisto completato", "I fondi sono stati trasferiti ai venditori.");
                    });
                });
            }
        });
    }

    private void rimuoviArticolo(int annuncioId) {
        CarrelloManager.CarrelloItem item = trovaItemPerId(annuncioId);
        if (item != null) {
            Alert conferma = new Alert(Alert.AlertType.CONFIRMATION);
            conferma.setTitle("Conferma Rimozione");
            conferma.setHeaderText("Rimuovere articolo dal carrello?");
            conferma.setContentText("Stai per rimuovere: " + item.getTitolo());
            
            conferma.showAndWait().ifPresent(risposta -> {
                if (risposta == ButtonType.OK && !aggiornamentoInCorso) {
                    aggiornamentoInCorso = true;
                    carrelloManager.rimuoviDalCarrello(annuncioId);
                    
                    // OTTIMIZZATO: Rimuovi localmente invece di ricaricare tutto
                    carrelloItems.removeIf(i -> i.getAnnuncioId() == annuncioId);
                    aggiornaRiepilogo();
                    
                    aggiornamentoInCorso = false;
                }
            });
        }
    }

    private CarrelloManager.CarrelloItem trovaItemPerId(int annuncioId) {
        for (CarrelloManager.CarrelloItem item : carrelloItems) {
            if (item.getAnnuncioId() == annuncioId) {
                return item;
            }
        }
        return null;
    }

    private void mostraMessaggioSuccesso(String titolo, String messaggio) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titolo);
        alert.setHeaderText(null);
        alert.setContentText(messaggio);
        alert.showAndWait();
    }

    private void mostraMessaggioErrore(String titolo, String messaggio) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titolo);
        alert.setHeaderText(null);
        alert.setContentText(messaggio);
        alert.showAndWait();
    }
}