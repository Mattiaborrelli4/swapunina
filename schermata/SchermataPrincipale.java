package schermata;

import application.DB.AnnuncioDAO;
import application.Classe.Annuncio;
import application.Classe.Oggetto;
import application.Classe.utente;
import application.Enum.Categoria;
import application.Enum.OrigineOggetto;
import application.Enum.Tipologia;
import application.messagistica.ChatListDialog;
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

/**
 * SchermataPrincipale - Schermata principale dell'applicazione Marketplace
 * Gestisce la visualizzazione, filtraggio e gestione degli annunci
 */
public class SchermataPrincipale extends BorderPane {

    // Componenti UI
    private final TopBar topBar;
    private final CategoryMenu categoryMenu = new CategoryMenu();
    private final ProductGrid productGrid = new ProductGrid();
    private final FilterBar filterBar = new FilterBar();

    // Gestori dati
    private final AnnuncioDAO annuncioDAO = new AnnuncioDAO();
    private List<Annuncio> tuttiGliAnnunci;
    private List<Annuncio> annunciFiltrati;

    // Stato applicazione
    private Stage palcoscenicoPrincipale;
    private Categoria categoriaSelezionata;
    private Tipologia tipologiaSelezionata;
    private String ordinamento = "recent";
    private String queryRicerca = "";
    private static SchermataPrincipale instance;

    /**
     * Costruttore principale della schermata
     * @param matricolaUtente Matricola dell'utente corrente
     * @param palcoscenicoPrincipale Stage principale dell'applicazione
     * @param topBar Riferimento alla top bar dell'applicazione
     */
    public SchermataPrincipale(String matricolaUtente, Stage palcoscenicoPrincipale, TopBar topBar) {
        this.palcoscenicoPrincipale = palcoscenicoPrincipale;
        this.topBar = topBar;
        initializeUI();
        setupEventHandlers();
        loadAnnunci();
        instance = this; 
    }

    /**
     * Inizializza l'interfaccia utente principale
     */
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

    /**
     * Configura tutti gli event handler della schermata
     */
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
        
        // Callback per l'aggiornamento degli annunci modificati
        productGrid.setOnAnnuncioModificato(this::handleAnnuncioModificato);

