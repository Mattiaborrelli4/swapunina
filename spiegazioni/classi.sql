-- =============================================
-- Database: Marketplace Universitario
-- =============================================

-- Eliminazione tabelle se esistono (in ordine di dipendenze)
DROP TABLE IF EXISTS annuncio_caratteristica CASCADE;
DROP TABLE IF EXISTS carrello_item CASCADE;
DROP TABLE IF EXISTS recensione CASCADE;
DROP TABLE IF EXISTS messaggio CASCADE;
DROP TABLE IF EXISTS offerta CASCADE;
DROP TABLE IF EXISTS transazione CASCADE;
DROP TABLE IF EXISTS ordine CASCADE;
DROP TABLE IF EXISTS conto CASCADE;
DROP TABLE IF EXISTS annuncio CASCADE;
DROP TABLE IF EXISTS oggetto CASCADE;
DROP TABLE IF EXISTS utente CASCADE;
DROP TABLE IF EXISTS categoria CASCADE;

-- =============================================
-- Creazione Tabelle Principali
-- =============================================

-- Tabella UTENTE
CREATE TABLE utente (
    id SERIAL PRIMARY KEY,
    matricola VARCHAR(20) UNIQUE NOT NULL,
    nome VARCHAR(100) NOT NULL,
    cognome VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    data_registrazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabella CATEGORIA
CREATE TABLE categoria (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(50) UNIQUE NOT NULL
);

-- Tabella OGGETTO
CREATE TABLE oggetto (
    id SERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    descrizione TEXT,
    categoria_id INTEGER REFERENCES categoria(id),
    origine VARCHAR(50) NOT NULL CHECK (origine IN ('NUOVO', 'USATO', 'RICONDIZIONATO', 'REGALO', 'SCAMBIO')),
    image_url VARCHAR(255)
);

-- Tabella ANNUNCIO
CREATE TABLE annuncio (
    id SERIAL PRIMARY KEY,
    titolo VARCHAR(255) NOT NULL,
    oggetto_id INTEGER REFERENCES oggetto(id) ON DELETE CASCADE,
    prezzo DECIMAL(10,2) NOT NULL,
    in_evidenza BOOLEAN DEFAULT FALSE,
    tipologia VARCHAR(50) NOT NULL CHECK (tipologia IN ('VENDITA', 'SCAMBIO', 'REGALO', 'ASTA')),
    modalita_consegna VARCHAR(100) NOT NULL,
    stato VARCHAR(50) DEFAULT 'ATTIVO' CHECK (stato IN ('ATTIVO', 'VENDUTO', 'RITIRATO', 'SCADUTO')),
    venditore_id INTEGER REFERENCES utente(id) ON DELETE CASCADE,
    data_pubblicazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    image_url TEXT,
    descrizione TEXT,
    citta VARCHAR(100),
    nome_venditore VARCHAR(200)
);

-- Tabella CARRELLO_ITEM
CREATE TABLE carrello_item (
    id SERIAL PRIMARY KEY,
    utente_id INTEGER REFERENCES utente(id) ON DELETE CASCADE,
    annuncio_id INTEGER REFERENCES annuncio(id) ON DELETE CASCADE,
    quantita INTEGER DEFAULT 1 CHECK (quantita > 0),
    data_aggiunta TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(utente_id, annuncio_id)
);

-- Tabella RECENSIONE
CREATE TABLE recensione (
    id SERIAL PRIMARY KEY,
    acquirente_id INTEGER REFERENCES utente(id) ON DELETE CASCADE,
    venditore_id INTEGER REFERENCES utente(id) ON DELETE CASCADE,
    annuncio_id INTEGER REFERENCES annuncio(id) ON DELETE CASCADE,
    commento TEXT,
    punteggio INTEGER NOT NULL CHECK (punteggio >= 1 AND punteggio <= 5),
    data_recensione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    visibile BOOLEAN DEFAULT TRUE,
    UNIQUE(acquirente_id, annuncio_id)
);

-- Tabella MESSAGGIO
CREATE TABLE messaggio (
    id SERIAL PRIMARY KEY,
    mittente_id INTEGER REFERENCES utente(id) ON DELETE CASCADE,
    destinatario_id INTEGER REFERENCES utente(id) ON DELETE CASCADE,
    testo_plaintext_backup TEXT,
    testo_encrypted BYTEA,
    iv BYTEA,
    data_invio TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    annuncio_id INTEGER REFERENCES annuncio(id) ON DELETE SET NULL,
    algoritmo_encryption VARCHAR(20) DEFAULT 'AES'
);

-- Tabella OFFERTA
CREATE TABLE offerta (
    id SERIAL PRIMARY KEY,
    annuncio_id INTEGER REFERENCES annuncio(id) ON DELETE CASCADE,
    offerente_id INTEGER REFERENCES utente(id) ON DELETE CASCADE,
    importo DECIMAL(10,2) NOT NULL CHECK (importo > 0),
    data_offerta TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    accettata BOOLEAN DEFAULT FALSE
);

-- Tabella TRANSAZIONE
CREATE TABLE transazione (
    id SERIAL PRIMARY KEY,
    acquirente_id INTEGER REFERENCES utente(id) ON DELETE CASCADE,
    venditore_id INTEGER REFERENCES utente(id) ON DELETE CASCADE,
    annuncio_id INTEGER REFERENCES annuncio(id) ON DELETE CASCADE,
    importo DECIMAL(10,2) NOT NULL CHECK (importo > 0),
    data_transazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    categoria VARCHAR(100),
    tipo VARCHAR(50) CHECK (tipo IN ('VENDITA', 'ASTA', 'SCAMBIO'))
);

-- Tabella CONTO
CREATE TABLE conto (
    id SERIAL PRIMARY KEY,
    utente_id INTEGER REFERENCES utente(id) ON DELETE CASCADE UNIQUE,
    saldo DECIMAL(10,2) DEFAULT 0.00 CHECK (saldo >= 0)
);

-- Tabella ORDINE
CREATE TABLE ordine (
    id SERIAL PRIMARY KEY,
    acquirente_id INTEGER REFERENCES utente(id) ON DELETE CASCADE,
    venditore_id INTEGER REFERENCES utente(id) ON DELETE CASCADE,
    annuncio_id INTEGER REFERENCES annuncio(id) ON DELETE CASCADE,
    quantita INTEGER DEFAULT 1 CHECK (quantita > 0),
    prezzo DECIMAL(10,2) NOT NULL CHECK (prezzo > 0),
    stato VARCHAR(50) DEFAULT 'IN_ATTESA' CHECK (stato IN ('IN_ATTESA', 'PAGATO', 'SPEDITO', 'CONSEGNATO', 'ANNULLATO')),
    modalita_consegna VARCHAR(100) NOT NULL,
    indirizzo_spedizione TEXT,
    tracking_number VARCHAR(100),
    data_creazione TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabella ANNUNCIO_CARATTERISTICA (per caratteristiche aggiuntive)
CREATE TABLE annuncio_caratteristica (
    annuncio_id INTEGER REFERENCES annuncio(id) ON DELETE CASCADE,
    caratteristica VARCHAR(255) NOT NULL,
    valore VARCHAR(255) NOT NULL,
    PRIMARY KEY (annuncio_id, caratteristica)
);

-- =============================================
-- Indici per Performance
-- =============================================

-- Indici per ricerche frequenti
CREATE INDEX idx_annuncio_titolo ON annuncio(titolo);
CREATE INDEX idx_annuncio_prezzo ON annuncio(prezzo);
CREATE INDEX idx_annuncio_stato ON annuncio(stato);
CREATE INDEX idx_annuncio_venditore ON annuncio(venditore_id);
CREATE INDEX idx_annuncio_tipologia ON annuncio(tipologia);

CREATE INDEX idx_oggetto_categoria ON oggetto(categoria_id);
CREATE INDEX idx_oggetto_nome ON oggetto(nome);

CREATE INDEX idx_messaggio_mittente ON messaggio(mittente_id);
CREATE INDEX idx_messaggio_destinatario ON messaggio(destinatario_id);
CREATE INDEX idx_messaggio_annuncio ON messaggio(annuncio_id);
CREATE INDEX idx_messaggio_data ON messaggio(data_invio);

CREATE INDEX idx_offerta_annuncio ON offerta(annuncio_id);
CREATE INDEX idx_offerta_offerente ON offerta(offerente_id);

CREATE INDEX idx_recensione_venditore ON recensione(venditore_id);
CREATE INDEX idx_recensione_acquirente ON recensione(acquirente_id);

CREATE INDEX idx_transazione_acquirente ON transazione(acquirente_id);
CREATE INDEX idx_transazione_venditore ON transazione(venditore_id);
CREATE INDEX idx_transazione_data ON transazione(data_transazione);

CREATE INDEX idx_ordine_acquirente ON ordine(acquirente_id);
CREATE INDEX idx_ordine_venditore ON ordine(venditore_id);
CREATE INDEX idx_ordine_stato ON ordine(stato);

-- =============================================
-- Inserimento Dati di Default
-- =============================================

-- Inserimento categorie
INSERT INTO categoria (nome) VALUES 
('LIBRI'),
('INFORMATICA'),
('ABBIGLIAMENTO'),
('ELETTRONICA'),
('MUSICA'),
('CASA'),
('SPORT'),
('GIOCATTOLI'),
('ALTRO');

-- Inserimento utente admin di default
INSERT INTO utente (matricola, nome, cognome, email, password) VALUES 
('ADMIN001', 'Admin', 'Sistema', 'admin@universitymarket.it', 'admin123');

-- =============================================
-- Viste Utili
-- =============================================

-- Vista per annunci attivi con dettagli completi
CREATE VIEW vista_annunci_attivi AS
SELECT 
    a.id,
    a.titolo,
    a.prezzo,
    a.tipologia,
    a.stato,
    a.data_pubblicazione,
    a.citta,
    a.nome_venditore,
    o.nome as nome_oggetto,
    o.descrizione as descrizione_oggetto,
    o.origine,
    o.image_url as image_url_oggetto,
    c.nome as categoria,
    u.nome as venditore_nome,
    u.cognome as venditore_cognome
FROM annuncio a
JOIN oggetto o ON a.oggetto_id = o.id
JOIN categoria c ON o.categoria_id = c.id
JOIN utente u ON a.venditore_id = u.id
WHERE a.stato = 'ATTIVO';

-- Vista per statistiche utente
CREATE VIEW vista_statistiche_utente AS
SELECT 
    u.id as utente_id,
    u.nome,
    u.cognome,
    COUNT(DISTINCT a.id) as totale_annunci,
    COUNT(DISTINCT r.id) as totale_recensioni_ricevute,
    COALESCE(AVG(r.punteggio), 0) as valutazione_media,
    COUNT(DISTINCT t.id) as totale_transazioni_vendite,
    COALESCE(SUM(t.importo), 0) as totale_vendite
FROM utente u
LEFT JOIN annuncio a ON u.id = a.venditore_id
LEFT JOIN recensione r ON u.id = r.venditore_id
LEFT JOIN transazione t ON u.id = t.venditore_id
GROUP BY u.id, u.nome, u.cognome;

-- =============================================
-- Funzioni e Trigger
-- =============================================

-- Funzione per aggiornare automaticamente il saldo del conto dopo una transazione
CREATE OR REPLACE FUNCTION aggiorna_saldo_transazione()
RETURNS TRIGGER AS $$
BEGIN
    -- Aggiorna il saldo del venditore (accredita)
    UPDATE conto 
    SET saldo = saldo + NEW.importo 
    WHERE utente_id = NEW.venditore_id;
    
    -- Aggiorna il saldo dell'acquirente (addebita, se necessario)
    -- Nota: Questo dipende dal tuo modello di business
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger per aggiornare il saldo dopo una transazione
CREATE TRIGGER trigger_aggiorna_saldo
    AFTER INSERT ON transazione
    FOR EACH ROW
    EXECUTE FUNCTION aggiorna_saldo_transazione();

-- Funzione per verificare che un utente non recensisce se stesso
CREATE OR REPLACE FUNCTION verifica_recensione_utente()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.acquirente_id = NEW.venditore_id THEN
        RAISE EXCEPTION 'Un utente non può recensire se stesso';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger per verificare la recensione
CREATE TRIGGER trigger_verifica_recensione
    BEFORE INSERT ON recensione
    FOR EACH ROW
    EXECUTE FUNCTION verifica_recensione_utente();

-- =============================================
-- Query di Verifica
-- =============================================

-- Verifica che tutte le tabelle siano state create correttamente
SELECT 
    table_name,
    COUNT(*) as column_count
FROM information_schema.columns 
WHERE table_schema = 'public'
GROUP BY table_name
ORDER BY table_name;

-- Mostra la struttura del database
SELECT 
    tc.table_name, 
    kcu.column_name, 
    ccu.table_name AS foreign_table_name,
    ccu.column_name AS foreign_column_name 
FROM information_schema.table_constraints AS tc 
JOIN information_schema.key_column_usage AS kcu ON tc.constraint_name = kcu.constraint_name
JOIN information_schema.constraint_column_usage AS ccu ON ccu.constraint_name = tc.constraint_name
WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_schema = 'public'
ORDER BY tc.table_name;

COMMENT ON TABLE utente IS 'Tabella degli utenti del marketplace universitario';
COMMENT ON TABLE annuncio IS 'Tabella degli annunci di vendita/scambio/regalo';
COMMENT ON TABLE oggetto IS 'Tabella degli oggetti in vendita negli annunci';
COMMENT ON TABLE categoria IS 'Tabella delle categorie di oggetti';