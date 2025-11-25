package schermata;

import application.Classe.Annuncio;
import application.Classe.Messaggio;
import application.Classe.ModificaAnnuncioDialog;
import application.Classe.Oggetto;
import application.Classe.utente;
import application.Enum.OrigineOggetto;
import application.Enum.Tipologia;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import schermata.button.CarrelloManager;
import schermata.button.MessaggiDialog;
import schermata.button.RecensioneDialog;
import application.DB.AnnuncioDAO;
import application.DB.MessaggioDAO;
import application.DB.RecensioneDAO;
import application.DB.SessionManager;
import application.DB.UserDAO;
import application.DB.CodiceDAO;
import application.DB.ConnessioneDB;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * ProductCard - Componente per la visualizzazione di un singolo annuncio
 * Gestione completa delle interazioni utente: dettagli, acquisto, scambio, recensioni
 */
public class ProductCard extends VBox {
    
    // Costanti per configurazione
    private static final int IMAGE_WIDTH = 280;
    private static final int IMAGE_HEIGHT = 200;
    private static final int CARD_PADDING = 12;
    private static final int CONTENT_PADDING = 20;
    private static final int IMAGE_CONTAINER_PADDING = 10;

    // Componenti UI
    private final ImageView productImage = new ImageView();
    private final Label badge = new Label();
    private final Button contactButton = new Button();
    private final Label title = new Label();
    private final Text price = new Text();
    private final Text description = new Text();
    private final Button detailsButton = new Button("Dettagli");
    private final Button actionButton = new Button();
    private final Label vendutoBadge = new Label("VENDUTO");
    private final Label acquistatoBadge = new Label("ACQUISTATO - IN ATTESA RITIRO");
    
    // Callback per azioni utente
    private Consumer<Annuncio> onDetailsAction;
    private Consumer<Annuncio> onAction;
    private Consumer<Annuncio> onFavoriteAction;
    private Consumer<Annuncio> onAnnuncioModificato;
    
    // Dati dell'annuncio
    private final int currentUserId = SessionManager.getCurrentUserId();
    private final Annuncio annuncio;

    /**
     * Costruttore principale della ProductCard
     * @param annuncio L'annuncio da visualizzare nella card
     */
    public ProductCard(Annuncio annuncio) {
        super(10);
        this.annuncio = annuncio;
        
        initializeCard();
        setupImageSection();
        setupContentSection();
        setupEventHandlers();
        applyStyles();
        setupTooltips();
        
        checkStatoAnnuncio();
    }

    /**
     * Restituisce l'ID dell'annuncio visualizzato
     * @return ID univoco dell'annuncio
     */
    public int getAnnuncioId() {
        return annuncio.getId();
    }

    /**
     * Inizializza le proprietà base della card
     */
    private void initializeCard() {
        setPadding(new Insets(CARD_PADDING));
        setAlignment(Pos.TOP_CENTER);
        getStyleClass().add("product-card");
        setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 8; -fx-background-radius: 8;");
    }

    /**
     * Verifica e gestisce lo stato dell'annuncio
     */
private void checkStatoAnnuncio() {
    if ("CONSEGNATO".equalsIgnoreCase(annuncio.getStato())) {
        mostraStatoConsegnato(); // Nuovo metodo per stato CONSEGNATO
    } else if ("VENDUTO".equalsIgnoreCase(annuncio.getStato())) {
        mostraStatoVenduto();
    } else if (isAcquistatoMaNonRitirato()) {
        mostraStatoAcquistato();
    }
}

/**
 * Mostra lo stato CONSEGNATO disabilitando i pulsanti e mostrando il badge
 */
private void mostraStatoConsegnato() {
    Label consegnatoBadge = new Label("CONSEGNATO");
    consegnatoBadge.getStyleClass().add("consegnato-badge");
    consegnatoBadge.setStyle(
        "-fx-background-color: #27ae60; " +
        "-fx-text-fill: white; " +
        "-fx-font-weight: bold; " +
        "-fx-padding: 5px 10px; " +
        "-fx-border-radius: 10; " +
        "-fx-background-radius: 10;"
    );
    consegnatoBadge.setVisible(true);
    
    // Aggiungi badge al container
    StackPane imageContainer = (StackPane) getChildren().get(0);
    StackPane.setAlignment(consegnatoBadge, Pos.TOP_RIGHT);
    StackPane.setMargin(consegnatoBadge, new Insets(10));
    imageContainer.getChildren().add(consegnatoBadge);
    
    // Disabilita tutti i pulsanti
    disableActionButtons();
    
    // Configura il pulsante principale
    actionButton.setText("✅ Consegnato");
    actionButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white;");
    Tooltip.install(actionButton, new Tooltip("Questo articolo è stato consegnato"));
    Tooltip.install(consegnatoBadge, new Tooltip("Questo articolo è stato consegnato"));
    
    // Applica stile
    setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #ddd; -fx-border-radius: 8; -fx-background-radius: 8; -fx-opacity: 0.8;");
}

