package application.DB;

import application.Classe.utente;
import java.sql.*;

/**
 * Gestisce le operazioni di accesso ai dati per gli utenti
 * Fornisce metodi per recupero e gestione degli utenti nel database
 */
public class UserDAO {
    
    /**
     * Recupera un utente tramite ID
     * @param userId l'ID dell'utente da recuperare
     * @return l'oggetto utente, null se non trovato
     */
    public utente getUserById(int userId) {
        String sql = "SELECT id, nome, cognome, email, matricola FROM utente WHERE id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUtente(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore recupero utente per ID " + userId + ": " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Recupera un utente tramite email
     * @param email l'email dell'utente
     * @return l'oggetto utente, null se non trovato
     */
    public utente getUserByEmail(String email) {
        String sql = "SELECT id, nome, cognome, email, matricola FROM utente WHERE email = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, email);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUtente(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore recupero utente per email " + email + ": " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Recupera un utente tramite matricola
     * @param matricola la matricola dell'utente
     * @return l'oggetto utente, null se non trovato
     */
    public utente getUserByMatricola(String matricola) {
        String sql = "SELECT id, nome, cognome, email, matricola FROM utente WHERE matricola = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, matricola);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUtente(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore recupero utente per matricola " + matricola + ": " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Verifica le credenziali di login
     * @param email l'email dell'utente
     * @param password la password dell'utente
     * @return l'oggetto utente se le credenziali sono valide, null altrimenti
     */
    public utente login(String email, String password) {
        String sql = "SELECT id, nome, cognome, email, matricola FROM utente WHERE email = ? AND password = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, email);
            stmt.setString(2, password); // Nota: in produzione usare hash delle password
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUtente(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore durante il login per email " + email + ": " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Crea un nuovo utente nel database
     * @param user l'utente da creare
     * @return true se l'operazione è riuscita, false altrimenti
     */
    public boolean createUser(utente user) {
        String sql = "INSERT INTO utente (nome, cognome, email, password, matricola) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, user.getNome());
            stmt.setString(2, user.getCognome());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPassword()); // Nota: in produzione usare hash
            stmt.setString(5, user.getMatricola());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Errore creazione utente: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Aggiorna i dati di un utente esistente
     * @param user l'utente con i dati aggiornati
     * @return true se l'operazione è riuscita, false altrimenti
     */
    public boolean updateUser(utente user) {
        String sql = "UPDATE utente SET nome = ?, cognome = ?, email = ?, matricola = ? WHERE id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, user.getNome());
            stmt.setString(2, user.getCognome());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getMatricola());
            stmt.setInt(5, user.getId());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Errore aggiornamento utente " + user.getId() + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifica se un'email è già utilizzata
     * @param email l'email da verificare
     * @return true se l'email è già in uso, false altrimenti
     */
    public boolean isEmailUsed(String email) {
        String sql = "SELECT 1 FROM utente WHERE email = ? LIMIT 1";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, email);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Errore verifica email: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifica se una matricola è già utilizzata
     * @param matricola la matricola da verificare
     * @return true se la matricola è già in uso, false altrimenti
     */
    public boolean isMatricolaUsed(String matricola) {
        String sql = "SELECT 1 FROM utente WHERE matricola = ? LIMIT 1";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, matricola);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Errore verifica matricola: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Mappa un ResultSet a un oggetto utente
     * @param rs il ResultSet da mappare
     * @return l'oggetto utente
     * @throws SQLException in caso di errore di accesso ai dati
     */
    private utente mapResultSetToUtente(ResultSet rs) throws SQLException {
        utente user = new utente(
            rs.getString("matricola"),
            rs.getString("nome"),
            rs.getString("cognome"),
            rs.getString("email"),
            "" // Password non viene recuperata per sicurezza
        );
        user.setId(rs.getInt("id"));
        return user;
    }
    
    /**
     * Cambia la password di un utente
     * @param userId l'ID dell'utente
     * @param newPassword la nuova password
     * @return true se l'operazione è riuscita, false altrimenti
     */
    public boolean changePassword(int userId, String newPassword) {
        String sql = "UPDATE utente SET password = ? WHERE id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, newPassword); // Nota: in produzione usare hash
            stmt.setInt(2, userId);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Errore cambio password utente " + userId + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Elimina un utente dal database
     * @param userId l'ID dell'utente da eliminare
     * @return true se l'operazione è riuscita, false altrimenti
     */
    public boolean deleteUser(int userId) {
        String sql = "DELETE FROM utente WHERE id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Errore eliminazione utente " + userId + ": " + e.getMessage());
            return false;
        }
    }
}