package application.Classe;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.sql.Timestamp;

import application.Enum.Tipologia;

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
    // === COSTRUTTORI ===

    public Annuncio() {
        this.caratteristicheSpeciali = new ArrayList<>();
        this.offerte = new ArrayList<>();
        this.stato = "ATTIVO"; // 🔥 Cambiato da "Disponibile" a "ATTIVO"
        this.dataPubblicazione = LocalDateTime.now();
    }

    public Annuncio(Oggetto oggetto, double prezzo, String tipologiaStr, String modalitaConsegna, int venditoreId) {
        this();
        this.oggetto = oggetto;
        this.prezzo = prezzo;
        this.modalitaConsegna = modalitaConsegna;
        this.venditoreId = venditoreId;

        if (tipologiaStr == null || tipologiaStr.isBlank()) {
            throw new IllegalArgumentException("Tipologia non può essere vuota");
        }

        Tipologia tipologia = Tipologia.fromDisplayName(tipologiaStr);
        if (tipologia == null) {
            throw new IllegalArgumentException("Tipologia non valida: " + tipologiaStr);
        }

        this.tipologia = tipologia;
    }

    public Annuncio(Oggetto oggetto, double prezzo, Tipologia tipologia, String modalitaConsegna, int venditoreId) {
        this();
        this.oggetto = oggetto;
        this.prezzo = prezzo;
        this.tipologia = tipologia;
        this.modalitaConsegna = modalitaConsegna;
        this.venditoreId = venditoreId;
    }

    // === METODI DI UTILITÀ ===

    public String getPrezzoFormattato() {
        return String.format("€%.2f", prezzo);
    }

    public void aggiungiOfferta(Offerta offerta) {
        if (isDisponibile()) {
            this.offerte.add(offerta);
        }
    }

    public boolean isDisponibile() {
        return "ATTIVO".equalsIgnoreCase(this.stato); // 🔥 Aggiornato
    }

    public boolean isAcquistoDiretto() {
        return this.tipologia == Tipologia.VENDITA;
    }

    public boolean isAsta() {
        return this.tipologia == Tipologia.ASTA;
    }

    public Offerta getOffertaPiuAlta() {
        return offerte.stream()
                .max(Comparator.comparingDouble(Offerta::getImporto))
                .orElse(null);
    }

    public double calcolaPrezzoScontato(double percentualeSconto) {
        return prezzo * (1 - percentualeSconto / 100);
    }

    public void ritira() {
        this.stato = "Ritirato";
    }

    public String getDescrizioneBreve() {
        return oggetto.getNome() + " - " + getPrezzoFormattato() +
               (inEvidenza ? " (IN EVIDENZA)" : "");
    }

    public boolean hasCaratteristiche() {
        return !caratteristicheSpeciali.isEmpty();
    }

    public void aggiungiCaratteristiche(List<String> caratteristiche) {
        this.caratteristicheSpeciali.addAll(caratteristiche);
    }

    public String getDataPubblicazioneFormattata() {
        return dataPubblicazione.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }



    public String getUtenteEmail() {
        return "email_venditore_" + venditoreId + "@esempio.com";
    }

    public static LocalDateTime calcolaDataPubblicazione() {
        return LocalDateTime.now();
    }

    public static Timestamp calcolaTimestampDataPubblicazione() {
        return new Timestamp(System.currentTimeMillis());
    }

    // 🔥 NUOVO: Metodo per verificare se ha un'immagine
    public boolean hasImmagine() {
        return oggetto != null && oggetto.getImageUrl() != null && 
               !oggetto.getImageUrl().isEmpty() && !oggetto.getImageUrl().equals("null");
    }

    // 🔥 NUOVO: Metodo per ottenere l'URL dell'immagine (safe)
    public String getImageUrlSafe() {
        return (oggetto != null && oggetto.getImageUrl() != null) ? oggetto.getImageUrl() : "";
    }

    // === GETTER E SETTER ===

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
        this.oggetto = oggetto;
    }

    public double getPrezzo() {
        return prezzo;
    }

    public void setPrezzo(double prezzo) {
        this.prezzo = prezzo;
    }

    public List<String> getCaratteristicheSpeciali() {
        return caratteristicheSpeciali;
    }

    public void setCaratteristicheSpeciali(List<String> caratteristiche) {
        this.caratteristicheSpeciali = caratteristiche;
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

    public void setVenditoreId(int venditoreId) {
        this.venditoreId = venditoreId;
    }

    public LocalDateTime getDataPubblicazione() {
        return dataPubblicazione;
    }

    public void setDataPubblicazione(LocalDateTime dataPubblicazione) {
        this.dataPubblicazione = dataPubblicazione;
    }

    public List<Offerta> getOfferte() {
        return new ArrayList<>(offerte);
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
        this.tipologia = tipologia;
    }

    public void setTipologia(String tipologiaStr) {
        Tipologia t = Tipologia.fromDisplayName(tipologiaStr);
        if (t == null) throw new IllegalArgumentException("Tipologia non valida: " + tipologiaStr);
        this.tipologia = t;
    }

    public Oggetto getOggettoPrincipale() {
        return oggetto;
    }

    public void setTitolo(String titolo) {
        this.titolo = titolo;
    }

    public String getTitolo() {
        return titolo;
    }

    public void setCitta(String citta) {
        this.citta = citta;
    }

    public String getCitta() {
        return citta;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public boolean richiedePrezzo() {
        return tipologia == Tipologia.VENDITA || tipologia == Tipologia.ASTA;
    }

    public boolean isScambio() {
        return tipologia == Tipologia.SCAMBIO;
    }

    public boolean isRegalo() {
        return tipologia == Tipologia.REGALO;
    }

    // === toString ===

    @Override
    public String toString() {
        return "Annuncio{" +
                "id=" + id +
                ", oggetto=" + oggetto +
                ", prezzo=" + prezzo +
                ", caratteristicheSpeciali=" + caratteristicheSpeciali +
                ", inEvidenza=" + inEvidenza +
                ", tipologia=" + tipologia +
                ", modalitaConsegna='" + modalitaConsegna + '\'' +
                ", stato='" + stato + '\'' +
                ", venditoreId=" + venditoreId +
                ", dataPubblicazione=" + dataPubblicazione +
                ", sedeConsegna='" + sedeConsegna + '\'' +
                '}';
    }
    
    public int getVenditoreId() {
        return this.venditoreId; // Assuming you have this field
    }
   
    
    public String getNomeVenditore() {
        return nomeVenditore;
    }
    
    public void setNomeVenditore(String nomeVenditore) {
        this.nomeVenditore = nomeVenditore;
    }
    
    // Modifica il metodo esistente per restituire il nome invece dell'ID
    public String getNomeUtenteVenditore() {
        return nomeVenditore != null ? nomeVenditore : "Utente #" + venditoreId;
    }
}