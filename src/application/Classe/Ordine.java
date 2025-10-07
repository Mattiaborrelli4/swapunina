package application.Classe;

import java.util.Date;

import application.Enum.ModalitaConsegna;

public class Ordine {
    private int id;
    private int acquirenteId;
    private int venditoreId;
    private int annuncioId;
    private int quantita;
    private double prezzo;
    private StatoOrdine stato;
    private ModalitaConsegna consegna;
    private String indirizzoSpedizione;
    private String trackingNumber;
    private Date dataCreazione;

    // Enum per stato ordine
    public enum StatoOrdine {
        IN_ATTESA, PAGATO, SPEDITO, CONSEGNATO, ANNULLATO
    }
}
