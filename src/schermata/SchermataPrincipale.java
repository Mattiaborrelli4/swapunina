package schermata;

import application.DB.AnnuncioDAO;
import application.DB.UtentiDAO;
import application.Classe.Annuncio;
import application.Classe.Oggetto;
import application.Enum.Categoria;
import application.Enum.OrigineOggetto;
import application.Enum.Tipologia;
import application.messagistica.ChatListDialog;
import application.messagistica.ChatListScreen;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import schermata.button.InserisciAnnuncioDialog;

import java.sql.SQLException;
import java.util.List;

import application.DB.FilterManager;
import application.DB.OggettoDAO;
import application.DB.SessionManager;

public class SchermataPrincipale extends BorderPane {

    private final TopBar topBar;
    private final CategoryMenu categoryMenu = new CategoryMenu();
    private final ProductGrid productGrid = new ProductGrid();

    private final FilterBar filterBar = new FilterBar();

    private final AnnuncioDAO annuncioDAO = new AnnuncioDAO();
    private List<Annuncio> tuttiGliAnnunci;
    private List<Annuncio> annunciFiltrati;

    private String nomeUtente;
    private Stage palcoscenicoPrincipale;
    private Categoria categoriaSelezionata;
    private Tipologia tipologiaSelezionata;
    private String ordinamento = "recent";
    private String queryRicerca = "";

    public SchermataPrincipale(String nomeUtente, Stage palcoscenicoPrincipale, TopBar topBar) {
        this.nomeUtente = nomeUtente;
        this.palcoscenicoPrincipale = palcoscenicoPrincipale;
        this.topBar = topBar;
        initializeUI();
        setupEventHandlers();
        loadAnnunci();
    }

    private void initializeUI() {
        getStyleClass().add("main-layout");

        VBox contentBox = new VBox(20);
        contentBox.getStyleClass().add("content-area");
        contentBox.getChildren().addAll(
            createHeader(),
            filterBar.getRoot(),
            productGrid.creaProductGrid()
        );

        ScrollPane scrollPane = new ScrollPane(contentBox);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("main-scroll");

        setLeft(categoryMenu.getView());
        setCenter(scrollPane);
    }

    private void setupEventHandlers() {
        // Search handler
        topBar.setOnSearch(this::handleSearch);
        topBar.setOnMessages(this::handleMessages);
        
        // Category filter
        categoryMenu.setOnCategorySelected(categoria -> {
            this.categoriaSelezionata = categoria;
            applyFilters();
        });

        // Type filter
        filterBar.setOnTypeChange(tipologia -> {
            this.tipologiaSelezionata = tipologia;
            applyFilters();
        });

        // Sort handler
        filterBar.setOnSortChange(ordinamento -> {
            this.ordinamento = ordinamento;
            applyFilters();
        });

        // Product actions
        productGrid.setOnDetailsAction(this::showProductDetails);
        productGrid.setOnOfferAction(this::handleOffer);
        
        // ✅ NUOVO: Collega il callback per l'aggiornamento
        productGrid.setOnAnnuncioModificato(this::handleAnnuncioModificato);

        // Insert ad handler
        topBar.setOnInserisciAnnuncio(this::handleInsertAd);
        topBar.setOnMessages(this::handleMessages);
    }

    // ✅ NUOVO METODO: Gestisce l'aggiornamento degli annunci modificati
    private void handleAnnuncioModificato(Annuncio annuncioModificato) {
        System.out.println("🔄 Annuncio modificato ricevuto, aggiorno la card...");
        productGrid.aggiornaCardAnnuncio(annuncioModificato);
    }

