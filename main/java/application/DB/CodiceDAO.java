package application.DB;

import application.Classe.Codice;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;
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

        // Genera codice casuale a 6 cifre
        String codicePlain = generaCodiceCasuale();
        
        // Cripta il codice usando jBCrypt
        String codiceHash = BCrypt.hashpw(codicePlain, BCrypt.gensalt());

        String sql = "INSERT INTO codice_conferma (utente_id, annuncio_id, codice_hash, data_creazione, tentativi_errati) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, utenteId);
            stmt.setInt(2, annuncioId);
            stmt.setString(3, codiceHash);
            stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(5, 0);

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
    public boolean verificaCodiceConferma(int utenteId, int annuncioId, String codiceInserito) {
        Codice codice = getCodiceByUtenteAnnuncio(utenteId, annuncioId);
        
        if (codice == null) {
            System.err.println("Nessun codice di conferma trovato per utente " + utenteId + " e annuncio " + annuncioId);
            return false;
        }

        if (!codice.isValido()) {
            // Se non è valido (scaduto o troppi tentativi), elimina il codice
            eliminaCodice(codice.getId());
            System.err.println("Codice non valido - scaduto o troppi tentativi");
            return false;
        }

        // Verifica il codice usando jBCrypt
        boolean codiceCorretto = BCrypt.checkpw(codiceInserito, codice.getCodiceHash());
        
        if (codiceCorretto) {
            // Codice corretto - elimina il codice e completa la transazione
            eliminaCodice(codice.getId());
            return true;
        } else {
            // Codice errato - incrementa tentativi
            incrementaTentativiErrati(codice.getId());
            return false;
        }
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
            
            LocalDateTime scadenza = LocalDateTime.now().minusHours(24);
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
        Timestamp dataCreazione = rs.getTimestamp("data_creazione");
        int tentativiErrati = rs.getInt("tentativi_errati");

        LocalDateTime dataCreazioneLD = dataCreazione.toLocalDateTime();

        return new Codice(id, utenteId, annuncioId, codiceHash, dataCreazioneLD, tentativiErrati);
    }

    /**
     * Genera un codice casuale a 6 cifre
     */
    private String generaCodiceCasuale() {
        Random random = new Random();
        int numero = random.nextInt(999999);
        return String.format("%06d", numero);
    }
}