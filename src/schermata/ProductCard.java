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

import java.io.InputStream;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public class ProductCard extends VBox {
    
    private static final int IMAGE_WIDTH = 280;
    private static final int IMAGE_HEIGHT = 200;

    private final ImageView productImage = new ImageView();
    private final Label badge = new Label();
    private final Button contactButton = new Button();
    private final Label title = new Label();
    private final Text price = new Text();
    private final Text description = new Text();
    private final Button detailsButton = new Button("Dettagli");
    private final Button actionButton = new Button();
    private final Label vendutoBadge = new Label("VENDUTO");
    
    private Consumer<Annuncio> onDetailsAction;
    private Consumer<Annuncio> onAction;
    private Consumer<Annuncio> onFavoriteAction;
    private Consumer<Annuncio> onAnnuncioModificato;
    private final int currentUserId = SessionManager.getCurrentUserId();
    private final Annuncio annuncio;

    public ProductCard(Annuncio annuncio) {
        super(10);
        this.annuncio = annuncio;
        setPadding(new Insets(12));
        getStyleClass().add("product-card");

        initializeCard();
        setupImageSection(annuncio);
        setupContentSection(annuncio);
        setupEventHandlers(annuncio);
        applyStyles();
        setupTooltips();
        
        // ✅ AGGIUNTA: Controlla se l'annuncio è venduto
        if ("VENDUTO".equalsIgnoreCase(annuncio.getStato())) {
            mostraStatoVenduto();
        }
    }

    public int getAnnuncioId() {
        return annuncio.getId();
    }

    private void initializeCard() {
        setAlignment(Pos.TOP_CENTER);
    }

    private void setupImageSection(Annuncio annuncio) {
        StackPane imageContainer = new StackPane();
        imageContainer.setAlignment(Pos.TOP_CENTER);
        imageContainer.getStyleClass().add("product-image-container");

        loadProductImage(annuncio);

        productImage.setFitWidth(IMAGE_WIDTH);
        productImage.setFitHeight(IMAGE_HEIGHT);
        productImage.setPreserveRatio(true);
        productImage.getStyleClass().add("product-image");

        if (annuncio.getOggetto() != null) {
            badge.setText(annuncio.getOggetto().getOrigine().toString());
            badge.getStyleClass().add(getBadgeStyle(annuncio.getOggetto().getOrigine()));
        } else {
            badge.setText("N/D");
            badge.getStyleClass().add("badge-vendita");
        }

        // Nascondi pulsante contatto se l'utente è il venditore
        if (currentUserId != -1 && currentUserId == annuncio.getVenditoreId()) {
            contactButton.setVisible(false);
            contactButton.setManaged(false);
        }

        // Setup contact button
        InputStream contactStream = getClass().getResourceAsStream("/icons/message-icon.png");
        if (contactStream != null) {
            ImageView messageIcon = new ImageView(new Image(contactStream));
            messageIcon.setFitWidth(28);
            messageIcon.setFitHeight(28);
            contactButton.setGraphic(messageIcon);
        } else {
            contactButton.setText("💬");
        }
        
        contactButton.getStyleClass().add("contact-button-large");
        contactButton.setTooltip(new Tooltip("Contatta venditore"));

        // Setup badge "VENDUTO"
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

        // Layout
        HBox badgeRow = new HBox(12, badge, new Region(), contactButton);
        HBox.setHgrow(badgeRow.getChildren().get(1), Priority.ALWAYS);
        badgeRow.setAlignment(Pos.CENTER_LEFT);
        badgeRow.setPadding(new Insets(10));

        imageContainer.getChildren().addAll(productImage, badgeRow, vendutoBadge);
        getChildren().add(imageContainer);
    }

    /**
     * ✅ NUOVO METODO: Mostra lo stato venduto disabilitando i pulsanti e mostrando il badge
     */
    private void mostraStatoVenduto() {
        // Mostra badge "VENDUTO"
        vendutoBadge.setVisible(true);
        
        // Disabilita tutti i pulsanti
        actionButton.setDisable(true);
        detailsButton.setDisable(true);
        contactButton.setDisable(true);
        
        // Cambia il testo del pulsante principale
        actionButton.setText("✅ Venduto");
        actionButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white;");
        
        // Aggiungi tooltip
        Tooltip.install(actionButton, new Tooltip("Questo articolo è stato venduto"));
        Tooltip.install(vendutoBadge, new Tooltip("Questo articolo è stato venduto"));
        
        // Applica stile opaco alla card
        setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #ddd; -fx-border-radius: 8; -fx-background-radius: 8; -fx-opacity: 0.8;");
    }

    private void loadProductImage(Annuncio annuncio) {
        try {
            Oggetto oggetto = annuncio.getOggetto();
            if (oggetto != null && oggetto.getImageUrl() != null && !oggetto.getImageUrl().isEmpty()) {
                String imageUrl = oggetto.getImageUrl();
                if (imageUrl.startsWith("file:")) {
                    Image image = new Image(imageUrl, IMAGE_WIDTH, IMAGE_HEIGHT, true, true, true);
                    productImage.setImage(image);
                } else {
                    InputStream imageStream = getClass().getResourceAsStream(imageUrl);
                    if (imageStream != null) {
                        Image image = new Image(imageStream, IMAGE_WIDTH, IMAGE_HEIGHT, true, true);
                        productImage.setImage(image);
                    } else {
                        loadDefaultImage();
                    }
                }
            } else {
                loadDefaultImage();
            }
        } catch (Exception e) {
            loadDefaultImage();
        }
    }

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

    private void setupContentSection(Annuncio annuncio) {
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        content.getStyleClass().add("product-content");

        String titoloAnnuncio = annuncio.getTitolo() != null ? annuncio.getTitolo() : "Senza titolo";
        
        title.setText(titoloAnnuncio);
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2c3e50;");
        title.setWrapText(true);
        title.setMaxWidth(Double.MAX_VALUE);
        
        // Prezzo formattato (gratuito per regali)
        String formattedPrice = annuncio.getPrezzo() > 0 ? 
            annuncio.getPrezzoFormattato() : "Gratuito";
        price.setText(formattedPrice);
        price.getStyleClass().add("product-price");
        price.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-fill: #e74c3c;");

        // Descrizione
        String descrizioneTesto = "Nessuna descrizione";
        if (annuncio.getOggetto() != null && annuncio.getOggetto().getDescrizione() != null) {
            descrizioneTesto = annuncio.getOggetto().getDescrizione();
        } else if (annuncio.getDescrizione() != null) {
            descrizioneTesto = annuncio.getDescrizione();
        }
        description.setText(descrizioneTesto);
        description.getStyleClass().add("product-description");
        description.setStyle("-fx-font-size: 14px; -fx-fill: #7f8c8d;");

        // Header con titolo e prezzo
        HBox header = new HBox(title, new Region(), price);
        header.getStyleClass().add("product-header");
        HBox.setHgrow(header.getChildren().get(1), Priority.ALWAYS);
        header.setAlignment(Pos.CENTER_LEFT);

        // Informazioni aggiuntive
        VBox metaInfo = createMetaInfo(annuncio);
        metaInfo.getStyleClass().add("product-meta");

        // Gestione pulsante azione principale - solo se non è venduto
        if (!"VENDUTO".equalsIgnoreCase(annuncio.getStato())) {
            if (currentUserId != -1 && currentUserId == annuncio.getVenditoreId()) {
                actionButton.setText("Modifica Annuncio");
                actionButton.setStyle("-fx-background-color: #6b7280; -fx-text-fill: white; -fx-font-weight: bold;");
            } else if (annuncio.getTipologia() == Tipologia.ASTA) {
                actionButton.setText("💰 Fai Offerta");
                actionButton.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-font-weight: bold;");
            } else if (annuncio.getOggetto() != null) {
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
                        actionButton.setText("📞 Contatta");
                        actionButton.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-font-weight: bold;");
                }
            } else {
                actionButton.setText("📞 Contatta");
                actionButton.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-font-weight: bold;");
            }
        }

        // Pulsante dettagli
        detailsButton.getStyleClass().add("details-button");
        detailsButton.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-font-weight: bold;");
        
        // Layout pulsanti principali
        HBox actions = new HBox(10, detailsButton, actionButton);
        actions.setAlignment(Pos.CENTER);
        actions.getStyleClass().add("product-actions");

        // Pulsanti recensioni
        if (currentUserId != -1) {
            Button recensioniBtn = new Button("⭐ Recensioni");
            recensioniBtn.setStyle("-fx-background-color: #f1c40f; -fx-text-fill: white; -fx-font-weight: bold;");
            recensioniBtn.setOnAction(e -> mostraRecensioniVenditore(annuncio));
            
            HBox recensioniBox = new HBox(10);
            recensioniBox.setAlignment(Pos.CENTER);
            recensioniBox.getStyleClass().add("review-actions");
            
            recensioniBox.getChildren().add(recensioniBtn);
            
            if (currentUserId != annuncio.getVenditoreId()) {
                Button lasciaRecensioneBtn = new Button("✍️ Lascia Recensione");
                lasciaRecensioneBtn.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-font-weight: bold;");
                lasciaRecensioneBtn.setOnAction(e -> lasciaRecensione(annuncio));
                recensioniBox.getChildren().add(lasciaRecensioneBtn);
            }
            
            content.getChildren().addAll(header, description, metaInfo, actions, recensioniBox);
        } else {
            content.getChildren().addAll(header, description, metaInfo, actions);
        }

        getChildren().add(content);
    }

    private VBox createMetaInfo(Annuncio annuncio) {
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

    private HBox createIconText(String icon, String text) {
        Text iconText = new Text(icon);
        iconText.setStyle("-fx-font-size: 12px;");
        
        Text contentText = new Text(text);
        contentText.setStyle("-fx-font-size: 12px; -fx-fill: #7f8c8d;");
        
        HBox container = new HBox(4, iconText, contentText);
        container.setAlignment(Pos.CENTER_LEFT);
        return container;
    }

    private void setupEventHandlers(Annuncio annuncio) {
        detailsButton.setOnAction(e -> {
            if (onDetailsAction != null) onDetailsAction.accept(annuncio);
        });
        
        actionButton.setOnAction(e -> {
            // Se l'annuncio è venduto, non fare nulla
            if ("VENDUTO".equalsIgnoreCase(annuncio.getStato())) {
                return;
            }
            
            // CONTROLLO PRINCIPALE: Gestisci modifica annuncio per il venditore
            if (currentUserId != -1 && currentUserId == annuncio.getVenditoreId()) {
                modificaAnnuncio(annuncio);
                return;
            }
            
            // CONTROLLO SECONDARIO: Gestisci in base al testo del pulsante
            String buttonText = actionButton.getText();
            if (buttonText.contains("Aggiungi al Carrello")) {
                aggiungiAlCarrello(annuncio);
            } else if (buttonText.contains("Fai Offerta")) {
                faiOfferta(annuncio);
            } else if (buttonText.contains("Proponi Scambio")) {
                proponiScambio(annuncio);
            } else if (buttonText.contains("Contatta")) {
                contattaPerRegalo(annuncio);
            } else {
                handleDefaultAction(annuncio);
            }
        });
        
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

    private void modificaAnnuncio(Annuncio annuncio) {
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
            } catch (SQLException ex) {
                mostraMessaggio("Errore di database: " + ex.getMessage());
            }
        });
    }

    private void handleDefaultAction(Annuncio annuncio) {
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
            faiOfferta(annuncio);
        } else if (annuncio.getOggetto() != null) {
            switch (annuncio.getOggetto().getOrigine()) {
                case USATO:
                    aggiungiAlCarrello(annuncio);
                    break;
                case SCAMBIO:
                    proponiScambio(annuncio);
                    break;
                case REGALO:
                    contattaPerRegalo(annuncio);
                    break;
            }
        } else {
            contattaPerRegalo(annuncio);
        }
    }

    private void aggiungiAlCarrello(Annuncio annuncio) {
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

    private void proponiScambio(Annuncio annuncio) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Proponi Scambio");
        dialog.setHeaderText("Proponi uno scambio per: " + annuncio.getTitolo());
        dialog.setContentText("Descrivi cosa offri in cambio:");

        dialog.showAndWait().ifPresent(proposta -> {
            if (proposta.trim().isEmpty()) {
                mostraMessaggio("Devi descrivere cosa offri in cambio");
                return;
            }
            
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
        });
    }

    private void contattaPerRegalo(Annuncio annuncio) {
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

    private void faiOfferta(Annuncio annuncio) {
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
                
            } catch (NumberFormatException e) {
                mostraMessaggio("Inserisci un importo valido");
            }
        });
    }

    private void mostraRecensioniVenditore(Annuncio annuncio) {
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
    
    private void lasciaRecensione(Annuncio annuncio) {
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

    private boolean isUtenteLoggato() {
        return SessionManager.getCurrentUser() != null;
    }

    private void mostraMessaggio(String messaggio) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informazione");
        alert.setHeaderText(null);
        alert.setContentText(messaggio);
        alert.showAndWait();
    }

    private void applyStyles() {
        getStyleClass().add("product-card");
        setStyle("-fx-background-color: white; -fx-border-color: #ddd; -fx-border-radius: 8; -fx-background-radius: 8;");
        
        contactButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 4px;");
        contactButton.hoverProperty().addListener((obs, oldVal, isHovering) -> {
            if (isHovering) {
                contactButton.setStyle("-fx-background-color: #e0e0e0; -fx-border-radius: 3px; -fx-padding: 4px;");
            } else {
                contactButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 4px;");
            }
        });
    }

    private void setupTooltips() {
        Tooltip.install(detailsButton, new Tooltip("Visualizza dettagli prodotto"));
        
        String actionTooltip = "";
        if (actionButton.getText().contains("Aggiungi al Carrello")) {
            actionTooltip = "Aggiungi questo articolo al carrello per l'acquisto";
        } else if (actionButton.getText().contains("Proponi Scambio")) {
            actionTooltip = "Proponi uno scambio per questo articolo";
        } else if (actionButton.getText().contains("Contatta")) {
            actionTooltip = "Contatta per ricevere questo regalo";
        } else if (actionButton.getText().contains("Fai Offerta")) {
            actionTooltip = "Fai un'offerta per questa asta";
        } else if (actionButton.getText().contains("Modifica")) {
            actionTooltip = "Modifica il tuo annuncio";
        } else if (actionButton.getText().contains("Venduto")) {
            actionTooltip = "Questo articolo è stato venduto";
        }
        Tooltip.install(actionButton, new Tooltip(actionTooltip));
        
        Tooltip.install(contactButton, new Tooltip("Contatta venditore"));
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "Oggi";
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private String getBadgeStyle(OrigineOggetto origine) {
        if (origine == null) return "badge-vendita";
        switch (origine) {
            case USATO: return "badge-vendita";
            case SCAMBIO: return "badge-scambio";
            case REGALO:  return "badge-regalo";
            default:      return "badge-vendita";
        }
    }

    // GETTER E SETTER
    public void setOnDetailsAction(Consumer<Annuncio> handler) {
        this.onDetailsAction = handler;
    }

    public void setOnOfferAction(Consumer<Annuncio> handler) {
        this.onAction = handler;
    }

    public void setOnFavoriteAction(Consumer<Annuncio> handler) {
        this.onFavoriteAction = handler;
    }

    public void setOnAnnuncioModificato(Consumer<Annuncio> handler) {
        this.onAnnuncioModificato = handler;
    }
}