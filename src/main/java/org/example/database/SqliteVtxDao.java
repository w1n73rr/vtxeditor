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
                VtxBand band = new VtxBand();
                band.setIdBands(rs.getInt("id_bands"));
                band.setBandNumber(rs.getInt("band_number"));
                band.setBandName(rs.getString("band_name"));

                String letterStr = rs.getString("band_letter");
                if (letterStr != null && !letterStr.isEmpty()) {
                    band.setBandLetter(letterStr.charAt(0));
                }
                bands.add(band);
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
    public void updateChannelFrequency(int bandId, int channelNumber, int frequencyMhz) {
        String sql = "UPDATE vtx_channels SET frequency_mhz = ? WHERE band_id = ? AND channel_number = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, frequencyMhz);
            pstmt.setInt(2, bandId);
            pstmt.setInt(3, channelNumber);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка при обновлении частоты по сетке: " + e.getMessage());
        }
    }

    @Override
    public void saveChannel(VtxChannel channel) {
        String sql;
        if (channel.getIdChannels() > 0) {
            sql = "UPDATE vtx_channels SET channel_number = ?, frequency_mhz = ?, band_id = ? WHERE id_channels = ?;";
        } else {
            sql = "INSERT INTO vtx_channels (channel_number, frequency_mhz, band_id) VALUES (?, ?, ?);";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, channel.getChannelNumber());
            pstmt.setInt(2, channel.getFrequencyMhz());
            pstmt.setInt(3, channel.getBandId());

            if (channel.getIdChannels() > 0) {
                pstmt.setInt(4, channel.getIdChannels());
                pstmt.executeUpdate();
            } else {
                pstmt.executeUpdate();
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        channel.setIdChannels(rs.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при сохранении канала: " + e.getMessage());
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
    public void deleteChannel(int idChannels) {
        String sql = "DELETE FROM vtx_channels WHERE id_channels = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idChannels);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка при удалении канала: " + e.getMessage());
        }
    }


    //configs
    @Override
    public List<VtxConfig> getConfigByBandId(int bandId) {
        List<VtxConfig> configs = new ArrayList<>();
        // Обновленный SQL с новыми полями
        String sql = "SELECT id_config, level_index, label, value_mw, pitmode, band_id, " +
                "total_bands, total_channels FROM vtx_config WHERE band_id = ? ORDER BY level_index ASC;";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, bandId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    VtxConfig config = new VtxConfig(
                            rs.getInt("id_config"),
                            rs.getInt("band_id"),
                            rs.getInt("level_index"),
                            rs.getString("label"),
                            rs.getInt("value_mw"),
                            rs.getBoolean("pitmode"),
                            rs.getInt("total_bands"),
                            rs.getInt("total_channels")
                    );
                    configs.add(config);
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

    @Override
    public void saveConfig(VtxConfig config) {
        String sql;
        if (config.getIdConfig() > 0) {
            sql = "UPDATE vtx_config SET level_index = ?, label = ?, value_mw = ?, pitmode = ?, " +
                    "band_id = ?, total_bands = ?, total_channels = ? WHERE id_config = ?;";
        } else {
            sql = "INSERT INTO vtx_config (level_index, label, value_mw, pitmode, band_id, " +
                    "total_bands, total_channels) VALUES (?, ?, ?, ?, ?, ?, ?);";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setInt(1, config.getLevelIndex());
            pstmt.setString(2, config.getLabel());
            pstmt.setInt(3, config.getValueMw());
            pstmt.setBoolean(4, config.isPitMode());
            pstmt.setInt(5, config.getBandId());
            pstmt.setInt(6, config.getTotalBands());
            pstmt.setInt(7, config.getTotalChannels());

            if (config.getIdConfig() > 0) {
                pstmt.setInt(8, config.getIdConfig());
                pstmt.executeUpdate();
            } else {
                pstmt.executeUpdate();
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        config.setIdConfig(rs.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при сохранении конфигурации: " + e.getMessage());
        }
    }

    @Override
    public void deleteConfig(int idConfig) {
        String sql = "DELETE FROM vtx_config WHERE id_config = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, idConfig);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Ошибка при удалении конфигурации: " + e.getMessage());
        }
    }

    @Override
    public VtxBand getBandByLetter(char letter) {
        String sql = "SELECT id_bands, band_number, band_name, band_letter FROM vtx_bands WHERE band_letter = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, String.valueOf(letter));
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
            System.err.println("Ошибка при поиске сетки по букве: " + e.getMessage());
        }
        return null;
    }

    @Override
    public VtxChannel getChannelByBandAndNumber(int bandId, int channelNumber) {
        String sql = "SELECT id_channels, band_id, channel_number, frequency_mhz FROM vtx_channels WHERE band_id = ? AND channel_number = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, bandId);
            pstmt.setInt(2, channelNumber);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new VtxChannel(
                            rs.getInt("id_channels"),
                            rs.getInt("band_id"),
                            rs.getInt("channel_number"),
                            rs.getInt("frequency_mhz")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при поиске канала: " + e.getMessage());
        }
        return null;
    }

    @Override
    public VtxConfig getConfigByBandAndLevel(int bandId, int levelIndex) {
        String sql = "SELECT id_config, level_index, label, value_mw, pitmode, band_id FROM vtx_config WHERE band_id = ? AND level_index = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, bandId);
            pstmt.setInt(2, levelIndex);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new VtxConfig(
                            rs.getInt("id_config"),
                            rs.getInt("band_id"),
                            rs.getInt("level_index"),
                            rs.getString("label"),
                            rs.getInt("value_mw"),
                            rs.getBoolean("pitmode")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Ошибка при поиске конфигурации: " + e.getMessage());
        }
        return null;
    }

    @Override
    public void deleteAllChannelsForBand(int bandId) {
        String sql = "DELETE FROM vtx_channels WHERE band_id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, bandId);
            int deleted = pstmt.executeUpdate();
            System.out.println("Удалено каналов для bandId=" + bandId + ": " + deleted);

        } catch (SQLException e) {
            System.err.println("Ошибка при удалении всех каналов бэнда: " + e.getMessage());
        }
    }

    @Override
    public void deleteAllConfigsForBand(int bandId) {
        String sql = "DELETE FROM vtx_config WHERE band_id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, bandId);
            int deleted = pstmt.executeUpdate();
            System.out.println("Удалено конфигов для bandId=" + bandId + ": " + deleted);

        } catch (SQLException e) {
            System.err.println("Ошибка при удалении всех конфигов бэнда: " + e.getMessage());
        }
    }

    @Override
    public void replaceChannelsForBand(int bandId, List<VtxChannel> newChannels) {
        if (newChannels == null) {
            newChannels = new ArrayList<>();
        }

        String deleteSql = "DELETE FROM vtx_channels WHERE band_id = ?;";
        String insertSql = "INSERT INTO vtx_channels (channel_number, frequency_mhz, band_id) VALUES (?, ?, ?);";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
                 PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

                //Удаляем старые каналы
                deleteStmt.setInt(1, bandId);
                int deleted = deleteStmt.executeUpdate();
                System.out.println("Удалено старых каналов для bandId=" + bandId + ": " + deleted);

                //Вставляем новые каналы
                for (VtxChannel channel : newChannels) {
                    insertStmt.setInt(1, channel.getChannelNumber());
                    insertStmt.setInt(2, channel.getFrequencyMhz());
                    insertStmt.setInt(3, bandId);
                    insertStmt.addBatch();
                }

                int[] inserted = insertStmt.executeBatch();
                System.out.println("Вставлено новых каналов для bandId=" + bandId + ": " + inserted.length);

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Ошибка при замене каналов: " + e.getMessage());
        }
    }

    @Override
    public void replaceConfigsForBand(int bandId, List<VtxConfig> newConfigs) {
        if (newConfigs == null) {
            newConfigs = new ArrayList<>();
        }

        String deleteSql = "DELETE FROM vtx_config WHERE band_id = ?;";
        String insertSql = "INSERT INTO vtx_config (level_index, label, value_mw, pitmode, band_id, total_bands, total_channels) VALUES (?, ?, ?, ?, ?, ?, ?);";

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql);
                 PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {

                deleteStmt.setInt(1, bandId);
                int deleted = deleteStmt.executeUpdate();
                System.out.println("Удалено старых конфигов для bandId=" + bandId + ": " + deleted);

                for (VtxConfig config : newConfigs) {
                    insertStmt.setInt(1, config.getLevelIndex());
                    insertStmt.setString(2, config.getLabel());
                    insertStmt.setInt(3, config.getValueMw());
                    insertStmt.setBoolean(4, config.isPitMode());
                    insertStmt.setInt(5, bandId);
                    insertStmt.setInt(6, config.getTotalBands());
                    insertStmt.setInt(7, config.getTotalChannels());
                    insertStmt.addBatch();
                }

                int[] inserted = insertStmt.executeBatch();
                System.out.println("Вставлено новых конфигов для bandId=" + bandId + ": " + inserted.length);

                conn.commit();

            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            System.err.println("Ошибка при замене конфигов: " + e.getMessage());
        }
    }

    @Override
    public void updateTotalBands(int bandId, int totalBands) {
        if (totalBands < 1) {
            System.err.println("Количество бэндов должно быть больше 0");
            return;
        }

        String sql = "UPDATE vtx_config SET total_bands = ? WHERE band_id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, totalBands);
            pstmt.setInt(2, bandId);
            int updated = pstmt.executeUpdate();
            System.out.println("БД - Обновлено total_bands для bandId=" + bandId +
                    ": " + totalBands + " (обновлено записей: " + updated + ")");

        } catch (SQLException e) {
            System.err.println("БД - Ошибка при обновлении total_bands: " + e.getMessage());
        }
    }

    @Override
    public void updateTotalChannels(int bandId, int totalChannels) {
        if (totalChannels < 1) {
            System.err.println("БД - Количество каналов должно быть больше 0");
            return;
        }

        String sql = "UPDATE vtx_config SET total_channels = ? WHERE band_id = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, totalChannels);
            pstmt.setInt(2, bandId);
            int updated = pstmt.executeUpdate();
            System.out.println("БД - Обновлено total_channels для bandId=" + bandId +
                    ": " + totalChannels + " (обновлено записей: " + updated + ")");

        } catch (SQLException e) {
            System.err.println("БД - Ошибка при обновлении total_channels: " + e.getMessage());
        }
    }

    @Override
    public int getTotalBands(int bandId) {
        String sql = "SELECT total_bands FROM vtx_config WHERE band_id = ? LIMIT 1;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, bandId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int totalBands = rs.getInt("total_bands");
                    // Если значение null, возвращаем 8 по умолчанию
                    if (rs.wasNull()) {
                        return 8;
                    }
                    return totalBands;
                }
            }
        } catch (SQLException e) {
            System.err.println("БД - Ошибка при получении total_bands: " + e.getMessage());
        }
        return 8; // значение по умолчанию
    }

    @Override
    public int getTotalChannels(int bandId) {
        String sql = "SELECT total_channels FROM vtx_config WHERE band_id = ? LIMIT 1;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, bandId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    int totalChannels = rs.getInt("total_channels");
                    if (rs.wasNull()) {
                        return 8;
                    }
                    return totalChannels;
                }
            }
        } catch (SQLException e) {
            System.err.println("БД - Ошибка при получении total_channels: " + e.getMessage());
        }
        return 8; // значение по умолчанию
    }
}