package application.Classe;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.sql.Timestamp;
import java.util.Objects;

import application.Enum.Tipologia;

/**
 * Rappresenta un annuncio di vendita, asta, scambio o regalo
 * Gestisce tutte le informazioni relative a un annuncio e le operazioni associate
 */
public class Annuncio {
    private int id;
    private Oggetto oggetto;
    private double prezzo;
    private List<String> caratteristicheSpeciali;
    private boolean inEvidenza;
    private String modalitaConsegna;
    private String stato;
    private int venditoreId;
    private LocalDateTime dataPubblicazione;
    private List<Offerta> offerte;
    private String sedeConsegna;
    private Tipologia tipologia;
    private String titolo;
    private String citta;
    private String categoria;
    private String descrizione;
    private String nomeVenditore;

    // Formatter riutilizzabile per le date - ottimizzato per thread-safety
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // Costanti per stati dell'annuncio
    public static final String STATO_ATTIVO = "ATTIVO";
    public static final String STATO_RITIRATO = "RITIRATO";
    public static final String STATO_VENDUTO = "VENDUTO";
    public static final String STATO_SCADUTO = "SCADUTO";

    /**
     * Costruttore di default - inizializza le liste e lo stato
     */
    public Annuncio() {
        this.caratteristicheSpeciali = new ArrayList<>();
        this.offerte = new ArrayList<>();
        this.stato = STATO_ATTIVO;
        this.dataPubblicazione = LocalDateTime.now();
    }

    /**
     * Costruttore con validazione della tipologia come stringa
     */
    public Annuncio(Oggetto oggetto, double prezzo, String tipologiaStr, 
                   String modalitaConsegna, int venditoreId) {
        this();
        this.oggetto = Objects.requireNonNull(oggetto, "L'oggetto non può essere null");
        setPrezzo(prezzo);
        this.modalitaConsegna = modalitaConsegna;
        this.venditoreId = venditoreId;
        setTipologia(tipologiaStr);
    }

    /**
     * Costruttore con tipologia como enum
     */
    public Annuncio(Oggetto oggetto, double prezzo, Tipologia tipologia, 
                   String modalitaConsegna, int venditoreId) {
        this();
        this.oggetto = Objects.requireNonNull(oggetto, "L'oggetto non può essere null");
        setPrezzo(prezzo);
        this.tipologia = Objects.requireNonNull(tipologia, "La tipologia non può essere null");
        this.modalitaConsegna = modalitaConsegna;
        this.venditoreId = venditoreId;
    }

    /**
     * Costruttore di copia - crea una copia profonda dell'annuncio
     */
    public Annuncio(Annuncio altro) {
        this();
        this.id = altro.id;
        this.oggetto = altro.oggetto; // Condiviso - considerare clonazione se necessario
        this.prezzo = altro.prezzo;
        this.caratteristicheSpeciali = new ArrayList<>(altro.caratteristicheSpeciali);
        this.inEvidenza = altro.inEvidenza;
        this.modalitaConsegna = altro.modalitaConsegna;
        this.stato = altro.stato;
        this.venditoreId = altro.venditoreId;
        this.dataPubblicazione = altro.dataPubblicazione;
        this.offerte = new ArrayList<>(altro.offerte);
        this.sedeConsegna = altro.sedeConsegna;
        this.tipologia = altro.tipologia;
        this.titolo = altro.titolo;
        this.citta = altro.citta;
        this.categoria = altro.categoria;
        this.descrizione = altro.descrizione;
        this.nomeVenditore = altro.nomeVenditore;
    }

    // ========== METODI BUSINESS OTTIMIZZATI ==========

    /**
     * Restituisce il prezzo formattato in euro - ottimizzato per performance
     */
    public String getPrezzoFormattato() {
        return String.format("€%.2f", prezzo);
    }

    /**
     * Aggiunge un'offerta all'annuncio con validazione avanzata
     */
    public boolean aggiungiOfferta(Offerta offerta) {
        if (!isDisponibile() || offerta == null) {
            return false;
        }
        
        // Verifica che l'offerta sia maggiore dell'offerta corrente più alta
        Offerta offertaMassima = getOffertaPiuAlta();
        if (offertaMassima != null && offerta.getImporto() <= offertaMassima.getImporto()) {
            return false;
        }
        
        this.offerte.add(offerta);
        return true;
    }

    /**
     * Verifica se l'annuncio è disponibile per operazioni
     */
    public boolean isDisponibile() {
        return STATO_ATTIVO.equals(this.stato);
    }

