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

public class CarrelloManager {
    private static CarrelloManager instance;
    private CarrelloDAO carrelloDAO;
    private ContoDAO contoDAO;
    
    // Mappa per mantenere lo stato di selezione degli articoli
    private Map<Integer, Boolean> statiSelezione = new HashMap<>();
    
    // CLASSE INTERNA CarrelloItem con proprietà selected
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
        
        // Getter e Setter
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
        
        public double getSubtotale() {
            return prezzo * quantita;
        }
        
        public String getSubtotaleFormattato() {
            return String.format("€%.2f", getSubtotale());
        }
        
        public String getPrezzoFormattato() {
            return String.format("€%.2f", prezzo);
        }
    }
    
    private CarrelloManager() {
        this.carrelloDAO = new CarrelloDAO();
        this.contoDAO = new ContoDAO();
    }
    
    public static CarrelloManager getInstance() {
        if (instance == null) {
            instance = new CarrelloManager();
        }
        return instance;
    }
    
    // Metodo per sincronizzare gli stati di selezione
    private void sincronizzaStatiSelezione(List<CarrelloItem> nuoviItems) {
        for (CarrelloItem item : nuoviItems) {
            // Se l'articolo è già presente nella mappa, usa il suo stato
            // Altrimenti, inizializza come selezionato
            if (statiSelezione.containsKey(item.getAnnuncioId())) {
                item.setSelected(statiSelezione.get(item.getAnnuncioId()));
            } else {
                statiSelezione.put(item.getAnnuncioId(), true);
                item.setSelected(true);
            }
        }
        
        // Rimuovi dalla mappa gli articoli che non sono più nel carrello
        List<Integer> articoliDaRimuovere = new ArrayList<>();
        for (Integer annuncioId : statiSelezione.keySet()) {
            boolean presente = false;
            for (CarrelloItem item : nuoviItems) {
                if (item.getAnnuncioId() == annuncioId) {
                    presente = true;
                    break;
                }
            }
            if (!presente) {
                articoliDaRimuovere.add(annuncioId);
            }
        }
        
        for (Integer annuncioId : articoliDaRimuovere) {
            statiSelezione.remove(annuncioId);
        }
    }
    
    // Metodo per aggiornare lo stato di selezione di un articolo
    public void aggiornaStatoSelezione(int annuncioId, boolean selected) {
        statiSelezione.put(annuncioId, selected);
    }
    
    // Metodo per ottenere lo stato di selezione di un articolo
    public boolean getStatoSelezione(int annuncioId) {
        return statiSelezione.getOrDefault(annuncioId, true);
    }
    
    private int getCurrentUserId() {
        return SessionManager.getCurrentUserId();
    }
    
    private boolean isUserLoggedIn() {
        return getCurrentUserId() > 0;
    }

    // Metodo helper per mostrare alert
    private void mostraAlert(String titolo, String messaggio) {
        try {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle(titolo);
                alert.setHeaderText(null);
                alert.setContentText(messaggio);
                alert.showAndWait();
            });
        } catch (Exception e) {
            System.out.println(titolo + ": " + messaggio);
        }
    }
    
    // Metodo helper per mostrare alert di errore
    private void mostraAlertErrore(String titolo, String messaggio) {
        try {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle(titolo);
                alert.setHeaderText(null);
                alert.setContentText(messaggio);
                alert.showAndWait();
            });
        } catch (Exception e) {
            System.out.println("ERRORE - " + titolo + ": " + messaggio);
        }
    }
    
    // ==================== METODI BASE CARRELLO ====================
    
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
    
    public void rimuoviDalCarrello(int annuncioId) {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return;
        carrelloDAO.rimuoviDalCarrello(utenteId, annuncioId);
    }
    
    public void rimuoviDalCarrello(Annuncio annuncio) {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return;
        carrelloDAO.rimuoviDalCarrello(utenteId, annuncio.getId());
    }
    
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
    
    public List<application.Classe.CarrelloItem> getCarrelloConQuantita() {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return new ArrayList<>();
        return carrelloDAO.getCarrelloPerUtente(utenteId);
    }
    
    // METODO getCarrelloItems UNICO E CORRETTO
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
        
        // Sincronizza gli stati di selezione
        sincronizzaStatiSelezione(nuoviItems);
        
        return nuoviItems;
    }

    // Metodo helper per trovare un item per ID
    private CarrelloItem trovaItemPerId(List<CarrelloItem> items, int annuncioId) {
        for (CarrelloItem item : items) {
            if (item.getAnnuncioId() == annuncioId) {
                return item;
            }
        }
        return null;
    }
    
    public void svuotaCarrello() {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return;
        carrelloDAO.svuotaCarrello(utenteId);
    }
    
    public double getTotale() {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return 0.0;
        return carrelloDAO.getPrezzoTotaleCarrello(utenteId);
    }
    
    public int getNumeroArticoli() {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return 0;
        return carrelloDAO.contaElementiCarrello(utenteId);
    }
    
    public boolean contieneArticolo(Annuncio annuncio) {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return false;
        return carrelloDAO.isNelCarrello(utenteId, annuncio.getId());
    }
    
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
     * Metodo per forzare il ricaricamento del carrello
     */
    public void ricaricaCarrello() {
        // Il DAO ricarica sempre dal database, quindi non serve fare nulla qui
    }
    
    // ==================== METODI SELEZIONE ARTICOLI ====================
    
    /**
     * Ottiene solo gli articoli selezionati nel carrello
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
     * Seleziona/deseleziona tutti gli articoli nel carrello
     */
    public void selezionaTutti(boolean selezionato) {
        List<CarrelloItem> tuttiItems = getCarrelloItems();
        for (CarrelloItem item : tuttiItems) {
            statiSelezione.put(item.getAnnuncioId(), selezionato);
            item.setSelected(selezionato);
        }
    }
    
    /**
     * Seleziona/deseleziona un articolo specifico
     */
    public void selezionaArticolo(int annuncioId, boolean selezionato) {
        statiSelezione.put(annuncioId, selezionato);
    }
    
    /**
     * Inverte la selezione di tutti gli articoli
     */
    public void invertiSelezione() {
        List<CarrelloItem> tuttiItems = getCarrelloItems();
        for (CarrelloItem item : tuttiItems) {
            boolean nuovoStato = !item.isSelected();
            item.setSelected(nuovoStato);
            statiSelezione.put(item.getAnnuncioId(), nuovoStato);
        }
    }
    
    /**
     * Verifica se un articolo è selezionato
     */
    public boolean isArticoloSelezionato(int annuncioId) {
        return statiSelezione.getOrDefault(annuncioId, true);
    }
    
    /**
     * Ottiene il numero di articoli selezionati
     */
    public int getNumeroArticoliSelezionati() {
        return getCarrelloItemsSelezionati().size();
    }
    
    /**
     * Ottiene il totale degli articoli selezionati
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
     * Ottiene il totale formattato degli articoli selezionati
     */
    public String getTotaleSelezionatiFormattato() {
        return String.format("€%.2f", getTotaleSelezionati());
    }
    
    /**
     * Verifica se ci sono articoli selezionati
     */
    public boolean haArticoliSelezionati() {
        return getNumeroArticoliSelezionati() > 0;
    }
    
    /**
     * Verifica se tutti gli articoli sono selezionati
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
    
    // ==================== METODI GESTIONE ARTICOLI SELEZIONATI ====================
    
    /**
     * Rimuove gli articoli selezionati dal carrello
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
                // Rimuovi anche dalla mappa di selezione
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
     * Rimuove un singolo articolo selezionato
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
    
    // ==================== METODI CHECKOUT ====================
    
    /**
     * Effettua il checkout solo degli articoli selezionati
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
        
        // Verifica saldo sufficiente
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
            
            System.out.println("[DEBUG] Checkout selezionati per utente ID: " + utenteId);
            System.out.println("[DEBUG] Numero articoli selezionati: " + selezionati.size());
            
            for (CarrelloItem item : selezionati) {
                Annuncio annuncio = item.getAnnuncio();
                
                System.out.println("[DEBUG] Processando articolo selezionato: " + annuncio.getTitolo() + 
                                 " | Venditore ID: " + annuncio.getVenditoreId() +
                                 " | Quantità: " + item.getQuantita() +
                                 " | Prezzo: " + annuncio.getPrezzo());
                
                if (annuncio.getVenditoreId() <= 0) {
                    System.err.println("[DEBUG] ❌ Venditore ID non valido per: " + annuncio.getTitolo());
                    articoliNonProcessati.add(annuncio.getTitolo() + " (venditore non valido)");
                    successoCompleto = false;
                    continue;
                }
                
                BigDecimal importoArticolo = BigDecimal.valueOf(annuncio.getPrezzo() * item.getQuantita());
                String descrizione = "Acquisto: " + annuncio.getTitolo() + " (x" + item.getQuantita() + ")";
                
                // 1. Trasferisci fondi dall'acquirente al venditore
                boolean successTrasferimento = contoDAO.trasferisciFondi(
                    utenteId, 
                    annuncio.getVenditoreId(), 
                    importoArticolo, 
                    descrizione
                );
                
                if (successTrasferimento) {
                    // 2. Aggiorna lo stato dell'annuncio a "VENDUTO"
                    try {
                        AnnuncioDAO annuncioDAO = new AnnuncioDAO();
                        boolean successAggiornamento = annuncioDAO.aggiornaStatoAnnuncio(annuncio.getId(), "VENDUTO");
                        
                        if (successAggiornamento) {
                            articoliProcessati.add(annuncio.getTitolo());
                            // 3. Rimuovi SOLO l'articolo processato dal carrello
                            rimuoviArticoloSelezionato(annuncio.getId());
                        } else {
                            articoliNonProcessati.add(annuncio.getTitolo() + " (errore aggiornamento stato)");
                            successoCompleto = false;
                        }
                    } catch (SQLException e) {
                        articoliNonProcessati.add(annuncio.getTitolo() + " (errore database)");
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
            mostraAlertErrore("Errore", "Si è verificato un errore durante el checkout: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Effettua il checkout completo di tutti gli articoli nel carrello
     */
    public boolean checkoutCompleto() {
        // Seleziona tutti gli articoli e poi fa checkout
        selezionaTutti(true);
        return checkoutSelezionati();
    }
    
    // ==================== METODI GESTIONE CONTO ====================
    
    /**
     * Ricarica il conto dell'utente
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
     * Ottiene il saldo corrente dell'utente
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
     * Verifica se l'utente può effettuare l'acquisto degli articoli selezionati
     */
    public boolean puòAcquistareSelezionati() {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return false;
        
        double totaleSelezionati = getTotaleSelezionati();
        return contoDAO.verificaSaldoSufficiente(utenteId, BigDecimal.valueOf(totaleSelezionati));
    }
    
    /**
     * Ottiene i movimenti del conto
     */
    public List<Conto.Movimento> getMovimentiConto() {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return new ArrayList<>();
        
        Conto conto = contoDAO.getContoByUtenteId(utenteId);
        return conto != null ? conto.getMovimenti() : new ArrayList<>();
    }
    
    /**
     * Ottiene il conto completo dell'utente
     */
    public Conto getContoUtente() {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return null;
        return contoDAO.getContoByUtenteId(utenteId);
    }
    
    /**
     * Verifica se l'utente ha un conto
     */
    public boolean haConto() {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return false;
        return contoDAO.getContoByUtenteId(utenteId) != null;
    }
    
    /**
     * Crea un conto per l'utente se non esiste
     */
    public boolean creaContoSeMancante() {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return false;
        
        Conto conto = contoDAO.creaContoSeMancante(utenteId);
        return conto != null;
    }
    
    /**
     * Ottiene il saldo formattato come stringa
     */
    public String getSaldoFormattato() {
        return String.format("€%.2f", getSaldoUtente());
    }
    
    /**
     * Ottiene il totale formattato come stringa
     */
    public String getTotaleFormattato() {
        return String.format("€%.2f", getTotale());
    }
    
    /**
     * Verifica se il checkout è possibile e restituisce un messaggio di stato
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
     * Verifica se il checkout degli articoli selezionati è possibile
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
     * Pulisce la selezione (deseleziona tutto)
     */
    public void pulisciSelezione() {
        selezionaTutti(false);
    }
    
    /**
     * Debug: stampa lo stato del carrello
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
     * Metodo per compatibilità - verifica se l'utente può acquistare tutto il carrello
     */
    public boolean puòAcquistare() {
        int utenteId = getCurrentUserId();
        if (utenteId <= 0) return false;
        
        double totale = getTotale();
        return contoDAO.verificaSaldoSufficiente(utenteId, BigDecimal.valueOf(totale));
    }
    
    /**
     * Metodo per debug - mostra articoli selezionati vs totali
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