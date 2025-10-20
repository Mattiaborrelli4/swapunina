package application;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.util.Map;

public class config {
    private static Cloudinary cloudinary;
    
    static {
        initializeCloudinary();
    }
    
    private static void initializeCloudinary() {
        try {
            String cloudinaryUrl = System.getenv("CLOUDINARY_URL");
            
            if (cloudinaryUrl == null || cloudinaryUrl.trim().isEmpty()) {
                System.err.println("⚠️  Avviso: CLOUDINARY_URL non trovata. Cloudinary non sarà disponibile.");
                // Puoi anche usare configurazioni di fallback qui
                return;
            }
            
            cloudinary = new Cloudinary(cloudinaryUrl);
            System.out.println("✅ Cloudinary configurato correttamente");
            
        } catch (Exception e) {
            System.err.println("❌ Errore nella configurazione di Cloudinary: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static Cloudinary getCloudinary() {
        if (cloudinary == null) {
            throw new IllegalStateException("Cloudinary non è configurato. Verifica la variabile CLOUDINARY_URL.");
        }
        return cloudinary;
    }
    
    public static boolean isAvailable() {
        return cloudinary != null;
    }
}