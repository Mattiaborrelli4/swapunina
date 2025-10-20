package application.Classe;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import application.Enum.Categoria;
import application.Enum.OrigineOggetto;

/**
 * Rappresenta un oggetto che può essere venduto, scambiato o regalato nel sistema
 * Gestisce tutte le informazioni relative a un oggetto e i suoi dettagli specifici
 * Supporta Cloudinary per la gestione avanzata delle immagini
 */
public class Oggetto {
    private int id;
    private String nome;
    private String descrizione;
    private Categoria categoria;
    private File immagine;
    private String imageUrl;
    private Map<String, String> dettagli;
    private OrigineOggetto origine;

    // Costanti per Cloudinary
    private static final String CLOUDINARY_BASE_URL = "https://res.cloudinary.com";
    private static final String CARD_TRANSFORMATION = "w_280,h_200,c_fill";
    private static final String LARGE_TRANSFORMATION = "w_600,h_400,c_fill";
    private static final String THUMBNAIL_TRANSFORMATION = "w_100,h_100,c_fill";

    /**
     * Costruttore completo per oggetto esistente
     */
    public Oggetto(int id, String nome, String descrizione, Categoria categoria, 
                  String imageUrl, File immagine, OrigineOggetto origine) {
        this.id = id;
        this.nome = nome != null ? nome : "";
        this.descrizione = descrizione != null ? descrizione : "";
        this.categoria = categoria != null ? categoria : Categoria.ALTRO;
        this.imageUrl = imageUrl != null ? imageUrl : "";
        this.immagine = immagine;
        this.dettagli = new HashMap<>();
        this.origine = origine != null ? origine : OrigineOggetto.USATO;
    }

    /**
     * Costruttore per nuovo oggetto senza ID
     */
    public Oggetto(String nome, String descrizione, Categoria categoria, 
                  String imageUrl, OrigineOggetto origine) {
        this(0, nome, descrizione, categoria, imageUrl, null, origine);
    }

    /**
     * Costruttore semplificato per oggetto base
     */
    public Oggetto(String nome, String descrizione, Categoria categoria) {
        this(0, nome, descrizione, categoria, "", null, OrigineOggetto.USATO);
    }

    // ========== METODI CLOUDINARY ==========

    /**
     * Verifica se l'oggetto ha un'immagine Cloudinary valida
     */
    public boolean hasCloudinaryImage() {
        return imageUrl != null && 
               !imageUrl.isEmpty() && 
               !imageUrl.equals("null") &&
               imageUrl.contains("cloudinary");
    }

    /**
     * Verifica se l'oggetto ha un'immagine locale
     */
    public boolean hasLocalImage() {
        return (immagine != null && immagine.exists()) || 
               (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("null") && !imageUrl.contains("cloudinary"));
    }

    /**
     * Restituisce l'URL dell'immagine ottimizzato per le card prodotto
     */
    public String getImageUrlForCard() {
        if (!hasCloudinaryImage()) {
            return getImageUrlSafe();
        }
        
        // Applica trasformazione Cloudinary per card
        return imageUrl.replace("/upload/", "/upload/" + CARD_TRANSFORMATION + "/");
    }

    /**
     * Restituisce l'URL dell'immagine per visualizzazione grande
     */
    public String getImageUrlForLarge() {
        if (!hasCloudinaryImage()) {
            return getImageUrlSafe();
        }
        
        // Applica trasformazione Cloudinary per visualizzazione grande
        return imageUrl.replace("/upload/", "/upload/" + LARGE_TRANSFORMATION + "/");
    }

    /**
     * Restituisce l'URL dell'immagine per thumbnail
     */
    public String getImageUrlForThumbnail() {
        if (!hasCloudinaryImage()) {
            return getImageUrlSafe();
        }
        
        // Applica trasformazione Cloudinary per thumbnail
        return imageUrl.replace("/upload/", "/upload/" + THUMBNAIL_TRANSFORMATION + "/");
    }

    /**
     * Restituisce l'URL originale dell'immagine Cloudinary
     */
    public String getImageUrlOriginal() {
        return getImageUrlSafe();
    }

    /**
     * Restituisce l'URL dell'immagine in modo sicuro
     */
    public String getImageUrlSafe() {
        if (imageUrl == null || imageUrl.equals("null")) {
            return "";
        }
        return imageUrl;
    }

    // ========== METODI COMPATIBILITÀ ==========

    /**
     * Restituisce l'URL dell'immagine ottimizzato per le card prodotto
     * (alias per getImageUrlForCard() per compatibilità con ProductCard)
     */
    public String getImageUrlOptimized() {
        return getImageUrlForCard();
    }

    /**
     * Restituisce l'URL dell'immagine per visualizzazione dettagliata
     * (alias per getImageUrlForLarge() per naming consistente)
     */
    public String getImageUrlForDetail() {
        return getImageUrlForLarge();
    }

    /**
     * Restituisce l'URL dell'immagine per anteprima
     * (alias per getImageUrlForThumbnail() per naming consistente)
     */
    public String getImageUrlForPreview() {
        return getImageUrlForThumbnail();
    }