    /**
     * Verifica se l'annuncio è per acquisto diretto
     */
    public boolean isAcquistoDiretto() {
        return this.tipologia == Tipologia.VENDITA;
    }

    /**
     * Verifica se l'annuncio è un'asta
     */
    public boolean isAsta() {
        return this.tipologia == Tipologia.ASTA;
    }

    /**
     * Restituisce l'offerta più alta tra quelle ricevute - ottimizzato
     */
    public Offerta getOffertaPiuAlta() {
        if (offerte.isEmpty()) {
            return null;
        }
        
        return offerte.stream()
                .max(Comparator.comparingDouble(Offerta::getImporto))
                .orElse(null);
    }

    /**
     * Calcola il prezzo scontato con validazione avanzata
     */
    public double calcolaPrezzoScontato(double percentualeSconto) {
        if (percentualeSconto < 0 || percentualeSconto > 100) {
            throw new IllegalArgumentException("Percentuale sconto deve essere tra 0 e 100: " + percentualeSconto);
        }
        
        double sconto = prezzo * (percentualeSconto / 100);
        double risultato = prezzo - sconto;
        
        return Math.round(risultato * 100.0) / 100.0;
    }

    /**
     * Ritira l'annuncio dal mercato
     */
    public void ritira() {
        this.stato = STATO_RITIRATO;
    }

    /**
     * Ripristina l'annuncio come attivo
     */
    public void riattiva() {
        this.stato = STATO_ATTIVO;
    }

    /**
     * Restituisce una descrizione breve dell'annuncio
     */
    public String getDescrizioneBreve() {
        String base = (titolo != null ? titolo : oggetto.getNome()) + " - " + getPrezzoFormattato();
        return inEvidenza ? base + " (IN EVIDENZA)" : base;
    }

    /**
     * Verifica se l'annuncio ha caratteristiche speciali
     */
    public boolean hasCaratteristiche() {
        return !caratteristicheSpeciali.isEmpty();
    }

    /**
     * Aggiunge una lista di caratteristiche speciali
     */
    public void aggiungiCaratteristiche(List<String> caratteristiche) {
        if (caratteristiche != null) {
            this.caratteristicheSpeciali.addAll(caratteristiche);
        }
    }

    /**
     * Aggiunge una singola caratteristica speciale
     */
    public void aggiungiCaratteristica(String caratteristica) {
        if (caratteristica != null && !caratteristica.trim().isEmpty()) {
            this.caratteristicheSpeciali.add(caratteristica.trim());
        }
    }

    /**
     * Restituisce la data di pubblicazione formattata
     */
    public String getDataPubblicazioneFormattata() {
        return dataPubblicazione.format(DATE_FORMATTER);
    }

    /**
     * Calcola la data di pubblicazione corrente
     */
    public static LocalDateTime calcolaDataPubblicazione() {
        return LocalDateTime.now();
    }

    /**
     * Calcola il timestamp di pubblicazione corrente
     */
    public static Timestamp calcolaTimestampDataPubblicazione() {
        return Timestamp.valueOf(LocalDateTime.now());
    }

    /**
     * Verifica se l'annuncio ha un'immagine associata
     */
    public boolean hasImmagine() {
        return getImageUrlSafe() != null && !getImageUrlSafe().isEmpty();
    }

    /**
     * Restituisce l'URL dell'immagine in modo sicuro
     */
    public String getImageUrlSafe() {
        if (oggetto == null) return "";
        String url = oggetto.getImageUrl();
        return (url != null && !url.equals("null")) ? url : "";
    }

    /**
     * Verifica se l'annuncio richiede un prezzo
     */
    public boolean richiedePrezzo() {
        return tipologia == Tipologia.VENDITA || tipologia == Tipologia.ASTA;
    }

    /**
     * Verifica se l'annuncio è per scambio
     */
    public boolean isScambio() {
        return tipologia == Tipologia.SCAMBIO;
    }

    /**
     * Verifica se l'annuncio è per regalo
     */
    public boolean isRegalo() {
        return tipologia == Tipologia.REGALO;
    }

    /**
     * Verifica se l'annuncio appartiene all'utente specificato
     */
    public boolean isProprietario(int utenteId) {
        return this.venditoreId == utenteId;
    }

    /**
     * Verifica se l'annuncio può essere acquistato dall'utente specificato
     */
    public boolean puoEssereAcquistatoDa(int utenteId) {
        return isDisponibile() && 
               !isProprietario(utenteId) && 
               (isAcquistoDiretto() || isAsta());
    }

