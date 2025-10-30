package application.DB;

import application.Classe.Recensioni;
import application.Classe.Annuncio;
import application.Classe.utente;
import application.Classe.StatisticheEconomiche;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

/**
 * Gestisce le operazioni CRUD per le recensioni nel database
 * Fornisce metodi per recupero, inserimento e statistiche delle recensioni
 */
public class RecensioneDAO {
    private static final String TABLE_NAME = "recensione";

    public RecensioneDAO() {
        creaTabellaSeMancante();
    }

    /**
     * Crea la tabella recensioni se non esiste
     */
    private void creaTabellaSeMancante() {
        String sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                "id SERIAL PRIMARY KEY, " +
                "acquirente_id INTEGER NOT NULL REFERENCES utente(id), " +
                "venditore_id INTEGER NOT NULL REFERENCES utente(id), " +
                "annuncio_id INTEGER NOT NULL REFERENCES annuncio(id), " +
                "commento TEXT NOT NULL, " +
                "punteggio INTEGER CHECK (punteggio >= 1 AND punteggio <= 5), " +
                "data_recensione TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "visibile BOOLEAN DEFAULT TRUE, " +
                "CONSTRAINT recensione_acquirente_id_annuncio_id_key UNIQUE (acquirente_id, annuncio_id)" +
                ")";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Errore creazione tabella recensione: " + e.getMessage());
        }
    }

    /**
     * Recupera recensioni e statistiche per un annuncio specifico
     */
    public StatisticheRecensioni getRecensioniEStatistichePerAnnuncio(int idAnnuncio) {
        List<Recensioni> recensioni = new ArrayList<>();
        StatisticheEconomiche statistiche;
        
        String sqlRecensioni = "SELECT r.*, " +
                 "acquirente.nome as acqu_nome, acquirente.cognome as acqu_cognome, " +
                 "venditore.nome as vend_nome, venditore.cognome as vend_cognome, " +
                 "a.titolo as annuncio_titolo " +
                 "FROM " + TABLE_NAME + " r " +
                 "JOIN utente acquirente ON r.acquirente_id = acquirente.id " +
                 "JOIN utente venditore ON r.venditore_id = venditore.id " +
                 "JOIN annuncio a ON r.annuncio_id = a.id " +
                 "WHERE r.annuncio_id = ? AND r.visibile = TRUE " +
                 "ORDER BY r.data_recensione DESC";
        
        String sqlStatistiche = "SELECT * FROM statistiche_recensioni_annuncio WHERE annuncio_id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmtRecensioni = conn.prepareStatement(sqlRecensioni);
             PreparedStatement stmtStatistiche = conn.prepareStatement(sqlStatistiche)) {
            
            // Recupera recensioni
            stmtRecensioni.setInt(1, idAnnuncio);
            try (ResultSet rsRecensioni = stmtRecensioni.executeQuery()) {
                List<BigDecimal> punteggi = new ArrayList<>();
                
                while (rsRecensioni.next()) {
                    Recensioni recensione = creaRecensioneDaResultSet(rsRecensioni);
                    recensioni.add(recensione);
                    punteggi.add(BigDecimal.valueOf(recensione.getPunteggio()));
                }
                
                // Recupera statistiche
                stmtStatistiche.setInt(1, idAnnuncio);
                try (ResultSet rsStatistiche = stmtStatistiche.executeQuery()) {
                    if (rsStatistiche.next()) {
                        statistiche = StatisticheEconomiche.daBigDecimal(
                            punteggi,
                            LocalDateTime.now().minusMonths(1).toLocalDate(),
                            LocalDateTime.now().toLocalDate()
                        );
                    } else {
                        statistiche = creaStatisticheVuote();
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Errore recupero recensioni per annuncio " + idAnnuncio + ": " + e.getMessage());
            statistiche = creaStatisticheVuote();
        }
        
        return new StatisticheRecensioni(recensioni, statistiche);
    }

    /**
     * Recupera solo le recensioni per un annuncio (senza statistiche)
     */
    public List<Recensioni> getRecensioniPerAnnuncio(int idAnnuncio) {
        return getRecensioniEStatistichePerAnnuncio(idAnnuncio).getRecensioni();
    }

    /**
     * Recupera tutte le recensioni per un venditore
     */
    public List<Recensioni> getRecensioniPerVenditore(int idVenditore) {
        List<Recensioni> recensioni = new ArrayList<>();
        
        String sql = "SELECT r.*, " +
                     "acquirente.nome as acqu_nome, acquirente.cognome as acqu_cognome, " +
                     "venditore.nome as vend_nome, venditore.cognome as vend_cognome, " +
                     "a.titolo as annuncio_titolo " +
                     "FROM " + TABLE_NAME + " r " +
                     "JOIN utente acquirente ON r.acquirente_id = acquirente.id " +
                     "JOIN utente venditore ON r.venditore_id = venditore.id " +
                     "JOIN annuncio a ON r.annuncio_id = a.id " +
                     "WHERE r.venditore_id = ? AND r.visibile = TRUE " +
                     "ORDER BY r.data_recensione DESC";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, idVenditore);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    recensioni.add(creaRecensioneDaResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore recupero recensioni per venditore " + idVenditore + ": " + e.getMessage());
        }
        
        return recensioni;
    }

    /**
     * Recupera statistiche complete per un venditore
     */
    public StatisticheEconomiche getStatisticheVenditore(int venditoreId) {
        String sql = "SELECT * FROM statistiche_recensioni_venditore WHERE venditore_id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, venditoreId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int numeroRecensioni = rs.getInt("numero_recensioni");
                    double punteggioMedio = rs.getDouble("punteggio_medio");
                    
                    List<BigDecimal> punteggi = new ArrayList<>();
                    for (int i = 0; i < numeroRecensioni; i++) {
                        punteggi.add(BigDecimal.valueOf(punteggioMedio));
                    }
                    
                    return StatisticheEconomiche.daBigDecimal(
                        punteggi,
                        LocalDateTime.now().minusMonths(1).toLocalDate(),
                        LocalDateTime.now().toLocalDate()
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore recupero statistiche venditore " + venditoreId + ": " + e.getMessage());
        }
        
        return creaStatisticheVuote();
    }

    /**
     * Recupera il punteggio medio di un annuncio
     */
    public double getPunteggioMedioAnnuncio(int annuncioId) {
        String sql = "SELECT punteggio_medio FROM statistiche_recensioni_annuncio WHERE annuncio_id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, annuncioId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getDouble("punteggio_medio") : 0.0;
            }
        } catch (SQLException e) {
            System.err.println("Errore calcolo punteggio medio annuncio " + annuncioId + ": " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * Recupera il punteggio medio di un venditore
     */
    public double getPunteggioMedioVenditore(int venditoreId) {
        String sql = "SELECT punteggio_medio FROM statistiche_recensioni_venditore WHERE venditore_id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, venditoreId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getDouble("punteggio_medio") : 0.0;
            }
        } catch (SQLException e) {
            System.err.println("Errore calcolo punteggio medio venditore " + venditoreId + ": " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * Verifica se un utente ha già recensito un annuncio
     */
    public boolean haGiaRecensito(int idAcquirente, int idAnnuncio) {
        String sql = "SELECT 1 FROM " + TABLE_NAME + " WHERE acquirente_id = ? AND annuncio_id = ? LIMIT 1";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, idAcquirente);
            stmt.setInt(2, idAnnuncio);
            
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Errore verifica recensione esistente: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Inserisce una nuova recensione nel database
     */
    public boolean inserisciRecensione(Recensioni recensione) {
        String sql = "INSERT INTO " + TABLE_NAME + " (acquirente_id, venditore_id, annuncio_id, commento, punteggio) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, recensione.getAcquirente().getId());
            stmt.setInt(2, recensione.getVenditore().getId());
            stmt.setInt(3, recensione.getAnnuncio().getId());
            stmt.setString(4, recensione.getCommento());
            stmt.setInt(5, recensione.getPunteggio());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Errore inserimento recensione: " + e.getMessage());
            return false;
        }
    }

    /**
     * Metodo legacy per compatibilità
     */
    public boolean aggiungiRecensione(Recensioni recensione) {
        return inserisciRecensione(recensione);
    }

    /**
     * Crea un oggetto Recensioni da un ResultSet
     */
    private Recensioni creaRecensioneDaResultSet(ResultSet rs) throws SQLException {
        // Crea utente acquirente
        utente acquirente = new utente();
        acquirente.setId(rs.getInt("acquirente_id"));
        acquirente.setNome(rs.getString("acqu_nome"));
        acquirente.setCognome(rs.getString("acqu_cognome"));
        
        // Crea utente venditore
        utente venditore = new utente();
        venditore.setId(rs.getInt("venditore_id"));
        venditore.setNome(rs.getString("vend_nome"));
        venditore.setCognome(rs.getString("vend_cognome"));
        
        // Crea annuncio
        Annuncio annuncio = new Annuncio();
        annuncio.setId(rs.getInt("annuncio_id"));
        annuncio.setTitolo(rs.getString("annuncio_titolo"));
        
        // Crea recensione
        String commento = rs.getString("commento");
        int punteggio = rs.getInt("punteggio");
        Recensioni recensione = new Recensioni(acquirente, venditore, annuncio, commento, punteggio);
        
        recensione.setId(rs.getInt("id"));
        
        Timestamp timestamp = rs.getTimestamp("data_recensione");
        if (timestamp != null) {
            recensione.setDataRecensione(timestamp.toLocalDateTime());
        }
        
        recensione.setVisibile(rs.getBoolean("visibile"));
        return recensione;
    }

    /**
     * Crea statistiche vuote per casi di fallback
     */
    private StatisticheEconomiche creaStatisticheVuote() {
        return StatisticheEconomiche.daBigDecimal(
            new ArrayList<>(),
            LocalDateTime.now().minusMonths(1).toLocalDate(),
            LocalDateTime.now().toLocalDate()
        );
    }

    /**
     * Conta il numero totale di recensioni per un venditore
     */
    public int contaRecensioniVenditore(int venditoreId) {
        String sql = "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE venditore_id = ? AND visibile = TRUE";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, venditoreId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            System.err.println("Errore conteggio recensioni venditore: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Recupera le recensioni più recenti per un venditore
     */
    public List<Recensioni> getRecensioniRecentiPerVenditore(int venditoreId, int limite) {
        List<Recensioni> recensioni = new ArrayList<>();
        
        String sql = "SELECT r.*, " +
                     "acquirente.nome as acqu_nome, acquirente.cognome as acqu_cognome, " +
                     "venditore.nome as vend_nome, venditore.cognome as vend_cognome, " +
                     "a.titolo as annuncio_titolo " +
                     "FROM " + TABLE_NAME + " r " +
                     "JOIN utente acquirente ON r.acquirente_id = acquirente.id " +
                     "JOIN utente venditore ON r.venditore_id = venditore.id " +
                     "JOIN annuncio a ON r.annuncio_id = a.id " +
                     "WHERE r.venditore_id = ? AND r.visibile = TRUE " +
                     "ORDER BY r.data_recensione DESC LIMIT ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, venditoreId);
            stmt.setInt(2, limite);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    recensioni.add(creaRecensioneDaResultSet(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Errore recupero recensioni recenti: " + e.getMessage());
        }
        
        return recensioni;
    }

    /**
     * Classe helper per restituire recensioni + statistiche
     */
    public static class StatisticheRecensioni {
        private final List<Recensioni> recensioni;
        private final StatisticheEconomiche statistiche;
        
        public StatisticheRecensioni(List<Recensioni> recensioni, StatisticheEconomiche statistiche) {
            this.recensioni = recensioni;
            this.statistiche = statistiche;
        }
        
        public List<Recensioni> getRecensioni() { return recensioni; }
        public StatisticheEconomiche getStatistiche() { return statistiche; }
        
        public double getPunteggioMedio() {
            return statistiche.getValoreMedio().doubleValue();
        }
        
        public int getNumeroRecensioni() {
            return recensioni.size();
        }
    }
}