    /**
     * Configura la sezione immagine della card
     */
    private void setupImageSection() {
        StackPane imageContainer = createImageContainer();
        loadProductImage();
        setupImageProperties();
        setupBadgeAndContact();
        setupStatoBadges();
        
        HBox badgeRow = createBadgeRow();
        imageContainer.getChildren().addAll(productImage, badgeRow);
        getChildren().add(imageContainer);
    }

    /**
     * Crea il container per l'immagine
     */
    private StackPane createImageContainer() {
        StackPane imageContainer = new StackPane();
        imageContainer.setAlignment(Pos.TOP_CENTER);
        imageContainer.getStyleClass().add("product-image-container");
        return imageContainer;
    }

    /**
     * Configura le proprietà dell'immagine
     */
    private void setupImageProperties() {
        productImage.setFitWidth(IMAGE_WIDTH);
        productImage.setFitHeight(IMAGE_HEIGHT);
        productImage.setPreserveRatio(true);
        productImage.getStyleClass().add("product-image");
    }

    /**
     * Configura badge e pulsante contatto
     */
    private void setupBadgeAndContact() {
        setupBadge();
        setupContactButton();
    }

    /**
     * Configura i badge di stato
     */
    private void setupStatoBadges() {
        setupVendutoBadge();
        setupAcquistatoBadge();
    }

    /**
     * Configura il badge dell'origine dell'oggetto
     */
    private void setupBadge() {
        if (annuncio.getOggetto() != null) {
            badge.setText(annuncio.getOggetto().getOrigine().toString());
            badge.getStyleClass().add(getBadgeStyle(annuncio.getOggetto().getOrigine()));
        } else {
            badge.setText("N/D");
            badge.getStyleClass().add("badge-vendita");
        }
    }

    /**
     * Configura il pulsante di contatto
     */
    private void setupContactButton() {
        // Nascondi pulsante contatto se l'utente è il venditore
        if (currentUserId != -1 && currentUserId == annuncio.getVenditoreId()) {
            contactButton.setVisible(false);
            contactButton.setManaged(false);
            return;
        }

        setupContactButtonIcon();
        contactButton.getStyleClass().add("contact-button-large");
        contactButton.setTooltip(new Tooltip("Contatta venditore"));
    }

    /**
     * Configura l'icona del pulsante contatto
     */
    private void setupContactButtonIcon() {
        InputStream contactStream = getClass().getResourceAsStream("/icons/message-icon.png");
        if (contactStream != null) {
            ImageView messageIcon = new ImageView(new Image(contactStream));
            messageIcon.setFitWidth(28);
            messageIcon.setFitHeight(28);
            contactButton.setGraphic(messageIcon);
        } else {
            contactButton.setText("💬");
        }
    }

