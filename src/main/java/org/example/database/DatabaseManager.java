package org.example.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Класс управления базой данных
 * Отвечает за:
 * -Установление соединения с локальным файлом БД SQLite.
 * -Автоматическое создание таблиц при первом запуске приложения.
 * -Поддержку механизма миграций схемы данных
 * -Первичное наполнение базы стандартными сетками частот.
 */
public class DatabaseManager {

    // имя файла базы данных, которая создается автоматом
    private static final String DB_FILE = "vtxeditor.db";

    // url подключения для драйвера
    private static final String CONNECTION_URL = "jdbc:sqlite:" + DB_FILE;

    // статический блок инициализации для принудительной загрузки драйвера в память jvm
    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Драйвер SQLite JDBC не найден в classpath: " + e.getMessage());
        }
    }

    /**
     * Метод получения активного подключения к базе данных.
     * Используется во всем приложении для выполнения SQL-запросов.
     * @return Connection объект подключения к SQLite
     * @throws SQLException в случае ошибок подключения
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(CONNECTION_URL);
    }

    /**
     * Главный инициализирующий метод.
     * Проверяет текущую версию схемы БД и запускает цепочку обновлений/миграции.
     */
    public void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // активируем поддержку внешних ключей для каждой сессии соединения
            stmt.execute("PRAGMA foreign_keys = ON;");

            // считываем текущую версию схемы базы данных с помощью системной переменной user_version
            int currentVersion = 0;
            try (ResultSet rs = stmt.executeQuery("PRAGMA user_version;")) {
                if (rs.next()) {
                    currentVersion = rs.getInt(1);
                }
            }

            System.out.println("Текущая версия схемы данных: " + currentVersion);


            // Если версия равна 0, накатываем версию 1 (создание таблиц и сидирование данных).
            if (currentVersion < 1) {
                runMigrationV1(conn);
                currentVersion = 1;
            }

            System.out.println("Текущая версия схемы: " + currentVersion);

            // Имитация будущей миграции до версии 2
            // Если в будущем нам потребуется добавить новые колонки
            // мы напишем метод runMigrationV2 и раскомментируем этот блок
            /*
            if (currentVersion < 2) {
                runMigrationV2(conn);
                currentVersion = 2;
            }
            */

            System.out.println("База данных успешно инициализирована и готова к работе.");

        } catch (SQLException e) {
            System.err.println("Ошибка при инициализации базы данных: " + e.getMessage());
        }
    }

    /**
     * Миграция Версии 1: Создание таблиц и первичное заполнение данными частот.
     */
    private void runMigrationV1(Connection conn) throws SQLException {
        System.out.println("[БД] Запуск миграции версии 1");

        try (Statement stmt = conn.createStatement()) {
            // Создаем родительскую таблицу vtx_bands (Сетки частот)
            String createBandsTable = "CREATE TABLE IF NOT EXISTS vtx_bands (" +
                    "id_bands INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "band_number INTEGER NOT NULL," +
                    "band_name VARCHAR(50) NOT NULL," +
                    "band_letter CHAR(1) NOT NULL" +
                    ");";
            stmt.execute(createBandsTable);

            // Создаем дочернюю таблицу vtx_channels (Каналы частот)
            String createChannelsTable = "CREATE TABLE IF NOT EXISTS vtx_channels (" +
                    "id_channels INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "channel_number INTEGER NOT NULL," +
                    "frequency_mhz INTEGER NOT NULL," +
                    "band_id INTEGER NOT NULL," +
                    "FOREIGN KEY (band_id) REFERENCES vtx_bands(id_bands) ON DELETE CASCADE" +
                    ");";
            stmt.execute(createChannelsTable);

            // Создаем дочернюю таблицу vtx_config
            String createConfigTable = "CREATE TABLE IF NOT EXISTS vtx_config (" +
                    "id_config INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "level_index INTEGER NOT NULL," +
                    "label VARCHAR(10) NOT NULL," +
                    "value_mw INTEGER NOT NULL," +
                    "pitmode BOOLEAN NOT NULL," +
                    "band_id INTEGER NOT NULL," +
                    "FOREIGN KEY (band_id) REFERENCES vtx_bands(id_bands) ON DELETE CASCADE" +
                    ");";
            stmt.execute(createConfigTable);

            // Первичное заполнение базы данных частотами
            seedInitialData(stmt);

            // Записываем новую версию схемы данных в заголовок SQLite
            stmt.execute("PRAGMA user_version = 1;");
            System.out.println("[БД] Миграция версии 1 успешно завершена.");
        }
    }

    /**
     * Наполнение базы данных стартовыми сетками
     */
    private void seedInitialData(Statement stmt) throws SQLException {
        System.out.println("[БД] Наполнение базы данных сетками");

        int[][] frequencies = {
                {7300, 7579, 7859, 8138, 8417, 8697, 8976, 9256}, // BAND_A (1)
                {7335, 7614, 7894, 8173, 8452, 8732, 9011, 9290}, // BAND_B (2)
                {7370, 7649, 7929, 8208, 8487, 8767, 9046, 9325}, // BAND_E (3)
                {7405, 7684, 7963, 8243, 8522, 8802, 9081, 9360}, // BAND_F (4)
                {7440, 7719, 7998, 8278, 8557, 8837, 9116, 9395}, // BAND_R (5)
                {7475, 7754, 8033, 8313, 8592, 8871, 9151, 9430}, // BAND_P (6)
                {7510, 7789, 8068, 8348, 8627, 8906, 9186, 9465}, // BAND_H (7)
                {7544, 7824, 8103, 8383, 8662, 8941, 9221, 9500}  // BAND_U (8)
        };

        String[] bandNames = {"BAND_A", "BAND_B", "BAND_E", "BAND_F", "BAND_R", "BAND_P", "BAND_H", "BAND_U"};
        char[] bandLetters = {'A', 'B', 'E', 'F', 'R', 'P', 'H', 'U'};

        // Проходим в цикле по всем сеткам
        for (int i = 0; i < 8; i++) {
            // Вставляем сетку частот (vtx_bands)
            String insertBandSql = String.format(
                    "INSERT INTO vtx_bands (band_number, band_name, band_letter) VALUES (%d, '%s', '%c');",
                    (i + 1), bandNames[i], bandLetters[i]
            );
            stmt.execute(insertBandSql);

            // Получаем сгенерированный ID вставленной сетки для связывания дочерних таблиц
            int bandId;
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    bandId = rs.getInt(1);
                } else {
                    throw new SQLException("Не удалось получить сгенерированный ID сетки " + bandNames[i]);
                }
            }

            // Вставляем 8 каналов для этой сетки (vtx_channels)
            for (int ch = 0; ch < 8; ch++) {
                String insertChannelSql = String.format(
                        "INSERT INTO vtx_channels (channel_number, frequency_mhz, band_id) VALUES (%d, %d, %d);",
                        (ch + 1), frequencies[i][ch], bandId
                );
                stmt.execute(insertChannelSql);
            }

            // Вставляем настройки мощности для этой сетки (vtx_config)
            // уровень 1: MIN (20 мВт), Pitmode выключен по умолчанию (0)
            String insertConfigMin = String.format(
                    "INSERT INTO vtx_config (level_index, label, value_mw, pitmode, band_id) VALUES (1, 'MIN', 20, 0, %d);",
                    bandId
            );
            // Уровень 2: MAX (1995 мВт), Pitmode выключен (0)
            String insertConfigMax = String.format(
                    "INSERT INTO vtx_config (level_index, label, value_mw, pitmode, band_id) VALUES (2, 'MAX', 1995, 0, %d);",
                    bandId
            );

            stmt.execute(insertConfigMin);
            stmt.execute(insertConfigMax);
        }
    }
}