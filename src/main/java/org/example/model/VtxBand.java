package org.example.model;

public class VtxBand {
    // Primary Key в таблице vtx_bands
    private int idBands;

    // Номер сетки
    private int bandNumber;

    // Полное имя сетки частот
    private String bandName;

    // Однобуквенное обозначение сетки
    private char bandLetter;

    public VtxBand() {
    }

    //Конструктор со всеми параметрами для чтения из БД
    public VtxBand(int idBands, int bandNumber, String bandName, char bandLetter) {
        this.idBands = idBands;
        setBandNumber(bandNumber);
        this.bandName = bandName;
        this.bandLetter = bandLetter;
    }

    // Конструктор без айди для создания новой записи
    public VtxBand(int bandNumber, String bandName, char bandLetter) {
        setBandNumber(bandNumber);
        this.bandName = bandName;
        this.bandLetter = bandLetter;
    }

    public int getIdBands() {
        return idBands;
    }

    public void setIdBands(int idBands) {
        this.idBands = idBands;
    }

    public int getBandNumber() {
        return bandNumber;
    }

    public void setBandNumber(int bandNumber) {
        if (bandNumber > 0) {
            this.bandNumber = bandNumber;
        } else {
            throw new IllegalArgumentException("Номер сетки должен быть положительным числом");
        }
    }

    public String getBandName() {
        return bandName;
    }

    public void setBandName(String bandName) {
        if (bandName != null && !bandName.trim().isEmpty()) {
            this.bandName = bandName;
        } else {
            throw new IllegalArgumentException("Имя сетки не может быть пустым");
        }
    }

    public char getBandLetter() {
        return bandLetter;
    }

    public void setBandLetter(char bandLetter) {
        char upper = Character.toUpperCase(bandLetter);
        if (upper >= 'A' && upper <= 'Z') {
            this.bandLetter = upper;
        } else {
            throw new IllegalArgumentException("Символьный маркер сетки должен быть буквой от A до Z");
        }
    }

    @Override
    public String toString() {
        return "VtxBand{" +
                "idBands=" + idBands +
                ", bandNumber=" + bandNumber +
                ", bandName='" + bandName + '\'' +
                ", bandLetter='" + bandLetter + '\'' +
                '}';
    }
}
