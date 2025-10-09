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
                    "UNIQUE(utente_id, annuncio_id)" + // Evita duplicati
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
        String sql = "INSERT INTO carrello (utente_id, annuncio_id, quantita, data_aggiunta) " +
                     "VALUES (?, ?, 1, CURRENT_TIMESTAMP) " +
                     "ON CONFLICT (utente_id, annuncio_id) " +
                     "DO UPDATE SET quantita = carrello.quantita + 1, " +
                     "data_aggiunta = CURRENT_TIMESTAMP";
        
        try (Connection conn = ConnessioneDB.getConnessione(); // CORRETTO: usa ConnessioneDB
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, utenteId);
            pstmt.setInt(2, annuncioId);
            
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
            
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
            if (rowsAffected > 0) {
                System.out.println("✅ Annuncio " + annuncioId + " rimosso dal carrello per utente " + utenteId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Errore nella rimozione dal carrello: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Recupera tutti gli elementi nel carrello di un utente (versione semplificata e sicura)
     */
    public List<CarrelloItem> getCarrelloPerUtente(int utenteId) {
        List<CarrelloItem> carrelloItems = new ArrayList<>();
        
        // Query semplificata con solo i dati essenziali che sappiamo esistono in Annuncio
        String sql = "SELECT c.id AS carrello_id, c.quantita, c.data_aggiunta, " +
                    "a.id AS annuncio_id, a.titolo, a.prezzo, " +
                    "u.nome AS venditore_nome " +
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
                    // Crea un Annuncio usando solo metodi che sappiamo esistono
                    Annuncio annuncio = new Annuncio();
                    annuncio.setId(rs.getInt("annuncio_id"));
                    annuncio.setTitolo(rs.getString("titolo"));
                    annuncio.setPrezzo(rs.getDouble("prezzo"));
                    
                    // Usa setNomeVenditore se esiste, altrimenti ignora
                    try {
                        annuncio.setNomeVenditore(rs.getString("venditore_nome"));
                    } catch (Exception e) {
                        System.out.println("⚠️ setNomeVenditore non disponibile in Annuncio");
                    }
                    
                    // Crea l'item del carrello
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
            System.err.println("❌ Errore nel recupero del carrello: " + e.getMessage());
            e.printStackTrace();
        }
        
        return carrelloItems;
    }

    /**
     * Recupera gli elementi del carrello con gestione sicura degli errori
     */
    public List<CarrelloItem> getCarrelloSicuroPerUtente(int utenteId) {
        List<CarrelloItem> carrelloItems = new ArrayList<>();
        
        String sql = "SELECT c.id, c.quantita, c.data_aggiunta, " +
                    "a.id AS annuncio_id, a.titolo, a.prezzo " +
                    "FROM " + TABLE_NAME + " c " +
                    "JOIN annuncio a ON c.annuncio_id = a.id " +
                    "WHERE c.utente_id = ? " +
                    "ORDER BY c.data_aggiunta DESC";
    
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, utenteId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Crea Annuncio con solo i campi base che sicuramente esistono
                    Annuncio annuncio = creaAnnuncioBase(rs);
                    
                    CarrelloItem item = new CarrelloItem(
                        rs.getInt("id"),
                        annuncio,
                        rs.getInt("quantita"),
                        rs.getTimestamp("data_aggiunta").toLocalDateTime()
                    );
                    
                    carrelloItems.add(item);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Errore nel recupero del carrello: " + e.getMessage());
            e.printStackTrace();
        }
        
        return carrelloItems;
    }

    /**
     * Crea un Annuncio solo con i campi base che sicuramente esistono
     */
    private Annuncio creaAnnuncioBase(ResultSet rs) throws SQLException {
        Annuncio annuncio = new Annuncio();
        
        // Solo i campi che sappiamo per certo esistono in Annuncio
        annuncio.setId(rs.getInt("annuncio_id"));
        annuncio.setTitolo(rs.getString("titolo"));
        annuncio.setPrezzo(rs.getDouble("prezzo"));
        
        return annuncio;
    }

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
            return rowsAffected > 0;
            
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
            return rowsAffected > 0;
            
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
            System.out.println("✅ Svuotato carrello per utente " + utenteId + ", rimossi " + rowsAffected + " items");
            return rowsAffected >= 0;
            
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
                    return rs.getInt(1);
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
                    return rs.getInt(1) > 0;
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
                    return rs.getDouble("totale");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Errore nel calcolo del totale carrello: " + e.getMessage());
            e.printStackTrace();
        }
        return 0.0;
    }
}