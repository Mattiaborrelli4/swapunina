package application.messagistica;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ChatSummary {
    private final int annuncioId;
    private final StringProperty annuncioTitolo;
    private final StringProperty venditoreNome;
    private final StringProperty ultimoMessaggio;
    private final LocalDateTime timestamp;
    private final StringProperty timestampFormatted;

    public ChatSummary(int annuncioId, String annuncioTitolo, String venditoreNome, 
                      String ultimoMessaggio, LocalDateTime timestamp) {
        this.annuncioId = annuncioId;
        this.annuncioTitolo = new SimpleStringProperty(annuncioTitolo);
        this.venditoreNome = new SimpleStringProperty(venditoreNome);
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
    public int getAnnuncioId() { return annuncioId; }
    public String getAnnuncioTitolo() { return annuncioTitolo.get(); }
    public String getVenditoreNome() { return venditoreNome.get(); }
    public String getUltimoMessaggio() { return ultimoMessaggio.get(); }
    public LocalDateTime getTimestamp() { return timestamp; }

    // Property methods for JavaFX binding
    public StringProperty annuncioTitoloProperty() { return annuncioTitolo; }
    public StringProperty venditoreNomeProperty() { return venditoreNome; }
    public StringProperty ultimoMessaggioProperty() { return ultimoMessaggio; }
    public StringProperty timestampFormattedProperty() { return timestampFormatted; }
}