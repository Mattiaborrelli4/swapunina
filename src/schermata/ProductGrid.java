package schermata;

import application.Classe.Annuncio;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.List;
import java.util.function.Consumer;

/**
 * ProductGrid - Componente per la visualizzazione a griglia degli annunci
 * Gestisce la visualizzazione, caricamento, stati vuoti/errore e aggiornamenti dinamici
 */
public class ProductGrid {

    // Componenti UI
    private VBox container;               // Contenitore principale
    private TilePane productGrid;         // Griglia di card prodotti
    private VBox loadingContainer;        // Container per stato di caricamento
    private VBox emptyContainer;          // Container per stato vuoto

    // Callback per azioni utente
    private Consumer<Annuncio> onDetailsAction;
    private Consumer<Annuncio> onOfferAction;
    private Consumer<Annuncio> onFavoriteAction;
    private Consumer<Annuncio> onAnnuncioModificato;

    // Costanti per configurazione
    private static final int GRID_PADDING = 20;
    private static final int GRID_HGAP = 24;
    private static final int GRID_VGAP = 24;
    private static final int GRID_PREF_COLUMNS = 3;
    private static final int LOADING_PADDING = 40;
    private static final int EMPTY_PADDING = 30;

    /**
     * Crea e restituisce la struttura principale della griglia prodotti
     * @return VBox contenente la griglia completa
     */
    public VBox creaProductGrid() {
        initializeContainers();
        setupProductGrid();
        container.getChildren().add(productGrid);
        return container;
    }

    /**
     * Inizializza tutti i container principali
     */
    private void initializeContainers() {
        container = new VBox();
        container.setId("productGridContainer");

        loadingContainer = creaLoadingContainer();
        emptyContainer = creaEmptyContainer();
    }

    /**
     * Configura la griglia principale dei prodotti
     */
    private void setupProductGrid() {
        productGrid = new TilePane();
        productGrid.getStyleClass().add("product-grid");
        productGrid.setPadding(new Insets(GRID_PADDING));
        productGrid.setHgap(GRID_HGAP);
        productGrid.setVgap(GRID_VGAP);
        productGrid.setPrefColumns(GRID_PREF_COLUMNS);
        productGrid.setId("productTilePane");
    }

    /**
     * Mostra l'indicatore di caricamento
     */
    public void mostraLoading() {
        Platform.runLater(() -> {
            container.getChildren().setAll(loadingContainer);
        });
    }

    /**
     * Nasconde l'indicatore di caricamento e mostra la griglia prodotti
     */
    public void nascondiLoading() {
        Platform.runLater(() -> {
            container.getChildren().setAll(productGrid);
        });
    }

    /**
     * Mostra lo stato "nessun prodotto trovato"
     */
    public void mostraStatoVuoto() {
        Platform.runLater(() -> {
            container.getChildren().setAll(emptyContainer);
        });
    }

    /**
     * Mostra un messaggio di errore
     * @param messaggio Il messaggio di errore da visualizzare
     */
    public void mostraErrore(String messaggio) {
        System.out.println("[DEBUG] ERRORE: " + messaggio);

        VBox erroreBox = createErrorBox(messaggio);
        Platform.runLater(() -> container.getChildren().setAll(erroreBox));
    }

    /**
     * Crea il container per il messaggio di errore
     */
    private VBox createErrorBox(String messaggio) {
        Label erroreLabel = new Label(messaggio);
        erroreLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px; -fx-font-weight: bold;");

        VBox erroreBox = new VBox(erroreLabel);
        erroreBox.setSpacing(10);
        erroreBox.setPadding(new Insets(30));
        erroreBox.setStyle("-fx-alignment: center;");

        return erroreBox;
    }

    /**
     * Aggiorna la griglia con una nuova lista di annunci
     * @param annunci Lista di annunci da visualizzare
     */
    public void aggiornaAnnunci(List<Annuncio> annunci) {
        Platform.runLater(() -> {
            if (annunci == null || annunci.isEmpty()) {
                mostraStatoVuoto();
                return;
            }

            clearAndPopulateGrid(annunci);
            container.getChildren().setAll(productGrid);
        });
    }

    /**
     * Pulisce e popola la griglia con gli annunci
     */
    private void clearAndPopulateGrid(List<Annuncio> annunci) {
        productGrid.getChildren().clear();

        for (Annuncio annuncio : annunci) {
            ProductCard card = createProductCard(annuncio);
            productGrid.getChildren().add(card);
        }
    }

    /**
     * Crea una card prodotto configurata con tutti i callback
     */
    private ProductCard createProductCard(Annuncio annuncio) {
        ProductCard card = new ProductCard(annuncio);
        card.setOnDetailsAction(onDetailsAction);
        card.setOnOfferAction(onOfferAction);
        card.setOnFavoriteAction(onFavoriteAction);
        card.setOnAnnuncioModificato(onAnnuncioModificato);
        return card;
    }

    /**
     * Aggiorna una card specifica quando un annuncio viene modificato
     * @param annuncioModificato L'annuncio con i dati aggiornati
     */
    public void aggiornaCardAnnuncio(Annuncio annuncioModificato) {
        Platform.runLater(() -> {
            for (int i = 0; i < productGrid.getChildren().size(); i++) {
                javafx.scene.Node node = productGrid.getChildren().get(i);
                if (node instanceof ProductCard) {
                    ProductCard oldCard = (ProductCard) node;
                    if (oldCard.getAnnuncioId() == annuncioModificato.getId()) {
                        replaceCardAtPosition(i, annuncioModificato);
                        break;
                    }
                }
            }
        });
    }

