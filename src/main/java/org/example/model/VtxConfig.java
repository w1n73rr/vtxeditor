package org.example.model;

public class VtxConfig {
    //Primary Key в таблице vtx_config
    private int idConfig;

    //FK, привязывающий конфигурацию к сетке vtx_bands
    private int bandId;

    // Индекс уровня мощности
    private int levelIndex;

    // Символьное обозначение мощности "Min"/"Max"
    private String label;

    // Физическая мощность передатчика(мВт)
    private int valueMw;

    // Флаг включения передатчика
    private boolean pitMode;

    private int totalBands;
    private int totalChannels;

    public VtxConfig() {
        this.totalBands = 8;
        this.totalChannels = 8;
    }

    // Конструктор со всеми параметрами для чтения из БД
    public VtxConfig(int idConfig, int bandId, int levelIndex, String label, int valueMw,
                     boolean pitMode, int totalBands, int totalChannels) {
        this.idConfig = idConfig;
        this.bandId = bandId;
        setLevelIndex(levelIndex);
        setLabel(label);
        setValueMw(valueMw);
        this.pitMode = pitMode;
        this.totalBands = totalBands > 0 ? totalBands : 8;
        this.totalChannels = totalChannels > 0 ? totalChannels : 8;
    }

    public VtxConfig(int idConfig, int bandId, int levelIndex, String label, int valueMw, boolean pitMode) {
        this(idConfig, bandId, levelIndex, label, valueMw, pitMode, 8, 8);
    }

    // Конструктор без id для создания новой конфигурации мощности
    public VtxConfig(int bandId, int levelIndex, String label, int valueMw, boolean pitMode) {
        this.bandId = bandId;
        setLevelIndex(levelIndex);
        setLabel(label);
        setValueMw(valueMw);
        this.pitMode = pitMode;
        this.totalBands = 8;
        this.totalChannels = 8;
    }

    public int getIdConfig() {
        return idConfig;
    }

    public void setIdConfig(int idConfig) {
        this.idConfig = idConfig;
    }

    public int getBandId() {
        return bandId;
    }

    public void setBandId(int bandId) {
        this.bandId = bandId;
    }

    public int getLevelIndex() {
        return levelIndex;
    }

    public void setLevelIndex(int levelIndex) {
        if (levelIndex > 0) {
            this.levelIndex = levelIndex;
        } else {
            throw new IllegalArgumentException("Индекс уровня мощности должен быть положительным числом");
        }
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        if (label != null && !label.trim().isEmpty()) {
            this.label = label;
        } else {
            throw new IllegalArgumentException("Обозначение мощности не может быть пустым");
        }
    }

    public int getValueMw() {
        return valueMw;
    }

    public void setValueMw(int valueMw) {
        if (valueMw >= 0 && valueMw <= 2000) {
            this.valueMw = valueMw;
        } else {
            throw new IllegalArgumentException("Мощность должна быть в диапазоне от 0 до 2000 мВт");
        }
    }

    public boolean isPitMode() {
        return pitMode;
    }

    public void setPitMode(boolean pitMode) {
        this.pitMode = pitMode;
    }

    public int getTotalBands() {
        return totalBands;
    }

    public void setTotalBands(int totalBands) {
        this.totalBands = totalBands > 0 ? totalBands : 8;
    }

    public int getTotalChannels() {
        return totalChannels;
    }

    public void setTotalChannels(int totalChannels) {
        this.totalChannels = totalChannels > 0 ? totalChannels : 8;
    }

    @Override
    public String toString() {
        return "VtxConfig{" +
                "idConfig=" + idConfig +
                ", bandId=" + bandId +
                ", levelIndex=" + levelIndex +
                ", label='" + label + '\'' +
                ", valueMw=" + valueMw +
                ", pitMode=" + pitMode +
                ", totalBands=" + totalBands +
                ", totalChannels=" + totalChannels +
                '}';
    }
}