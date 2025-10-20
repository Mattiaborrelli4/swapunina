package application.messagistica;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.KeyGenerator;
import java.security.SecureRandom;
import java.util.Base64;
import java.security.MessageDigest;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.SecretKeyFactory;
import java.security.spec.KeySpec;

public class MessageEncryptor {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;
    private static final int SALT_LENGTH = 16;
    
    // Genera una chiave AES random
    public static byte[] generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(256);
        return keyGen.generateKey().getEncoded();
    }
    
    // Encrypt del messaggio
    public static EncryptedMessage encryptMessage(String message, byte[] key) throws Exception {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH, iv);
        SecretKey secretKey = new SecretKeySpec(key, "AES");
        
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
        byte[] encryptedText = cipher.doFinal(message.getBytes("UTF-8"));
        
        return new EncryptedMessage(encryptedText, iv);
    }
    
    // Decrypt del messaggio
    public static String decryptMessage(EncryptedMessage encryptedMessage, byte[] key) throws Exception {
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec parameterSpec = new GCMParameterSpec(TAG_LENGTH, encryptedMessage.getIv());
        SecretKey secretKey = new SecretKeySpec(key, "AES");
        
        cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
        byte[] decryptedText = cipher.doFinal(encryptedMessage.getEncryptedData());
        
        return new String(decryptedText, "UTF-8");
    }
    
    // Deriva una chiave dalla password dell'utente
    public static byte[] deriveKeyFromPassword(String password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return factory.generateSecret(spec).getEncoded();
    }
    
    // Classe per contenere il messaggio encrypted
    public static class EncryptedMessage {
        private byte[] encryptedData;
        private byte[] iv;
        
        public EncryptedMessage(byte[] encryptedData, byte[] iv) {
            this.encryptedData = encryptedData;
            this.iv = iv;
        }
        
        public byte[] getEncryptedData() { return encryptedData; }
        public byte[] getIv() { return iv; }
    }
}