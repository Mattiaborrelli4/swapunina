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

public class ProductGrid {

    private VBox container;               // Contenitore principale
    private TilePane productGrid;         // Griglia di card
    private VBox loadingContainer;        // Schermata di caricamento
    private VBox emptyContainer;          // Schermata "nessun prodotto"

    private Consumer<Annuncio> onDetailsAction;
    private Consumer<Annuncio> onOfferAction;
    private Consumer<Annuncio> onFavoriteAction;

    /**
     * Crea la struttura principale della griglia dei prodotti.
     */
    public VBox creaProductGrid() {
        System.out.println("[DEBUG] Inizializzazione ProductGrid...");

        container = new VBox();
        container.setId("productGridContainer");

        productGrid = new TilePane();
        productGrid.getStyleClass().add("product-grid");
        productGrid.setPadding(new Insets(20));
        productGrid.setHgap(24);
        productGrid.setVgap(24);
        productGrid.setPrefColumns(3);
        productGrid.setId("productTilePane");

        loadingContainer = creaLoadingContainer();
        emptyContainer = creaEmptyContainer();

        container.getChildren().add(productGrid);
        System.out.println("[DEBUG] ProductGrid inizializzata con griglia vuota.");
        return container;
    }

    /**
     * Mostra la schermata di caricamento.
     */
    public void mostraLoading() {
        Platform.runLater(() -> {
            System.out.println("[DEBUG] Mostro schermata di caricamento...");
            container.getChildren().setAll(loadingContainer);
        });
    }

    /**
     * Nasconde il caricamento e mostra la griglia.
     */
    public void nascondiLoading() {
        Platform.runLater(() -> {
            System.out.println("[DEBUG] Nascondo schermata di caricamento, mostro griglia prodotti...");
            container.getChildren().setAll(productGrid);
        });
    }

    /**
     * Mostra schermata vuota se non ci sono annunci.
     */
    public void mostraStatoVuoto() {
        Platform.runLater(() -> {
            System.out.println("[DEBUG] Nessun annuncio trovato. Mostro stato vuoto.");
            container.getChildren().setAll(emptyContainer);
        });
    }

    /**
     * Mostra un messaggio di errore a schermo.
     */
    public void mostraErrore(String messaggio) {
        System.out.println("[DEBUG] ERRORE: " + messaggio);

        Label erroreLabel = new Label(messaggio);
        erroreLabel.setStyle("-fx-text-fill: red; -fx-font-size: 14px; -fx-font-weight: bold;");

        VBox erroreBox = new VBox(erroreLabel);
        erroreBox.setSpacing(10);
        erroreBox.setPadding(new Insets(30));
        erroreBox.setStyle("-fx-alignment: center;");

        Platform.runLater(() -> container.getChildren().setAll(erroreBox));
    }

    /**
     * Aggiorna la griglia con gli annunci ricevuti.
     */
    public void aggiornaAnnunci(List<Annuncio> annunci) {
        Platform.runLater(() -> {
            if (annunci == null || annunci.isEmpty()) {
                System.out.println("[DEBUG] aggiornaAnnunci: lista nulla o vuota.");
                mostraStatoVuoto();
                return;
            }

            System.out.println("[DEBUG] aggiornaAnnunci: caricamento " + annunci.size() + " annunci.");
            productGrid.getChildren().clear();

            for (Annuncio annuncio : annunci) {
            	String nomeOggetto = annuncio.getOggetto() != null
            	        ? annuncio.getOggettoPrincipale().getNome()
            	        : "Oggetto sconosciuto";
                System.out.println("[DEBUG] Aggiungo card per annuncio: " + nomeOggetto);

                ProductCard card = new ProductCard(annuncio);
                card.setOnDetailsAction(onDetailsAction);
                card.setOnOfferAction(onOfferAction);
                card.setOnFavoriteAction(onFavoriteAction);

                productGrid.getChildren().add(card);
            }

            container.getChildren().setAll(productGrid);
            System.out.println("[DEBUG] Numero totale card visualizzate: " + productGrid.getChildren().size());
        });
    }

    // === COMPONENTI UI SECONDARI ===

    private VBox creaLoadingContainer() {
        VBox box = new VBox();
        box.setSpacing(10);
        box.setPadding(new Insets(40));
        box.setStyle("-fx-alignment: center;");

        ProgressIndicator progress = new ProgressIndicator();
        progress.setPrefSize(50, 50);

        Text testo = new Text("Caricamento prodotti...");
        testo.setStyle("-fx-font-size: 14px;");

        box.getChildren().addAll(progress, testo);
        return box;
    }

    private VBox creaEmptyContainer() {
        VBox box = new VBox();
        box.setSpacing(12);
        box.setPadding(new Insets(30));
        box.setStyle("-fx-alignment: center;");

        Text icona = new Text("📦");
        icona.setStyle("-fx-font-size: 48px;");

        Text titolo = new Text("Nessun prodotto trovato");
        titolo.setStyle("-fx-font-weight: bold; -fx-font-size: 18px;");

        Text descrizione = new Text("Prova a modificare i filtri di ricerca o esplora altre categorie.");
        descrizione.setStyle("-fx-font-size: 13px;");

        box.getChildren().addAll(icona, titolo, descrizione);
        return box;
    }

    // === SETTER CALLBACKS ===

    public void setOnDetailsAction(Consumer<Annuncio> onDetailsAction) {
        this.onDetailsAction = onDetailsAction;
    }

    public void setOnOfferAction(Consumer<Annuncio> onOfferAction) {
        this.onOfferAction = onOfferAction;
    }

    public void setOnFavoriteAction(Consumer<Annuncio> onFavoriteAction) {
        this.onFavoriteAction = onFavoriteAction;
    }
}
