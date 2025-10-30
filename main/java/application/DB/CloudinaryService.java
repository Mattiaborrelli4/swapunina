package application.DB;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CloudinaryService {
    private static final Logger LOGGER = Logger.getLogger(CloudinaryService.class.getName());
    private final Cloudinary cloudinary;
    private final boolean enabled;
    
    public CloudinaryService() {
        Cloudinary tempCloudinary = null;
        boolean tempEnabled = false;
        
        try {
            // Leggi la Cloudinary URL dalle variabili d'ambiente
            String cloudinaryUrl = System.getenv("CLOUDINARY_URL");
            
            // DEBUG: stampa per verificare se la variabile d'ambiente è presente
            System.out.println("🔍 CLOUDINARY_URL dall'ambiente: " + 
                (cloudinaryUrl != null ? "Presente (" + cloudinaryUrl.length() + " caratteri)" : "Non presente"));
            
            if (cloudinaryUrl == null || cloudinaryUrl.isEmpty()) {
                LOGGER.log(Level.WARNING, "⚠️ CLOUDINARY_URL non trovata nelle variabili d'ambiente - Cloudinary disabilitato");
                tempEnabled = false;
            } else if (cloudinaryUrl.contains("<your_api_key>") || cloudinaryUrl.contains("<your_api_secret>")) {
                LOGGER.log(Level.SEVERE, "❌ CLOUDINARY_URL contiene segnaposto - Cloudinary disabilitato");
                LOGGER.log(Level.SEVERE, "❌ Sostituisci <your_api_key> e <your_api_secret> con le tue credenziali reali");
                tempEnabled = false;
            } else {
                try {
                    tempCloudinary = new Cloudinary(cloudinaryUrl);
                    tempCloudinary.config.secure = true;
                    tempEnabled = true;
                    LOGGER.log(Level.INFO, "✅ Cloudinary configurato con successo");
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "❌ Errore nella creazione di Cloudinary: " + e.getMessage());
                    tempEnabled = false;
                }
            }
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Errore critico nella configurazione di Cloudinary: " + e.getMessage(), e);
            tempEnabled = false;
        }
        
        this.cloudinary = tempCloudinary;
        this.enabled = tempEnabled;
        
        if (!this.enabled) {
            LOGGER.log(Level.WARNING, "🚫 Cloudinary Service disabilitato - uso percorsi locali");
        }
    }
    
    /**
     * Carica un'immagine su Cloudinary e restituisce l'URL pubblico
     * Se Cloudinary è disabilitato, restituisce il percorso originale
     */
    public String uploadImage(String imagePath, String publicId) {
        // Se Cloudinary è disabilitato, ritorna il percorso locale
        if (!enabled) {
            LOGGER.log(Level.WARNING, "⚠️ Cloudinary disabilitato - ritorno percorso locale: {0}", imagePath);
            return imagePath;
        }
        
        if (cloudinary == null) {
            LOGGER.log(Level.SEVERE, "❌ Cloudinary non inizializzato");
            return imagePath;
        }
        
        try {
            Map<?, ?> uploadResult;
            Map<String, Object> uploadOptions = ObjectUtils.emptyMap();
            
            if (publicId != null && !publicId.isEmpty()) {
                uploadOptions.put("public_id", publicId);
            }
            
            LOGGER.log(Level.INFO, "🔄 Tentativo di upload su Cloudinary: {0}", imagePath);
            
            if (imagePath.startsWith("http") || imagePath.startsWith("cloudinary")) {
                // Se è già un URL, carica da URL
                LOGGER.log(Level.INFO, "📤 Upload da URL: {0}", imagePath);
                uploadResult = cloudinary.uploader().upload(imagePath, uploadOptions);
            } else {
                // Altrimenti carica da file
                File file = new File(imagePath);
                if (!file.exists()) {
                    LOGGER.log(Level.WARNING, "❌ File non trovato: {0}", imagePath);
                    return null;
                }
                LOGGER.log(Level.INFO, "📤 Upload da file locale: {0}", file.getAbsolutePath());
                uploadResult = cloudinary.uploader().upload(file, uploadOptions);
            }
            
            String secureUrl = (String) uploadResult.get("secure_url");
            LOGGER.log(Level.INFO, "✅ Immagine caricata su Cloudinary: {0}", secureUrl);
            return secureUrl;
            
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "❌ Errore IO nel caricamento su Cloudinary: " + e.getMessage(), e);
            return imagePath; // Fallback al percorso locale
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Errore generico Cloudinary: " + e.getMessage(), e);
            return imagePath; // Fallback al percorso locale
        }
    }
    
    /**
     * Elimina un'immagine da Cloudinary
     */
    public boolean deleteImage(String publicId) {
        if (!enabled || cloudinary == null) {
            LOGGER.log(Level.WARNING, "⚠️ Cloudinary disabilitato - impossibile eliminare: {0}", publicId);
            return false;
        }
        
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            String outcome = (String) result.get("result");
            if ("ok".equals(outcome)) {
                LOGGER.log(Level.INFO, "✅ Immagine eliminata da Cloudinary: {0}", publicId);
                return true;
            } else {
                LOGGER.log(Level.WARNING, "⚠️ Impossibile eliminare l'immagine: {0}", outcome);
                return false;
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Errore nell'eliminazione da Cloudinary: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Genera un public_id univoco per un utente
     */
    public String generateUserPublicId(int userId, String email) {
        String safeEmail = email.split("@")[0].replaceAll("[^a-zA-Z0-9]", "_");
        return "swapunina/profiles/user_" + userId + "_" + safeEmail;
    }
    
    /**
     * Verifica se Cloudinary è abilitato
     */
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * Metodo di utilità per testare la connessione a Cloudinary
     */
    public boolean testConnection() {
        if (!enabled || cloudinary == null) {
            LOGGER.log(Level.WARNING, "❌ Test connessione fallito: Cloudinary disabilitato");
            return false;
        }
        
        try {
            // Prova a fare una operazione semplice per testare la connessione
            cloudinary.uploader().explicit("test", ObjectUtils.asMap("type", "upload"));
            LOGGER.log(Level.INFO, "✅ Test connessione Cloudinary: SUCCESSO");
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Test connessione Cloudinary: FALLITO - " + e.getMessage());
            return false;
        }
    }

    // ========== METODI COMPATIBILITÀ ==========

    /**
     * Upload per immagini di annunci (compatibilità con servic)
     */
    public String uploadImmagineAnnuncio(File imageFile, String publicId) {
        if (imageFile == null || !imageFile.exists()) {
            return null;
        }
        return uploadImage(imageFile.getAbsolutePath(), publicId);
    }

    /**
     * Upload generico di file (compatibilità con servic)
     */
    public String upload(File file, String publicId) {
        if (file == null || !file.exists()) {
            return null;
        }
        return uploadImage(file.getAbsolutePath(), publicId);
    }
}