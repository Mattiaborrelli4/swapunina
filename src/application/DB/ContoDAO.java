package application.DB;

import application.Classe.Conto;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ContoDAO {
    private static final String TABLE_NAME = "conto";
    private static final String MOVIMENTI_TABLE = "movimento_conto";

    public ContoDAO() {
        creaTabelleSeMancanti();
    }

    private void creaTabelleSeMancanti() {
        try (Connection conn = ConnessioneDB.getConnessione()) {
            // Tabella conto
            String sqlConto = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                    "id SERIAL PRIMARY KEY, " +
                    "utente_id INTEGER UNIQUE NOT NULL REFERENCES utente(id) ON DELETE CASCADE, " +
                    "saldo DECIMAL(10,2) NOT NULL DEFAULT 0.0)";
            
            // Tabella movimenti
            String sqlMovimenti = "CREATE TABLE IF NOT EXISTS " + MOVIMENTI_TABLE + " (" +
                    "id SERIAL PRIMARY KEY, " +
                    "conto_id INTEGER NOT NULL REFERENCES conto(id) ON DELETE CASCADE, " +
                    "importo DECIMAL(10,2) NOT NULL, " +
                    "tipo VARCHAR(50) NOT NULL, " +
                    "descrizione TEXT, " +
                    "data_operazione TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)";

            try (PreparedStatement stmt1 = conn.prepareStatement(sqlConto);
                 PreparedStatement stmt2 = conn.prepareStatement(sqlMovimenti)) {
                stmt1.execute();
                stmt2.execute();
                System.out.println("✅ Tabelle conto verificate/creata");
            }
        } catch (SQLException e) {
            System.err.println("❌ Errore nella creazione delle tabelle per Conto");
            e.printStackTrace();
        }
    }

    /**
     * Crea un conto per un utente se non esiste già
     */
    public Conto creaContoSeMancante(int utenteId) {
        if (utenteId <= 0) {
            System.err.println("❌ ID utente non valido per creazione conto: " + utenteId);
            return null;
        }
        
        // Verifica se l'utente esiste nel database
        if (!utenteEsiste(utenteId)) {
            System.err.println("❌ Utente non trovato nel database: " + utenteId);
            return null;
        }
        
        Conto conto = getContoByUtenteId(utenteId);
        if (conto == null) {
            return creaConto(utenteId);
        }
        return conto;
    }

    
    /**
     * Verifica se un utente esiste nel database
     */
    private boolean utenteEsiste(int utenteId) {
        String sql = "SELECT COUNT(*) FROM utente WHERE id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, utenteId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Errore nella verifica esistenza utente: " + e.getMessage());
        }
        return false;
    }
    
    

    /**
     * Crea un nuovo conto per un utente
     */
    public Conto creaConto(int utenteId) {
        // ✅ CONTROLLO: Verifica che l'ID utente sia valido
        if (utenteId <= 0) {
            System.err.println("❌ ID utente non valido: " + utenteId);
            return null;
        }
        
        // Verifica che l'utente esista
        if (!utenteEsiste(utenteId)) {
            System.err.println("❌ Impossibile creare conto: utente " + utenteId + " non esiste");
            return null;
        }
        
        String sql = "INSERT INTO " + TABLE_NAME + " (utente_id, saldo) VALUES (?, 0.0) RETURNING id";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, utenteId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Conto conto = new Conto(utenteId);
                    conto.setId(rs.getInt("id"));
                    return conto;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Errore nella creazione del conto per utente: " + utenteId);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Recupera il conto di un utente
     */
    public Conto getContoByUtenteId(int utenteId) {
        String sql = "SELECT * FROM " + TABLE_NAME + " WHERE utente_id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, utenteId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Conto conto = new Conto(utenteId);
                    conto.setId(rs.getInt("id"));
                    conto.accredita(rs.getBigDecimal("saldo"), "Saldo iniziale");
                    
                    // Carica i movimenti
                    caricaMovimenti(conto);
                    
                    System.out.println("✅ Conto caricato per utente: " + utenteId + " - Saldo: " + conto.getSaldo());
                    return conto;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Errore nel recupero conto per utente: " + utenteId);
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Aggiorna il saldo del conto
     */
    public boolean aggiornaSaldo(Conto conto) {
        String sql = "UPDATE " + TABLE_NAME + " SET saldo = ? WHERE id = ?";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setBigDecimal(1, conto.getSaldo());
            stmt.setInt(2, conto.getId());
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Errore nell'aggiornamento del saldo per conto: " + conto.getId());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Registra un movimento nel conto
     */
    public boolean registraMovimento(Conto conto, Conto.Movimento movimento) {
        String sql = "INSERT INTO " + MOVIMENTI_TABLE + " (conto_id, importo, tipo, descrizione, data_operazione) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, conto.getId());
            stmt.setBigDecimal(2, movimento.getImporto());
            stmt.setString(3, movimento.getTipo().name());
            stmt.setString(4, movimento.getDescrizione());
            stmt.setTimestamp(5, Timestamp.valueOf(movimento.getData()));
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Errore nella registrazione del movimento per conto: " + conto.getId());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Carica i movimenti del conto
     */
    private void caricaMovimenti(Conto conto) {
        String sql = "SELECT * FROM " + MOVIMENTI_TABLE + " WHERE conto_id = ? ORDER BY data_operazione DESC";
        
        try (Connection conn = ConnessioneDB.getConnessione();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, conto.getId());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    BigDecimal importo = rs.getBigDecimal("importo");
                    Conto.TipoMovimento tipo = Conto.TipoMovimento.valueOf(rs.getString("tipo"));
                    String descrizione = rs.getString("descrizione");
                    
                    // Creiamo un movimento fittizio per la storia (la data viene dal DB)
                    // Nota: nella classe Conto i movimenti vengono aggiunti automaticamente con le operazioni
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Errore nel caricamento movimenti per conto: " + conto.getId());
            e.printStackTrace();
        }
    }

    /**
     * Ricarica il conto di un utente
     */
    public boolean ricaricaConto(int utenteId, BigDecimal importo, String metodoPagamento) {
        Conto conto = creaContoSeMancante(utenteId);
        if (conto != null) {
            conto.ricarica(importo, metodoPagamento);
            boolean success = aggiornaSaldo(conto) && registraMovimento(conto, 
                conto.getMovimenti().get(conto.getMovimenti().size() - 1));
            
            if (success) {
                System.out.println("✅ Ricarica effettuata: " + importo + " per utente " + utenteId);
            }
            return success;
        }
        return false;
    }

    /**
     * Verifica se l'utente ha saldo sufficiente
     */
    public boolean verificaSaldoSufficiente(int utenteId, BigDecimal importoRichiesto) {
        Conto conto = getContoByUtenteId(utenteId);
        return conto != null && conto.saldoSufficiente(importoRichiesto);
    }

    /**
     * Effettua un acquisto scalando il saldo
     */
    public boolean effettuaAcquisto(int utenteId, BigDecimal importo, String descrizione) {
        try (Connection conn = ConnessioneDB.getConnessione()) {
            conn.setAutoCommit(false);
            
            try {
                // Verifica saldo
                Conto conto = getContoByUtenteId(utenteId);
                if (conto == null || !conto.saldoSufficiente(importo)) {
                    conn.rollback();
                    return false;
                }
                
                // Effettua l'addebito
                boolean successAcquisto = conto.effettuaAcquisto(importo, descrizione);
                if (!successAcquisto) {
                    conn.rollback();
                    return false;
                }
                
                // Aggiorna saldo nel database
                boolean successUpdate = aggiornaSaldo(conto);
                boolean successMovimento = registraMovimento(conto, 
                    conto.getMovimenti().get(conto.getMovimenti().size() - 1));
                
                if (successUpdate && successMovimento) {
                    conn.commit();
                    System.out.println("✅ Acquisto effettuato: " + importo + " per utente " + utenteId);
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Errore durante l'acquisto per utente: " + utenteId);
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Trasferisce fondi da acquirente a venditore
     */
    public boolean trasferisciFondi(int acquirenteId, int venditoreId, BigDecimal importo, String descrizione) {
        // ✅ CONTROLLO: Verifica che gli ID siano validi
        if (acquirenteId <= 0 || venditoreId <= 0) {
            System.err.println("❌ ID utenti non validi per trasferimento: acquirente=" + acquirenteId + ", venditore=" + venditoreId);
            return false;
        }
        
        // ✅ CONTROLLO: Verifica che l'importo sia positivo
        if (importo.compareTo(BigDecimal.ZERO) <= 0) {
            System.err.println("❌ Importo non valido per trasferimento: " + importo);
            return false;
        }
        
        try (Connection conn = ConnessioneDB.getConnessione()) {
            conn.setAutoCommit(false);
            
            try {
                // Verifica saldo acquirente
                Conto contoAcquirente = getContoByUtenteId(acquirenteId);
                if (contoAcquirente == null || !contoAcquirente.saldoSufficiente(importo)) {
                    conn.rollback();
                    return false;
                }
                
                // Assicurati che il venditore abbia un conto
                Conto contoVenditore = creaContoSeMancante(venditoreId);
                if (contoVenditore == null) {
                    System.err.println("❌ Impossibile creare conto per venditore: " + venditoreId);
                    conn.rollback();
                    return false;
                }
                
                // Addebita all'acquirente
                boolean successAcquisto = contoAcquirente.effettuaAcquisto(importo, descrizione);
                if (!successAcquisto) {
                    conn.rollback();
                    return false;
                }
                
                // Accredita al venditore
                contoVenditore.accredita(importo, "Vendita: " + descrizione);
                
                // Aggiorna entrambi i conti
                boolean successUpdate1 = aggiornaSaldo(contoAcquirente);
                boolean successUpdate2 = aggiornaSaldo(contoVenditore);
                boolean successMovimento1 = registraMovimento(contoAcquirente, 
                    contoAcquirente.getMovimenti().get(contoAcquirente.getMovimenti().size() - 1));
                boolean successMovimento2 = registraMovimento(contoVenditore, 
                    contoVenditore.getMovimenti().get(contoVenditore.getMovimenti().size() - 1));
                
                if (successUpdate1 && successUpdate2 && successMovimento1 && successMovimento2) {
                    conn.commit();
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
            
        } catch (SQLException e) {
            System.err.println("❌ Errore durante il trasferimento fondi: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
}