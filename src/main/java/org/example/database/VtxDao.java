package org.example.database;

import org.example.model.VtxBand;
import org.example.model.VtxChannel;
import org.example.model.VtxConfig;

import java.util.List;

/**
 * Интерфейс доступа к данным(DAO)
 * Определяет набор crud-методов для работы с базой данных частот
 */
public interface VtxDao {

    //операции с сетками
    List<VtxBand> getAllBands();
    VtxBand getBandByNumber(int bandNumber);
    VtxBand getBandByLetter(char letter);
    void saveBand(VtxBand band);
    void deleteBand(int idBands);

    //операции с каналами
    List<VtxChannel> getChannelsByBandId(int bandId);
    VtxChannel getChannelByBandAndNumber(int bandId, int channelNumber);
    void updateChannelFrequency(int idChannels, int frequencyMhz);
    void updateChannelFrequency(int bandId, int channelNumber, int frequencyMhz);
    void saveChannels(List<VtxChannel> channels);
    void saveChannel(VtxChannel channel);
    void deleteChannel(int idChannel);
    void deleteAllChannelsForBand(int bandId);
    void replaceChannelsForBand(int bandId, List<VtxChannel> newChannels);

    //работа с конфигурацией
    List<VtxConfig> getConfigByBandId(int bandId);
    VtxConfig getConfigByBandAndLevel(int bandId, int levelIndex);
    void updateConfigPower(int idConfig, int valueMw, String label);
    void updatePitmode(int bandId, boolean pitmode);
    void saveConfig(VtxConfig config);
    void deleteConfig(int idConfig);
    void deleteAllConfigsForBand(int bandId);
    void replaceConfigsForBand(int bandId, List<VtxConfig> newConfigs);
}