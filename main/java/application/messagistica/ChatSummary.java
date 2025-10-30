package application.messagistica;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatSummary {
    private final StringProperty nomeInterlocutore;
    private final StringProperty ultimoMessaggio;
    private final LocalDateTime timestamp;
    private final StringProperty timestampFormatted;

    public ChatSummary(int annuncioId, String annuncioTitolo, String nomeInterlocutore, 
                      String ultimoMessaggio, LocalDateTime timestamp) {
        this.nomeInterlocutore = new SimpleStringProperty(nomeInterlocutore);
        this.ultimoMessaggio = new SimpleStringProperty(ultimoMessaggio);
        this.timestamp = timestamp;
        this.timestampFormatted = new SimpleStringProperty(formatTimestamp(timestamp));
    }

    private String formatTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) return "";
        
        LocalDateTime now = LocalDateTime.now();
        if (timestamp.toLocalDate().equals(now.toLocalDate())) {
            return timestamp.format(DateTimeFormatter.ofPattern("HH:mm"));
        } else {
            return timestamp.format(DateTimeFormatter.ofPattern("dd/MM/yy"));
        }
    }

    // Getter methods
    public String getNomeInterlocutore() { return nomeInterlocutore.get(); }
    public String getUltimoMessaggio() { return ultimoMessaggio.get(); }
    public LocalDateTime getTimestamp() { return timestamp; }

    // Property methods for JavaFX binding
    public StringProperty nomeInterlocutoreProperty() { return nomeInterlocutore; }
    public StringProperty ultimoMessaggioProperty() { return ultimoMessaggio; }
    public StringProperty timestampFormattedProperty() { return timestampFormatted; }
    
    // Metodi deprecati (per compatibilità)
    public String getVenditoreNome() { return nomeInterlocutore.get(); }
    public String getAnnuncioTitolo() { return "Chat"; }
}