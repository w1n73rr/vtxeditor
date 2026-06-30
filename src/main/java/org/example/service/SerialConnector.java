package org.example.service;

import com.fazecast.jSerialComm.SerialPort;
import org.example.database.VtxDao;
import org.example.model.VtxBand;
import org.example.model.VtxChannel;
import org.example.model.VtxConfig;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Класс управления физическим обменом данными по COM-порту.
 * Обеспечивает:
 * Сканирование доступных COM-портов в ОС;
 * Установление сессии связи с полетным контроллером на скорости 115200 бод;
 * Перевод прошивки в CLI;
 * Запись/Чтение буферов последовательного порта с защитой от тайм-аутов;
 * Синхронизацию данных между БД и контроллером.
 */
public class SerialConnector {

    private SerialPort activePort;
    private InputStream inputStream;
    private OutputStream outputStream;
    private final ConfigParser parser;
    private final VtxDao dao;

    /**
     * Результат синхронизации с контроллером
     */
    public static class SyncResult {
        public boolean success;
        public String message;
        public List<VtxBand> foundBands = new ArrayList<>();
        public List<VtxChannel> foundChannels = new ArrayList<>();
        public List<VtxConfig> foundConfigs = new ArrayList<>();
        public int bandsCreated = 0;
        public int channelsUpdated = 0;
        public int configsUpdated = 0;

        @Override
        public String toString() {
            return "SyncResult{" +
                    "success=" + success +
                    ", message='" + message + '\'' +
                    ", bands=" + foundBands.size() +
                    ", channels=" + foundChannels.size() +
                    ", configs=" + foundConfigs.size() +
                    ", bandsCreated=" + bandsCreated +
                    ", channelsUpdated=" + channelsUpdated +
                    ", configsUpdated=" + configsUpdated +
                    '}';
        }
    }

    /** Результат инкрементальной записи в контроллер. */
    public static class WriteResult {
        public boolean success;
        public String error;
        public String vtxTableResponse = "";
    }

    /**
     * Конструктор инициализирует парсер команд и слой доступа к БД.
     *
     * @param dao слой доступа к данным
     * @param parser парсер команд
     */
    public SerialConnector(VtxDao dao, ConfigParser parser) {
        this.dao = dao;
        this.parser = parser;
    }

    /**
     * Поиск всех доступных физических и виртуальных COM-портов в ОС.
     * Метод jSerialComm сканирует реестр Windows или директорию /dev в Unix.
     *
     * @return список объектов последовательных портов
     */
    public List<SerialPort> getAvailablePorts() {
        SerialPort[] ports = SerialPort.getCommPorts();
        List<SerialPort> result = new ArrayList<>();
        for (SerialPort port : ports) {
            result.add(port);
        }
        return result;
    }

    /**
     * Подключение к выбранному последовательному порту.
     * Конфигурирует стандартные характеристики порта:
     * 115200 бод, 8 бит данных, 1 стоп-бит, без контроля четности.
     *
     * @param portName системное имя порта
     * @return true, если подключение успешно открыто, иначе false
     */
    public boolean connect(String portName) {
        if (activePort != null && activePort.isOpen()) {
            disconnect();
        }

        activePort = SerialPort.getCommPort(portName);

        // Установка характеристик физического уровня связи
        activePort.setBaudRate(115200);
        activePort.setNumDataBits(8);
        activePort.setNumStopBits(SerialPort.ONE_STOP_BIT);
        activePort.setParity(SerialPort.NO_PARITY);

        // Настройка таймаутов на чтение/запись
        activePort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 2000, 5000);

