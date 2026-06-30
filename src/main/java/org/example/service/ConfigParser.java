package org.example.service;

import org.example.model.VtxBand;
import org.example.model.VtxChannel;
import org.example.model.VtxConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Класс трансляции и парсинга конфигураций
 * Отвечает за:
 * Преобразование объектов моделей (Band, Channel, Config) в строки cli - команд;
 * Чтение текстовых логов последовательного порта и воссоздание объектов моделей Java.
 */
public class ConfigParser {

    /**
     * Вспомогательный класс для хранения результатов парсинга
     */
    public static class ParseResult {
        public List<VtxBand> bands = new ArrayList<>();
        public Map<VtxBand, List<VtxChannel>> channelsMap = new HashMap<>();
        public Map<VtxBand, List<VtxConfig>> configsMap = new HashMap<>();
        public int totalBands = 0;
        public int totalChannels = 0;
        public int totalPowerLevels = 0;

        @Override
        public String toString() {
            return "ParseResult{" +
                    "bands=" + bands.size() +
                    ", channels=" + channelsMap.values().stream().mapToInt(List::size).sum() +
                    ", configs=" + configsMap.values().stream().mapToInt(List::size).sum() +
                    '}';
        }
    }

    /**
     * Парсинг сырого ответа от контроллера на команду vtxtable
     * Извлекает все данные: бэнды, каналы, конфиги мощностей
     *
     * @param response сырой текст из COM-порта
     * @param existingBands список сеток, которые уже есть в нашей локальной БД
     * @return ParseResult объект с разобранными данными
     */
    public ParseResult parseVtxTableResponse(String response, List<VtxBand> existingBands) {
        ParseResult result = new ParseResult();
        if (response == null || response.isEmpty()) {
            System.out.println("Ответ от контроллера пустой");
            return result;
        }

        System.out.println("Начало парсинга ответа");
        String[] lines = response.split("\\r?\\n");

        int bandsCount = 0;
        int channelsCount = 0;
        List<Integer> powerValues = new ArrayList<>();
        List<String> powerLabels = new ArrayList<>();

        for (String line : lines) {
            line = line.trim();

            // Парсим количество бэндов
            if (line.startsWith("vtxtable bands")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    try {
                        bandsCount = Integer.parseInt(parts[2]);
                        result.totalBands = bandsCount;
                        System.out.println("Количество бэндов = " + bandsCount);
                    } catch (NumberFormatException ignored) {}
                }
            }

            // Парсим количество каналов
            if (line.startsWith("vtxtable channels")) {
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    try {
                        channelsCount = Integer.parseInt(parts[2]);
                        result.totalChannels = channelsCount;
                        System.out.println("Количество каналов = " + channelsCount);
                    } catch (NumberFormatException ignored) {}
                }
            }

            // Парсим значения мощностей
            if (line.startsWith("vtxtable powervalues")) {
                String[] parts = line.split("\\s+");
                for (int i = 2; i < parts.length; i++) {
                    try {
                        powerValues.add(Integer.parseInt(parts[i]));
                    } catch (NumberFormatException ignored) {}
                }
                System.out.println("Количество значений мощностей = " + powerValues.size());
            }

