package schermata.button;

import application.Classe.Annuncio;
import application.DB.CarrelloDAO;
import application.DB.SessionManager;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import application.Classe.Conto;
import application.DB.ContoDAO;
import application.DB.AnnuncioDAO;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestore centrale del carrello acquisti che coordina operazioni tra
 * interfaccia utente, database e sistema di pagamento
 */
public class CarrelloManager {
    private static CarrelloManager instance;
    private CarrelloDAO carrelloDAO;
    private ContoDAO contoDAO;
    
    /**
     * Mappa per mantenere lo stato di selezione degli articoli tra diverse sessioni UI
     * Key: annuncioId, Value: stato selezione
     */
    private Map<Integer, Boolean> statiSelezione = new HashMap<>();
    
    /**
     * Item del carrello con proprietà per la visualizzazione nell'interfaccia
     */
    public static class CarrelloItem {
        private int annuncioId;
        private String titolo;
        private double prezzo;
        private int quantita;
        private Annuncio annuncio;
        private boolean selected;
        
        public CarrelloItem(int annuncioId, String titolo, double prezzo, int quantita) {
            this.annuncioId = annuncioId;
            this.titolo = titolo;
            this.prezzo = prezzo;
            this.quantita = quantita;
            this.selected = true;
        }
        
        public CarrelloItem(Annuncio annuncio, int quantita) {
            this.annuncioId = annuncio.getId();
            this.titolo = annuncio.getTitolo();
            this.prezzo = annuncio.getPrezzo();
            this.quantita = quantita;
            this.annuncio = annuncio;
            this.selected = true;
        }
        
        // GETTER E SETTER
        public int getAnnuncioId() { return annuncioId; }
        public void setAnnuncioId(int annuncioId) { this.annuncioId = annuncioId; }
        public String getTitolo() { return titolo; }
        public void setTitolo(String titolo) { this.titolo = titolo; }
        public double getPrezzo() { return prezzo; }
        public void setPrezzo(double prezzo) { this.prezzo = prezzo; }
        public int getQuantita() { return quantita; }
        public void setQuantita(int quantita) { this.quantita = quantita; }
        public Annuncio getAnnuncio() { return annuncio; }
        public void setAnnuncio(Annuncio annuncio) { this.annuncio = annuncio; }
        public boolean isSelected() { return selected; }
        public void setSelected(boolean selected) { this.selected = selected; }
        
        /**
         * Calcola il subtotale per questo item (prezzo * quantità)
         */
        public double getSubtotale() {
            return prezzo * quantita;
        }
        
        /**
         * Restituisce il subtotale formattato come stringa
         */
        public String getSubtotaleFormattato() {
            return String.format("€%.2f", getSubtotale());
        }
        
        /**
         * Restituisce il prezzo unitario formattato come stringa
         */
        public String getPrezzoFormattato() {
            return String.format("€%.2f", prezzo);
        }
    }
    
    /**
     * Costruttore privato per il pattern Singleton
     */
    private CarrelloManager() {
        this.carrelloDAO = new CarrelloDAO();
        this.contoDAO = new ContoDAO();
    }
    
    /**
     * Restituisce l'istanza Singleton del CarrelloManager
     */
    public static CarrelloManager getInstance() {
        if (instance == null) {
            instance = new CarrelloManager();
        }
        return instance;
    }
    
    /**
     * Sincronizza gli stati di selezione tra i nuovi item caricati e quelli esistenti
     */
    private void sincronizzaStatiSelezione(List<CarrelloItem> nuoviItems) {
        Map<Integer, Boolean> nuoviStati = new HashMap<>();
        
        for (CarrelloItem item : nuoviItems) {
            boolean statoEsistente = statiSelezione.getOrDefault(item.getAnnuncioId(), true);
            item.setSelected(statoEsistente);
            nuoviStati.put(item.getAnnuncioId(), statoEsistente);
        }
        
        statiSelezione = nuoviStati;
    }
    
