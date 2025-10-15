package schermata.button;

import application.Classe.Annuncio;
import application.DB.CarrelloDAO;
import application.DB.SessionManager;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import application.Classe.Conto;
import application.DB.ContoDAO;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class CarrelloManager {
    private static CarrelloManager instance;
    private CarrelloDAO carrelloDAO;
    private ContoDAO contoDAO;
    private int utenteId;
    
    // CLASSE INTERNA CarrelloItem
    public static class CarrelloItem {
        private int annuncioId;
        private String titolo;
        private double prezzo;
        private int quantita;
        
        public CarrelloItem(int annuncioId, String titolo, double prezzo, int quantita) {
            this.annuncioId = annuncioId;
            this.titolo = titolo;
            this.prezzo = prezzo;
            this.quantita = quantita;
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
    }
    
    private CarrelloManager() {
        this.carrelloDAO = new CarrelloDAO();
        this.contoDAO = new ContoDAO(); // ✅ INIZIALIZZATO ContoDAO
        this.utenteId = SessionManager.getCurrentUserId();
    }
    
    public static CarrelloManager getInstance() {
        if (instance == null) {
            instance = new CarrelloManager();
        }
        return instance;
    }
    
    public void aggiungiAlCarrello(Annuncio annuncio) {
        if (utenteId == -1) {
            System.out.println("❌ Utente non loggato, impossibile aggiungere al carrello");
            mostraAlert("Accesso richiesto", "Devi effettuare il login per aggiungere articoli al carrello");
            return;
        }
        
        try {
            boolean success = carrelloDAO.aggiungiAlCarrello(utenteId, annuncio.getId());
            if (success) {
                System.out.println("✅ Articolo '" + annuncio.getTitolo() + "' aggiunto al carrello");
                mostraAlert("Successo", "Articolo aggiunto al carrello!");
            } else {
                System.out.println("❌ Errore nell'aggiunta al carrello");
                mostraAlert("Errore", "Impossibile aggiungere l'articolo al carrello");
            }
        } catch (Exception e) {
            System.err.println("❌ Errore durante l'aggiunta al carrello: " + e.getMessage());
            mostraAlert("Errore", "Si è verificato un errore: " + e.getMessage());
        }
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
    
    public void rimuoviDalCarrello(int annuncioId) {
        if (utenteId == -1) return;
        carrelloDAO.rimuoviDalCarrello(utenteId, annuncioId);
    }
    
    public void rimuoviDalCarrello(Annuncio annuncio) {
        if (utenteId == -1) return;
        carrelloDAO.rimuoviDalCarrello(utenteId, annuncio.getId());
    }
    
    public void modificaQuantita(Annuncio annuncio, int nuovaQuantita) {
        if (utenteId == -1) return;
        
        // Prima trova l'ID del carrello per questo annuncio
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
        if (utenteId == -1) return result;
        
        List<application.Classe.CarrelloItem> items = carrelloDAO.getCarrelloPerUtente(utenteId);
        for (application.Classe.CarrelloItem item : items) {
            // Aggiungi l'annuncio tante volte quanto la quantità
            for (int i = 0; i < item.getQuantita(); i++) {
                result.add(item.getAnnuncio());
            }
        }
        return result;
    }
    
    public List<application.Classe.CarrelloItem> getCarrelloConQuantita() {
        if (utenteId == -1) return new ArrayList<>();
        return carrelloDAO.getCarrelloPerUtente(utenteId);
    }
    
    // METODO AGGIUNTO: Restituisce la lista di CarrelloItem per la TableView
    public List<CarrelloItem> getCarrelloItems() {
        List<CarrelloItem> result = new ArrayList<>();
        if (utenteId == -1) return result;
        
        List<application.Classe.CarrelloItem> itemsDB = carrelloDAO.getCarrelloPerUtente(utenteId);
        for (application.Classe.CarrelloItem itemDB : itemsDB) {
            // Converti dal CarrelloItem del database al CarrelloItem per la UI
            CarrelloItem uiItem = new CarrelloItem(
                itemDB.getAnnuncio().getId(),
                itemDB.getAnnuncio().getTitolo(),
                itemDB.getAnnuncio().getPrezzo(),
                itemDB.getQuantita()
            );
            result.add(uiItem);
        }
        return result;
    }
    
    public void svuotaCarrello() {
        if (utenteId == -1) return;
        carrelloDAO.svuotaCarrello(utenteId);
    }
    
    public double getTotale() {
        if (utenteId == -1) return 0.0;
        return carrelloDAO.getPrezzoTotaleCarrello(utenteId);
    }
    
    public int getNumeroArticoli() {
        if (utenteId == -1) return 0;
        return carrelloDAO.contaElementiCarrello(utenteId);
    }
    
    public boolean contieneArticolo(Annuncio annuncio) {
        if (utenteId == -1) return false;
        return carrelloDAO.isNelCarrello(utenteId, annuncio.getId());
    }
    
    public Annuncio getArticoloById(int id) {
        if (utenteId == -1) return null;
        
        List<application.Classe.CarrelloItem> items = carrelloDAO.getCarrelloPerUtente(utenteId);
        for (application.Classe.CarrelloItem item : items) {
            if (item.getAnnuncio().getId() == id) {
                return item.getAnnuncio();
            }
        }
        return null;
    }
    
    public int getQuantitaArticolo(Annuncio annuncio) {
        if (utenteId == -1) return 0;
        
        List<application.Classe.CarrelloItem> items = carrelloDAO.getCarrelloPerUtente(utenteId);
        for (application.Classe.CarrelloItem item : items) {
            if (item.getAnnuncio().getId() == annuncio.getId()) {
                return item.getQuantita();
            }
        }
        return 0;
    }
    
    /**
     * Metodo per aggiornare l'ID utente quando cambia la sessione
     */
    public void aggiornaUtente() {
        this.utenteId = SessionManager.getCurrentUserId();
    }
    
    /**
     * Metodo per forzare il ricaricamento del carrello
     */
    public void ricaricaCarrello() {
        // Il DAO ricarica sempre dal database, quindi non serve fare nulla qui
        System.out.println("🔄 Carrello ricaricato dal database");
    }
    
    // ✅ METODI AGGIUNTI PER LA GESTIONE DEL CONTO
    
    /**
     * Effettua il checkout del carrello trasferendo i fondi dall'acquirente ai venditori
     */
    public boolean checkout() {
        if (utenteId == -1) {
            mostraAlertErrore("Errore", "Devi effettuare il login per completare l'acquisto");
            return false;
        }
        
        double totale = getTotale();
        if (totale <= 0) {
            mostraAlertErrore("Errore", "Il carrello è vuoto");
            return false;
        }
        
        // Verifica saldo sufficiente
        BigDecimal importoTotale = BigDecimal.valueOf(totale);
        if (!contoDAO.verificaSaldoSufficiente(utenteId, importoTotale)) {
            mostraAlertErrore("Saldo Insufficiente", 
                "Saldo insufficiente per completare l'acquisto.\n" +
                "Totale: €" + String.format("%.2f", totale) + "\n" +
                "Il tuo saldo: €" + String.format("%.2f", getSaldoUtente()) + "\n" +
                "Carica soldi per procedere.");
            return false;
        }
        
        try {
            // Processa ogni articolo nel carrello
            List<application.Classe.CarrelloItem> items = getCarrelloConQuantita();
            boolean successoCompleto = true;
            List<String> articoliNonProcessati = new ArrayList<>();
            
            for (application.Classe.CarrelloItem item : items) {
                Annuncio annuncio = item.getAnnuncio();
                BigDecimal importoArticolo = BigDecimal.valueOf(annuncio.getPrezzo() * item.getQuantita());
                String descrizione = "Acquisto: " + annuncio.getTitolo() + " (x" + item.getQuantita() + ")";
                
                // Trasferisci fondi dall'acquirente al venditore
                boolean success = contoDAO.trasferisciFondi(
                    utenteId, 
                    annuncio.getVenditoreId(), 
                    importoArticolo, 
                    descrizione
                );
                
                if (!success) {
                    successoCompleto = false;
                    articoliNonProcessati.add(annuncio.getTitolo());
                    System.err.println("❌ Errore nel trasferimento per articolo: " + annuncio.getTitolo());
                } else {
                    System.out.println("✅ Articolo processato: " + annuncio.getTitolo());
                }
            }
            
            if (successoCompleto) {
                // Svuota il carrello dopo l'acquisto
                svuotaCarrello();
                mostraAlert("Successo", 
                    "Acquisto completato con successo!\n" +
                    "Totale speso: €" + String.format("%.2f", totale) + "\n" +
                    "Nuovo saldo: €" + String.format("%.2f", getSaldoUtente()));
                System.out.println("✅ Checkout completato per utente: " + utenteId);
                return true;
            } else {
                mostraAlertErrore("Errore Parziale", 
                    "Alcuni articoli non sono stati processati:\n" +
                    String.join("\n", articoliNonProcessati) + "\n\n" +
                    "Controlla il saldo e riprova.");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Errore durante il checkout: " + e.getMessage());
            mostraAlertErrore("Errore", "Si è verificato un errore durante il checkout: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Ricarica il conto dell'utente
     */
    public boolean ricaricaConto(double importo, String metodoPagamento) {
        if (utenteId == -1) {
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
                System.out.println("✅ Ricarica conto: " + importo + " per utente " + utenteId);
                return true;
            } else {
                mostraAlertErrore("Errore", "Errore durante la ricarica del conto");
                return false;
            }
            
        } catch (Exception e) {
            System.err.println("❌ Errore durante la ricarica: " + e.getMessage());
            mostraAlertErrore("Errore", "Si è verificato un errore durante la ricarica: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Ottiene il saldo corrente dell'utente
     */
    public BigDecimal getSaldoUtente() {
        if (utenteId == -1) {
            return BigDecimal.ZERO;
        }
        
        Conto conto = contoDAO.getContoByUtenteId(utenteId);
        return conto != null ? conto.getSaldo() : BigDecimal.ZERO;
    }
    
    /**
     * Verifica se l'utente può effettuare l'acquisto
     */
    public boolean puòAcquistare() {
        if (utenteId == -1) return false;
        
        double totale = getTotale();
        return contoDAO.verificaSaldoSufficiente(utenteId, BigDecimal.valueOf(totale));
    }
    
    /**
     * Ottiene i movimenti del conto
     */
    public List<Conto.Movimento> getMovimentiConto() {
        if (utenteId == -1) return new ArrayList<>();
        
        Conto conto = contoDAO.getContoByUtenteId(utenteId);
        return conto != null ? conto.getMovimenti() : new ArrayList<>();
    }
    
    /**
     * Ottiene il conto completo dell'utente
     */
    public Conto getContoUtente() {
        if (utenteId == -1) return null;
        return contoDAO.getContoByUtenteId(utenteId);
    }
    
    /**
     * Verifica se l'utente ha un conto
     */
    public boolean haConto() {
        if (utenteId == -1) return false;
        return contoDAO.getContoByUtenteId(utenteId) != null;
    }
    
    /**
     * Crea un conto per l'utente se non esiste
     */
    public boolean creaContoSeMancante() {
        if (utenteId == -1) return false;
        
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
        if (utenteId == -1) {
            return "Devi effettuare il login per acquistare";
        }
        
        if (getNumeroArticoli() == 0) {
            return "Il carrello è vuoto";
        }
        
        double totale = getTotale();
        BigDecimal saldo = getSaldoUtente();
        
        if (saldo.compareTo(BigDecimal.valueOf(totale)) >= 0) {
            return "Pronto per l'acquisto - Saldo sufficiente";
        } else {
            return "Saldo insufficiente - Ricarica il conto";
        }
    }
}