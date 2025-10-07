package application.Classe;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import application.Enum.Categoria;
import application.Enum.OrigineOggetto;

public class Oggetto {
    private int id;
    private String nome; // Corrisponde alla colonna 'titolo' nel DB
    private String descrizione;
    private Categoria categoria;
    private File immagine;
    private String imageUrl;
    private Map<String, String> dettagli;
    private OrigineOggetto origine;

    // Costruttore principale
    public Oggetto(int id, String nome, String descrizione, Categoria categoria, String imageUrl, File immagine, OrigineOggetto origine) {
        this.id = id;
        this.nome = nome;
        this.descrizione = descrizione;
        this.categoria = categoria;
        this.immagine = immagine;
        this.imageUrl = imageUrl;
        this.dettagli = new HashMap<>();
        this.origine = origine;
    }

    // Costruttore senza ID e File immagine (per nuovi oggetti)
    public Oggetto(String nome, String descrizione, Categoria categoria, String imageUrl, OrigineOggetto origine) {
        this.nome = nome;
        this.descrizione = descrizione;
        this.categoria = categoria;
        this.imageUrl = imageUrl;
        this.immagine = null;
        this.dettagli = new HashMap<>();
        this.origine = origine;
    }

    // GETTER e SETTER
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
    }

    public File getImmagine() {
        return immagine;
    }

    public void setImmagine(File immagine) {
        this.immagine = immagine;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public OrigineOggetto getOrigine() {
        return origine;
    }

    public void setOrigine(OrigineOggetto origine) {
        this.origine = origine;
    }

    // Metodi per la gestione dei dettagli
    public void aggiungiDettaglio(String chiave, String valore) {
        dettagli.put(chiave, valore);
    }

    public Map<String, String> getDettagli() {
        return new HashMap<>(dettagli);
    }

    public boolean hasDettaglio(String chiave) {
        return dettagli.containsKey(chiave);
    }

    public String getDettaglio(String chiave) {
        return dettagli.getOrDefault(chiave, "N/D");
    }

    // Metodi di utilità
    @Override
    public Oggetto clone() {
        Oggetto copia = new Oggetto(this.id, this.nome, this.descrizione, this.categoria, this.imageUrl, this.immagine, this.origine);
        copia.dettagli.putAll(this.dettagli);
        return copia;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Oggetto oggetto = (Oggetto) o;
        return id == oggetto.id;
    }

    @Override
    public String toString() {
        return "Oggetto{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", descrizione='" + descrizione + '\'' +
                ", categoria=" + categoria +
                ", imageUrl='" + imageUrl + '\'' +
                ", origine=" + origine +
                '}';
    }
}