    /**
     * Estrae il public_id Cloudinary dall'URL
     */
    public String getCloudinaryPublicId() {
        if (!hasCloudinaryImage()) {
            return null;
        }
        
        try {
            String[] parts = imageUrl.split("/upload/");
            if (parts.length > 1) {
                String path = parts[1];
                // Rimuovi la versione se presente (v1234567/)
                if (path.startsWith("v")) {
                    path = path.substring(path.indexOf('/') + 1);
                }
                // Rimuovi l'estensione del file
                int dotIndex = path.lastIndexOf('.');
                if (dotIndex > 0) {
                    path = path.substring(0, dotIndex);
                }
                return path;
            }
        } catch (Exception e) {
            System.err.println("Errore nell'estrazione del public_id: " + e.getMessage());
        }
        return null;
    }

    // ========== METODI DI VALIDAZIONE ==========

    /**
     * Verifica se l'oggetto è valido per l'inserimento
     */
    public boolean isValid() {
        return nome != null && !nome.trim().isEmpty() &&
               categoria != null;
    }

    /**
     * Restituisce un messaggio di errore di validazione
     */
    public String getValidationMessage() {
        if (nome == null || nome.trim().isEmpty()) {
            return "Il nome dell'oggetto è obbligatorio";
        }
        if (categoria == null) {
            return "La categoria è obbligatoria";
        }
        return null;
    }

    /**
     * Verifica se l'oggetto ha un'immagine associata (qualsiasi tipo)
     */
    public boolean hasImmagine() {
        return hasCloudinaryImage() || hasLocalImage();
    }

    /**
     * Verifica se l'oggetto ha una descrizione
     */
    public boolean hasDescrizione() {
        return descrizione != null && !descrizione.trim().isEmpty();
    }

    /**
     * Verifica se l'oggetto ha dettagli aggiuntivi
     */
    public boolean hasDettagli() {
        return !dettagli.isEmpty();
    }

    // ========== METODI GESTIONE DETTAGLI ==========

    /**
     * Aggiunge un dettaglio all'oggetto
     */
    public void aggiungiDettaglio(String chiave, String valore) {
        if (chiave != null && !chiave.trim().isEmpty() && valore != null) {
            dettagli.put(chiave.trim(), valore);
        }
    }

    /**
     * Rimuove un dettaglio dall'oggetto
     */
    public boolean rimuoviDettaglio(String chiave) {
        return dettagli.remove(chiave) != null;
    }

    /**
     * Restituisce il valore di un dettaglio specifico
     */
    public String getDettaglio(String chiave) {
        return dettagli.getOrDefault(chiave, "Non specificato");
    }

    /**
     * Verifica la presenza di un dettaglio specifico
     */
    public boolean hasDettaglio(String chiave) {
        return dettagli.containsKey(chiave) && 
               dettagli.get(chiave) != null && 
               !dettagli.get(chiave).trim().isEmpty();
    }

