package application.DB;

import application.Classe.utente;
import java.sql.*;

public class UserDAO {
    
    public utente getUserById(int userId) {
        String sql = "SELECT id, nome, cognome, email FROM utente WHERE id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                utente user = new utente();
                user.setId(rs.getInt("id"));
                user.setNome(rs.getString("nome"));
                user.setCognome(rs.getString("cognome"));
                user.setEmail(rs.getString("email"));
                return user;
            }
        } catch (SQLException e) {
            System.err.println("Errore recupero utente: " + e.getMessage());
        }
        
        return null;
    }
}