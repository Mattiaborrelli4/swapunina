package schermata;

import application.Classe.Annuncio;
import application.Classe.AzioneAnnuncioHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import application.Enum.Tipologia;

public class DettagliProdottoView {
    private static final String DEFAULT_IMAGE = "/images/default-product.png";
    private static final int WINDOW_WIDTH = 600;
    private static final int WINDOW_HEIGHT = 500;
    
    private final Annuncio annuncio;
    private final Stage stage = new Stage();
    private final ImageView productImage = new ImageView();
    private final Text productTitle = new Text();
    private final Text productPrice = new Text();
    private final Text productDescription = new Text();
    private final Button actionButton = new Button();
    
    public DettagliProdottoView(Annuncio annuncio) {
        this.annuncio = annuncio;
        initializeStage();
        setupUI();
    }
    
    private void initializeStage() {
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Dettagli Prodotto - " + annuncio.getOggettoPrincipale().getNome());
        stage.setMinWidth(400);
        stage.setMinHeight(400);
    }
    
    private void setupUI() {
        VBox root = new VBox(20);
        root.setPadding(new Insets(20));
        root.getStyleClass().add("product-detail-container");
        
        // Header con immagine e info
        HBox header = createHeader();
        
        // Sezione azioni
        HBox actionSection = createActionSection();
        
        root.getChildren().addAll(header, actionSection);
        stage.setScene(new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT));
    }
    
    private HBox createHeader() {
        HBox header = new HBox(20);
        header.setAlignment(Pos.TOP_LEFT);
        
        // Sezione immagine
        VBox imageSection = new VBox();
        imageSection.setAlignment(Pos.CENTER);
        imageSection.setMinWidth(250);
        
        loadProductImage();
        productImage.setFitWidth(250);
        productImage.setFitHeight(200);
        productImage.setPreserveRatio(true);
        productImage.getStyleClass().add("product-detail-image");
        
        imageSection.getChildren().add(productImage);
        
        // Sezione informazioni
        VBox infoSection = new VBox(15);
        infoSection.setAlignment(Pos.TOP_LEFT);
        
        // Titolo e prezzo
        productTitle.setText(annuncio.getOggettoPrincipale().getNome());
        productTitle.getStyleClass().add("product-detail-title");
        
        productPrice.setText(annuncio.getPrezzoFormattato());
        productPrice.getStyleClass().add("product-detail-price");
        
        // Badge tipologia
        Label typeBadge = new Label(annuncio.getTipologia().getDisplayName());
        typeBadge.getStyleClass().addAll("badge", getBadgeStyle(annuncio.getTipologia()));
        
        // Descrizione
        productDescription.setText(annuncio.getOggettoPrincipale().getDescrizione());
        productDescription.setWrappingWidth(300);
        productDescription.getStyleClass().add("product-detail-description");
        
        // Info consegna
        VBox deliveryInfo = createDeliveryInfo();
        
        // Info venditore
        VBox sellerInfo = createSellerInfo();
        
        infoSection.getChildren().addAll(
            productTitle, 
            productPrice, 
            typeBadge, 
            productDescription, 
            deliveryInfo, 
            sellerInfo
        );
        
        header.getChildren().addAll(imageSection, infoSection);
        return header;
    }
    
    private void loadProductImage() {
        try {
        	String imageUrl = annuncio.getOggettoPrincipale().getImmagine() != null ?
                    annuncio.getOggettoPrincipale().getImmagine().toURI().toString() : DEFAULT_IMAGE;

  productImage.setImage(new Image(imageUrl));

        } catch (Exception e) {
            productImage.setImage(new Image(getClass().getResourceAsStream(DEFAULT_IMAGE)));
        }
    }
    
    private VBox createDeliveryInfo() {
        VBox deliveryInfo = new VBox(5);
        deliveryInfo.getStyleClass().add("delivery-info");
        
        Text title = new Text("Modalità di consegna:");
        title.getStyleClass().add("section-title");
        
        Text location = new Text("📍 " + annuncio.getSedeConsegna());
        Text method = new Text("🚚 " + annuncio.getModalitaConsegna());
        
        deliveryInfo.getChildren().addAll(title, location, method);
        return deliveryInfo;
    }
    
    private VBox createSellerInfo() {
        VBox sellerInfo = new VBox(5);
        sellerInfo.getStyleClass().add("seller-info");
        
        Text title = new Text("Informazioni venditore:");
        title.getStyleClass().add("section-title");
        
        Text name = new Text("👤 " + annuncio.getNomeUtenteVenditore());
        Text email = new Text("✉️ " + annuncio.getUtenteEmail());
        
        sellerInfo.getChildren().addAll(title, name, email);
        return sellerInfo;
    }
    
    private HBox createActionSection() {
        HBox actionSection = new HBox(15);
        actionSection.setAlignment(Pos.CENTER);
        actionSection.getStyleClass().add("action-section");
        
        Button closeButton = new Button("Chiudi");
        closeButton.getStyleClass().add("close-button");
        closeButton.setOnAction(e -> stage.close());
        
        configureActionButton();
        
        actionSection.getChildren().addAll(closeButton, actionButton);
        return actionSection;
    }
    
    private void configureActionButton() {
        switch (annuncio.getTipologia()) {
            case VENDITA:
                actionButton.setText("Acquista");
                actionButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/cart.png"))));
                break;
            case SCAMBIO:
                actionButton.setText("Proponi scambio");
                actionButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/exchange.png"))));
                break;
            case REGALO:
                actionButton.setText("Contatta");
                actionButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/message.png"))));
                break;
            case ASTA:
                actionButton.setText("Partecipa all'asta");
                actionButton.setGraphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/auction.png"))));
                break;
            default:
                actionButton.setText("Azione");
                actionButton.setGraphic(null);
                break;
        }

        actionButton.getStyleClass().add("action-button");
        actionButton.setOnAction(e -> handleAction());
        Tooltip.install(actionButton, new Tooltip(getActionTooltip()));
    }

    
    private String getActionTooltip() {
        switch (annuncio.getTipologia()) {
            case VENDITA: return "Procedi all'acquisto";
            case SCAMBIO: return "Proponi un oggetto in scambio";
            case REGALO: return "Contatta il donatore";
            default: return "Azione principale";
        }
    }
    
    private String getBadgeStyle(Tipologia tipologia) {
        switch (tipologia) {
            case VENDITA: return "badge-sale";
            case SCAMBIO: return "badge-exchange";
            case REGALO: return "badge-gift";
            default: return "badge-sale";
        }
    }
    
    private void handleAction() {
        AzioneAnnuncioHandler handler = new AzioneAnnuncioHandler();
        handler.gestisciAzione(annuncio);
        stage.close();
    }
    
    public void mostra() {
        stage.show();
    }
}