    /**
     * Restituisce una rappresentazione JSON-like dei dettagli
     */
    public String getDettagliFormattati() {
        if (dettagli.isEmpty()) {
            return "Nessun dettaglio aggiuntivo";
        }
        
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : dettagli.entrySet()) {
            sb.append("• ")
              .append(entry.getKey())
              .append(": ")
              .append(entry.getValue())
              .append("\n");
        }
        return sb.toString().trim();
    }

    /**
     * Restituisce i dettagli come mappa per il database
     */
    public String getDettagliPerDatabase() {
        if (dettagli.isEmpty()) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : dettagli.entrySet()) {
            if (sb.length() > 0) {
                sb.append(";");
            }
            // Escape dei punti e virgola nei valori
            String valoreEscaped = entry.getValue().replace(";", "\\;");
            sb.append(entry.getKey())
              .append("=")
              .append(valoreEscaped);
        }
        return sb.toString();
    }

    // ========== METODI UTILITY ==========

    /**
     * Restituisce una descrizione breve dell'oggetto
     */
    public String getDescrizioneBreve() {
        if (descrizione == null || descrizione.isEmpty()) {
            return nome != null ? nome : "Oggetto senza nome";
        }
        
        int maxLength = 100;
        if (descrizione.length() <= maxLength) {
            return descrizione;
        }
        
        return descrizione.substring(0, maxLength - 3) + "...";
    }

    /**
     * Restituisce il nome dell'oggetto in modo sicuro
     */
    public String getNomeSicuro() {
        return nome != null ? nome : "Oggetto #" + id;
    }

    /**
     * Restituisce la categoria in formato stringa
     */
    public String getCategoriaNome() {
        return categoria != null ? categoria.name() : "ALTRO";
    }

    /**
     * Restituisce l'origine in formato stringa
     */
    public String getOrigineNome() {
        return origine != null ? origine.name() : "USATO";
    }

    /**
     * Restituisce il nome visualizzabile dell'origine
     */
    public String getOrigineDisplayName() {
        if (origine == null) return "Usato";
        
        switch (origine) {
            case NUOVO: return "Nuovo";
            case USATO: return "Usato";
            case RICONDIZIONATO: return "Ricondizionato";
            case REGALO: return "Ricevuto in regalo";
            case SCAMBIO: return "Ottenuto per scambio";
            default: return "Usato";
        }
    }

    /**
     * Verifica se l'oggetto è nuovo
     */
    public boolean isNuovo() {
        return origine == OrigineOggetto.NUOVO;
    }

    /**
     * Verifica se l'oggetto è usato
     */
    public boolean isUsato() {
        return origine == OrigineOggetto.USATO;
    }

    /**
     * Verifica se l'oggetto è ricondizionato
     */
    public boolean isRicondizionato() {
        return origine == OrigineOggetto.RICONDIZIONATO;
    }

    /**
     * Restituisce il colore del badge in base all'origine
     */
    public String getBadgeColor() {
        if (origine == null) return "#3498db";
        
        switch (origine) {
            case NUOVO: return "#28a745"; // Verde
            case USATO: return "#6c757d"; // Grigio
            case RICONDIZIONATO: return "#17a2b8"; // Azzurro
            case REGALO: return "#e83e8c"; // Rosa
            case SCAMBIO: return "#ffc107"; // Giallo
            default: return "#3498db";
        }
    }

    // ========== METODI CLONAZIONE ==========

    /**
     * Crea una copia profonda dell'oggetto
     */
    public Oggetto clone() {
        Oggetto copia = new Oggetto(this.id, this.nome, this.descrizione, this.categoria, 
                                  this.imageUrl, this.immagine, this.origine);
        copia.dettagli.putAll(this.dettagli);
        return copia;
    }

    /**
     * Crea una copia dell'oggetto senza l'ID (per nuovi record)
     */
    public Oggetto cloneSenzaId() {
        Oggetto copia = new Oggetto(this.nome, this.descrizione, this.categoria, 
                                  this.imageUrl, this.origine);
        copia.dettagli.putAll(this.dettagli);
        copia.immagine = this.immagine;
        return copia;
    }

    /**
     * Aggiorna i dati dell'oggetto da un altro oggetto
     */
    public void aggiornaDa(Oggetto altro) {
        if (altro == null) return;
        
        this.nome = altro.nome;
        this.descrizione = altro.descrizione;
        this.categoria = altro.categoria;
        this.imageUrl = altro.imageUrl;
        this.immagine = altro.immagine;
        this.origine = altro.origine;
        this.dettagli.clear();
        this.dettagli.putAll(altro.dettagli);
    }

    // ========== GETTER E SETTER ==========

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome != null ? nome : "";
    }

    public void setNome(String nome) {
        this.nome = nome != null ? nome.trim() : "";
    }

    public String getDescrizione() {
        return descrizione != null ? descrizione : "";
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione != null ? descrizione.trim() : "";
    }

    public Categoria getCategoria() {
        return categoria != null ? categoria : Categoria.ALTRO;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria != null ? categoria : Categoria.ALTRO;
    }

    public File getImmagine() {
        return immagine;
    }

    public void setImmagine(File immagine) {
        this.immagine = immagine;
    }

    public String getImageUrl() {
        return imageUrl != null ? imageUrl : "";
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl != null ? imageUrl.trim() : "";
    }

    public OrigineOggetto getOrigine() {
        return origine != null ? origine : OrigineOggetto.USATO;
    }

    public void setOrigine(OrigineOggetto origine) {
        this.origine = origine != null ? origine : OrigineOggetto.USATO;
    }

    public Map<String, String> getDettagli() {
        return new HashMap<>(dettagli);
    }

    public void setDettagli(Map<String, String> dettagli) {
        this.dettagli.clear();
        if (dettagli != null) {
            this.dettagli.putAll(dettagli);
        }
    }

    // ========== EQUALS, HASHCODE, TO STRING ==========

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Oggetto oggetto = (Oggetto) o;
        return id == oggetto.id &&
                Objects.equals(nome, oggetto.nome) &&
                categoria == oggetto.categoria &&
                origine == oggetto.origine;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nome, categoria, origine);
    }

    @Override
    public String toString() {
        return String.format(
            "Oggetto{id=%d, nome='%s', categoria=%s, origine=%s, hasImage=%s, dettagli=%d}",
            id, getNomeSicuro(), getCategoriaNome(), getOrigineNome(), hasImmagine(), dettagli.size()
        );
    }

    /**
     * Restituisce una rappresentazione JSON dell'oggetto
     */
    public String toJsonString() {
        return String.format(
            "{\"id\":%d,\"nome\":\"%s\",\"categoria\":\"%s\",\"origine\":\"%s\",\"hasImage\":%s}",
            id, 
            nome != null ? nome.replace("\"", "\\\"") : "",
            getCategoriaNome(),
            getOrigineNome(),
            hasImmagine()
        );
    }

    /**
     * Restituisce una stringa per il debug
     */
    public String toDebugString() {
        return String.format(
            "Oggetto[ID=%d, Nome=%s, Categoria=%s, Origine=%s, Immagine=%s, Dettagli=%d]",
            id, nome, categoria, origine, imageUrl, dettagli.size()
        );
    }
}