    /**
     * Configura il badge "VENDUTO"
     */
    private void setupVendutoBadge() {
        vendutoBadge.getStyleClass().add("venduto-badge");
        vendutoBadge.setStyle(
            "-fx-background-color: #e74c3c; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 5px 10px; " +
            "-fx-border-radius: 10; " +
            "-fx-background-radius: 10;"
        );
        vendutoBadge.setVisible(false);
        StackPane.setAlignment(vendutoBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(vendutoBadge, new Insets(10));
    }

    /**
     * Configura il badge "ACQUISTATO"
     */
    private void setupAcquistatoBadge() {
        acquistatoBadge.getStyleClass().add("acquistato-badge");
        acquistatoBadge.setStyle(
            "-fx-background-color: #3498db; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-padding: 5px 10px; " +
            "-fx-border-radius: 10; " +
            "-fx-background-radius: 10;"
        );
        acquistatoBadge.setVisible(false);
        StackPane.setAlignment(acquistatoBadge, Pos.TOP_CENTER);
        StackPane.setMargin(acquistatoBadge, new Insets(10));
    }

    /**
     * Crea la riga contenente badge e pulsante contatto
     */
    private HBox createBadgeRow() {
        HBox badgeRow = new HBox(12, badge, new Region(), contactButton);
        HBox.setHgrow(badgeRow.getChildren().get(1), Priority.ALWAYS);
        badgeRow.setAlignment(Pos.CENTER_LEFT);
        badgeRow.setPadding(new Insets(IMAGE_CONTAINER_PADDING));
        return badgeRow;
    }

    /**
     * Mostra lo stato venduto disabilitando i pulsanti e mostrando il badge
     */
    private void mostraStatoVenduto() {
        vendutoBadge.setVisible(true);
        disableActionButtons();
        setupVendutoButton();
        applyVendutoStyle();
        
        // Aggiungi badge al container
        StackPane imageContainer = (StackPane) getChildren().get(0);
        if (!imageContainer.getChildren().contains(vendutoBadge)) {
            imageContainer.getChildren().add(vendutoBadge);
        }
    }

    /**
     * Mostra lo stato acquistato disabilitando i pulsanti e mostrando il badge
     */
    private void mostraStatoAcquistato() {
        acquistatoBadge.setVisible(true);
        disableActionButtons();
        setupAcquistatoButton();
        applyAcquistatoStyle();
        
        // Aggiungi badge al container
        StackPane imageContainer = (StackPane) getChildren().get(0);
        if (!imageContainer.getChildren().contains(acquistatoBadge)) {
            imageContainer.getChildren().add(acquistatoBadge);
        }
    }

    /**
     * Disabilita tutti i pulsanti di azione
     */
    private void disableActionButtons() {
        actionButton.setDisable(true);
        detailsButton.setDisable(true);
        contactButton.setDisable(true);
    }

    /**
     * Configura il pulsante principale per stato venduto
     */
    private void setupVendutoButton() {
        actionButton.setText("✅ Venduto");
        actionButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white;");
        Tooltip.install(actionButton, new Tooltip("Questo articolo è stato venduto"));
        Tooltip.install(vendutoBadge, new Tooltip("Questo articolo è stato venduto"));
    }

    /**
     * Configura il pulsante principale per stato acquistato
     */
    private void setupAcquistatoButton() {
        actionButton.setText("📦 In Attesa di Ritiro");
        actionButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white;");
        Tooltip.install(actionButton, new Tooltip("Questo articolo è stato acquistato e attende il ritiro"));
        Tooltip.install(acquistatoBadge, new Tooltip("Questo articolo è stato acquistato e attende il ritiro"));
    }

    /**
     * Applica lo stile per annuncio venduto
     */
    private void applyVendutoStyle() {
        setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #ddd; -fx-border-radius: 8; -fx-background-radius: 8; -fx-opacity: 0.8;");
    }

    /**
     * Applica lo stile per annuncio acquistato
     */
    private void applyAcquistatoStyle() {
        setStyle("-fx-background-color: #f0f8ff; -fx-border-color: #3498db; -fx-border-radius: 8; -fx-background-radius: 8; -fx-opacity: 0.9;");
    }

    /**
     * Verifica se l'annuncio è stato acquistato ma non ancora ritirato
     */
    private boolean isAcquistatoMaNonRitirato() {
        try {
            String sql = "SELECT COUNT(*) FROM codice_conferma WHERE annuncio_id = ? AND data_creazione > CURRENT_TIMESTAMP - INTERVAL '14 days'";
            
            try (Connection conn = ConnessioneDB.getConnessione();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, annuncio.getId());
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (Exception e) {
            System.err.println("Errore nel verificare lo stato acquisto per annuncio " + annuncio.getId() + ": " + e.getMessage());
        }
        return false;
    }

    private void loadProductImage() {
        try {
            Oggetto oggetto = annuncio.getOggetto();
            if (oggetto != null && oggetto.hasCloudinaryImage()) {
                loadImageFromCloudinary(oggetto);
            } else if (oggetto != null && oggetto.getImageUrl() != null && !oggetto.getImageUrl().isEmpty()) {
                loadImageFromUrl(oggetto.getImageUrl());
            } else {
                loadDefaultImage();
            }
        } catch (Exception e) {
            System.err.println("Errore nel caricamento dell'immagine per l'annuncio " + annuncio.getId() + ": " + e.getMessage());
            loadDefaultImage();
        }
    }

    /**
     * Carica l'immagine da Cloudinary con trasformazioni
     */
    private void loadImageFromCloudinary(Oggetto oggetto) {
        try {
            String imageUrl = oggetto.getImageUrlOptimized();
            Image image = new Image(imageUrl, IMAGE_WIDTH, IMAGE_HEIGHT, true, true, true);
            
            image.progressProperty().addListener((obs, oldProgress, newProgress) -> {
                if (newProgress.doubleValue() == 1.0) {
                    System.out.println("Immagine Cloudinary caricata: " + oggetto.getImageUrlOptimized());
                }
            });
            
            image.errorProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    System.err.println("Errore nel caricamento dell'immagine Cloudinary");
                    loadDefaultImage();
                }
            });
            
            productImage.setImage(image);
            
        } catch (Exception e) {
            System.err.println("Errore nel caricamento da Cloudinary: " + e.getMessage());
            loadDefaultImage();
        }
    }

    /**
     * Carica l'immagine dall'URL (vecchio sistema)
     */
    private void loadImageFromUrl(String imageUrl) {
        try {
            if (imageUrl.startsWith("file:")) {
                Image image = new Image(imageUrl, IMAGE_WIDTH, IMAGE_HEIGHT, true, true, true);
                productImage.setImage(image);
            } else if (imageUrl.startsWith("/")) {
                InputStream imageStream = getClass().getResourceAsStream(imageUrl);
                if (imageStream != null) {
                    Image image = new Image(imageStream, IMAGE_WIDTH, IMAGE_HEIGHT, true, true);
                    productImage.setImage(image);
                } else {
                    loadDefaultImage();
                }
            } else {
                Image image = new Image(imageUrl, IMAGE_WIDTH, IMAGE_HEIGHT, true, true, true);
                productImage.setImage(image);
            }
        } catch (Exception e) {
            System.err.println("Errore nel caricamento dell'immagine: " + e.getMessage());
            loadDefaultImage();
        }
    }

    /**
     * Carica l'immagine di default
     */
    private void loadDefaultImage() {
        try {
            InputStream defaultStream = getClass().getResourceAsStream("/application/img/default-product.png");
            if (defaultStream != null) {
                productImage.setImage(new Image(defaultStream, IMAGE_WIDTH, IMAGE_HEIGHT, true, true));
            } else {
                productImage.setImage(new Image("https://via.placeholder.com/280x200.png?text=No+Image", 
                    IMAGE_WIDTH, IMAGE_HEIGHT, true, true));
            }
        } catch (Exception e) {
            productImage.setImage(new Image("https://via.placeholder.com/280x200.png?text=No+Image", 
                IMAGE_WIDTH, IMAGE_HEIGHT, true, true));
        }
    }

    /**
     * Configura la sezione contenuto della card
     */
    private void setupContentSection() {
        VBox content = createContentContainer();
        setupHeader(content);
        setupDescription(content);
        setupMetaInfo(content);
        setupActionButtons(content);
        
        getChildren().add(content);
    }

    /**
     * Crea il container del contenuto
     */
    private VBox createContentContainer() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(CONTENT_PADDING));
        content.getStyleClass().add("product-content");
        return content;
    }

    /**
     * Configura l'header con titolo e prezzo
     */
    private void setupHeader(VBox content) {
        HBox header = createHeader();
        content.getChildren().add(header);
    }

    /**
     * Crea l'header con titolo e prezzo
     */
    private HBox createHeader() {
        setupTitle();
        setupPrice();
        
        HBox header = new HBox(title, new Region(), price);
        header.getStyleClass().add("product-header");
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    /**
     * Configura il titolo dell'annuncio
     */
    private void setupTitle() {
        String titoloAnnuncio = annuncio.getTitolo() != null ? annuncio.getTitolo() : "Senza titolo";
        title.setText(titoloAnnuncio);
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2c3e50;");
        title.setWrapText(true);
        title.setMaxWidth(Double.MAX_VALUE);
    }

    /**
     * Configura il prezzo dell'annuncio
     */
    private void setupPrice() {
        String formattedPrice = annuncio.getPrezzo() > 0 ? 
            annuncio.getPrezzoFormattato() : "Gratuito";
        price.setText(formattedPrice);
        price.getStyleClass().add("product-price");
        price.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-fill: #e74c3c;");
    }

    /**
     * Configura la descrizione
     */
    private void setupDescription(VBox content) {
        String descrizioneTesto = getDescrizioneTesto();
        description.setText(descrizioneTesto);
        description.getStyleClass().add("product-description");
        description.setStyle("-fx-font-size: 14px; -fx-fill: #7f8c8d;");
        content.getChildren().add(description);
    }

    /**
     * Ottiene il testo della descrizione
     */
    private String getDescrizioneTesto() {
        if (annuncio.getOggetto() != null && annuncio.getOggetto().getDescrizione() != null) {
            return annuncio.getOggetto().getDescrizione();
        } else if (annuncio.getDescrizione() != null) {
            return annuncio.getDescrizione();
        }
        return "Nessuna descrizione";
    }

    /**
     * Configura le informazioni meta
     */
    private void setupMetaInfo(VBox content) {
        VBox metaInfo = createMetaInfo();
        metaInfo.getStyleClass().add("product-meta");
        content.getChildren().add(metaInfo);
    }

    /**
     * Crea le informazioni meta dell'annuncio
     */
    private VBox createMetaInfo() {
        VBox meta = new VBox(4);
        
        HBox row1 = new HBox(8,
                createIconText("📍", annuncio.getSedeConsegna() != null ? annuncio.getSedeConsegna() : "Non specificato"),
                new Text("•"),
                createIconText("🚚", annuncio.getModalitaConsegna() != null ? annuncio.getModalitaConsegna() : "Non specificata")
        );
        row1.getStyleClass().add("meta-row");
        
        HBox row2 = new HBox(8,
                createIconText("👤", annuncio.getNomeUtenteVenditore() != null ? annuncio.getNomeUtenteVenditore() : "Venditore"),
                new Text("•"),
                createIconText("📅", formatDate(annuncio.getDataPubblicazione()))
        );
        row2.getStyleClass().add("meta-row");
        
        meta.getChildren().addAll(row1, row2);
        return meta;
    }

    /**
     * Crea un elemento testo con icona
     */
    private HBox createIconText(String icon, String text) {
        Text iconText = new Text(icon);
        iconText.setStyle("-fx-font-size: 12px;");
        
        Text contentText = new Text(text);
        contentText.setStyle("-fx-font-size: 12px; -fx-fill: #7f8c8d;");
        
        HBox container = new HBox(4, iconText, contentText);
        container.setAlignment(Pos.CENTER_LEFT);
        return container;
    }

    /**
     * Configura i pulsanti di azione
     */
    private void setupActionButtons(VBox content) {
        // Solo se l'annuncio non è venduto o acquistato
        if (!"VENDUTO".equalsIgnoreCase(annuncio.getStato()) && !isAcquistatoMaNonRitirato()) {
            setupMainActionButton();
        }
        
        setupDetailsButton();
        HBox actions = createActionButtons();
        content.getChildren().add(actions);
        
        // Aggiungi pulsanti recensioni se utente loggato
        if (currentUserId != -1) {
            HBox recensioniBox = createRecensioniButtons();
            content.getChildren().add(recensioniBox);
        }
    }

    /**
     * Configura il pulsante azione principale
     */
    private void setupMainActionButton() {
        if (currentUserId != -1 && currentUserId == annuncio.getVenditoreId()) {
            setupModificaButton();
        } else if (annuncio.getTipologia() == Tipologia.ASTA) {
            setupAstaButton();
        } else if (annuncio.getOggetto() != null) {
            setupOrigineButton();
        } else {
            setupDefaultButton();
        }
    }

    /**
     * Configura pulsante modifica per venditore
     */
    private void setupModificaButton() {
        actionButton.setText("Modifica Annuncio");
        actionButton.setStyle("-fx-background-color: #6b7280; -fx-text-fill: white; -fx-font-weight: bold;");
    }

    /**
     * Configura pulsante per aste
     */
    private void setupAstaButton() {
        actionButton.setText("💰 Fai Offerta");
        actionButton.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold;");
    }

    /**
     * Configura pulsante in base all'origine dell'oggetto
     */
    private void setupOrigineButton() {
        switch (annuncio.getOggetto().getOrigine()) {
            case USATO:
                actionButton.setText("🛒 Aggiungi al Carrello");
                actionButton.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold;");
                break;
            case SCAMBIO:
                actionButton.setText("🔄 Proponi Scambio");
                actionButton.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-font-weight: bold;");
                break;
            case REGALO:
                actionButton.setText("📞 Contatta");
                actionButton.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-font-weight: bold;");
                break;
            default:
                setupDefaultButton();
        }
    }

    /**
     * Configura pulsante default
     */
    private void setupDefaultButton() {
        actionButton.setText("📞 Contatta");
        actionButton.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-font-weight: bold;");
    }

    /**
     * Configura il pulsante dettagli
     */
    private void setupDetailsButton() {
        detailsButton.getStyleClass().add("details-button");
        detailsButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold;");
    }

    /**
     * Crea el container dei pulsanti azione
     */
    private HBox createActionButtons() {
        HBox actions = new HBox(10, detailsButton, actionButton);
        actions.setAlignment(Pos.CENTER);
        actions.getStyleClass().add("product-actions");
        return actions;
    }

    /**
     * Crea i pulsanti per le recensioni
     */
    private HBox createRecensioniButtons() {
        HBox recensioniBox = new HBox(10);
        recensioniBox.setAlignment(Pos.CENTER);
        recensioniBox.getStyleClass().add("review-actions");
        
        Button recensioniBtn = createRecensioniButton();
        recensioniBox.getChildren().add(recensioniBtn);
        
        if (currentUserId != annuncio.getVenditoreId()) {
            Button lasciaRecensioneBtn = createLasciaRecensioneButton();
            recensioniBox.getChildren().add(lasciaRecensioneBtn);
        }
        
        return recensioniBox;
    }

    /**
     * Crea il pulsante visualizza recensioni
     */
    private Button createRecensioniButton() {
        Button recensioniBtn = new Button("⭐ Recensioni");
        recensioniBtn.setStyle("-fx-background-color: #f1c40f; -fx-text-fill: white; -fx-font-weight: bold;");
        recensioniBtn.setOnAction(e -> mostraRecensioniVenditore());
        return recensioniBtn;
    }

    /**
     * Crea il pulsante lascia recensione
     */
    private Button createLasciaRecensioneButton() {
        Button lasciaRecensioneBtn = new Button("✍️ Lascia Recensione");
        lasciaRecensioneBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold;");
        lasciaRecensioneBtn.setOnAction(e -> lasciaRecensione());
        return lasciaRecensioneBtn;
    }

    /**
     * Configura gli event handler della card
     */
    private void setupEventHandlers() {
        setupDetailsHandler();
        setupActionHandler();
        setupContactHandler();
    }

    /**
     * Configura l'handler per il pulsante dettagli
     */
    private void setupDetailsHandler() {
        detailsButton.setOnAction(e -> {
            if (onDetailsAction != null) onDetailsAction.accept(annuncio);
        });
    }

    /**
     * Configura l'handler per il pulsante azione principale
     */
    private void setupActionHandler() {
        actionButton.setOnAction(e -> {
            if ("VENDUTO".equalsIgnoreCase(annuncio.getStato()) || isAcquistatoMaNonRitirato()) {
                return;
            }
            
            if (currentUserId != -1 && currentUserId == annuncio.getVenditoreId()) {
                modificaAnnuncio();
                return;
            }
            
            handleUserAction();
        });
    }

    /**
     * Gestisce l'azione dell'utente in base al tipo di annuncio
     */
    private void handleUserAction() {
        String buttonText = actionButton.getText();
        if (buttonText.contains("Aggiungi al Carrello")) {
            aggiungiAlCarrello();
        } else if (buttonText.contains("Fai Offerta")) {
            faiOfferta();
        } else if (buttonText.contains("Proponi Scambio")) {
            proponiScambio();
        } else if (buttonText.contains("Contatta")) {
            contattaPerRegalo();
        } else {
            handleDefaultAction();
        }
    }

    /**
     * Configura l'handler per il pulsante contatto
     */
    private void setupContactHandler() {
        contactButton.setOnAction(e -> {
            if (!isUtenteLoggato()) {
                mostraMessaggio("Devi essere loggato per visualizzare le chat");
                return;
            }
            
            String messaggioIniziale = "Salve, sono interessato al suo articolo: " + annuncio.getTitolo();
            MessaggiDialog dialog = new MessaggiDialog(annuncio, currentUserId, messaggioIniziale);
            dialog.showAndWait();
        });
    }

    /**
     * Gestisce la modifica dell'annuncio da parte del venditore
     */
    private void modificaAnnuncio() {
        ModificaAnnuncioDialog dialog = new ModificaAnnuncioDialog(annuncio);
        
        dialog.showAndWait().ifPresent(annuncioModificato -> {
            try {
                AnnuncioDAO annuncioDAO = new AnnuncioDAO();
                boolean successo = annuncioDAO.aggiornaAnnuncioCompleto(annuncioModificato);
                
                if (successo) {
                    mostraMessaggio("Annuncio aggiornato con successo!");
                    
                    if (onAnnuncioModificato != null) {
                        onAnnuncioModificato.accept(annuncioModificato);
                    }
                } else {
                    mostraMessaggio("Errore durante l'aggiornamento dell'annuncio.");
                }
            } catch (Exception ex) {
                mostraMessaggio("Errore durante l'aggiornamento dell'annuncio: " + ex.getMessage());
            }
        });
    }

    /**
     * Gestisce l'azione di default
     */
    private void handleDefaultAction() {
        if (!annuncio.isDisponibile()) {
            mostraMessaggio("Questo annuncio non è più disponibile");
            return;
        }
        
        if (!isUtenteLoggato()) {
            mostraMessaggio("Devi essere loggato per effettuare questa azione");
            return;
        }
        
        if (currentUserId != -1 && currentUserId == annuncio.getVenditoreId()) {
            mostraMessaggio("Sei il venditore di questo annuncio");
            return;
        }
        
        if (annuncio.getTipologia() == Tipologia.ASTA) {
            faiOfferta();
        } else if (annuncio.getOggetto() != null) {
            handleByOrigine();
        } else {
            contattaPerRegalo();
        }
    }

    /**
     * Gestisce l'azione in base all'origine dell'oggetto
     */
    private void handleByOrigine() {
        switch (annuncio.getOggetto().getOrigine()) {
            case USATO:
                aggiungiAlCarrello();
                break;
            case SCAMBIO:
                proponiScambio();
                break;
            case REGALO:
                contattaPerRegalo();
                break;
        }
    }

    /**
     * Aggiunge l'annuncio al carrello
     */
    private void aggiungiAlCarrello() {
        try {
            CarrelloManager carrelloManager = CarrelloManager.getInstance();
            carrelloManager.aggiungiAlCarrello(annuncio);
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Aggiunto al Carrello");
            alert.setHeaderText(null);
            alert.setContentText(annuncio.getTitolo() + " è stato aggiunto al carrello!");
            alert.showAndWait();
            
        } catch (Exception e) {
            mostraMessaggio("Errore nell'aggiunta al carrello: " + e.getMessage());
        }
    }

    /**
     * Propone uno scambio per l'annuncio
     */
    private void proponiScambio() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Proponi Scambio");
        dialog.setHeaderText("Proponi uno scambio per: " + annuncio.getTitolo());
        dialog.setContentText("Descrivi cosa offri in cambio:");

        dialog.showAndWait().ifPresent(proposta -> {
            if (proposta.trim().isEmpty()) {
                mostraMessaggio("Devi descrivere cosa offri in cambio");
                return;
            }
            
            inviaMessaggioScambio(proposta);
        });
    }

    /**
     * Invia il messaggio di proposta scambio
     */
    private void inviaMessaggioScambio(String proposta) {
        String messaggioTesto = "Proposta di scambio per il tuo articolo: " + annuncio.getTitolo() + 
                              "\nOffro in cambio: " + proposta;
        
        Messaggio messaggio = new Messaggio(
            currentUserId,
            annuncio.getVenditoreId(),
            messaggioTesto,
            annuncio.getId()
        );
        
        MessaggioDAO messaggioDAO = new MessaggioDAO();
        boolean successo = messaggioDAO.inviaMessaggio(messaggio);
        
        if (successo) {
            mostraMessaggio("Proposta di scambio inviata: " + proposta);
        } else {
            mostraMessaggio("Errore durante l'invio della proposta di scambio.");
        }
    }

    /**
     * Contatta per articolo in regalo
     */
    private void contattaPerRegalo() {
        String messaggioTesto = "Salve, sono interessato al tuo articolo in regalo: " + annuncio.getTitolo();
        
        Messaggio messaggio = new Messaggio(
            currentUserId,
            annuncio.getVenditoreId(),
            messaggioTesto,
            annuncio.getId()
        );
        
        MessaggioDAO messaggioDAO = new MessaggioDAO();
        boolean successo = messaggioDAO.inviaMessaggio(messaggio);
        
        if (successo) {
            mostraMessaggio("Richiesta inviata! Il venditore ti contatterà presto.");
        } else {
            mostraMessaggio("Errore durante l'invio della richiesta.");
        }
    }

    /**
     * Gestisce la creazione di un'offerta
     */
    private void faiOfferta() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Fai un'offerta");
        dialog.setHeaderText("Fai un'offerta per: " + annuncio.getTitolo());
        dialog.setContentText("Importo offerta (€):");

        dialog.showAndWait().ifPresent(importoStr -> {
            try {
                double importo = Double.parseDouble(importoStr);
                if (importo <= 0) {
                    mostraMessaggio("L'importo deve essere positivo");
                    return;
                }
                
                inviaOfferta(importo);
                
            } catch (NumberFormatException e) {
                mostraMessaggio("Inserisci un importo valido");
            }
        });
    }

    /**
     * Invia l'offerta al venditore
     */
    private void inviaOfferta(double importo) {
        String messaggioTesto = "Nuova offerta di €" + importo + " per il tuo articolo: " + annuncio.getTitolo();
        
        Messaggio messaggio = new Messaggio(
            currentUserId,
            annuncio.getVenditoreId(),
            messaggioTesto,
            annuncio.getId()
        );
        
        MessaggioDAO messaggioDAO = new MessaggioDAO();
        boolean successo = messaggioDAO.inviaMessaggio(messaggio);
        
        if (successo) {
            mostraMessaggio("Offerta di €" + importo + " inviata con successo!");
        } else {
            mostraMessaggio("Errore durante l'invio dell'offerta.");
        }
    }

    /**
     * Mostra le recensioni del venditore
     */
    private void mostraRecensioniVenditore() {
        try {
            RecensioneDAO recensioneDAO = new RecensioneDAO();
            RecensioneDAO.StatisticheRecensioni risultato = 
                recensioneDAO.getRecensioniEStatistichePerAnnuncio(annuncio.getId());
            
            List<application.Classe.Recensioni> recensioni = risultato.getRecensioni();
            
            if (recensioni.isEmpty()) {
                mostraMessaggio("Questo annuncio non ha ancora recensioni.");
                return;
            }
            
            double punteggioMedio = risultato.getPunteggioMedio();
            RecensioneDialog dialog = new RecensioneDialog(annuncio, recensioni, punteggioMedio);
            dialog.showAndWait();
            
        } catch (Exception e) {
            mostraMessaggio("Errore nel caricamento delle recensioni. Riprova più tardi.");
        }
    }
    
    /**
     * Gestisce l'invio di una recensione
     */
    private void lasciaRecensione() {
        if (!isUtenteLoggato()) {
            mostraMessaggio("Devi essere loggato per lasciare una recensione");
            return;
        }

        if (currentUserId == annuncio.getVenditoreId()) {
            mostraMessaggio("Non puoi lasciare una recensione a te stesso");
            return;
        }

        try {
            RecensioneDAO recensioneDAO = new RecensioneDAO();

            if (recensioneDAO.haGiaRecensito(currentUserId, annuncio.getId())) {
                mostraMessaggio("Hai già lasciato una recensione per questo annuncio.");
                return;
            }

            utente venditore = recuperaVenditore(annuncio.getVenditoreId());

            if (venditore == null) {
                mostraMessaggio("Impossibile trovare le informazioni del venditore");
                return;
            }

            RecensioneDialog dialog = new RecensioneDialog(annuncio, venditore, currentUserId);
            
            boolean recensioneInviata = dialog.showAndWait().orElse(false);
            
            if (recensioneInviata) {
                mostraMessaggio("Recensione inviata con successo! Grazie per il feedback.");
            }

        } catch (Exception e) {
            mostraMessaggio("Errore durante l'invio della recensione: " + e.getMessage());
        }
    }

    /**
     * Recupera i dati del venditore
     */
    private utente recuperaVenditore(int venditoreId) {
        try {
            UserDAO userDAO = new UserDAO();
            return userDAO.getUserById(venditoreId);
        } catch (Exception e) {
            utente venditore = new utente();
            venditore.setId(venditoreId);
            venditore.setNome("Venditore");
            venditore.setCognome("#" + venditoreId);
            return venditore;
        }
    }

    /**
     * Verifica se l'utente è loggato
     */
    private boolean isUtenteLoggato() {
        return SessionManager.getCurrentUser() != null;
    }

    /**
     * Mostra un messaggio all'utente
     */
    private void mostraMessaggio(String messaggio) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informazione");
        alert.setHeaderText(null);
        alert.setContentText(messaggio);
        alert.showAndWait();
    }

    /**
     * Applica gli stili alla card
     */
    private void applyStyles() {
        getStyleClass().add("product-card");
        
        setupContactButtonHover();
    }

    /**
     * Configura l'effetto hover per il pulsante contatto
     */
    private void setupContactButtonHover() {
        contactButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 4px;");
        contactButton.hoverProperty().addListener((obs, oldVal, isHovering) -> {
            if (isHovering) {
                contactButton.setStyle("-fx-background-color: #e0e0e0; -fx-border-radius: 3px; -fx-padding: 4px;");
            } else {
                contactButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 4px;");
            }
        });
    }

    /**
     * Configura i tooltip per i componenti
     */
    private void setupTooltips() {
        Tooltip.install(detailsButton, new Tooltip("Visualizza dettagli prodotto"));
        setupActionButtonTooltip();
        Tooltip.install(contactButton, new Tooltip("Contatta venditore"));
    }

    /**
     * Configura il tooltip per il pulsante azione
     */
    private void setupActionButtonTooltip() {
        String actionTooltip = getActionButtonTooltip();
        Tooltip.install(actionButton, new Tooltip(actionTooltip));
    }

    /**
     * Restituisce il tooltip appropriato per il pulsante azione
     */
    private String getActionButtonTooltip() {
        String buttonText = actionButton.getText();
        if (buttonText.contains("Aggiungi al Carrello")) {
            return "Aggiungi questo articolo al carrello per l'acquisto";
        } else if (buttonText.contains("Proponi Scambio")) {
            return "Proponi uno scambio per questo articolo";
        } else if (buttonText.contains("Contatta")) {
            return "Contatta per ricevere questo regalo";
        } else if (buttonText.contains("Fai Offerta")) {
            return "Fai un'offerta per questa asta";
        } else if (buttonText.contains("Modifica")) {
            return "Modifica il tuo annuncio";
        } else if (buttonText.contains("Venduto")) {
            return "Questo articolo è stato venduto";
        } else if (buttonText.contains("In Attesa")) {
            return "Questo articolo è stato acquistato e attende il ritiro";
        }
        return "";
    }

    /**
     * Formatta la data per la visualizzazione
     */
    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "Oggi";
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /**
     * Restituisce lo stile CSS per il badge in base all'origine
     */