    private void handleInsertAd() {
        try {
            UtentiDAO utentiDAO = new UtentiDAO();
            int userId = utentiDAO.getIdDaUsername(nomeUtente);

            if (userId == -1) {
                showError("Utente non trovato!");
                return;
            }

            InserisciAnnuncioDialog dialog = new InserisciAnnuncioDialog(userId);
            dialog.showAndWait().ifPresent(adData -> {
                try {
                	// Prima verifica se l'oggetto esiste già nel DB e recupera il suo ID
                	int oggettoId = OggettoDAO.recuperaIdOggettoEsistente(adData.getOggetto());

                	Oggetto oggetto = new Oggetto(
                	    oggettoId,
                	    adData.getOggetto().getNome(),
                	    adData.getOggetto().getDescrizione(),
                	    adData.getOggetto().getCategoria(),
                	    adData.getOggetto().getImageUrl(),
                	    adData.getOggetto().getImmagine(),
                	    OrigineOggetto.USATO
                	);

                    Annuncio nuovoAnnuncio = new Annuncio(
                        oggetto,
                        adData.getPrezzo(),
                        adData.getTipologia().getDisplayName(), 
                        adData.getModalitaConsegna(),
                        userId
                    );

                    int result = annuncioDAO.inserisciAnnuncioComplessivo(nuovoAnnuncio, userId);
                    
                    if (result > 0) {
                        showSuccess("Annuncio pubblicato con successo!");
                        refresh();
                    } else {
                        showError("Errore durante la pubblicazione dell'annuncio");
                    }
                } catch (Exception ex) {
                    showError("Errore: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });
        } catch (Exception e) {
            showError("Errore nel recupero dell'utente: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadAnnunci() {
        productGrid.mostraLoading();

        Task<List<Annuncio>> loadTask = new Task<>() {
            @Override
            protected List<Annuncio> call() {
                try {
                    AnnuncioDAO dao = new AnnuncioDAO();
                    return dao.getAnnunciAttivi();
                } catch (SQLException e) {
                    e.printStackTrace();
                    throw new RuntimeException("Errore nel caricamento degli annunci", e);
                }
            }

            @Override
            protected void succeeded() {
                try {
                    tuttiGliAnnunci = getValue();
                    System.out.println("Annunci caricati: " + tuttiGliAnnunci.size());
                    applyFilters();
                    productGrid.nascondiLoading();
                } catch (Exception e) {
                    e.printStackTrace();
                    productGrid.mostraErrore("Errore nel filtraggio");
                }
            }

            @Override
            protected void failed() {
                Platform.runLater(() -> {
                    productGrid.mostraErrore("Errore di caricamento");
                    showError("Impossibile caricare gli annunci: " + getException().getMessage());
                });
            }
        };

        new Thread(loadTask).start();
    }

    private void applyFilters() {
        if (tuttiGliAnnunci == null) {
            System.out.println("[DEBUG] tuttiGliAnnunci è null");
            return;
        }

        System.out.println("[DEBUG] Annunci da filtrare: " + tuttiGliAnnunci.size());
        System.out.println("[DEBUG] Filtri applicati: " 
            + "Categoria=" + categoriaSelezionata
            + ", Tipologia=" + tipologiaSelezionata
            + ", Query=" + queryRicerca
            + ", Ordinamento=" + ordinamento);

        // Utilizza il FilterManager per applicare i filtri
        annunciFiltrati = FilterManager.applicaFiltri(
            tuttiGliAnnunci, 
            categoriaSelezionata, 
            tipologiaSelezionata, 
            queryRicerca, 
            ordinamento
        );

        System.out.println("[DEBUG] Annunci filtrati: " + annunciFiltrati.size());
        
        if (annunciFiltrati.isEmpty()) {
            System.out.println("[DEBUG] Nessun annuncio supera i filtri");
            productGrid.mostraStatoVuoto();
        } else {
            productGrid.aggiornaAnnunci(annunciFiltrati);
        }
        
        // Aggiorna il conteggio nella filterBar
        int count = FilterManager.contaAnnunciFiltrati(
            tuttiGliAnnunci, 
            categoriaSelezionata, 
            tipologiaSelezionata, 
            queryRicerca
        );
        filterBar.updateCount(count);
    }

    private void handleSearch(String query) {
        this.queryRicerca = query;
        applyFilters();
    }

    private void showProductDetails(Annuncio annuncio) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(annuncio.getOggetto().getNome());
        alert.setHeaderText("Prezzo: €" + String.format("%.2f", annuncio.getPrezzo()));
        alert.setContentText(
            "Categoria: " + annuncio.getOggetto().getCategoria().toString() + "\n" +
            "Tipologia: " + annuncio.getTipologia().toString() + "\n\n" +
            "Descrizione:\n" + annuncio.getOggetto().getDescrizione()
        );
        alert.showAndWait();
    }

    private void handleOffer(Annuncio annuncio) {
        showSuccess("Hai inviato un'offerta per: " + annuncio.getOggetto().getNome());
    }

    private Node createHeader() {
        VBox header = new VBox(10);
        header.getStyleClass().add("header-section");

        Text title = new Text("Marketplace Universitario");
        title.getStyleClass().add("main-title");

        Text subtitle = new Text("Scambia libri, dispositivi e materiale didattico");
        subtitle.getStyleClass().add("main-subtitle");

        header.getChildren().addAll(title, subtitle);
        return header;
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Errore");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Successo");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void refresh() {
        loadAnnunci();
    }

    public Categoria getCategoria() {
        return categoriaSelezionata;
    }
    

    // Metodo handleMessages nella schermata principale
    private void handleMessages() {
        try {
            // Verifica se l'utente è loggato
            if (SessionManager.getCurrentUserId() == -1) {
                showAlert("Accesso richiesto", "Devi effettuare l'accesso per visualizzare le chat");
                return;
            }
            
            // Apri la lista delle chat
            ChatListDialog chatListDialog = new ChatListDialog();
            chatListDialog.show();
        } catch (Exception e) {
            showError("Impossibile aprire le chat: " + e.getMessage());
        }
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            
            // Applica stili consistenti con il tema dell'applicazione
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.getStyleClass().add("custom-alert");
            
            alert.showAndWait();
        });
    }
}