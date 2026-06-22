package org.example.database;

import org.example.model.VtxBand;
import org.example.model.VtxChannel;
import org.example.model.VtxConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Класс реализации интерфейса VtxDao для встроенной СУБД SQLite.
 */
public class SqliteVtxDao implements VtxDao {

    @Override
    public List<VtxBand> getAllBands() {
        List<VtxBand> bands = new ArrayList<>();
        String sql = "SELECT id_bands, band_number, band_name, band_letter FROM vtx_bands ORDER BY band_number ASC;";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                // Извлекаем данные из строк таблицы БД и преобразуем в объекты Java
                int id = rs.getInt("id_bands");
                int number = rs.getInt("band_number");
                String name = rs.getString("band_name");
                // Превращаем String из SQLite в тип char для модели VtxBand
                char letter = rs.getString("band_letter").charAt(0);

                bands.add(new VtxBand(id, number, name, letter));
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при чтении списка сеток: " + e.getMessage());
        }
        return bands;
    }

    @Override
    public VtxBand getBandByNumber(int bandNumber) {
        String sql = "SELECT id_bands, band_number, band_name, band_letter FROM vtx_bands WHERE band_number = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, bandNumber); // Подставляем значение вместо знака вопроса
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new VtxBand(
                            rs.getInt("id_bands"),
                            rs.getInt("band_number"),
                            rs.getString("band_name"),
                            rs.getString("band_letter").charAt(0)
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при поиске сетки по номеру: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void saveBand(VtxBand band) {
        String sql;
        if (band.getIdBands() > 0) {
            // Если ID у объекта уже есть, значит он существует в БД, поэтому обновляем его
            sql = "UPDATE vtx_bands SET band_number = ?, band_name = ?, band_letter = ? WHERE id_bands = ?;";
        } else {
            // Если ID нет, значит это новая сетка, поэтому добавляем ее
            sql = "INSERT INTO vtx_bands (band_number, band_name, band_letter) VALUES (?, ?, ?);";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, band.getBandNumber());
            pstmt.setString(2, band.getBandName());
            pstmt.setString(3, String.valueOf(band.getBandLetter()));

            if (band.getIdBands() > 0) {
                pstmt.setInt(4, band.getIdBands());
                pstmt.executeUpdate();
            } else {
                pstmt.executeUpdate();
                // Получаем сгенерированный ID новой сетки и записываем его обратно в объект модели
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        band.setIdBands(rs.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при сохранении сетки частот: " + e.getMessage());
        }
    }

    @Override
    public void deleteBand(int idBands) {
        String sql = "DELETE FROM vtx_bands WHERE id_bands = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idBands);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка при удалении сетки частот: " + e.getMessage());
        }
    }

    @Override
    public List<VtxChannel> getChannelsByBandId(int bandId) {
        List<VtxChannel> channels = new ArrayList<>();
        String sql = "SELECT id_channels, channel_number, frequency_mhz, band_id FROM vtx_channels WHERE band_id = ? ORDER BY channel_number ASC;";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, bandId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    channels.add(new VtxChannel(
                            rs.getInt("id_channels"),
                            rs.getInt("band_id"),
                            rs.getInt("channel_number"),
                            rs.getInt("frequency_mhz")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при чтении каналов сетки: " + e.getMessage());
        }
        return channels;
    }

    @Override
    public void updateChannelFrequency(int idChannels, int frequencyMhz) {
        String sql = "UPDATE vtx_channels SET frequency_mhz = ? WHERE id_channels = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, frequencyMhz);
            pstmt.setInt(2, idChannels);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка при обновлении частоты канала: " + e.getMessage());
        }
    }

    @Override
    public void saveChannels(List<VtxChannel> channels) {
        String insertSql = "INSERT INTO vtx_channels (channel_number, frequency_mhz, band_id) VALUES (?, ?, ?);";
        String updateSql = "UPDATE vtx_channels SET channel_number = ?, frequency_mhz = ?, band_id = ? WHERE id_channels = ?;";

        try (Connection conn = DatabaseManager.getConnection()) {
            // Отключаем авто-коммит для запуска транзакции.
            conn.setAutoCommit(false);

            try (PreparedStatement insertPstmt = conn.prepareStatement(insertSql);
                 PreparedStatement updatePstmt = conn.prepareStatement(updateSql)) {

                for (VtxChannel channel : channels) {
                    if (channel.getIdChannels() > 0) {
                        // Канал уже существует — готовим обновление
                        updatePstmt.setInt(1, channel.getChannelNumber());
                        updatePstmt.setInt(2, channel.getFrequencyMhz());
                        updatePstmt.setInt(3, channel.getBandId());
                        updatePstmt.setInt(4, channel.getIdChannels());
                        updatePstmt.addBatch(); // Добавляем запрос в пачку
                    } else {
                        // Новый канал — готовим вставку
                        insertPstmt.setInt(1, channel.getChannelNumber());
                        insertPstmt.setInt(2, channel.getFrequencyMhz());
                        insertPstmt.setInt(3, channel.getBandId());
                        insertPstmt.addBatch();
                    }
                }

                updatePstmt.executeBatch();
                insertPstmt.executeBatch();

                conn.commit(); // Применяем изменения транзакции
            } catch (SQLException e) {
                conn.rollback(); // В случае ошибки откатываем ВСЕ изменения назад
                throw e;
            } finally {
                conn.setAutoCommit(true); // Возвращаем дефолтный режим соединения
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при транзакционном сохранении каналов: " + e.getMessage());
        }
    }

    @Override
    public List<VtxConfig> getConfigByBandId(int bandId) {
        List<VtxConfig> configs = new ArrayList<>();
        String sql = "SELECT id_config, level_index, label, value_mw, pitmode, band_id FROM vtx_config WHERE band_id = ? ORDER BY level_index ASC;";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, bandId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    configs.add(new VtxConfig(
                            rs.getInt("id_config"),
                            rs.getInt("band_id"),
                            rs.getInt("level_index"),
                            rs.getString("label"),
                            rs.getInt("value_mw"),
                            rs.getBoolean("pitmode") // Автоматически преобразует 0/1 из SQLite в boolean в Java
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при чтении конфигураций мощности: " + e.getMessage());
        }
        return configs;
    }

    @Override
    public void updateConfigPower(int idConfig, int valueMw, String label) {
        String sql = "UPDATE vtx_config SET value_mw = ?, label = ? WHERE id_config = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, valueMw);
            pstmt.setString(2, label);
            pstmt.setInt(3, idConfig);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка при обновлении мощности конфигурации: " + e.getMessage());
        }
    }

    @Override
    public void updatePitmode(int bandId, boolean pitmode) {
        String sql = "UPDATE vtx_config SET pitmode = ? WHERE band_id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

             pstmt.setBoolean(1, pitmode);
             pstmt.setInt(2, bandId);
             pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка при изменении режима Pitmode: " + e.getMessage());
        }
    }
}