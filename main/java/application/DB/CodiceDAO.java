package application.DB;

import application.Classe.Codice;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * DAO per la gestione dei codici di conferma criptati
 */
public class CodiceDAO {

    /**
     * Genera un nuovo codice di conferma criptato per un utente e annuncio
     * Restituisce il codice in chiaro per mostrarlo al venditore
     */
    public String generaCodiceConferma(int utenteId, int annuncioId) {
        // Prima elimina eventuali codici esistenti per questa combinazione
        eliminaCodiciEsistenti(utenteId, annuncioId);

        // Genera codice casuale a 6 caratteri alfanumerici
        String codicePlain = generaCodiceAlfanumerico();
        
        // Cripta il codice usando jBCrypt
        String codiceHash = BCrypt.hashpw(codicePlain, BCrypt.gensalt());

        String sql = "INSERT INTO codice_conferma (utente_id, annuncio_id, codice_hash, codice_plain, data_creazione, tentativi_errati) VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, utenteId);
            stmt.setInt(2, annuncioId);
            stmt.setString(3, codiceHash);
            stmt.setString(4, codicePlain);
            stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(6, 0);

            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                return codicePlain; // Restituisce il codice in chiaro solo per la visualizzazione
            }
        } catch (SQLException e) {
            System.err.println("Errore nella generazione del codice di conferma: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Verifica il codice di conferma inserito dall'utente
     * Se corretto, elimina il codice dal database
     */
    /**
 * Verifica un codice senza bisogno dell'ID annuncio
 * Cerca tra tutti i codici attivi e verifica se il codice inserito corrisponde
 * Se corretto, elimina l'annuncio correlato
 */
/**
 * Verifica un codice senza bisogno dell'ID annuncio
 * Cerca tra tutti i codici attivi e verifica se il codice inserito corrisponde
 * Se corretto, elimina l'annuncio correlato
 */
public boolean verificaCodice(String codiceInserito) {
    String sql = "SELECT cc.*, cc.annuncio_id FROM codice_conferma cc " +
             "WHERE cc.data_creazione > CURRENT_TIMESTAMP - INTERVAL '14 days'";

    try (Connection conn = ConnessioneDB.getConnessione();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

        ResultSet rs = stmt.executeQuery();

        while (rs.next()) {
            String codiceHash = rs.getString("codice_hash");
            int tentativi = rs.getInt("tentativi_errati");
            int codiceId = rs.getInt("id");
            int annuncioId = rs.getInt("annuncio_id");

            // Verifica se il codice è ancora valido (meno di 3 tentativi errati)
            if (tentativi >= 3) {
                continue; // Passa al codice successivo
            }

            // Verifica il codice usando BCrypt
            boolean codiceCorretto = BCrypt.checkpw(codiceInserito, codiceHash);

            if (codiceCorretto) {
                // Codice corretto - elimina l'annuncio correlato
                AnnuncioDAO annuncioDAO = new AnnuncioDAO();
                boolean annuncioEliminato = annuncioDAO.eliminaAnnuncioCompleto(annuncioId);
                
                if (annuncioEliminato) {
                    // Elimina anche il codice
                    eliminaCodice(codiceId);
                    return true;
                } else {
                    System.err.println("Errore nell'eliminazione dell'annuncio " + annuncioId);
                    return false;
                }
            } else {
                // Codice errato - incrementa tentativi
                incrementaTentativiErrati(codiceId);
            }
        }
    } catch (SQLException e) {
        System.err.println("Errore nella verifica del codice: " + codiceInserito);
        e.printStackTrace();
    }
    return false;
}

    /**
     * Recupera un codice per utente e annuncio
     */
    private Codice getCodiceByUtenteAnnuncio(int utenteId, int annuncioId) {
        String sql = "SELECT * FROM codice_conferma WHERE utente_id = ? AND annuncio_id = ?";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, utenteId);
            stmt.setInt(2, annuncioId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return mappaCodice(rs);
            }
        } catch (SQLException e) {
            System.err.println("Errore nel recupero del codice per utente " + utenteId + " e annuncio " + annuncioId);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Elimina i codici esistenti per una combinazione utente-annuncio
     */
    private void eliminaCodiciEsistenti(int utenteId, int annuncioId) {
        String sql = "DELETE FROM codice_conferma WHERE utente_id = ? AND annuncio_id = ?";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, utenteId);
            stmt.setInt(2, annuncioId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Errore nell'eliminazione dei codici esistenti: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Elimina un codice specifico dal database
     */
    private void eliminaCodice(int codiceId) {
        String sql = "DELETE FROM codice_conferma WHERE id = ?";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, codiceId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Errore nell'eliminazione del codice: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Incrementa il contatore dei tentativi errati
     */
    private void incrementaTentativiErrati(int codiceId) {
        String sql = "UPDATE codice_conferma SET tentativi_errati = tentativi_errati + 1 WHERE id = ?";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, codiceId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            System.err.println("Errore nell'incremento dei tentativi errati: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Verifica se esiste un codice di conferma pendente per utente e annuncio
     */
    public boolean hasCodiceConfermaPendente(int utenteId, int annuncioId) {
        Codice codice = getCodiceByUtenteAnnuncio(utenteId, annuncioId);
        return codice != null && codice.isValido();
    }

    /**
     * Pulisce i codici scaduti dal database (manutenzione)
     */
    public int pulisciCodiciScaduti() {
        String sql = "DELETE FROM codice_conferma WHERE data_creazione < ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            // MODIFICA: Cambiato da 24 ore a 2 settimane (14 giorni)
            LocalDateTime scadenza = LocalDateTime.now().minusDays(14);
            stmt.setTimestamp(1, Timestamp.valueOf(scadenza));
            
            return stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Errore nella pulizia dei codici scaduti: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Mappa un ResultSet in un oggetto Codice
     */
    private Codice mappaCodice(ResultSet rs) throws SQLException {
        int id = rs.getInt("id");
        int utenteId = rs.getInt("utente_id");
        int annuncioId = rs.getInt("annuncio_id");
        String codiceHash = rs.getString("codice_hash");
        String codicePlain = rs.getString("codice_plain");
        Timestamp dataCreazione = rs.getTimestamp("data_creazione");
        int tentativiErrati = rs.getInt("tentativi_errati");

        LocalDateTime dataCreazioneLD = dataCreazione.toLocalDateTime();

        return new Codice(id, utenteId, annuncioId, codiceHash, codicePlain, dataCreazioneLD, tentativiErrati);
    }

    /**
     * Genera un codice casuale a 6 caratteri alfanumerici
     */
    private String generaCodiceAlfanumerico() {
        String caratteri = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random = new Random();
        StringBuilder codice = new StringBuilder();
        
        for (int i = 0; i < 6; i++) {
            codice.append(caratteri.charAt(random.nextInt(caratteri.length())));
        }
        
        return codice.toString();
    }

    /**
     * Recupera tutti i codici attivi per un utente con i dettagli dell'annuncio
     */
    public List<Map<String, String>> getCodiciAttiviPerUtente(int utenteId) {
        List<Map<String, String>> codici = new ArrayList<>();
        
        // MODIFICA: Cambiato da 24 ore a 14 giorni
        String sql = "SELECT cc.id, cc.codice_plain, cc.data_creazione, a.titolo, a.id as annuncio_id " +
                 "FROM codice_conferma cc " +
                 "JOIN annuncio a ON cc.annuncio_id = a.id " +
                 "WHERE cc.utente_id = ? AND cc.data_creazione > CURRENT_TIMESTAMP - INTERVAL '14 days' " +
                 "ORDER BY cc.data_creazione DESC";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, utenteId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Map<String, String> codiceInfo = new HashMap<>();
                codiceInfo.put("id", String.valueOf(rs.getInt("id")));
                codiceInfo.put("codice", rs.getString("codice_plain"));
                codiceInfo.put("titolo", rs.getString("titolo"));
                codiceInfo.put("annuncio_id", String.valueOf(rs.getInt("annuncio_id")));
                codiceInfo.put("data_creazione", rs.getTimestamp("data_creazione").toString());
                codici.add(codiceInfo);
            }
        } catch (SQLException e) {
            System.err.println("Errore nel recupero dei codici attivi per l'utente " + utenteId);
            e.printStackTrace();
        }
        return codici;
    }

    /**
     * Verifica un codice per un annuncio specifico (per il venditore)
     */
    public boolean verificaCodicePerAnnuncio(int annuncioId, String codiceInserito) {
        // MODIFICA: Cambiato da 24 ore a 14 giorni
        String sql = "SELECT cc.* FROM codice_conferma cc " +
                 "WHERE cc.annuncio_id = ? AND cc.data_creazione > CURRENT_TIMESTAMP - INTERVAL '14 days'";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, annuncioId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String codiceHash = rs.getString("codice_hash");
                int tentativi = rs.getInt("tentativi_errati");
                int codiceId = rs.getInt("id");

                // Verifica se il codice è ancora valido (meno di 3 tentativi errati)
                if (tentativi >= 3) {
                    return false;
                }

                // Verifica il codice usando BCrypt
                boolean codiceCorretto = BCrypt.checkpw(codiceInserito, codiceHash);

                if (codiceCorretto) {
                    // Codice corretto - elimina il codice
                    eliminaCodice(codiceId);
                    return true;
                } else {
                    // Codice errato - incrementa tentativi
                    incrementaTentativiErrati(codiceId);
                    return false;
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore nella verifica del codice per l'annuncio " + annuncioId);
            e.printStackTrace();
        }
        return false;
    }

    
}
