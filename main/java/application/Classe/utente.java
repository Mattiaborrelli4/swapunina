package application.Classe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rappresenta un utente del sistema con tutte le informazioni personali
 * e le operazioni associate alla gestione del profilo e delle attività
 */
public class utente {
    private int id;
    private String matricola;
    private String email;
    private String nome;
    private String cognome;
    private String password;
    private List<Annuncio> annunci = new ArrayList<>();
    private List<Offerta> offerte = new ArrayList<>();
    private Map<String, Object> properties = new HashMap<>();
    private String fotoProfilo;

    public utente() {}

    public utente(String matricola, String nome, String cognome, String email, String password) {
        this.matricola = matricola;
        this.nome = nome;
        this.cognome = cognome;
        this.email = email;
        this.password = password;
    }

    /**
     * Verifica se l'utente ha dati validi per la registrazione
     */
    public boolean valido() {
        return matricola != null && !matricola.isEmpty() &&
               email != null && !email.isEmpty() &&
               nome != null && !nome.isEmpty() &&
               cognome != null && !cognome.isEmpty() &&
               password != null && password.length() >= 8;
    }

    /**
     * Verifica le credenziali di login
     */
    public boolean verificaCredenziali(String emailInput, String passwordInput) {
        return this.email.equals(emailInput) && this.password.equals(passwordInput);
    }

    /**
     * Aggiorna la password dell'utente
     */
    public void aggiornaPassword(String nuovaPassword) {
        this.password = nuovaPassword;
    }

    /**
     * Aggiunge una proprietà custom all'utente
     */
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }
    
    /**
     * Recupera una proprietà custom dell'utente
     */
    public Object getProperty(String key) {
        return properties.get(key);
    }
    
    /**
     * Recupera il titolo dell'annuncio dalla proprietà custom
     */
    public String getTitoloAnnuncio() {
        return (String) properties.getOrDefault("titolo_annuncio", "Conversazione");
    }

    /**
     * Restituisce il nome completo dell'utente
     */
    public String getNomeCompleto() {
        return nome + " " + cognome;
    }

    /**
     * Verifica se l'utente ha annunci pubblicati
     */
    public boolean hasAnnunci() {
        return !annunci.isEmpty();
    }

    /**
     * Verifica se l'utente ha effettuato offerte
     */
    public boolean hasOfferte() {
        return !offerte.isEmpty();
    }

    /**
     * Aggiunge un annuncio alla lista dell'utente
     */
    public void aggiungiAnnuncio(Annuncio annuncio) {
        if (annuncio != null) {
            annunci.add(annuncio);
        }
    }

    /**
     * Aggiunge un'offerta alla lista dell'utente
     */
    public void aggiungiOfferta(Offerta offerta) {
        if (offerta != null) {
            offerte.add(offerta);
        }
    }

    /**
     * Restituisce il numero di annunci pubblicati
     */
    public int getNumeroAnnunci() {
        return annunci.size();
    }

    /**
     * Restituisce il numero di offerte effettuate
     */
    public int getNumeroOfferte() {
        return offerte.size();
    }

    // ========== GETTER E SETTER ==========

    public int getId() { 
        return id; 
    }
    
    public void setId(int id) { 
        this.id = id; 
    }

    public String getMatricola() { 
        return matricola; 
    }
    
    public void setMatricola(String matricola) { 
        this.matricola = matricola; 
    }

    public String getEmail() { 
        return email; 
    }
    
    public void setEmail(String email) { 
        this.email = email; 
    }

    public String getNome() { 
        return nome; 
    }
    
    public void setNome(String nome) { 
        this.nome = nome; 
    }

    public String getCognome() { 
        return cognome; 
    }
    
    public void setCognome(String cognome) { 
        this.cognome = cognome; 
    }

    public String getPassword() { 
        return password; 
    }
    
    public void setPassword(String password) { 
        this.password = password; 
    }

    public List<Annuncio> getAnnunci() { 
        return new ArrayList<>(annunci); 
    }
    
    public void setAnnunci(List<Annuncio> annunci) { 
        this.annunci = new ArrayList<>(annunci); 
    }

    public List<Offerta> getOfferte() { 
        return new ArrayList<>(offerte); 
    }
    
    public void setOfferte(List<Offerta> offerte) { 
        this.offerte = new ArrayList<>(offerte); 
    }

    public Map<String, Object> getProperties() { 
        return new HashMap<>(properties); 
    }
    
    public void setProperties(Map<String, Object> properties) { 
        this.properties = new HashMap<>(properties); 
    }

    /**
     * Metodi alias per compatibilità
     */
    public List<Annuncio> getAnnunciPubblicati() {
        return getAnnunci();
    }

    public List<Offerta> getOfferteEffettuate() {
        return getOfferte();
    }

    @Override
    public String toString() {
        return "utente{" +
                "id=" + id +
                ", matricola='" + matricola + '\'' +
                ", email='" + email + '\'' +
                ", nome='" + nome + '\'' +
                ", cognome='" + cognome + '\'' +
                ", annunci=" + annunci.size() +
                ", offerte=" + offerte.size() +
                '}';
    }
    
    
    // GETTER e SETTER per fotoProfilo
    public String getFotoProfilo() {
        return fotoProfilo;
    }
    
    public void setFotoProfilo(String fotoProfilo) {
        this.fotoProfilo = fotoProfilo;
    }
    
    /**
     * Verifica se l'utente ha una foto profilo
     */
    public boolean hasFotoProfilo() {
        return fotoProfilo != null && !fotoProfilo.trim().isEmpty();
    }
    
    /**
     * Restituisce l'URL della foto profilo o null se non presente
     */
    public String getFotoProfiloSafe() {
        return hasFotoProfilo() ? fotoProfilo : null;
    }
}