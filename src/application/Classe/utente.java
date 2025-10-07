package application.Classe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public utente() {}

    public utente(String matricola, String nome, String cognome, String email, String password) {
        this.matricola = matricola;
        this.nome = nome;
        this.cognome = cognome;
        this.email = email;
        this.password = password;
    }

    // Getter e Setter
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
        return annunci; 
    }
    
    public void setAnnunci(List<Annuncio> annunci) { 
        this.annunci = annunci; 
    }

    public List<Offerta> getOfferte() { 
        return offerte; 
    }
    
    public void setOfferte(List<Offerta> offerte) { 
        this.offerte = offerte; 
    }

    public Map<String, Object> getProperties() { 
        return properties; 
    }
    
    public void setProperties(Map<String, Object> properties) { 
        this.properties = properties; 
    }

    public boolean valido() {
        return matricola != null && !matricola.isEmpty() &&
               email != null && !email.isEmpty() &&
               nome != null && !nome.isEmpty() &&
               cognome != null && !cognome.isEmpty() &&
               password != null && password.length() >= 8;
    }

    public boolean verificaCredenziali(String emailInput, String passwordInput) {
        return this.email.equals(emailInput) && this.password.equals(passwordInput);
    }

    public void aggiornaPassword(String nuovaPassword) {
        this.password = nuovaPassword;
    }

    public List<Annuncio> getAnnunciPubblicati() {
        return annunci;
    }

    public List<Offerta> getOfferteEffettuate() {
        return offerte;
    }
    
    public void setProperty(String key, Object value) {
        properties.put(key, value);
    }
    
    public Object getProperty(String key) {
        return properties.get(key);
    }
    
    public String getTitoloAnnuncio() {
        return (String) properties.getOrDefault("titolo_annuncio", "Conversazione");
    }

    @Override
    public String toString() {
        return "utente{" +
                "id=" + id +
                ", matricola='" + matricola + '\'' +
                ", email='" + email + '\'' +
                ", nome='" + nome + '\'' +
                ", cognome='" + cognome + '\'' +
                ", password='" + password + '\'' +
                ", annunci=" + annunci.size() +
                ", offerte=" + offerte.size() +
                '}';
    }
}