    /**
     * Aggiorna lo stato di selezione di un articolo specifico
     */
    public void aggiornaStatoSelezione(int annuncioId, boolean selected) {
        statiSelezione.put(annuncioId, selected);
    }
    
    /**
     * Restituisce lo stato di selezione di un articolo specifico
     */
    public boolean getStatoSelezione(int annuncioId) {
        return statiSelezione.getOrDefault(annuncioId, true);
    }
    
    /**
     * Restituisce l'ID dell'utente corrente dalla sessione
     */
    private int getCurrentUserId() {
        return SessionManager.getCurrentUserId();
    }
    
    /**
     * Verifica se l'utente è loggato
     */
    private boolean isUserLoggedIn() {
        return getCurrentUserId() > 0;
    }

    /**
     * Mostra un alert informativo all'utente
     */
    private void mostraAlert(String titolo, String messaggio) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(titolo);
            alert.setHeaderText(null);
            alert.setContentText(messaggio);
            alert.showAndWait();
        });
    }
    
    /**
     * Mostra un alert di errore all'utente
     */
    private void mostraAlertErrore(String titolo, String messaggio) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(titolo);
            alert.setHeaderText(null);
            alert.setContentText(messaggio);
            alert.showAndWait();
        });
    }
    
    // ==================== METODI GESTIONE CARRELLO ====================
    
    /**
     * Aggiunge un annuncio al carrello dell'utente corrente
     */
    public void aggiungiAlCarrello(Annuncio annuncio) {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) {
            mostraAlert("Accesso richiesto", "Devi effettuare il login per aggiungere articoli al carrello");
            return;
        }
        
        try {
            boolean success = carrelloDAO.aggiungiAlCarrello(utenteId, annuncio.getId());
            if (success) {
                mostraAlert("Successo", "Articolo aggiunto al carrello!");
            } else {
                mostraAlert("Errore", "Impossibile aggiungere l'articolo al carrello");
            }
        } catch (Exception e) {
            mostraAlert("Errore", "Si è verificato un errore: " + e.getMessage());
        }
    }
    
    /**
     * Rimuove un articolo dal carrello tramite ID annuncio
     */
    public void rimuoviDalCarrello(int annuncioId) {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return;
        carrelloDAO.rimuoviDalCarrello(utenteId, annuncioId);
    }
    
    /**
     * Rimuove un articolo dal carrello tramite oggetto Annuncio
     */
    public void rimuoviDalCarrello(Annuncio annuncio) {
        rimuoviDalCarrello(annuncio.getId());
    }
    
    /**
     * Modifica la quantità di un articolo nel carrello
     */
    public void modificaQuantita(Annuncio annuncio, int nuovaQuantita) {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return;
        
        List<application.Classe.CarrelloItem> items = getCarrelloConQuantita();
        for (application.Classe.CarrelloItem item : items) {
            if (item.getAnnuncio().getId() == annuncio.getId()) {
                carrelloDAO.aggiornaQuantita(item.getId(), nuovaQuantita);
                break;
            }
        }
    }
    
    /**
     * Restituisce la lista di annunci nel carrello come oggetti Annuncio
     */
    public List<Annuncio> getCarrello() {
        List<Annuncio> result = new ArrayList<>();
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return result;
        
        List<application.Classe.CarrelloItem> items = carrelloDAO.getCarrelloPerUtente(utenteId);
        for (application.Classe.CarrelloItem item : items) {
            for (int i = 0; i < item.getQuantita(); i++) {
                result.add(item.getAnnuncio());
            }
        }
        return result;
    }
    
    /**
     * Restituisce il carrello con informazioni sulla quantità
     */
    public List<application.Classe.CarrelloItem> getCarrelloConQuantita() {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return new ArrayList<>();
        return carrelloDAO.getCarrelloPerUtente(utenteId);
    }
    
    /**
     * Restituisce gli item del carrello per l'interfaccia utente
     */
    public List<CarrelloItem> getCarrelloItems() {
        List<CarrelloItem> result = new ArrayList<>();
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return result;
        
        List<application.Classe.CarrelloItem> itemsDB = carrelloDAO.getCarrelloPerUtente(utenteId);
        List<CarrelloItem> nuoviItems = new ArrayList<>();
        
        for (application.Classe.CarrelloItem itemDB : itemsDB) {
            CarrelloItem uiItem = new CarrelloItem(
                itemDB.getAnnuncio().getId(),
                itemDB.getAnnuncio().getTitolo(),
                itemDB.getAnnuncio().getPrezzo(),
                itemDB.getQuantita()
            );
            uiItem.setAnnuncio(itemDB.getAnnuncio());
            nuoviItems.add(uiItem);
        }
        
        sincronizzaStatiSelezione(nuoviItems);
        return nuoviItems;
    }
    
    /**
     * Svuota completamente il carrello dell'utente corrente
     */
    public void svuotaCarrello() {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return;
        carrelloDAO.svuotaCarrello(utenteId);
        statiSelezione.clear();
    }
    
    /**
     * Calcola il totale complessivo del carrello
     */
    public double getTotale() {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return 0.0;
        return carrelloDAO.getPrezzoTotaleCarrello(utenteId);
    }
    
    /**
     * Restituisce il numero totale di articoli nel carrello
     */
    public int getNumeroArticoli() {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return 0;
        return carrelloDAO.contaElementiCarrello(utenteId);
    }
    
    /**
     * Verifica se un articolo è presente nel carrello
     */
    public boolean contieneArticolo(Annuncio annuncio) {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return false;
        return carrelloDAO.isNelCarrello(utenteId, annuncio.getId());
    }
    
    /**
     * Restituisce un articolo dal carrello tramite ID
     */
    public Annuncio getArticoloById(int id) {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return null;
        
        List<application.Classe.CarrelloItem> items = carrelloDAO.getCarrelloPerUtente(utenteId);
        for (application.Classe.CarrelloItem item : items) {
            if (item.getAnnuncio().getId() == id) {
                return item.getAnnuncio();
            }
        }
        return null;
    }
    
    /**
     * Restituisce la quantità di un articolo specifico nel carrello
     */
    public int getQuantitaArticolo(Annuncio annuncio) {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return 0;
        
        List<application.Classe.CarrelloItem> items = carrelloDAO.getCarrelloPerUtente(utenteId);
        for (application.Classe.CarrelloItem item : items) {
            if (item.getAnnuncio().getId() == annuncio.getId()) {
                return item.getQuantita();
            }
        }
        return 0;
    }
    
    /**
     * Forza il ricaricamento del carrello dal database
     */
    public void ricaricaCarrello() {
        // Il DAO ricarica sempre dal database
    }
    
    // ==================== METODI SELEZIONE ARTICOLI ====================
    
    /**
     * Restituisce solo gli articoli attualmente selezionati nel carrello
     */
    public List<CarrelloItem> getCarrelloItemsSelezionati() {
        List<CarrelloItem> tuttiItems = getCarrelloItems();
        List<CarrelloItem> selezionati = new ArrayList<>();
        
        for (CarrelloItem item : tuttiItems) {
            if (item.isSelected()) {
                selezionati.add(item);
            }
        }
        return selezionati;
    }
    
    /**
     * Seleziona o deseleziona tutti gli articoli nel carrello
     */
    public void selezionaTutti(boolean selezionato) {
        for (Integer annuncioId : statiSelezione.keySet()) {
            statiSelezione.put(annuncioId, selezionato);
        }
    }
    
    /**
     * Seleziona o deseleziona un articolo specifico
     */
    public void selezionaArticolo(int annuncioId, boolean selezionato) {
        statiSelezione.put(annuncioId, selezionato);
    }
    
    /**
     * Inverte lo stato di selezione di tutti gli articoli
     */
    public void invertiSelezione() {
        for (Integer annuncioId : statiSelezione.keySet()) {
            statiSelezione.put(annuncioId, !statiSelezione.get(annuncioId));
        }
    }
    
    /**
     * Verifica se un articolo specifico è selezionato
     */
    public boolean isArticoloSelezionato(int annuncioId) {
        return statiSelezione.getOrDefault(annuncioId, true);
    }
    
    /**
     * Restituisce il numero di articoli attualmente selezionati
     */
    public int getNumeroArticoliSelezionati() {
        return getCarrelloItemsSelezionati().size();
    }
    
    /**
     * Calcola il totale degli articoli selezionati
     */
    public double getTotaleSelezionati() {
        double totale = 0.0;
        List<CarrelloItem> selezionati = getCarrelloItemsSelezionati();
        for (CarrelloItem item : selezionati) {
            totale += item.getSubtotale();
        }
        return totale;
    }
    
    /**
     * Restituisce il totale degli articoli selezionati formattato
     */
    public String getTotaleSelezionatiFormattato() {
        return String.format("€%.2f", getTotaleSelezionati());
    }
    
    /**
     * Verifica se ci sono articoli selezionati nel carrello
     */
    public boolean haArticoliSelezionati() {
        return getNumeroArticoliSelezionati() > 0;
    }
    
    /**
     * Verifica se tutti gli articoli nel carrello sono selezionati
     */
    public boolean sonoTuttiSelezionati() {
        List<CarrelloItem> tuttiItems = getCarrelloItems();
        if (tuttiItems.isEmpty()) return false;
        
        for (CarrelloItem item : tuttiItems) {
            if (!item.isSelected()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Verifica se nessun articolo è selezionato
     */
    public boolean nessunoSelezionato() {
        return !haArticoliSelezionati();
    }
    
    // ==================== METODI RIMOZIONE ARTICOLI ====================
    
    /**
     * Rimuove dal carrello tutti gli articoli attualmente selezionati
     */
    public boolean rimuoviSelezionati() {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return false;
        
        List<CarrelloItem> selezionati = getCarrelloItemsSelezionati();
        if (selezionati.isEmpty()) {
            mostraAlert("Nessuna selezione", "Seleziona gli articoli da rimuovere");
            return false;
        }
        
        int countRimossi = 0;
        for (CarrelloItem item : selezionati) {
            boolean success = carrelloDAO.rimuoviDalCarrello(utenteId, item.getAnnuncioId());
            if (success) {
                countRimossi++;
                statiSelezione.remove(item.getAnnuncioId());
            }
        }
        
        if (countRimossi > 0) {
            mostraAlert("Successo", "Rimossi " + countRimossi + " articoli dal carrello");
            return true;
        } else {
            mostraAlertErrore("Errore", "Impossibile rimuovere gli articoli selezionati");
            return false;
        }
    }
    
    /**
     * Rimuove un singolo articolo selezionato dal carrello
     */
    public boolean rimuoviArticoloSelezionato(int annuncioId) {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return false;
        
        boolean success = carrelloDAO.rimuoviDalCarrello(utenteId, annuncioId);
        if (success) {
            statiSelezione.remove(annuncioId);
        }
        return success;
    }
    
    // ==================== METODI CHECKOUT E PAGAMENTI ====================
    
    /**
     * Effettua il checkout degli articoli selezionati
     * Gestisce il trasferimento dei fondi e l'aggiornamento dello stato degli annunci
     */
    public boolean checkoutSelezionati() {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) {
            mostraAlertErrore("Errore", "Devi effettuare il login per completare l'acquisto");
            return false;
        }
        
        List<CarrelloItem> selezionati = getCarrelloItemsSelezionati();
        if (selezionati.isEmpty()) {
            mostraAlertErrore("Errore", "Seleziona gli articoli da acquistare");
            return false;
        }
        
        double totaleSelezionati = getTotaleSelezionati();
        if (totaleSelezionati <= 0) {
            mostraAlertErrore("Errore", "Il totale degli articoli selezionati non è valido");
            return false;
        }
        
        BigDecimal importoTotale = BigDecimal.valueOf(totaleSelezionati);
        if (!contoDAO.verificaSaldoSufficiente(utenteId, importoTotale)) {
            mostraAlertErrore("Saldo Insufficiente", 
                "Saldo insufficiente per completare l'acquisto.\n" +
                "Totale selezionato: €" + String.format("%.2f", totaleSelezionati) + "\n" +
                "Il tuo saldo: €" + String.format("%.2f", getSaldoUtente()) + "\n" +
                "Carica soldi per procedere.");
            return false;
        }
        
        try {
            boolean successoCompleto = true;
            List<String> articoliProcessati = new ArrayList<>();
            List<String> articoliNonProcessati = new ArrayList<>();
            
            for (CarrelloItem item : selezionati) {
                Annuncio annuncio = item.getAnnuncio();
                
                if (annuncio.getVenditoreId() <= 0) {
                    articoliNonProcessati.add(annuncio.getTitolo() + " (venditore non valido)");
                    successoCompleto = false;
                    continue;
                }
                
                BigDecimal importoArticolo = BigDecimal.valueOf(annuncio.getPrezzo() * item.getQuantita());
                String descrizione = "Acquisto: " + annuncio.getTitolo() + " (x" + item.getQuantita() + ")";
                
                // Trasferisci fondi dall'acquirente al venditore
                boolean successTrasferimento = contoDAO.trasferisciFondi(
                    utenteId, 
                    annuncio.getVenditoreId(), 
                    importoArticolo, 
                    descrizione
                );
                
                if (successTrasferimento) {
                    // Aggiorna lo stato dell'annuncio a "VENDUTO"
                    try {
                        AnnuncioDAO annuncioDAO = new AnnuncioDAO();
                        boolean successAggiornamento = annuncioDAO.aggiornaStatoAnnuncio(annuncio.getId(), "VENDUTO");
                        
                        if (successAggiornamento) {
                            articoliProcessati.add(annuncio.getTitolo());
                            // Rimuovi l'articolo processato dal carrello
                            rimuoviArticoloSelezionato(annuncio.getId());
                        } else {
                            articoliNonProcessati.add(annuncio.getTitolo() + " (errore aggiornamento stato)");
                            successoCompleto = false;
                        }
                    } catch (Exception e) {
                        articoliNonProcessati.add(annuncio.getTitolo() + " (errore database: " + e.getMessage() + ")");
                        successoCompleto = false;
                    }
                } else {
                    articoliNonProcessati.add(annuncio.getTitolo() + " (errore trasferimento)");
                    successoCompleto = false;
                }
            }
            
            if (successoCompleto || !articoliProcessati.isEmpty()) {
                String messaggio = "Acquisto completato con successo!\n" +
                    "Totale speso: €" + String.format("%.2f", totaleSelezionati) + "\n" +
                    "Nuovo saldo: €" + String.format("%.2f", getSaldoUtente());
                
                if (!articoliProcessati.isEmpty()) {
                    messaggio += "\n\nArticoli acquistati:\n• " + String.join("\n• ", articoliProcessati);
                }
                
                if (!articoliNonProcessati.isEmpty()) {
                    messaggio += "\n\nArticoli non processati:\n• " + String.join("\n• ", articoliNonProcessati);
                }
                
                mostraAlert("Checkout Completato", messaggio);
                return successoCompleto;
            } else {
                mostraAlertErrore("Errore Checkout", 
                    "Nessun articolo è stato processato correttamente:\n" +
                    String.join("\n", articoliNonProcessati) + "\n\n" +
                    "Controlla il saldo e riprova.");
                return false;
            }
            
        } catch (Exception e) {
            mostraAlertErrore("Errore", "Si è verificato un errore durante il checkout: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Effettua il checkout di tutti gli articoli nel carrello
     */
    public boolean checkoutCompleto() {
        selezionaTutti(true);
        return checkoutSelezionati();
    }
    
    // ==================== METODI GESTIONE CONTO ====================
    
    /**
     * Ricarica il conto dell'utente con l'importo specificato
     */
    public boolean ricaricaConto(double importo, String metodoPagamento) {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) {
            mostraAlertErrore("Errore", "Devi effettuare il login per ricaricare il conto");
            return false;
        }
        
        if (importo <= 0) {
            mostraAlertErrore("Errore", "L'importo deve essere maggiore di 0");
            return false;
        }
        
        try {
            BigDecimal importoBigDecimal = BigDecimal.valueOf(importo);
            boolean success = contoDAO.ricaricaConto(utenteId, importoBigDecimal, metodoPagamento);
            
            if (success) {
                mostraAlert("Successo", 
                    "Ricarica effettuata con successo!\n" +
                    "Importo: €" + String.format("%.2f", importo) + "\n" +
                    "Metodo: " + metodoPagamento + "\n" +
                    "Nuovo saldo: €" + String.format("%.2f", getSaldoUtente()));
                return true;
            } else {
                mostraAlertErrore("Errore", "Errore durante la ricarica del conto");
                return false;
            }
            
        } catch (Exception e) {
            mostraAlertErrore("Errore", "Si è verificato un errore durante la ricarica: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Restituisce il saldo corrente dell'utente
     */
    public BigDecimal getSaldoUtente() {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) {
            return BigDecimal.ZERO;
        }
        
        Conto conto = contoDAO.getContoByUtenteId(utenteId);
        return conto != null ? conto.getSaldo() : BigDecimal.ZERO;
    }
    
    /**
     * Verifica se l'utente può acquistare gli articoli selezionati
     */
    public boolean puòAcquistareSelezionati() {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return false;
        
        double totaleSelezionati = getTotaleSelezionati();
        return contoDAO.verificaSaldoSufficiente(utenteId, BigDecimal.valueOf(totaleSelezionati));
    }
    
    /**
     * Restituisce la lista dei movimenti del conto dell'utente
     */
    public List<Conto.Movimento> getMovimentiConto() {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return new ArrayList<>();
        
        Conto conto = contoDAO.getContoByUtenteId(utenteId);
        return conto != null ? conto.getMovimenti() : new ArrayList<>();
    }
    
    /**
     * Restituisce il conto completo dell'utente
     */
    public Conto getContoUtente() {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return null;
        return contoDAO.getContoByUtenteId(utenteId);
    }
    
    /**
     * Verifica se l'utente ha un conto attivo
     */
    public boolean haConto() {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return false;
        return contoDAO.getContoByUtenteId(utenteId) != null;
    }
    
    /**
     * Crea un conto per l'utente se non esiste già
     */
    public boolean creaContoSeMancante() {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return false;
        
        Conto conto = contoDAO.creaContoSeMancante(utenteId);
        return conto != null;
    }
    
    /**
     * Restituisce il saldo formattato come stringa
     */
    public String getSaldoFormattato() {
        return String.format("€%.2f", getSaldoUtente());
    }
    
    /**
     * Restituisce il totale del carrello formattato como stringa
     */
    public String getTotaleFormattato() {
        return String.format("€%.2f", getTotale());
    }
    
    /**
     * Restituisce lo stato del checkout per l'interfaccia utente
     */
    public String getStatoCheckout() {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) {
            return "Devi effettuare il login per acquistare";
        }
        
        if (getNumeroArticoli() == 0) {
            return "Il carrello è vuoto";
        }
        
        if (nessunoSelezionato()) {
            return "Seleziona gli articoli da acquistare";
        }
        
        double totaleSelezionati = getTotaleSelezionati();
        BigDecimal saldo = getSaldoUtente();
        
        if (saldo.compareTo(BigDecimal.valueOf(totaleSelezionati)) >= 0) {
            return "Pronto per l'acquisto - " + getNumeroArticoliSelezionati() + " articoli selezionati";
        } else {
            return "Saldo insufficiente per gli articoli selezionati - Ricarica il conto";
        }
    }
    
    /**
     * Restituisce lo stato dettagliato del checkout per gli articoli selezionati
     */
    public String getStatoCheckoutSelezionati() {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) {
            return "Devi effettuare il login per acquistare";
        }
        
        if (nessunoSelezionato()) {
            return "Seleziona gli articoli da acquistare";
        }
        
        double totaleSelezionati = getTotaleSelezionati();
        BigDecimal saldo = getSaldoUtente();
        
        if (saldo.compareTo(BigDecimal.valueOf(totaleSelezionati)) >= 0) {
            return "Pronto per l'acquisto - " + getNumeroArticoliSelezionati() + " articoli selezionati (€" + 
                   String.format("%.2f", totaleSelezionati) + ")";
        } else {
            return "Saldo insufficiente - Ti mancano €" + 
                   String.format("%.2f", (totaleSelezionati - saldo.doubleValue()));
        }
    }
    
    // ==================== METODI UTILITY ====================
    
    /**
     * Deseleziona tutti gli articoli nel carrello
     */
    public void pulisciSelezione() {
        selezionaTutti(false);
    }
    
    /**
     * Stampa informazioni di debug sul carrello
     */
    public void debugCarrello() {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) {
            System.out.println("[DEBUG] Nessun utente loggato");
            return;
        }
        
        List<CarrelloItem> items = getCarrelloItems();
        System.out.println("=== DEBUG CARRELLO ===");
        System.out.println("Utente ID: " + utenteId);
        System.out.println("Numero articoli: " + items.size());
        System.out.println("Articoli selezionati: " + getNumeroArticoliSelezionati());
        System.out.println("Totale selezionato: €" + getTotaleSelezionati());
        
        for (CarrelloItem item : items) {
            System.out.println("• " + item.getTitolo() + 
                             " | Prezzo: " + item.getPrezzoFormattato() +
                             " | Quantità: " + item.getQuantita() +
                             " | Subtotale: " + item.getSubtotaleFormattato() +
                             " | Selezionato: " + item.isSelected());
        }
        System.out.println("======================");
    }
    
    /**
     * Verifica se l'utente può acquistare tutto il contenuto del carrello
     */
    public boolean puòAcquistare() {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return false;
        
        double totale = getTotale();
        return contoDAO.verificaSaldoSufficiente(utenteId, BigDecimal.valueOf(totale));
    }
    
    /**
     * Stampa informazioni di debug sulla selezione degli articoli
     */
    public void debugSelezione() {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) {
            System.out.println("[DEBUG] Nessun utente loggato");
            return;
        }
        
        List<CarrelloItem> tuttiItems = getCarrelloItems();
        List<CarrelloItem> selezionati = getCarrelloItemsSelezionati();
        
        System.out.println("=== DEBUG SELEZIONE ===");
        System.out.println("Articoli totali nel carrello: " + tuttiItems.size());
        System.out.println("Articoli selezionati: " + selezionati.size());
        
        System.out.println("--- ARTICOLI TOTALI ---");
        for (CarrelloItem item : tuttiItems) {
            System.out.println("• " + item.getTitolo() + 
                             " | Quantità: " + item.getQuantita() + 
                             " | Selezionato: " + item.isSelected());
        }
        
        System.out.println("--- ARTICOLI SELEZIONATI ---");
        for (CarrelloItem item : selezionati) {
            System.out.println("• " + item.getTitolo() + 
                             " | Quantità: " + item.getQuantita() + 
                             " | Subtotale: €" + item.getSubtotale());
        }
        
        System.out.println("Totale carrello: €" + getTotale());
        System.out.println("Totale selezionati: €" + getTotaleSelezionati());
        System.out.println("======================");
    }
}