    /**
     * Restituisce il tempo trascorso dalla pubblicazione in formato leggibile
     */
    public String getTempoTrascorso() {
        LocalDateTime now = LocalDateTime.now();
        long giorni = java.time.Duration.between(dataPubblicazione, now).toDays();
        
        if (giorni == 0) {
            long ore = java.time.Duration.between(dataPubblicazione, now).toHours();
            if (ore == 0) {
                long minuti = java.time.Duration.between(dataPubblicazione, now).toMinutes();
                return minuti <= 1 ? "Pochi secondi fa" : minuti + " minuti fa";
            }
            return ore + (ore == 1 ? " ora fa" : " ore fa");
        }
        
        return giorni + (giorni == 1 ? " giorno fa" : " giorni fa");
    }

    /**
     * Verifica se l'annuncio è scaduto (più di 30 giorni dalla pubblicazione)
     */
    public boolean isScaduto() {
        return java.time.Duration.between(dataPubblicazione, LocalDateTime.now()).toDays() > 30;
    }

    /**
     * Restituisce una lista di caratteristiche speciali come stringa formattata
     */
    public String getCaratteristicheFormattate() {
        if (caratteristicheSpeciali.isEmpty()) {
            return "Nessuna caratteristica speciale";
        }
        
        return String.join(" • ", caratteristicheSpeciali);
    }

    // ========== METODI DI VALIDAZIONE AVANZATI ==========

    /**
     * Valida l'intero annuncio per la pubblicazione
     */
    public boolean validaPerPubblicazione() {
        return titolo != null && !titolo.trim().isEmpty() &&
               descrizione != null && !descrizione.trim().isEmpty() &&
               oggetto != null &&
               (tipologia != Tipologia.VENDITA || prezzo > 0) &&
               venditoreId > 0;
    }

    /**
     * Restituisce un messaggio di errore di validazione
     */
    public String getMessaggioValidazione() {
        if (titolo == null || titolo.trim().isEmpty()) {
            return "Il titolo è obbligatorio";
        }
        if (descrizione == null || descrizione.trim().isEmpty()) {
            return "La descrizione è obbligatoria";
        }
        if (oggetto == null) {
            return "L'oggetto è obbligatorio";
        }
        if (tipologia == Tipologia.VENDITA && prezzo <= 0) {
            return "Il prezzo deve essere maggiore di 0 per annunci di vendita";
        }
        if (venditoreId <= 0) {
            return "Venditore non valido";
        }
        return null;
    }

    // ========== GETTER E SETTER CON VALIDAZIONE ==========

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Oggetto getOggetto() {
        return oggetto;
    }

    public void setOggetto(Oggetto oggetto) {
        this.oggetto = Objects.requireNonNull(oggetto, "L'oggetto non può essere null");
    }

    public double getPrezzo() {
        return prezzo;
    }

    public void setPrezzo(double prezzo) {
        if (prezzo < 0) {
            throw new IllegalArgumentException("Il prezzo non può essere negativo");
        }
        this.prezzo = prezzo;
    }

    /**
     * Restituisce una lista immodificabile delle caratteristiche speciali
     */
    public List<String> getCaratteristicheSpeciali() {
        return Collections.unmodifiableList(caratteristicheSpeciali);
    }

    public void setCaratteristicheSpeciali(List<String> caratteristiche) {
        this.caratteristicheSpeciali = new ArrayList<>(
            caratteristiche != null ? caratteristiche : Collections.emptyList()
        );
    }

    public boolean isInEvidenza() {
        return inEvidenza;
    }

    public void setInEvidenza(boolean inEvidenza) {
        this.inEvidenza = inEvidenza;
    }

    public String getModalitaConsegna() {
        return modalitaConsegna;
    }

    public void setModalitaConsegna(String modalitaConsegna) {
        this.modalitaConsegna = modalitaConsegna;
    }

    public String getStato() {
        return stato;
    }

    public void setStato(String stato) {
        this.stato = stato;
    }

    public int getVenditoreId() {
        return venditoreId;
    }

    public void setVenditoreId(int venditoreId) {
        if (venditoreId <= 0) {
            throw new IllegalArgumentException("L'ID venditore deve essere positivo");
        }
        this.venditoreId = venditoreId;
    }

    public LocalDateTime getDataPubblicazione() {
        return dataPubblicazione;
    }

    public void setDataPubblicazione(LocalDateTime dataPubblicazione) {
        this.dataPubblicazione = dataPubblicazione;
    }