        // Insert ad handler
        topBar.setOnInserisciAnnuncio(this::handleInsertAd);
    }

    /**
     * Gestisce l'aggiornamento di un annuncio modificato
     * @param annuncioModificato L'annuncio con i dati aggiornati
     */
    private void handleAnnuncioModificato(Annuncio annuncioModificato) {
        System.out.println("🔄 Annuncio modificato ricevuto, aggiorno la card...");
        productGrid.aggiornaCardAnnuncio(annuncioModificato);
    }

    /**
     * Gestisce l'inserimento di un nuovo annuncio
     */
    private void handleInsertAd() {
        try {
            // Ottieni l'utente corrente dalla SessionManager
            utente currentUser = SessionManager.getCurrentUser();
            
            if (currentUser == null || currentUser.getId() <= 0) {
                showError("Utente non trovato! Effettua il login.");
                return;
            }

            int userId = currentUser.getId();

            InserisciAnnuncioDialog dialog = new InserisciAnnuncioDialog(userId);
            dialog.showAndWait().ifPresent(adData -> {
                try {
                    // Verifica se l'oggetto esiste già nel DB e recupera il suo ID
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

                    // Il metodo inserisciAnnuncioComplessivo restituisce long, usiamo direttamente long
                    long result = annuncioDAO.inserisciAnnuncioComplessivo(nuovoAnnuncio, userId);
                    
                    if (result > 0) {
                        showSuccess("Annuncio pubblicato con successo! ID: " + result);
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

    /**
     * Carica gli annunci dal database in modo asincrono
     */
    private void loadAnnunci() {
        productGrid.mostraLoading();

        Task<List<Annuncio>> loadTask = new Task<>() {
            @Override
            protected List<Annuncio> call() {
                try {
                    AnnuncioDAO dao = new AnnuncioDAO();
                    return dao.getAnnunciAttivi();
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new RuntimeException("Errore nel caricamento degli annunci: " + e.getMessage(), e);
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

    /**
     * Applica i filtri correnti alla lista degli annunci
     */
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
        
        // CORREZIONE: Gestione corretta del tipo long
        long countLong = FilterManager.contaAnnunciFiltrati(
            tuttiGliAnnunci, 
            categoriaSelezionata, 
            tipologiaSelezionata, 
            queryRicerca
        );
        
        // Conversione sicura da long a int
        int count = convertLongToIntSafely(countLong);
        filterBar.updateCount(count);
    }

    /**
     * Converte in modo sicuro un long in int, gestendo eventuali overflow
     */
    private int convertLongToIntSafely(long value) {
        if (value > Integer.MAX_VALUE) {
            System.err.println("Warning: valore long " + value + " eccede Integer.MAX_VALUE, usando valore massimo int");
            return Integer.MAX_VALUE;
        } else if (value < Integer.MIN_VALUE) {
            System.err.println("Warning: valore long " + value + " inferiore a Integer.MIN_VALUE, usando valore minimo int");
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }

    /**
     * Gestisce la ricerca degli annunci
     * @param query Testo da cercare negli annunci
     */
    private void handleSearch(String query) {
        this.queryRicerca = query;
        applyFilters();
    }

    /**
     * Mostra i dettagli di un prodotto
     * @param annuncio L'annuncio di cui mostrare i dettagli
     */
    private void showProductDetails(Annuncio annuncio) {
        try {
            DettagliProdottoView dettagliView = new DettagliProdottoView(annuncio);
            dettagliView.mostra();
        } catch (Exception e) {
            // Fallback a Alert semplice in caso di errore
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(annuncio.getTitolo() != null ? annuncio.getTitolo() : "Dettagli Prodotto");
            alert.setHeaderText("Prezzo: " + annuncio.getPrezzoFormattato());
            
            String descrizione = annuncio.getDescrizione() != null ? 
                annuncio.getDescrizione() : 
                "Nessuna descrizione disponibile";
                
            alert.setContentText(
                "Categoria: " + (annuncio.getCategoria() != null ? annuncio.getCategoria() : "Non specificata") + "\n" +
                "Tipologia: " + annuncio.getTipologia().toString() + "\n\n" +
                "Descrizione:\n" + descrizione
            );
            alert.showAndWait();
        }
    }

    /**
     * Gestisce l'invio di un'offerta per un annuncio
     * @param annuncio L'annuncio per cui inviare l'offerta
     */
    private void handleOffer(Annuncio annuncio) {
        showSuccess("Hai inviato un'offerta per: " + 
                   (annuncio.getTitolo() != null ? annuncio.getTitolo() : annuncio.getOggetto().getNome()));
    }

    /**
     * Crea l'header della schermata principale
     */
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

    /**
     * Mostra un alert di errore
     */
    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Errore");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Mostra un alert di successo
     */
    private void showSuccess(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Successo");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Ricarica gli annunci
     */
    public void refresh() {
        loadAnnunci();
    }

    /**
     * Restituisce la categoria attualmente selezionata
     */
    public Categoria getCategoria() {
        return categoriaSelezionata;
    }
    
    /**
     * Gestisce l'apertura della schermata messaggi
     */
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

    /**
     * Mostra un alert generico
     */
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

    /**
     * Ottiene il numero totale di annunci caricati
     */
    public int getTotalAnnunci() {
        return tuttiGliAnnunci != null ? tuttiGliAnnunci.size() : 0;
    }

    /**
     * Ottiene il numero di annunci filtrati
     */
    public int getAnnunciFiltrati() {
        return annunciFiltrati != null ? annunciFiltrati.size() : 0;
    }

    /**
     * Pulisce le risorse quando la schermata viene chiusa
     */
    public void cleanup() {
        // CORREZIONE: Rimossa chiamata a metodo inesistente
        // Non chiamiamo productGrid.cleanup() perché non esiste
        System.out.println("Pulizia risorse SchermataPrincipale completata");
    }

    // In SchermataPrincipale.java - AGGIUNGI questi metodi alla classe

/**
 * Gestore globale per gli aggiornamenti degli annunci
 * Questo metodo viene chiamato quando un annuncio cambia stato (es: diventa VENDUTO)
 */
private void gestisciAggiornamentoAnnuncio(int annuncioId, String nuovoStato) {
    Platform.runLater(() -> {
        if ("VENDUTO".equals(nuovoStato)) {
            rimuoviAnnuncioDalleListe(annuncioId);
            productGrid.rimuoviCard(annuncioId);
            System.out.println("✅ Annuncio " + annuncioId + " segnato come VENDUTO e rimosso");
        }
    });
}

/**
 * Rimuove un annuncio dalle liste locali
 */
private void rimuoviAnnuncioDalleListe(int annuncioId) {
    // Rimuovi da tuttiGliAnnunci
    if (tuttiGliAnnunci != null) {
        tuttiGliAnnunci.removeIf(annuncio -> annuncio.getId() == annuncioId);
    }
    
    // Rimuovi da annunciFiltrati
    if (annunciFiltrati != null) {
        annunciFiltrati.removeIf(annuncio -> annuncio.getId() == annuncioId);
    }
    
    // Aggiorna il contatore
    int count = annunciFiltrati != null ? annunciFiltrati.size() : 0;
    filterBar.updateCount(count);
}

/**
 * Metodo per notificare che un annuncio è stato venduto
 * Può essere chiamato da qualsiasi parte dell'applicazione
 */
public void notificaAnnuncioVenduto(int annuncioId) {
    gestisciAggiornamentoAnnuncio(annuncioId, "VENDUTO");
}

}