private String getBadgeStyle(OrigineOggetto origine) {
    if (origine == null) return "badge-vendita";
    switch (origine) {
        case USATO: return "badge-vendita";  
        case SCAMBIO: return "badge-scambio";
        case REGALO:  return "badge-regalo";
        default:      return "badge-vendita";
    }
}

    /**
     * Imposta il callback per l'azione dettagli
     */
    public void setOnDetailsAction(Consumer<Annuncio> handler) {
        this.onDetailsAction = handler;
    }

    /**
     * Imposta il callback per l'azione offerta
     */
    public void setOnOfferAction(Consumer<Annuncio> handler) {
        this.onAction = handler;
    }

    /**
     * Imposta il callback per l'azione preferiti
     */
    public void setOnFavoriteAction(Consumer<Annuncio> handler) {
        this.onFavoriteAction = handler;
    }

    /**
     * Imposta il callback per l'aggiornamento annuncio
     */
    public void setOnAnnuncioModificato(Consumer<Annuncio> handler) {
        this.onAnnuncioModificato = handler;
    }

    // In ProductCard o nella classe che gestisce la lista
public void aggiornaStatoAnnuncio(int annuncioId, String nuovoStato) {
    // Trova la card corrispondente e aggiorna/rimuovi
    if ("VENDUTO".equals(nuovoStato)) {
    }
}
}