    /**
     * Restituisce una lista immodificabile delle offerte
     */
    public List<Offerta> getOfferte() {
        return Collections.unmodifiableList(offerte);
    }

    public void setOfferte(List<Offerta> offerte) {
        this.offerte = new ArrayList<>(offerte != null ? offerte : Collections.emptyList());
    }

    public String getSedeConsegna() {
        return sedeConsegna;
    }

    public void setSedeConsegna(String sedeConsegna) {
        this.sedeConsegna = sedeConsegna;
    }

    public Tipologia getTipologia() {
        return tipologia;
    }

    public void setTipologia(Tipologia tipologia) {
        this.tipologia = Objects.requireNonNull(tipologia, "La tipologia non può essere null");
    }

    public void setTipologia(String tipologiaStr) {
        if (tipologiaStr == null || tipologiaStr.isBlank()) {
            throw new IllegalArgumentException("Tipologia non può essere vuota");
        }

        Tipologia t = Tipologia.fromDisplayName(tipologiaStr.trim());
        if (t == null) {
            throw new IllegalArgumentException("Tipologia non valida: " + tipologiaStr);
        }
        this.tipologia = t;
    }

    public String getTitolo() {
        return titolo;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }

    public String getCitta() {
        return citta;
    }

    public void setCitta(String citta) {
        this.citta = citta;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getNomeVenditore() {
        return nomeVenditore;
    }

    public void setNomeVenditore(String nomeVenditore) {
        this.nomeVenditore = nomeVenditore;
    }

    /**
     * Restituisce il nome del venditore in modo sicuro
     */
    public String getNomeUtenteVenditore() {
        return nomeVenditore != null ? nomeVenditore : "Utente #" + venditoreId;
    }

    // ========== EQUALS, HASHCODE, TO STRING OTTIMIZZATI ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Annuncio annuncio = (Annuncio) o;
        return id == annuncio.id &&
               venditoreId == annuncio.venditoreId &&
               Objects.equals(titolo, annuncio.titolo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, venditoreId, titolo);
    }

    @Override
    public String toString() {
        return String.format(
            "Annuncio{id=%d, titolo='%s', prezzo=%.2f, tipologia=%s, stato='%s', venditore=%s}",
            id, titolo, prezzo, tipologia, stato, getNomeUtenteVenditore()
        );
    }

    /**
     * Confronta due annunci per data di pubblicazione (più recente prima)
     */
    public static Comparator<Annuncio> comparatorePerData() {
        return (a1, a2) -> a2.getDataPubblicazione().compareTo(a1.getDataPubblicazione());
    }

    /**
     * Confronta due annunci per prezzo (più economico prima)
     */
    public static Comparator<Annuncio> comparatorePerPrezzo() {
        return Comparator.comparingDouble(Annuncio::getPrezzo);
    }

    /**
     * Confronta due annunci per prezzo (più costoso prima)
     */
    public static Comparator<Annuncio> comparatorePerPrezzoDesc() {
        return (a1, a2) -> Double.compare(a2.getPrezzo(), a1.getPrezzo());
    }

    /**
     * Confronta due annunci per titolo (alfabetico)
     */
    public static Comparator<Annuncio> comparatorePerTitolo() {
        return Comparator.comparing(Annuncio::getTitolo, String.CASE_INSENSITIVE_ORDER);
    }

    /**
     * Verifica se l'annuncio corrisponde a una query di ricerca
     */
    public boolean matchesSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            return true;
        }
        
        String searchTerm = query.toLowerCase().trim();
        return (titolo != null && titolo.toLowerCase().contains(searchTerm)) ||
               (descrizione != null && descrizione.toLowerCase().contains(searchTerm)) ||
               (categoria != null && categoria.toLowerCase().contains(searchTerm)) ||
               (citta != null && citta.toLowerCase().contains(searchTerm)) ||
               (oggetto != null && oggetto.getNome().toLowerCase().contains(searchTerm));
    }

    /**
     * Restituisce una rappresentazione JSON-like dell'annuncio (semplificata)
     */
    public String toJsonString() {
        return String.format(
            "{\"id\":%d,\"titolo\":\"%s\",\"prezzo\":%.2f,\"tipologia\":\"%s\",\"citta\":\"%s\",\"categoria\":\"%s\"}",
            id, 
            titolo != null ? titolo.replace("\"", "\\\"") : "",
            prezzo,
            tipologia != null ? tipologia.name() : "",
            citta != null ? citta.replace("\"", "\\\"") : "",
            categoria != null ? categoria.replace("\"", "\\\"") : ""
        );
    }
}