        if (activePort.openPort()) {
            try {
                this.inputStream = activePort.getInputStream();
                this.outputStream = activePort.getOutputStream();
                System.out.println("Порт " + portName + " успешно открыт на скорости 115200.");
                return true;
            } catch (Exception e) {
                System.err.println("Ошибка при открытии потоков порта: " + e.getMessage());
                activePort.closePort();
                activePort = null;
                return false;
            }
        } else {
            System.err.println("Не удалось открыть порт " + portName);
            return false;
        }
    }

    /**
     * Безопасное закрытие сетевого соединения и освобождение ресурсов ОС.
     */
    public void disconnect() {
        try {
            if (inputStream != null) {
                inputStream.close();
                inputStream = null;
            }
            if (outputStream != null) {
                outputStream.close();
                outputStream = null;
            }
            if (activePort != null && activePort.isOpen()) {
                activePort.closePort();
                System.out.println("Соединение с портом закрыто.");
            }
        } catch (Exception e) {
            System.err.println("Ошибка при отключении порта: " + e.getMessage());
        } finally {
            activePort = null;
        }
    }

    /**
     * Проверка, активно ли текущее соединение.
     *
     * @return true если порт открыт и готов к работе
     */
    public boolean isConnected() {
        return activePort != null && activePort.isOpen();
    }

    /**
     * Отправка текстовой команды в последовательный порт.
     * Контроллер ожидает команду, завершающуюся символами \r\n.
     *
     * @param command текст команды
     * @return true в случае успешной записи в выходной буфер
     */
    public boolean sendCommand(String command) {
        if (!isConnected()) {
            System.err.println("Не удалось отправить команду: порт не подключен");
            return false;
        }
        try {
            String fullCommand = command + "\r\n";
            byte[] bytes = fullCommand.getBytes("UTF-8");

            // Проверяем, что порт все еще открыт
            if (activePort == null || !activePort.isOpen()) {
                System.err.println("Порт закрыт перед отправкой");
                return false;
            }

            // Очищаем буфер перед отправкой
            while (activePort.bytesAvailable() > 0) {
                inputStream.read(new byte[1024]);
            }

            outputStream.write(bytes);
            outputStream.flush();
            System.out.println("Отправлена команда: " + command);
            return true;
        } catch (Exception e) {
            System.err.println("Ошибка при отправке команды: " + e.getMessage());
            return false;
        }
    }

    /**
     * Чтение накопившихся символов из входного буфера порта.
     *
     * @return строка с текстовым ответом контроллера
     */
    public String readResponse() {
        if (!isConnected()) {
            return "Устройство не подключено";
        }

        StringBuilder sb = new StringBuilder();
        try {
            // Даем устройству время на формирование ответа
            Thread.sleep(200);

            byte[] buffer = new byte[4096];
            int timeout = 0;
            int maxTimeout = 30;

            while ((activePort.bytesAvailable() > 0 || timeout < maxTimeout) && timeout < maxTimeout) {
                while (activePort.bytesAvailable() > 0) {
                    int readBytes = inputStream.read(buffer);
                    if (readBytes > 0) {
                        sb.append(new String(buffer, 0, readBytes, "UTF-8"));
                    }
                }
                Thread.sleep(100);
                timeout++;
            }

            return sb.toString();

        } catch (Exception e) {
            System.err.println("Ошибка при чтении буфера порта: " + e.getMessage());
        }
        return sb.toString();
    }

    /**
     * Автоматический перевод полетного контроллера в сервисный режим CLI.
     * Отправляет символ '#' и проверяет, содержит ли ответ приветствие Betaflight.
     *
     * @return true, если CLI режим успешно активирован
     */
    public boolean enterCliMode() {
        return enterCliWithRetry(3, 800);
    }

    /**
     * Повторный вход в CLI после перезагрузки контроллера.
     */
    public boolean enterCliWithRetry(int maxAttempts, long pauseMs) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            if (!isConnected()) {
                sleepQuiet(pauseMs);
                continue;
            }
            drainInputQuiet();
            if (!sendCommand("#")) {
                sleepQuiet(pauseMs);
                continue;
            }
            sleepQuiet(600);
            String response = readResponse();
            if (response.contains("Entering CLI Mode") || response.contains("#")) {
                System.out.println("CLI активирован (попытка " + attempt + ")");
                return true;
            }
            sleepQuiet(pauseMs);
        }
        System.err.println("Не удалось войти в CLI после " + maxAttempts + " попыток");
        return false;
    }

    /**
     * Переподключение к порту после перезагрузки контроллера.
     */
    public boolean reconnect(String portName) throws InterruptedException {
        if (isConnected()) {
            return true;
        }
        for (int attempt = 1; attempt <= 8; attempt++) {
            Thread.sleep(1000);
            if (connect(portName)) {
                System.out.println("Переподключено к " + portName + " (попытка " + attempt + ")");
                Thread.sleep(400);
                return true;
            }
        }
        return false;
    }

    /**
     * Отправка инкрементальных команд, save, ожидание reboot и чтение vtxtable для верификации.
     */
    public WriteResult writeIncrementalCommands(List<String> commands, String portName) {
        WriteResult result = new WriteResult();
        try {
            if (!enterCliWithRetry(3, 800)) {
                result.error = "Не удалось войти в CLI режим";
                return result;
            }

            for (String cmd : commands) {
                System.out.println("Отправка: " + cmd);
                if (!sendCommand(cmd)) {
                    result.error = "Ошибка при отправке: " + cmd;
                    return result;
                }
                Thread.sleep(250);
                String resp = readResponse();
                if (containsCliError(resp)) {
                    result.error = extractCliError(resp);
                    return result;
                }
            }

            System.out.println("Отправка save");
            sendCommandBestEffort("save");
            waitForControllerReboot(portName);

            if (!enterCliWithRetry(6, 1500)) {
                result.error = "Не удалось войти в CLI после перезагрузки контроллера";
                return result;
            }

            if (!sendCommand("vtxtable")) {
                result.error = "Не удалось запросить vtxtable";
                return result;
            }
            Thread.sleep(700);
            result.vtxTableResponse = readResponse();
            if (result.vtxTableResponse == null || result.vtxTableResponse.isEmpty()) {
                result.error = "Пустой ответ vtxtable после перезагрузки";
                return result;
            }
            System.out.println("Верификация vtxtable: " + result.vtxTableResponse.length() + " байт");
            result.success = true;
            return result;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result.error = "Прервано: " + e.getMessage();
            return result;
        } catch (Exception e) {
            result.error = e.getMessage();
            return result;
        }
    }

    public static boolean containsCliError(String response) {
        if (response == null || response.isEmpty()) {
            return false;
        }
        String upper = response.toUpperCase();
        return upper.contains("###ERROR") || upper.contains("##ERROR");
    }

    public static String extractCliError(String response) {
        if (response == null || response.isEmpty()) {
            return "Неизвестная ошибка контроллера";
        }
        for (String line : response.split("\\r?\\n")) {
            String trimmed = line.trim();
            if (trimmed.toUpperCase().contains("ERROR")) {
                return trimmed;
            }
        }
        return response.length() > 200 ? response.substring(0, 200) + "..." : response;
    }

    private void waitForControllerReboot(String portName) throws InterruptedException {
        // Сразу закрываем порт, так как контроллер перезагрузился
        disconnect();
        System.out.println("Ожидание перезагрузки контроллера");

        // Ждем 3 секунды перед попыткой переподключения
        Thread.sleep(3000);

        // Пытаемся переподключиться с повторными попытками
        for (int attempt = 1; attempt <= 8; attempt++) {
            System.out.println("Попытка переподключения " + attempt + "/8...");
            if (connect(portName)) {
                System.out.println("Переподключено к " + portName);
                Thread.sleep(500);
                return;
            }
            Thread.sleep(1000);
        }
        System.out.println("Не удалось переподключиться после 8 попыток");
    }

    private void sendCommandBestEffort(String command) {
        try {
            // Отправляем команду без проверки ответа
            String fullCommand = command + "\r\n";
            byte[] bytes = fullCommand.getBytes("UTF-8");
            if (activePort != null && activePort.isOpen()) {
                // Очищаем буфер перед отправкой
                while (activePort.bytesAvailable() > 0) {
                    inputStream.read(new byte[1024]);
                }
                outputStream.write(bytes);
                outputStream.flush();
                System.out.println("Отправлена команда: " + command);
            }
        } catch (Exception e) {
            // Игнорируем ошибки, так как save может закрыть порт
            System.out.println("Save отправлен, контроллер перезагружается");
        }
    }

    private void drainInputQuiet() {
        if (!isConnected()) {
            return;
        }
        try {
            byte[] buffer = new byte[1024];
            while (activePort.bytesAvailable() > 0) {
                inputStream.read(buffer);
            }
        } catch (Exception ignored) {
        }
    }

    private void sleepQuiet(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Синхронизация данных с контроллером.
     * Читает VTX таблицу с контроллера, парсит её и сохраняет в БД.
     * Автоматически создает недостающие бэнды.
     *
     * @return SyncResult с результатами синхронизации
     */
    public SyncResult syncVtxTable() {
        SyncResult result = new SyncResult();

        if (!isConnected()) {
            result.success = false;
            result.message = "Устройство не подключено";
            return result;
        }

        System.out.println("Начинаем синхронизацию VTX таблицы");

        try {
            // Вход в CLI режим
            if (!enterCliMode()) {
                result.success = false;
                result.message = "Не удалось войти в CLI режим";
                return result;
            }

            // Отправляем команду vtxtable
            if (!sendCommand("vtxtable")) {
                result.success = false;
                result.message = "Не удалось отправить команду vtxtable";
                return result;
            }

            // Читаем ответ
            String response = readResponse();
            if (response == null || response.isEmpty()) {
                result.success = false;
                result.message = "Пустой ответ от контроллера";
                return result;
            }

            System.out.println("Получен ответ от vtxtable, длина: " + response.length());

            // Парсим ответ
            List<VtxBand> existingBands = dao.getAllBands();
            ConfigParser.ParseResult parseResult = parser.parseVtxTableResponse(response, existingBands);

            if (parseResult.bands.isEmpty()) {
                result.success = false;
                result.message = "Не найдено бэндов в ответе контроллера";
                return result;
            }

            System.out.println("Найдено бэндов: " + parseResult.bands.size());

            // Сохраняем бэнды в БД и запоминаем их ID
            Map<VtxBand, Integer> bandIdMap = new HashMap<>();
            for (VtxBand parsedBand : parseResult.bands) {
                // Проверяем, существует ли такой бэнд в БД
                VtxBand existing = dao.getBandByNumber(parsedBand.getBandNumber());
                if (existing == null) {
                    // Создаем новый бэнд
                    dao.saveBand(parsedBand);
                    result.bandsCreated++;
                    bandIdMap.put(parsedBand, parsedBand.getIdBands());
                } else {
                    // Обновляем существующий
                    existing.setBandName(parsedBand.getBandName());
                    existing.setBandLetter(parsedBand.getBandLetter());
                    dao.saveBand(existing);
                    bandIdMap.put(parsedBand, existing.getIdBands());
                }
            }

            // Сохраняем каналы и конфиги
            for (Map.Entry<VtxBand, List<VtxChannel>> entry : parseResult.channelsMap.entrySet()) {
                VtxBand band = entry.getKey();
                List<VtxChannel> channels = entry.getValue();

                // Получаем реальный ID бэнда из БД
                Integer realBandId = bandIdMap.get(band);
                if (realBandId == null) {
                    // Если бэнд был найден в БД, а не создан, он уже есть в bandIdMap
                    // Проверяем еще раз по номеру
                    VtxBand existing = dao.getBandByNumber(band.getBandNumber());
                    if (existing != null) {
                        realBandId = existing.getIdBands();
                    } else {
                        System.err.println("Не найден ID для бэнда: " + band.getBandName());
                        continue;
                    }
                }

                // Обновляем bandId у каналов
                for (VtxChannel ch : channels) {
                    ch.setBandId(realBandId);
                    ch.setIdChannels(0); // Сбрасываем ID для вставки
                }

                // Заменяем каналы для бэнда
                dao.replaceChannelsForBand(realBandId, channels);
                result.channelsUpdated += channels.size();
            }

            List<VtxConfig> referenceConfigs = null;
            if (!parseResult.bands.isEmpty()) {
                referenceConfigs = parseResult.configsMap.get(parseResult.bands.get(0));
            }

            for (Map.Entry<VtxBand, Integer> entry : bandIdMap.entrySet()) {
                Integer realBandId = entry.getValue();
                if (realBandId == null || referenceConfigs == null) {
                    continue;
                }
                List<VtxConfig> bandConfigs = new ArrayList<>();
                for (VtxConfig cfg : referenceConfigs) {
                    VtxConfig copy = new VtxConfig(
                            realBandId,
                            cfg.getLevelIndex(),
                            cfg.getLabel(),
                            cfg.getValueMw(),
                            cfg.isPitMode()
                    );
                    copy.setTotalBands(parseResult.totalBands);
                    copy.setTotalChannels(parseResult.totalChannels);
                    bandConfigs.add(copy);
                }
                dao.replaceConfigsForBand(realBandId, bandConfigs);
                result.configsUpdated += bandConfigs.size();
            }

            result.success = true;
            result.message = "Синхронизация успешно завершена";
            result.foundBands = parseResult.bands;
            result.foundChannels = parseResult.channelsMap.values().stream()
                    .flatMap(List::stream)
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            result.foundConfigs = referenceConfigs != null ? new ArrayList<>(referenceConfigs) : new ArrayList<>();

            System.out.println("Синхронизация завершена: " + result.bandsCreated + " новых бэндов, "
                    + result.channelsUpdated + " каналов, " + result.foundBands.size() + " бэндов всего");

        } catch (Exception e) {
            System.err.println("Ошибка при синхронизации: " + e.getMessage());
            e.printStackTrace();
            result.success = false;
            result.message = "Ошибка: " + e.getMessage();
        }

        return result;
    }

    /**
     * Запись текущей конфигурации из БД в контроллер.
     * Генерирует команды на основе данных из БД, отправляет их в контроллер
     * и сохраняет через команду save.
     *
     * @return true, если запись успешна
     */
    public boolean writeVtxTable() {
        if (!isConnected()) {
            System.err.println("Не удалось записать VTX: порт не подключен");
            return false;
        }

        System.out.println("Начинаем запись VTX таблицы в контроллер");

        try {
            // Загрузка данных из БД
            List<VtxBand> bands = dao.getAllBands();
            if (bands.isEmpty()) {
                System.err.println("Нет данных для записи");
                return false;
            }

            List<VtxChannel> allChannels = new ArrayList<>();
            List<VtxConfig> allConfigs = new ArrayList<>();

            for (VtxBand band : bands) {
                allChannels.addAll(dao.getChannelsByBandId(band.getIdBands()));
                allConfigs.addAll(dao.getConfigByBandId(band.getIdBands()));
            }

            // Генерируем команды
            String commands = parser.generateVtxTableCommand(bands, allChannels, allConfigs);
            if (commands.isEmpty()) {
                System.err.println("Не удалось сгенерировать команды");
                return false;
            }

            // Вход в CLI
            boolean cliEntered = false;
            for (int attempt = 0; attempt < 3; attempt++) {
                System.out.println("Попытка входа в CLI #" + (attempt + 1));

                // Отправляем '#'
                if (sendCommand("#")) {
                    // Ждем ответ дольше
                    Thread.sleep(500);
                    String response = readResponse();
                    if (response.contains("Entering CLI Mode") || response.contains("#")) {
                        System.out.println("CLI режим активирован");
                        cliEntered = true;
                        break;
                    }
                }
                Thread.sleep(1000);
            }

            if (!cliEntered) {
                System.err.println("Не удалось войти в CLI режим после 3 попыток");
                return false;
            }

            // Отправляем команды
            String[] cmdLines = commands.split("\\r?\\n");
            System.out.println("Отправка " + cmdLines.length + " команд");

            for (int i = 0; i < cmdLines.length; i++) {
                String cmd = cmdLines[i].trim();
                if (cmd.isEmpty()) continue;

                System.out.println("Отправка команды " + (i + 1) + "/" + cmdLines.length + ": " + cmd.substring(0, Math.min(50, cmd.length())));

                if (!sendCommand(cmd)) {
                    System.err.println("Ошибка при отправке команды: " + cmd);
                    // Пробуем восстановить соединение
                    Thread.sleep(2000);
                    continue;
                }

                // Ждем ответ с увеличенной задержкой
                Thread.sleep(300);
                String response = readResponse();
                System.out.println("Получен ответ на команду " + (i + 1) + ": " + response.length() + " байт");

                // Небольшая пауза между командами
                Thread.sleep(200);
            }

            System.out.println("Отправка команды save для сохранения в EEPROM");
            sendCommand("save");
            // Даем время на перезагрузку, НЕ читаем ответ
            Thread.sleep(500);

            disconnect();

            System.out.println("Запись VTX таблицы успешно завершена");
            return true;

        } catch (InterruptedException e) {
            System.err.println("Прервано выполнение: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            System.err.println("Ошибка при записи VTX: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Чтение сырой VTX таблицы с контроллера.
     *
     * @return сырой ответ от контроллера
     */
    public String readRawVtxTable() {
        if (!isConnected()) {
            return "Не подключено";
        }

        if (enterCliMode()) {
            sendCommand("vtxtable");
            String response = readResponse();
            sendCommand("exit");
            readResponse();
            return response;
        }
        return "Не удалось войти в CLI";
    }

    /**
     * Проверка связи с контроллером.
     * Отправляет команду 'status' для проверки ответа.
     *
     * @return true если контроллер отвечает
     */
    public boolean pingController() {
        if (!isConnected()) return false;

        try {
            System.out.println("Проверка связи с контроллером");

            // Очищаем буфер перед отправкой
            while (activePort.bytesAvailable() > 0) {
                inputStream.read(new byte[1024]);
            }

            // Отправляем команду 'status'
            outputStream.write("status\r\n".getBytes());
            outputStream.flush();

            // Ждем ответ с увеличенной задержкой
            Thread.sleep(300);

            // Читаем ответ
            String response = readResponse();
            System.out.println("Ответ на status: " + (response.length() > 50 ? response.substring(0, 50) + "..." : response));

            // Проверяем, что ответ содержит что-то осмысленное
            // В CLI режиме 'status' возвращает информацию о системе
            // Если мы не в CLI - ответ может быть пустым или содержать ошибку
            if (response != null && !response.isEmpty()) {
                // Если ответ содержит "ERROR" - значит мы не в CLI режиме
                if (response.contains("ERROR") || response.contains("UNKNOWN")) {
                    System.out.println("Контроллер отвечает, но не в CLI режиме");
                    return true; // Контроллер доступен, просто не в CLI
                }
                return true;
            }

            return false;
        } catch (Exception e) {
            System.err.println("Ошибка проверки связи: " + e.getMessage());
            return false;
        }
    }

    /**
     * Отправка ОДНОГО бэнда в контроллер (без save)
     */
    public boolean sendSingleBandCommand(int bandId) {
        if (!isConnected()) {
            System.err.println("[Порт не подключен");
            return false;
        }

        try {
            VtxBand band = null;
            List<VtxChannel> channels = new ArrayList<>();

            List<VtxBand> allBands = dao.getAllBands();
            for (VtxBand b : allBands) {
                if (b.getIdBands() == bandId) {
                    band = b;
                    channels = dao.getChannelsByBandId(bandId);
                    break;
                }
            }

            if (band == null) {
                System.err.println("Бэнд с ID=" + bandId + " не найден");
                return false;
            }

            String command = parser.generateSingleBandCommand(band, channels);
            if (command.isEmpty()) {
                System.err.println("Не удалось сгенерировать команду");
                return false;
            }

            System.out.println("Отправка команды для " + band.getBandName());

            if (!sendCommand(command)) {
                System.err.println("Ошибка при отправке команды");
                return false;
            }

            Thread.sleep(300);
            String response = readResponse();

            if (response.contains("###ERROR")) {
                System.err.println("Ошибка: " + response);
                return false;
            }

            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Прервано: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Сохранение и перезагрузка контроллера (один раз после всех команд)
     */
    public boolean saveAndReboot() {
        if (!isConnected()) {
            System.err.println("Порт не подключен");
            return false;
        }

        try {
            System.out.println("Сохранение в EEPROM...");
            sendCommand("save");
            Thread.sleep(500);
            disconnect();
            return true;
        } catch (Exception e) {
            System.err.println("Ошибка сохранения: " + e.getMessage());
            return false;
        }
    }

    /**
     * Отправка нескольких бэндов в контроллер (без save)
     */
    public boolean writeMultipleBands(List<Integer> bandIds) {
        if (!isConnected()) {
            System.err.println("Порт не подключен");
            return false;
        }

        System.out.println("Отправка " + bandIds.size() + " бэндов");

        try {
            boolean cliEntered = false;
            for (int attempt = 0; attempt < 3; attempt++) {
                if (sendCommand("#")) {
                    Thread.sleep(500);
                    String response = readResponse();
                    if (response.contains("Entering CLI Mode") || response.contains("#")) {
                        cliEntered = true;
                        break;
                    }
                }
                Thread.sleep(1000);
            }

            if (!cliEntered) {
                System.err.println("Не удалось войти в CLI режим");
                return false;
            }

            for (int bandId : bandIds) {
                // Загружаем данные бэнда
                VtxBand band = null;
                List<VtxChannel> channels = new ArrayList<>();

                List<VtxBand> allBands = dao.getAllBands();
                for (VtxBand b : allBands) {
                    if (b.getIdBands() == bandId) {
                        band = b;
                        channels = dao.getChannelsByBandId(bandId);
                        break;
                    }
                }

                if (band == null) {
                    System.err.println("Бэнд с ID=" + bandId + " не найден");
                    continue;
                }

                String command = parser.generateSingleBandCommand(band, channels);
                if (command.isEmpty()) {
                    System.err.println("Не удалось сгенерировать команду для бэнда " + band.getBandName());
                    continue;
                }

                System.out.println("Отправка команды для " + band.getBandName() + ": " + command.trim());

                if (!sendCommand(command)) {
                    System.err.println("Ошибка при отправке команды для " + band.getBandName());
                    return false;
                }

                Thread.sleep(300);
                String response = readResponse();

                // Проверяем ошибки
                if (response.contains("###ERROR")) {
                    System.err.println("Ошибка для " + band.getBandName() + ": " + response);
                    return false;
                }
            }

            System.out.println("Сохранение в EEPROM");
            sendCommand("save");
            Thread.sleep(500);

            disconnect();

            System.out.println("Все изменения записаны, контроллер перезагружен");
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Прервано: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Отправка изменений размерности таблицы (количество бэндов и каналов)
     */
    public boolean writeDimensions(int totalBands, int totalChannels) {
        if (!isConnected()) {
            System.err.println("Порт не подключен");
            return false;
        }

        System.out.println("Запись размерности: bands=" + totalBands + ", channels=" + totalChannels);

        try {
            // Генерируем команды размерности
            String commands = parser.generateDimensionCommands(totalBands, totalChannels);
            if (commands.isEmpty()) {
                System.err.println("[WRITE] Не удалось сгенерировать команды размерности");
                return false;
            }

            // Вход в CLI
            boolean cliEntered = false;
            for (int attempt = 0; attempt < 3; attempt++) {
                if (sendCommand("#")) {
                    Thread.sleep(500);
                    String response = readResponse();
                    if (response.contains("Entering CLI Mode") || response.contains("#")) {
                        cliEntered = true;
                        break;
                    }
                }
                Thread.sleep(1000);
            }

            if (!cliEntered) {
                System.err.println("Не удалось войти в CLI режим");
                return false;
            }

            // Отправляем команды размерности
            String[] cmdLines = commands.split("\\r?\\n");
            for (String cmd : cmdLines) {
                if (cmd.trim().isEmpty()) continue;

                if (!sendCommand(cmd)) {
                    System.err.println("Ошибка при отправке: " + cmd);
                    return false;
                }
                Thread.sleep(300);
                String response = readResponse();
                if (!parser.validateCommandResponse(response)) {
                    System.err.println("Ошибка в ответе на " + cmd + ": " + response);
                    return false;
                }
            }

            // Сохраняем
            System.out.println("Сохранение изменений размерности");
            sendCommand("save");
            Thread.sleep(500);
            disconnect();

            System.out.println("Запись размерности выполнена");
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Прервано: " + e.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Ошибка: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}