    /**
     * Sostituisce una card in una posizione specifica
     */
    private void replaceCardAtPosition(int index, Annuncio annuncioModificato) {
        ProductCard newCard = createProductCard(annuncioModificato);
        productGrid.getChildren().set(index, newCard);
    }

    // === METODI PER LA CREAZIONE DEI CONTAINER DI STATO ===

    /**
     * Crea il container per lo stato di caricamento
     */
    private VBox creaLoadingContainer() {
        VBox box = new VBox();
        box.setSpacing(10);
        box.setPadding(new Insets(LOADING_PADDING));
        box.setStyle("-fx-alignment: center;");

        ProgressIndicator progress = createProgressIndicator();
        Text testo = createLoadingText();

        box.getChildren().addAll(progress, testo);
        return box;
    }

    /**
     * Crea l'indicatore di progresso
     */
    private ProgressIndicator createProgressIndicator() {
        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(50, 50);
        return progress;
    }

    /**
     * Crea il testo di caricamento
     */
    private Text createLoadingText() {
        Text testo = new Text("Caricamento prodotti...");
        testo.setStyle("-fx-font-size: 14px;");
        return testo;
    }

    /**
     * Crea il container per lo stato vuoto
     */
    private VBox creaEmptyContainer() {
        VBox box = new VBox();
        box.setSpacing(12);
        box.setPadding(new Insets(EMPTY_PADDING));
        box.setStyle("-fx-alignment: center;");

        Text icona = createEmptyIcon();
        Text titolo = createEmptyTitle();
        Text descrizione = createEmptyDescription();

        box.getChildren().addAll(icona, titolo, descrizione);
        return box;
    }

    /**
     * Crea l'icona per stato vuoto
     */
    private Text createEmptyIcon() {
        Text icona = new Text("📦");
        icona.setStyle("-fx-font-size: 48px;");
        return icona;
    }

    /**
     * Crea il titolo per stato vuoto
     */
    private Text createEmptyTitle() {
        Text titolo = new Text("Nessun prodotto trovato");
        titolo.setStyle("-fx-font-weight: bold; -fx-font-size: 18px;");
        return titolo;
    }

    /**
     * Crea la descrizione per stato vuoto
     */
    private Text createEmptyDescription() {
        Text descrizione = new Text("Prova a modificare i filtri di ricerca o esplora altre categorie.");
        descrizione.setStyle("-fx-font-size: 13px;");
        return descrizione;
    }

    // === SETTER PER I CALLBACK ===

    /**
     * Imposta il callback per l'azione dettagli
     * @param onDetailsAction Consumer che gestisce la visualizzazione dettagli
     */
    public void setOnDetailsAction(Consumer<Annuncio> onDetailsAction) {
        this.onDetailsAction = onDetailsAction;
    }

    /**
     * Imposta il callback per l'azione offerta
     * @param onOfferAction Consumer che gestisce l'invio offerte
     */
    public void setOnOfferAction(Consumer<Annuncio> onOfferAction) {
        this.onOfferAction = onOfferAction;
    }

    /**
     * Imposta il callback per l'azione preferiti
     * @param onFavoriteAction Consumer che gestisce l'aggiunta ai preferiti
     */
    public void setOnFavoriteAction(Consumer<Annuncio> onFavoriteAction) {
        this.onFavoriteAction = onFavoriteAction;
    }

    /**
     * Imposta il callback per l'aggiornamento annuncio
     * @param onAnnuncioModificato Consumer che gestisce gli aggiornamenti annunci
     */
    public void setOnAnnuncioModificato(Consumer<Annuncio> onAnnuncioModificato) {
        this.onAnnuncioModificato = onAnnuncioModificato;
    }

    // === METODI UTILITY ===

    /**
     * Restituisce il numero di card attualmente visualizzate
     * @return Numero di card nella griglia
     */
    public int getNumeroCard() {
        return productGrid.getChildren().size();
    }

    /**
     * Pulisce completamente la griglia
     */
    public void clear() {
        Platform.runLater(() -> {
            productGrid.getChildren().clear();
        });
    }

    /**
     * Verifica se la griglia è vuota
     * @return true se non ci sono card, false altrimenti
     */
    public boolean isEmpty() {
        return productGrid.getChildren().isEmpty();
    }

    /**
     * Imposta il numero di colonne preferito per la griglia
     * @param colonne Numero di colonne desiderato
     */
    public void setNumeroColonne(int colonne) {
        productGrid.setPrefColumns(colonne);
    }

    /**
     * Aggiorna lo stile della griglia in base allo stato di autenticazione
     * @param isAuthenticated true se l'utente è autenticato, false altrimenti
     */
    public void setAuthState(boolean isAuthenticated) {
        // Potrebbe essere utilizzato per mostrare/nascondere funzionalità
        // che richiedono l'autenticazione
        String style = isAuthenticated ? 
            "-fx-opacity: 1.0;" : 
            "-fx-opacity: 0.8;";
        productGrid.setStyle(style);
    }
}