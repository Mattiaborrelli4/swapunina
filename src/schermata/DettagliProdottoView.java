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

/**
 * Classe per la visualizzazione dei dettagli di un prodotto/annuncio
 * 
 * <p>Questa classe gestisce la finestra modale che mostra i dettagli completi
 * di un annuncio, incluse immagini, descrizioni e azioni disponibili.</p>
 * 
 * <p><b>Funzionalità principali:</b>
 * <ul>
 *   <li>Visualizzazione immagine prodotto con fallback</li>
 *   <li>Dettagli completi prodotto e venditore</li>
 *   <li>Pulsanti azione contestuali in base alla tipologia</li>
 *   <li>Gestione responsive del layout</li>
 *   <li>Cache delle risorse per performance</li>
 * </ul>
 * </p>
 */
public class DettagliProdottoView {
    // ========== COSTANTI DI CONFIGURAZIONE ==========
    
    /** Percorso immagine di default per prodotti senza immagine */
    private static final String PERCORSO_IMMAGINE_DEFAULT = "/images/default-product.png";
    
    /** Dimensioni finestra principale */
    private static final int LARGHEZZA_FINESTRA = 600;
    private static final int ALTEZZA_FINESTRA = 500;
    
    /** Dimensioni immagine prodotto */
    private static final int LARGHEZZA_IMMAGINE = 250;
    private static final int ALTEZZA_IMMAGINE = 200;
    
    /** Dimensioni minime finestra */
    private static final int LARGHEZZA_MINIMA = 400;
    private static final int ALTEZZA_MINIMA = 400;
    
    // ========== CACHE RISORSE CONDIVISE ==========
    
    /** Cache delle icone per evitare ricaricamenti multipli e migliorare performance */
    private static final Image ICONA_CARRELLO = caricaIcona("/icons/cart.png");
    private static final Image ICONA_SCAMBIO = caricaIcona("/icons/exchange.png");
    private static final Image ICONA_MESSAGGIO = caricaIcona("/icons/message.png");
    private static final Image ICONA_ASTA = caricaIcona("/icons/auction.png");
    
    /** Stili CSS predefiniti per i badge di tipologia */
    private static final String STILE_BADGE_VENDITA = "badge-sale";
    private static final String STILE_BADGE_SCAMBIO = "badge-exchange";
    private static final String STILE_BADGE_REGALO = "badge-gift";
    private static final String STILE_BADGE_ASTA = "badge-auction";
    
    // ========== COMPONENTI UI ==========
    
    /** L'annuncio di cui visualizzare i dettagli */
    private final Annuncio annuncio;
    
    /** Stage della finestra modale */
    private final Stage stage = new Stage();
    
    /** Componenti dell'interfaccia grafica */
    private final ImageView visualizzatoreImmagine = new ImageView();
    private final Text testoNomeProdotto = new Text();
    private final Text testoPrezzoProdotto = new Text();
    private final Text testoDescrizioneProdotto = new Text();
    private final Button pulsanteAzione = new Button();
    
    // ========== COSTRUTTORE ==========
    
    /**
     * Costruttore principale della vista dettagli prodotto
     * 
     * @param annuncio L'annuncio di cui visualizzare i dettagli
     * @throws IllegalArgumentException Se l'annuncio è null
     */
    public DettagliProdottoView(Annuncio annuncio) {
        if (annuncio == null) {
            throw new IllegalArgumentException("L'annuncio non può essere null");
        }
        
        this.annuncio = annuncio;
        inizializzaStage();
        configuraInterfacciaUtente();
    }
    
    // ========== METODI DI INIZIALIZZAZIONE ==========
    
    /**
     * Inizializza le proprietà della finestra modale
     * Configura il titolo, modalità e dimensioni della stage
     */
    private void inizializzaStage() {
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Dettagli Prodotto - " + ottieniNomeProdotto());
        stage.setMinWidth(LARGHEZZA_MINIMA);
        stage.setMinHeight(ALTEZZA_MINIMA);
    }
    
