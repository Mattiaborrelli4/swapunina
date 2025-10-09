package schermata.button;

import application.Classe.Annuncio;
import application.DB.CarrelloDAO;
import application.DB.SessionManager;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.util.ArrayList;
import java.util.List;

public class CarrelloManager {
    private static CarrelloManager instance;
    private CarrelloDAO carrelloDAO;
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

    // Aggiungi questo metodo helper
    private void mostraAlert(String titolo, String messaggio) {
        // Se sei in un contesto JavaFX, usa Alert
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
}