package schermata.button;

import application.Classe.Annuncio;
import java.util.ArrayList;
import java.util.List;

public class CarrelloManager {
    private static CarrelloManager instance;
    private List<CarrelloItem> carrello;
    
    private CarrelloManager() {
        carrello = new ArrayList<>();
    }
    
    public static CarrelloManager getInstance() {
        if (instance == null) {
            instance = new CarrelloManager();
        }
        return instance;
    }
    
    public void aggiungiAlCarrello(Annuncio annuncio) {
        // Cerca se l'articolo è già nel carrello
        for (CarrelloItem item : carrello) {
            if (item.getAnnuncio().getId() == annuncio.getId()) {
                item.setQuantita(item.getQuantita() + 1);
                return;
            }
        }
        
        // Se non esiste, aggiungi nuovo articolo
        carrello.add(new CarrelloItem(annuncio, 1));
    }
    
    public void rimuoviDalCarrello(Annuncio annuncio) {
        carrello.removeIf(item -> item.getAnnuncio().getId() == annuncio.getId());
    }
    
    public void modificaQuantita(Annuncio annuncio, int nuovaQuantita) {
        for (CarrelloItem item : carrello) {
            if (item.getAnnuncio().getId() == annuncio.getId()) {
                if (nuovaQuantita <= 0) {
                    rimuoviDalCarrello(annuncio);
                } else {
                    item.setQuantita(nuovaQuantita);
                }
                return;
            }
        }
    }
    
    public List<Annuncio> getCarrello() {
        List<Annuncio> result = new ArrayList<>();
        for (CarrelloItem item : carrello) {
            // Aggiungi l'annuncio tante volte quanto la quantità
            for (int i = 0; i < item.getQuantita(); i++) {
                result.add(item.getAnnuncio());
            }
        }
        return result;
    }
    
    public List<CarrelloItem> getCarrelloConQuantita() {
        return new ArrayList<>(carrello);
    }
    
    public void svuotaCarrello() {
        carrello.clear();
    }
    
    public double getTotale() {
        double totale = 0;
        for (CarrelloItem item : carrello) {
            totale += item.getAnnuncio().getPrezzo() * item.getQuantita();
        }
        return totale;
    }
    
    public int getNumeroArticoli() {
        int count = 0;
        for (CarrelloItem item : carrello) {
            count += item.getQuantita();
        }
        return count;
    }
    
    public boolean contieneArticolo(Annuncio annuncio) {
        for (CarrelloItem item : carrello) {
            if (item.getAnnuncio().getId() == annuncio.getId()) {
                return true;
            }
        }
        return false;
    }
    
    public Annuncio getArticoloById(int id) {
        for (CarrelloItem item : carrello) {
            if (item.getAnnuncio().getId() == id) {
                return item.getAnnuncio();
            }
        }
        return null;
    }
    
    public int getQuantitaArticolo(Annuncio annuncio) {
        for (CarrelloItem item : carrello) {
            if (item.getAnnuncio().getId() == annuncio.getId()) {
                return item.getQuantita();
            }
        }
        return 0;
    }
    
    // Classe interna per gestire quantità
    public static class CarrelloItem {
        private Annuncio annuncio;
        private int quantita;
        
        public CarrelloItem(Annuncio annuncio, int quantita) {
            this.annuncio = annuncio;
            this.quantita = quantita;
        }
        
        public Annuncio getAnnuncio() {
            return annuncio;
        }
        
        public int getQuantita() {
            return quantita;
        }
        
        public void setQuantita(int quantita) {
            this.quantita = quantita;
        }
        
        public double getSubtotale() {
            return annuncio.getPrezzo() * quantita;
        }
    }
}