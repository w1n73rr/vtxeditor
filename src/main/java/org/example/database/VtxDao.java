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

    /**
     * Получить полный список всех сеток частот из БД
     * @return список объектов VtxBand
     */
    List<VtxBand> getAllBands();

    /**
     * Найти конкретную сетку частот по ее уникальному номеру.
     * @param bandNumber номер сетки (например, 1 для BAND_A)
     * @return объект VtxBand или null, если не найден
     */
    VtxBand getBandByNumber(int bandNumber);

    /**
     * Добавить новую или обновить существующую сетку частот.
     * @param band объект сетки для сохранения
     */
    void saveBand(VtxBand band);

    /**
     * Удалить сетку частот по ее id.
     * @param idBands уникальный идентификатор сетки
     */
    void deleteBand(int idBands);


    /**
     * Получить все частотные каналы, привязанные к конкретной сетке.
     * @param bandId первичный ключ сетки (id_bands)
     * @return список каналов VtxChannel
     */
    List<VtxChannel> getChannelsByBandId(int bandId);

    /**
     * Обновить физическое значение частоты для конкретного канала.
     * @param idChannels первичный ключ канала
     * @param frequencyMhz новое значение частоты в МГц
     */
    void updateChannelFrequency(int idChannels, int frequencyMhz);

    /**
     * Массовое сохранение или обновление списка каналов(синхронизация с контроллером).
     * @param channels список каналов для сохранения
     */
    void saveChannels(List<VtxChannel> channels);


    /**
     * Получить все настройки конфигураций мощности и Pitmode для конкретной сетки.
     * @param bandId первичный ключ сетки (id_bands)
     * @return список конфигураций VtxConfig
     */
    List<VtxConfig> getConfigByBandId(int bandId);

    /**
     * Обновить параметры мощности для конкретного уровня конфигурации.
     * @param idConfig первичный ключ конфигурации
     * @param valueMw новое значение мощности в мВт
     * @param label новая текстовая метка мощности (например, "MAX")
     */
    void updateConfigPower(int idConfig, int valueMw, String label);

    /**
     * Массово обновить состояние режима работы передатчика для конкретной сетки частот.
     * @param bandId первичный ключ сетки частот
     * @param pitmode статус передатчика (true - включен, false - выключен)
     */
    void updatePitmode(int bandId, boolean pitmode);
}