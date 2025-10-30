package application;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

/**
 * Servizio per caricare immagini su Cloudinary.
 */
public class servic {
    private final Cloudinary cloudinary;

    public servic() {
        this.cloudinary = config.getCloudinary();
    }

    /**
     * Carica un'immagine per un annuncio da InputStream
     */
    public String uploadImmagineAnnuncio(InputStream is, String originalFilename, int annuncioId) throws Exception {
        return upload(is, originalFilename, "annunci", String.valueOf(annuncioId));
    }

    /**
     * Carica un'immagine per un annuncio da File (con ID annuncio)
     */
    public String uploadImmagineAnnuncio(File file, int annuncioId) throws Exception {
        try (InputStream is = new FileInputStream(file)) {
            return upload(is, file.getName(), "annunci", String.valueOf(annuncioId));
        }
    }

    /**
     * Carica un'immagine per un annuncio (versione con String come identificatore)
     */
    public String uploadImmagineAnnuncio(File file, String publicIdSuffix) throws Exception {
        try (InputStream is = new FileInputStream(file)) {
            return upload(is, file.getName(), "annunci", publicIdSuffix);
        }
    }

    /**
     * Carica un file in una cartella specifica
     */
    public String uploadFile(File file, String folder, String publicIdSuffix) throws Exception {
        try (InputStream is = new FileInputStream(file)) {
            return upload(is, file.getName(), folder, publicIdSuffix);
        }
    }

    /**
     * Carica un'immagine generica su Cloudinary
     */
    public String upload(InputStream is, String originalFilename, String folder, String publicIdSuffix) throws Exception {
        if (!config.isAvailable()) {
            throw new IllegalStateException("Cloudinary non è configurato");
        }

        // Estrae l'estensione del file
        String ext = "jpg";
        int dot = originalFilename.lastIndexOf('.');
        if (dot >= 0) {
            ext = originalFilename.substring(dot + 1);
        }

        // Genera un publicId univoco
        String publicId = publicIdSuffix + "_" + UUID.randomUUID().toString();

        // Configura le opzioni di upload
        Map<String, Object> options = ObjectUtils.asMap(
                "folder", folder,
                "public_id", publicId,
                "resource_type", "image",
                "overwrite", true
        );

        // Esegue l'upload
        Map uploadResult = cloudinary.uploader().upload(is, options);
        return (String) uploadResult.get("secure_url");
    }

    /**
     * Elimina un'immagine da Cloudinary
     */
    public boolean eliminaImmagine(String imageUrl) throws Exception {
        if (!config.isAvailable() || imageUrl == null || imageUrl.isEmpty()) {
            return false;
        }

        // Estrai il public_id dall'URL
        String publicId = extractPublicIdFromUrl(imageUrl);
        if (publicId == null) return false;

        Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        return "ok".equals(result.get("result"));
    }

    /**
     * Estrae il public_id dall'URL di Cloudinary
     */
    private String extractPublicIdFromUrl(String imageUrl) {
        try {
            // L'URL di Cloudinary ha il formato: https://res.cloudinary.com/cloudname/image/upload/v1234567/folder/public_id.jpg
            String[] parts = imageUrl.split("/upload/");
            if (parts.length > 1) {
                String path = parts[1];
                // Rimuovi la versione se presente
                if (path.startsWith("v")) {
                    path = path.substring(path.indexOf('/') + 1);
                }
                // Rimuovi l'estensione
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

    /**
     * Verifica se Cloudinary è configurato e disponibile
     */
    public boolean isAvailable() {
        return config.isAvailable();
    }

    /**
     * Carica un'immagine per il profilo utente
     */
    public String uploadImmagineProfilo(File file, int userId) throws Exception {
        try (InputStream is = new FileInputStream(file)) {
            return upload(is, file.getName(), "profili", "user_" + userId);
        }
    }

    /**
     * Carica un'immagine per una categoria specifica
     */
    public String uploadImmagineCategoria(File file, String categoria) throws Exception {
        try (InputStream is = new FileInputStream(file)) {
            return upload(is, file.getName(), "categorie", "cat_" + categoria.toLowerCase());
        }
    }

    /**
     * Ottiene l'URL ottimizzato per una card prodotto
     */
    public String getUrlOttimizzatoCard(String originalUrl) {
        if (originalUrl == null || !originalUrl.contains("cloudinary")) {
            return originalUrl;
        }
        return originalUrl.replace("/upload/", "/upload/w_280,h_200,c_fill/");
    }

    /**
     * Ottiene l'URL ottimizzato per visualizzazione grande
     */
    public String getUrlOttimizzatoLarge(String originalUrl) {
        if (originalUrl == null || !originalUrl.contains("cloudinary")) {
            return originalUrl;
        }
        return originalUrl.replace("/upload/", "/upload/w_600,h_400,c_fill/");
    }

    /**
     * Ottiene l'URL ottimizzato per thumbnail
     */
    public String getUrlOttimizzatoThumbnail(String originalUrl) {
        if (originalUrl == null || !originalUrl.contains("cloudinary")) {
            return originalUrl;
        }
        return originalUrl.replace("/upload/", "/upload/w_100,h_100,c_fill/");
    }

    /**
     * Verifica se un URL è di Cloudinary
     */
    public boolean isCloudinaryUrl(String imageUrl) {
        return imageUrl != null && imageUrl.contains("cloudinary");
    }

    /**
     * Pulisce e normalizza l'URL dell'immagine
     */
    public String normalizzaUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.equals("null")) {
            return "";
        }
        return imageUrl.trim();
    }
}