    /**
     * Configura l'interfaccia utente principale
     * Crea il layout root e aggiunge tutte le sezioni componenti
     */
    private void configuraInterfacciaUtente() {
        VBox layoutPrincipale = new VBox(20);
        layoutPrincipale.setPadding(new Insets(20));
        layoutPrincipale.getStyleClass().add("product-detail-container");
        
        // Creazione e composizione delle sezioni principali
        HBox sezioneIntestazione = creaSezioneIntestazione();
        HBox sezioneAzioni = creaSezioneAzioni();
        
        layoutPrincipale.getChildren().addAll(sezioneIntestazione, sezioneAzioni);
        
        Scene scena = new Scene(layoutPrincipale, LARGHEZZA_FINESTRA, ALTEZZA_FINESTRA);
        applicaStiliScena(scena);
        stage.setScene(scena);
    }
    
    /**
     * Applica gli stili CSS alla scena
     * 
     * @param scena La scena a cui applicare gli stili
     */
    private void applicaStiliScena(Scene scena) {
        try {
            scena.getStylesheets().add(getClass().getResource("/styles/dettagli-prodotto.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("Errore nel caricamento fogli di stile: " + e.getMessage());
        }
    }
    
    // ========== METODI CREAZIONE SEZIONI UI ==========
    
    /**
     * Crea la sezione intestazione con immagine e informazioni prodotto
     * 
     * @return HBox contenente la sezione immagine e informazioni
     */
    private HBox creaSezioneIntestazione() {
        HBox intestazione = new HBox(20);
        intestazione.setAlignment(Pos.TOP_LEFT);
        
        VBox sezioneImmagine = creaSezioneImmagine();
        VBox sezioneInformazioni = creaSezioneInformazioni();
        
        intestazione.getChildren().addAll(sezioneImmagine, sezioneInformazioni);
        return intestazione;
    }
    
    /**
     * Crea la sezione dedicata all'immagine del prodotto
     * 
     * @return VBox configurata con l'immagine del prodotto
     */
    private VBox creaSezioneImmagine() {
        VBox sezioneImmagine = new VBox();
        sezioneImmagine.setAlignment(Pos.CENTER);
        sezioneImmagine.setMinWidth(LARGHEZZA_IMMAGINE);
        
        configuraVisualizzatoreImmagine();
        sezioneImmagine.getChildren().add(visualizzatoreImmagine);
        return sezioneImmagine;
    }
    
    /**
     * Crea la sezione informazioni con tutti i dettagli del prodotto
     * 
     * @return VBox contenente tutti i dettagli informativi
     */
    private VBox creaSezioneInformazioni() {
        VBox sezioneInformazioni = new VBox(15);
        sezioneInformazioni.setAlignment(Pos.TOP_LEFT);
        
        configuraInformazioniProdotto();
        Label badgeTipologia = creaBadgeTipologia();
        VBox informazioniConsegna = creaInformazioniConsegna();
        VBox informazioniVenditore = creaInformazioniVenditore();
        
        sezioneInformazioni.getChildren().addAll(
            testoNomeProdotto, 
            testoPrezzoProdotto, 
            badgeTipologia, 
            testoDescrizioneProdotto, 
            informazioniConsegna, 
            informazioniVenditore
        );
        
        return sezioneInformazioni;
    }
    
    /**
     * Crea la sezione azioni con i pulsanti
     * 
     * @return HBox contenente i pulsanti di azione e chiusura
     */
    private HBox creaSezioneAzioni() {
        HBox sezioneAzioni = new HBox(15);
        sezioneAzioni.setAlignment(Pos.CENTER);
        sezioneAzioni.getStyleClass().add("action-section");
        
        Button pulsanteChiudi = creaPulsanteChiudi();
        configuraPulsanteAzione();
        
        sezioneAzioni.getChildren().addAll(pulsanteChiudi, pulsanteAzione);
        return sezioneAzioni;
    }
    
    // ========== METODI CONFIGURAZIONE COMPONENTI ==========
    
    /**
     * Configura le informazioni testuali del prodotto
     * Imposta titolo, prezzo formattato e descrizione
     */
    private void configuraInformazioniProdotto() {
        testoNomeProdotto.setText(ottieniNomeProdotto());
        testoNomeProdotto.getStyleClass().add("product-detail-title");
        
        testoPrezzoProdotto.setText(annuncio.getPrezzoFormattato());
        testoPrezzoProdotto.getStyleClass().add("product-detail-price");
        
        testoDescrizioneProdotto.setText(ottieniDescrizioneProdotto());
        testoDescrizioneProdotto.setWrappingWidth(300);
        testoDescrizioneProdotto.getStyleClass().add("product-detail-description");
    }
    
    /**
     * Configura il visualizzatore immagine del prodotto
     * Imposta dimensioni, stile e carica l'immagine
     */
    private void configuraVisualizzatoreImmagine() {
        visualizzatoreImmagine.setFitWidth(LARGHEZZA_IMMAGINE);
        visualizzatoreImmagine.setFitHeight(ALTEZZA_IMMAGINE);
        visualizzatoreImmagine.setPreserveRatio(true);
        visualizzatoreImmagine.getStyleClass().add("product-detail-image");
        
        caricaImmagineProdotto();
    }
    
    /**
     * Crea e configura il pulsante di chiusura
     * 
     * @return Button configurato per chiudere la finestra
     */
    private Button creaPulsanteChiudi() {
        Button pulsanteChiudi = new Button("Chiudi");
        pulsanteChiudi.getStyleClass().add("close-button");
        pulsanteChiudi.setOnAction(e -> stage.close());
        
        // Tooltip informativo
        Tooltip.install(pulsanteChiudi, new Tooltip("Chiudi questa finestra"));
        
        return pulsanteChiudi;
    }
    
    /**
     * Configura il pulsante di azione principale in base alla tipologia annuncio
     * Imposta testo, icona, tooltip e gestore eventi appropriati
     */
    private void configuraPulsanteAzione() {
        ConfigurazionePulsanteAzione config = ottieniConfigurazionePulsante();
        
        pulsanteAzione.setText(config.testo());
        
        if (config.icona() != null) {
            ImageView iconaView = new ImageView(config.icona());
            iconaView.setFitWidth(16);
            iconaView.setFitHeight(16);
            pulsanteAzione.setGraphic(iconaView);
        }
        
        pulsanteAzione.getStyleClass().add("action-button");
        pulsanteAzione.setOnAction(e -> gestisciAzionePrincipale());
        
        // Tooltip contestuale
        Tooltip.install(pulsanteAzione, new Tooltip(config.tooltip()));
    }
    
    /**
     * Crea il badge della tipologia con stile appropriato
     * 
     * @return Label configurata come badge di tipologia
     */
    private Label creaBadgeTipologia() {
        Label badgeTipologia = new Label(annuncio.getTipologia().toString());
        badgeTipologia.getStyleClass().addAll("badge", ottieniStileBadge(annuncio.getTipologia()));
        return badgeTipologia;
    }
    
    /**
     * Crea la sezione informazioni di consegna
     * 
     * @return VBox con dettagli località e metodo consegna
     */
    private VBox creaInformazioniConsegna() {
        VBox informazioniConsegna = new VBox(5);
        informazioniConsegna.getStyleClass().add("delivery-info");
        
        Text titolo = creaTitoloSezione("Modalità di consegna:");
        Text localita = new Text("📍 " + ottieniLocalitaConsegna());
        Text metodo = new Text("🚚 " + ottieniMetodoConsegna());
        
        informazioniConsegna.getChildren().addAll(titolo, localita, metodo);
        return informazioniConsegna;
    }
    
    /**
     * Crea la sezione informazioni venditore
     * 
     * @return VBox con dettagli identificativi venditore
     */
    private VBox creaInformazioniVenditore() {
        VBox informazioniVenditore = new VBox(5);
        informazioniVenditore.getStyleClass().add("seller-info");
        
        Text titolo = creaTitoloSezione("Informazioni venditore:");
        Text nome = new Text("👤 " + annuncio.getNomeUtenteVenditore());
        
        informazioniVenditore.getChildren().addAll(titolo, nome);
        return informazioniVenditore;
    }
    
    /**
     * Crea un titolo di sezione con stile predefinito
     * 
     * @param testo Il testo da visualizzare come titolo
     * @return Text configurato come titolo di sezione
     */
    private Text creaTitoloSezione(String testo) {
        Text titolo = new Text(testo);
        titolo.getStyleClass().add("section-title");
        return titolo;
    }
    
    // ========== METODI GESTIONE RISORSE ==========
    
    /**
     * Carica un'icona in modo sicuro con gestione errori
     * 
     * @param percorso Il percorso della risorsa icona
     * @return L'immagine caricata o null in caso di errore
     */
    private static Image caricaIcona(String percorso) {
        try {
            return new Image(DettagliProdottoView.class.getResourceAsStream(percorso));
        } catch (Exception e) {
            System.err.println("⚠️ Errore nel caricamento icona: " + percorso + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Carica l'immagine del prodotto con gestione robusta degli errori
     * Utilizza l'immagine di default in caso di problemi
     */
    private void caricaImmagineProdotto() {
        try {
            String urlImmagine = annuncio.getImageUrlSafe();
            
            if (urlImmagine != null && !urlImmagine.isEmpty() && !urlImmagine.equals("null")) {
                caricaImmagineDaUrl(urlImmagine);
            } else {
                usaImmagineDefault();
            }
        } catch (Exception e) {
            System.err.println("❌ Errore nel caricamento immagine prodotto: " + e.getMessage());
            usaImmagineDefault();
        }
    }
    
    /**
     * Carica un'immagine da URL con supporto per diversi formati
     * 
     * @param urlImmagine L'URL o percorso dell'immagine
     */
    private void caricaImmagineDaUrl(String urlImmagine) {
        try {
            if (urlImmagine.startsWith("file:")) {
                // URL file system diretto
                visualizzatoreImmagine.setImage(new Image(urlImmagine));
            } else if (urlImmagine.startsWith("/")) {
                // Percorso assoluto file system
                visualizzatoreImmagine.setImage(new Image("file:" + urlImmagine));
            } else {
                // Tentativo come risorsa interna
                visualizzatoreImmagine.setImage(new Image(getClass().getResourceAsStream(urlImmagine)));
            }
        } catch (Exception e) {
            System.err.println("❌ Errore nel caricamento immagine da URL: " + urlImmagine);
            usaImmagineDefault();
        }
    }
    
    /**
     * Utilizza l'immagine di default come fallback
     */
    private void usaImmagineDefault() {
        try {
            visualizzatoreImmagine.setImage(new Image(getClass().getResourceAsStream(PERCORSO_IMMAGINE_DEFAULT)));
        } catch (Exception e) {
            System.err.println("💥 Errore critico: impossibile caricare l'immagine default");
        }
    }
    
    // ========== METODI BUSINESS LOGIC ==========
    
    /**
     * Gestisce l'azione principale del pulsante in base alla tipologia
     * Delegà all'handler appropriato e chiude la finestra
     */
    private void gestisciAzionePrincipale() {
        try {
            AzioneAnnuncioHandler handler = new AzioneAnnuncioHandler();
            handler.gestisciAzione(annuncio);
            stage.close();
        } catch (Exception e) {
            System.err.println("❌ Errore durante l'esecuzione dell'azione: " + e.getMessage());
            // Qui potresti mostrare un alert all'utente
        }
    }
    
    /**
     * Ottiene la configurazione del pulsante in base alla tipologia annuncio
     * 
     * @return ConfigurazionePulsanteAzione appropriata
     */
    private ConfigurazionePulsanteAzione ottieniConfigurazionePulsante() {
        return switch (annuncio.getTipologia()) {
            case VENDITA -> new ConfigurazionePulsanteAzione(
                "Acquista", 
                ICONA_CARRELLO, 
                "Procedi all'acquisto diretto del prodotto"
            );
            case SCAMBIO -> new ConfigurazionePulsanteAzione(
                "Proponi scambio", 
                ICONA_SCAMBIO, 
                "Proponi un oggetto in scambio per questo prodotto"
            );
            case REGALO -> new ConfigurazionePulsanteAzione(
                "Contatta", 
                ICONA_MESSAGGIO, 
                "Contatta il donatore per ricevere il prodotto"
            );
            case ASTA -> new ConfigurazionePulsanteAzione(
                "Partecipa all'asta", 
                ICONA_ASTA, 
                "Partecipa all'asta per questo prodotto"
            );
            default -> new ConfigurazionePulsanteAzione(
                "Azione", 
                null, 
                "Azione principale per questo annuncio"
            );
        };
    }
    
    /**
     * Restituisce lo stile CSS per il badge in base alla tipologia
     * 
     * @param tipologia La tipologia dell'annuncio
     * @return Il nome della classe CSS appropriata
     */
    private String ottieniStileBadge(Tipologia tipologia) {
        return switch (tipologia) {
            case VENDITA -> STILE_BADGE_VENDITA;
            case SCAMBIO -> STILE_BADGE_SCAMBIO;
            case REGALO -> STILE_BADGE_REGALO;
            case ASTA -> STILE_BADGE_ASTA;
            default -> STILE_BADGE_VENDITA;
        };
    }
    
    // ========== METODI ACCESSORI SICURI ==========
    
    /**
     * Ottiene il nome del prodotto in modo sicuro con fallback
     * 
     * @return Il nome del prodotto o testo default
     */
    private String ottieniNomeProdotto() {
        if (annuncio.getTitolo() != null && !annuncio.getTitolo().isEmpty()) {
            return annuncio.getTitolo();
        } else if (annuncio.getOggetto() != null && annuncio.getOggetto().getNome() != null) {
            return annuncio.getOggetto().getNome();
        } else {
            return "Prodotto senza nome";
        }
    }
    
    /**
     * Ottiene la descrizione del prodotto in modo sicuro con fallback
     * 
     * @return La descrizione del prodotto o testo default
     */
    private String ottieniDescrizioneProdotto() {
        if (annuncio.getDescrizione() != null && !annuncio.getDescrizione().isEmpty()) {
            return annuncio.getDescrizione();
        } else if (annuncio.getOggetto() != null && annuncio.getOggetto().getDescrizione() != null) {
            return annuncio.getOggetto().getDescrizione();
        } else {
            return "Nessuna descrizione disponibile per questo prodotto.";
        }
    }
    
    /**
     * Ottiene la località di consegna in modo sicuro con fallback
     * 
     * @return La località di consegna o testo default
     */
    private String ottieniLocalitaConsegna() {
        if (annuncio.getSedeConsegna() != null && !annuncio.getSedeConsegna().isEmpty()) {
            return annuncio.getSedeConsegna();
        } else if (annuncio.getCitta() != null && !annuncio.getCitta().isEmpty()) {
            return annuncio.getCitta();
        } else {
            return "Località non specificata";
        }
    }
    
    /**
     * Ottiene il metodo di consegna in modo sicuro con fallback
     * 
     * @return Il metodo di consegna o testo default
     */
    private String ottieniMetodoConsegna() {
        if (annuncio.getModalitaConsegna() != null && !annuncio.getModalitaConsegna().isEmpty()) {
            return annuncio.getModalitaConsegna();
        } else {
            return "Da concordare con il venditore";
        }
    }
    
    // ========== METODI PUBBLICI ==========
    
    /**
     * Mostra la finestra modale dei dettagli prodotto
     */
    public void mostra() {
        if (!stage.isShowing()) {
            stage.show();
        }
    }
    
    /**
     * Chiude la finestra modale
     */
    public void chiudi() {
        if (stage.isShowing()) {
            stage.close();
        }
    }
    
    /**
     * Verifica se la finestra è attualmente visibile
     * 
     * @return true se la finestra è visibile, false altrimenti
     */
    public boolean isVisibile() {
        return stage.isShowing();
    }
    
    /**
     * Restituisce l'annuncio associato a questa vista
     * 
     * @return L'annuncio visualizzato
     */
    public Annuncio getAnnuncio() {
        return annuncio;
    }
    
    // ========== CLASSI INTERNE ==========
    
    /**
     * Record per la configurazione del pulsante di azione principale
     * 
     * @param testo Il testo visualizzato sul pulsante
     * @param icona L'icona associata al pulsante (può essere null)
     * @param tooltip Il testo del tooltip esplicativo
     */
    private record ConfigurazionePulsanteAzione(String testo, Image icona, String tooltip) {}
}