            // Парсим метки мощностей
            if (line.startsWith("vtxtable powerlabels")) {
                String[] parts = line.split("\\s+");
                for (int i = 2; i < parts.length; i++) {
                    powerLabels.add(parts[i]);
                }
                System.out.println("Количество меток мощностей = " + powerLabels.size());
            }
        }

        // Парсим строки сеток
        Pattern bandPattern = Pattern.compile(
                "vtxtable\\s+band\\s+(\\d+)\\s+(\\S+)\\s+([A-Za-z])\\s+CUSTOM\\s+([\\d\\s]+)",
                Pattern.CASE_INSENSITIVE
        );

        // Создаем Map для быстрого поиска существующих бэндов по букве и номеру
        Map<Character, VtxBand> existingByLetter = new HashMap<>();
        Map<Integer, VtxBand> existingByNumber = new HashMap<>();
        for (VtxBand band : existingBands) {
            existingByLetter.put(band.getBandLetter(), band);
            existingByNumber.put(band.getBandNumber(), band);
        }

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("vtxtable band") && !line.contains("bands") && !line.contains("channels")) {
                Matcher matcher = bandPattern.matcher(line);
                if (matcher.find()) {
                    int bandNumber = Integer.parseInt(matcher.group(1));
                    String bandName = matcher.group(2);
                    char bandLetter = matcher.group(3).charAt(0);
                    String frequenciesStr = matcher.group(4);

                    System.out.println("Найден бэнд: " + bandNumber + " - " + bandName + " (" + bandLetter + ")");

                    // Ищем существующий бэнд
                    VtxBand matchedBand = null;

                    // Сначала ищем по номеру
                    if (existingByNumber.containsKey(bandNumber)) {
                        matchedBand = existingByNumber.get(bandNumber);
                        // Обновляем данные на случай, если изменились
                        matchedBand.setBandName(bandName);
                        matchedBand.setBandLetter(bandLetter);
                        System.out.println("Найден существующий бэнд по номеру: " + bandNumber);
                    }
                    // Затем ищем по букве
                    else if (existingByLetter.containsKey(bandLetter)) {
                        matchedBand = existingByLetter.get(bandLetter);
                        // Обновляем номер
                        matchedBand.setBandNumber(bandNumber);
                        System.out.println("Найден существующий бэнд по букве: " + bandLetter);
                    }

                    // Если бэнд не найден - создаем новый
                    if (matchedBand == null) {
                        matchedBand = new VtxBand(bandNumber, bandName, bandLetter);
                        System.out.println("Создан новый бэнд: " + bandName);
                    }

                    result.bands.add(matchedBand);

                    // Парсим частоты каналов
                    List<VtxChannel> channelsList = new ArrayList<>();
                    String[] freqTokens = frequenciesStr.trim().split("\\s+");
                    for (int i = 0; i < freqTokens.length; i++) {
                        try {
                            int channelNum = i + 1;
                            int freq = Integer.parseInt(freqTokens[i]);
                            // Используем ID бэнда, даже если он 0 (для новых бэндов)
                            // При сохранении ID будет обновлен
                            VtxChannel channel = new VtxChannel(
                                    matchedBand.getIdBands(),
                                    channelNum,
                                    freq
                            );
                            channelsList.add(channel);
                            System.out.println("Канал: Band=" + bandNumber +
                                    ", CH" + channelNum + ", Freq=" + freq);
                        } catch (NumberFormatException e) {
                            System.err.println("Ошибка парсинга частоты: " + freqTokens[i]);
                        }
                    }
                    result.channelsMap.put(matchedBand, channelsList);

                    // Создаем конфиги для этой сетки
                    List<VtxConfig> configsList = new ArrayList<>();
                    int levelsCount = Math.min(powerValues.size(), powerLabels.size());
                    for (int i = 0; i < levelsCount; i++) {
                        int levelIndex = i + 1;
                        VtxConfig config = new VtxConfig(
                                matchedBand.getIdBands(),
                                levelIndex,
                                powerLabels.get(i),
                                powerValues.get(i),
                                false // pitmode по умолчанию false
                        );
                        configsList.add(config);
                        System.out.println("Конфиг: Level=" + levelIndex +
                                ", Label=" + powerLabels.get(i) +
                                ", Value=" + powerValues.get(i));
                    }
                    result.configsMap.put(matchedBand, configsList);
                    result.totalPowerLevels = levelsCount;
                }
            }
        }

        System.out.println("Парсинг завершен. " + result);
        return result;
    }

    /**
     * Генерация полного пакета CLI-команд на основе данных из БД.
     * Возвращает единый текстовый блок, готовый к отправке в COM-порт.
     *
     * @param bands список сеток из БД
     * @param channels список каналов из БД
     * @param configs список конфигураций мощностей из БД
     * @return строка CLI-команд для полной прошивки таблицы в контроллер
     */
    public String generateVtxTableCommand(List<VtxBand> bands, List<VtxChannel> channels, List<VtxConfig> configs) {
        StringBuilder sb = new StringBuilder();

        if (bands == null || bands.isEmpty()) {
            System.out.println("Нет данных для генерации команд");
            return sb.toString();
        }

        System.out.println("Генерация команд для " + bands.size() + " бэндов");

        // Команды размерности таблиц
        sb.append("vtxtable bands ").append(bands.size()).append("\r\n");

        // Находим максимальное количество каналов в одной сетке
        int maxChannels = 0;
        for (VtxBand band : bands) {
            int count = 0;
            for (VtxChannel ch : channels) {
                if (ch.getBandId() == band.getIdBands()) {
                    count++;
                }
            }
            if (count > maxChannels) {
                maxChannels = count;
            }
        }
        sb.append("vtxtable channels ").append(maxChannels).append("\r\n");

        // Команды сеток и частот
        for (VtxBand band : bands) {
            sb.append("vtxtable band ")
                    .append(band.getBandNumber()).append(" ")
                    .append(band.getBandName()).append(" ")
                    .append(band.getBandLetter()).append(" CUSTOM");

            for (VtxChannel ch : channels) {
                if (ch.getBandId() == band.getIdBands()) {
                    sb.append(" ").append(ch.getFrequencyMhz());
                }
            }
            sb.append("\r\n");
        }

        // Команды мощностей
        List<VtxConfig> referenceConfigs = new ArrayList<>();
        if (!bands.isEmpty()) {
            int refBandId = bands.get(0).getIdBands();
            for (VtxConfig c : configs) {
                if (c.getBandId() == refBandId) {
                    referenceConfigs.add(c);
                }
            }
        }

        sb.append("vtxtable powerlevels ").append(referenceConfigs.size()).append("\r\n");

        sb.append("vtxtable powervalues");
        for (VtxConfig c : referenceConfigs) {
            sb.append(" ").append(c.getValueMw());
        }
        sb.append("\r\n");

        sb.append("vtxtable powerlabels");
        for (VtxConfig c : referenceConfigs) {
            sb.append(" ").append(c.getLabel());
        }
        sb.append("\r\n");

        String result = sb.toString();
        System.out.println("Сгенерировано команд. Длина: " + result.length() + " символов");
        return result;
    }

    /**
     * Парсинг только бэндов из ответа (используется при синхронизации)
     */
    public List<VtxBand> parseBandsOnly(String response) {
        List<VtxBand> bands = new ArrayList<>();
        if (response == null || response.isEmpty()) {
            return bands;
        }

        Pattern bandPattern = Pattern.compile(
                "vtxtable\\s+band\\s+(\\d+)\\s+(\\S+)\\s+([A-Za-z])\\s+CUSTOM",
                Pattern.CASE_INSENSITIVE
        );

        String[] lines = response.split("\\r?\\n");
        for (String line : lines) {
            Matcher matcher = bandPattern.matcher(line.trim());
            if (matcher.find()) {
                int bandNumber = Integer.parseInt(matcher.group(1));
                String bandName = matcher.group(2);
                char bandLetter = matcher.group(3).charAt(0);
                bands.add(new VtxBand(bandNumber, bandName, bandLetter));
            }
        }
        return bands;
    }

    /**
     * Генерирует команду только для одного бэнда
     */
    public String generateSingleBandCommand(VtxBand band, List<VtxChannel> channels) {
        if (band == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("vtxtable band ")
                .append(band.getBandNumber()).append(" ")
                .append(band.getBandName()).append(" ")
                .append(band.getBandLetter()).append(" CUSTOM");

        if (channels != null) {
            for (VtxChannel ch : channels) {
                sb.append(" ").append(ch.getFrequencyMhz());
            }
        }
        sb.append("\r\n");
        return sb.toString();
    }

    public String generateDimensionCommands(int totalBands, int totalChannels) {
        StringBuilder sb = new StringBuilder();
        sb.append("vtxtable bands ").append(totalBands).append("\r\n");
        sb.append("vtxtable channels ").append(totalChannels).append("\r\n");
        return sb.toString();
    }

    public boolean validateCommandResponse(String response) {
        if (response == null || response.isEmpty()) {
            return false;
        }
        if (response.contains("###ERROR")) {
            System.err.println("Ошибка: " + response);
            return false;
        }
        return true;
    }
}