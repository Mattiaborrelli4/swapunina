package application.DB;

import application.Classe.Annuncio;
import application.Classe.CarrelloItem;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CarrelloDAO {
    private static final String TABLE_NAME = "carrello";

    public CarrelloDAO() {
        creaTabellaSeMancante();
    }

    private void creaTabellaSeMancante() {
        try (Connection conn = ConnessioneDB.getConnessione()) {
            String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                    "id SERIAL PRIMARY KEY, " +
                    "utente_id INTEGER NOT NULL REFERENCES utente(id) ON DELETE CASCADE, " +
                    "annuncio_id INTEGER NOT NULL REFERENCES annuncio(id) ON DELETE CASCADE, " +
                    "quantita INTEGER NOT NULL DEFAULT 1, " +
                    "data_aggiunta TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE(utente_id, annuncio_id)" +
                    ")";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.execute();
                System.out.println("✅ Tabella carrello verificata/creata");
            }
        } catch (SQLException e) {
            System.err.println("❌ Errore nella creazione della tabella carrello");
            e.printStackTrace();
        }
    }

    /**
     * Aggiunge un annuncio al carrello dell'utente
     */
    public boolean aggiungiAlCarrello(int utenteId, int annuncioId) {
        // ✅ MIGLIORATO: Query più efficiente
        String sql = "INSERT INTO " + TABLE_NAME + " (utente_id, annuncio_id, quantita) " +
                    "VALUES (?, ?, 1) " +
                    "ON CONFLICT (utente_id, annuncio_id) " +
                    "DO UPDATE SET quantita = " + TABLE_NAME + ".quantita + 1, " +
                    "data_aggiunta = CURRENT_TIMESTAMP " +
                    "RETURNING id";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, utenteId);
            pstmt.setInt(2, annuncioId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                boolean success = rs.next();
                System.out.println("✅ Aggiunto al carrello - Utente: " + utenteId + ", Annuncio: " + annuncioId);
                return success;
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Errore SQL nell'aggiunta al carrello: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Rimuove un annuncio dal carrello dell'utente
     */
    public boolean rimuoviDalCarrello(int utenteId, int annuncioId) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE utente_id = ? AND annuncio_id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, utenteId);
            stmt.setInt(2, annuncioId);
            
            int rowsAffected = stmt.executeUpdate();
            boolean success = rowsAffected > 0;
            
            if (success) {
                System.out.println("✅ Rimosso dal carrello - Utente: " + utenteId + ", Annuncio: " + annuncioId);
            } else {
                System.out.println("⚠️ Nessun articolo trovato da rimuovere - Utente: " + utenteId + ", Annuncio: " + annuncioId);
            }
            
            return success;
            
        } catch (SQLException e) {
            System.err.println("❌ Errore nella rimozione dal carrello: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * ✅ CORRETTO: Recupera tutti gli elementi nel carrello di un utente con VENDITORE_ID
     */
    public List<CarrelloItem> getCarrelloPerUtente(int utenteId) {
        List<CarrelloItem> carrelloItems = new ArrayList<>();
        
        // ✅ CORREZIONE: Aggiunto a.venditore_id nella query
        String sql = "SELECT c.id AS carrello_id, c.quantita, c.data_aggiunta, " +
                    "a.id AS annuncio_id, a.titolo, a.prezzo, a.venditore_id, " + // ✅ AGGIUNTO venditore_id
                    "u.nome AS venditore_nome, u.cognome AS venditore_cognome " +
                    "FROM " + TABLE_NAME + " c " +
                    "JOIN annuncio a ON c.annuncio_id = a.id " +
                    "JOIN utente u ON a.venditore_id = u.id " +
                    "WHERE c.utente_id = ? " +
                    "ORDER BY c.data_aggiunta DESC";
    
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, utenteId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // ✅ CORREZIONE: Crea Annuncio con venditore_id
                    Annuncio annuncio = creaAnnuncioCompleto(rs);
                    
                    CarrelloItem item = new CarrelloItem(
                        rs.getInt("carrello_id"),
                        annuncio,
                        rs.getInt("quantita"),
                        rs.getTimestamp("data_aggiunta").toLocalDateTime()
                    );
                    
                    carrelloItems.add(item);
                }
            }
            
            System.out.println("✅ Caricati " + carrelloItems.size() + " elementi nel carrello per utente " + utenteId);
            
        } catch (SQLException e) {
            System.err.println("❌ Errore nel recupero del carrello per utente " + utenteId + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return carrelloItems;
    }

    /**
     * ✅ NUOVO: Crea un Annuncio completo con tutti i campi necessari
     */
    private Annuncio creaAnnuncioCompleto(ResultSet rs) throws SQLException {
        Annuncio annuncio = new Annuncio();
        
        // Campi base
        annuncio.setId(rs.getInt("annuncio_id"));
        annuncio.setTitolo(rs.getString("titolo"));
        annuncio.setPrezzo(rs.getDouble("prezzo"));
        
        // ✅ AGGIUNTO: Imposta venditore_id (CRUCIALE per il checkout)
        annuncio.setVenditoreId(rs.getInt("venditore_id"));
        
        // Campi opzionali (se esistono i setter)
        try {
            annuncio.setNomeVenditore(rs.getString("venditore_nome") + " " + rs.getString("venditore_cognome"));
        } catch (Exception e) {
            // Ignora se il setter non esiste
            System.out.println("⚠️ setNomeVenditore non disponibile in Annuncio");
        }
        
        return annuncio;
    }

    /**
     * ❌ RIMOSSO: getCarrelloSicuroPerUtente - metodo duplicato che causa confusione
     * Usiamo solo getCarrelloPerUtente che è più completo
     */

    /**
     * Aggiorna la quantità di un item nel carrello
     */
    public boolean aggiornaQuantita(int carrelloId, int nuovaQuantita) {
        if (nuovaQuantita <= 0) {
            return rimuoviItem(carrelloId);
        }
        
        String sql = "UPDATE " + TABLE_NAME + " SET quantita = ? WHERE id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, nuovaQuantita);
            stmt.setInt(2, carrelloId);
            
            int rowsAffected = stmt.executeUpdate();
            boolean success = rowsAffected > 0;
            
            if (success) {
                System.out.println("✅ Aggiornata quantità - CarrelloID: " + carrelloId + ", Nuova quantità: " + nuovaQuantita);
            }
            
            return success;
            
        } catch (SQLException e) {
            System.err.println("❌ Errore nell'aggiornamento della quantità: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Rimuove un item specifico dal carrello usando l'ID del carrello
     */
    public boolean rimuoviItem(int carrelloId) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, carrelloId);
            
            int rowsAffected = stmt.executeUpdate();
            boolean success = rowsAffected > 0;
            
            if (success) {
                System.out.println("✅ Rimosso item carrello - ID: " + carrelloId);
            }
            
            return success;
            
        } catch (SQLException e) {
            System.err.println("❌ Errore nella rimozione dell'item: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Svuota completamente il carrello di un utente
     */
    public boolean svuotaCarrello(int utenteId) {
        String sql = "DELETE FROM " + TABLE_NAME + " WHERE utente_id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, utenteId);
            
            int rowsAffected = stmt.executeUpdate();
            boolean success = rowsAffected >= 0; // 0 è successo se il carrello era vuoto
            
            System.out.println("✅ Svuotato carrello per utente " + utenteId + ", rimossi " + rowsAffected + " items");
            return success;
            
        } catch (SQLException e) {
            System.err.println("❌ Errore nello svuotamento del carrello: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Conta il numero di elementi nel carrello di un utente
     */
    public int contaElementiCarrello(int utenteId) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE utente_id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, utenteId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt(1);
                    System.out.println("✅ Elementi nel carrello per utente " + utenteId + ": " + count);
                    return count;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Errore nel conteggio elementi carrello: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Verifica se un annuncio è già nel carrello dell'utente
     */
    public boolean isNelCarrello(int utenteId, int annuncioId) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE utente_id = ? AND annuncio_id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, utenteId);
            stmt.setInt(2, annuncioId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    boolean presente = rs.getInt(1) > 0;
                    System.out.println("✅ Verifica carrello - Utente: " + utenteId + 
                                     ", Annuncio: " + annuncioId + ", Presente: " + presente);
                    return presente;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Errore nella verifica carrello: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Ottiene il prezzo totale del carrello per un utente
     */
    public double getPrezzoTotaleCarrello(int utenteId) {
        String sql = "SELECT SUM(a.prezzo * c.quantita) as totale " +
                    "FROM " + TABLE_NAME + " c " +
                    "JOIN annuncio a ON c.annuncio_id = a.id " +
                    "WHERE c.utente_id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, utenteId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    double totale = rs.getDouble("totale");
                    System.out.println("✅ Totale carrello per utente " + utenteId + ": €" + totale);
                    return totale;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Errore nel calcolo del totale carrello: " + e.getMessage());
            e.printStackTrace();
        }
        return 0.0;
    }

    /**
     * ✅ AGGIUNTO: Metodo per debug - stampa contenuto carrello
     */
    public void debugCarrello(int utenteId) {
        System.out.println("=== DEBUG CARRELLO Utente " + utenteId + " ===");
        List<CarrelloItem> items = getCarrelloPerUtente(utenteId);
        for (CarrelloItem item : items) {
            Annuncio annuncio = item.getAnnuncio();
            System.out.println("• " + annuncio.getTitolo() + 
                             " | Prezzo: €" + annuncio.getPrezzo() +
                             " | Venditore ID: " + annuncio.getVenditoreId() +
                             " | Quantità: " + item.getQuantita());
        }
        System.out.println("Totale: €" + getPrezzoTotaleCarrello(utenteId));
        System.out.println("=================================");
    }
}