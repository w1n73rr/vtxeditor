package org.example.model;

public class VtxChannel {
    //Primary Key в таблице vtx_channels
    private int idChannels;

    //Foreign Key, указывающий на родительскую сетку vtx_bands
    private int bandId;

    // Номер канала внутри сетки
    private int channelNumber;

    // Физическая частота(Мгц)
    private int frequencyMhz;

    public VtxChannel() {
    }

    // Конструктор со всеми параметрами для чтения из БД
    public VtxChannel(int idChannels, int bandId, int channelNumber, int frequencyMhz) {
        this.idChannels = idChannels;
        this.bandId = bandId;
        setChannelNumber(channelNumber);
        setFrequencyMhz(frequencyMhz);
    }

    // Конструктор без id для создания нового канала
    public VtxChannel(int bandId, int channelNumber, int frequencyMhz) {
        this.bandId = bandId;
        setChannelNumber(channelNumber);
        setFrequencyMhz(frequencyMhz);
    }

    public int getIdChannels() {
        return idChannels;
    }

    public void setIdChannels(int idChannels) {
        this.idChannels = idChannels;
    }

    public int getBandId() {
        return bandId;
    }

    public void setBandId(int bandId) {
        this.bandId = bandId;
    }

    public int getChannelNumber() {
        return channelNumber;
    }

    public void setChannelNumber(int channelNumber) {
        //номер канала строго положительный
        if (channelNumber > 0) {
            this.channelNumber = channelNumber;
        } else {
            throw new IllegalArgumentException("Номер канала должен быть положительным числом");
        }
    }

    public int getFrequencyMhz() {
        return frequencyMhz;
    }

    public void setFrequencyMhz(int frequencyMhz) {
        if (frequencyMhz >= 0 && frequencyMhz <= 12000) {
            this.frequencyMhz = frequencyMhz;
        } else {
            throw new IllegalArgumentException("Частота должна быть в диапазоне от 0 до 12000 МГц");
        }
    }

    @Override
    public String toString() {
        return "VtxChannel{" +
                "idChannels=" + idChannels +
                ", bandId=" + bandId +
                ", channelNumber=" + channelNumber +
                ", frequencyMhz=" + frequencyMhz +
                '}';
    }
}
