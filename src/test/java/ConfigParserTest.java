package org.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.example.model.VtxBand;
import org.example.model.VtxChannel;
import org.example.model.VtxConfig;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigParserTest {

    private ConfigParser parser;
    private List<VtxBand> bands;
    private List<VtxChannel> channels;
    private List<VtxConfig> configs;

    @BeforeEach
    void setUp() {
        parser = new ConfigParser();

        bands = new ArrayList<>();
        bands.add(new VtxBand(1, 1, "BAND_A", 'A'));
        bands.add(new VtxBand(2, 2, "BAND_B", 'B'));
        bands.add(new VtxBand(3, 3, "BAND_C", 'C'));

        channels = new ArrayList<>();
        int id = 1;
        for (int bandId = 1; bandId <= 3; bandId++) {
            for (int ch = 1; ch <= 8; ch++) {
                int freq = 7300 ;
                channels.add(new VtxChannel(id++,bandId, ch, freq));
            }
        }

        configs = new ArrayList<>();
        configs.add(new VtxConfig(1, 1, 1, "MIN", 20, false, 3, 8));
        configs.add(new VtxConfig(2, 1, 2, "MAX", 1995, false, 3, 8));
    }

    @Test
    void testGenerateVtxTableCommand() {
        String result = parser.generateVtxTableCommand(bands, channels, configs);
        assertNotNull(result);
        assertFalse(result.isEmpty());

        assertTrue(result.contains("vtxtable bands 3"));
        assertTrue(result.contains("vtxtable channels 8"));
        assertTrue(result.contains("vtxtable band 1 BAND_A A CUSTOM"));
        assertTrue(result.contains("vtxtable band 2 BAND_B B CUSTOM"));
        assertTrue(result.contains("vtxtable band 3 BAND_C C CUSTOM"));
        assertTrue(result.contains("vtxtable powerlevels 2"));
        assertTrue(result.contains("vtxtable powervalues 20 1995"));
        assertTrue(result.contains("vtxtable powerlabels MIN MAX"));

        System.out.println("Тест 1: генерация CLI-команд Betaflight выполнена успешно");
        System.out.println("Сгенерировано " + result.length() + " символов");
    }

    @Test
    void testGenerateVtxTableCommandWithEmptyBands() {
        List<VtxBand> emptyBands = new ArrayList<>();
        String result = parser.generateVtxTableCommand(emptyBands, channels, configs);
        assertNotNull(result);
        assertTrue(result.isEmpty());

        System.out.println("Тест 2: обработка пустого списка сеток выполнена успешно");
        System.out.println("Метод вернул пустую строку, исключений не возникло");
    }

    @Test
    void testGenerateVtxTableCommandWithNullBands() {
        String result = parser.generateVtxTableCommand(null, channels, configs);
        assertNotNull(result);
        assertTrue(result.isEmpty());

        System.out.println("Тест 2: обработка null данных выполнена успешно");
        System.out.println("Метод вернул пустую строку, исключений не возникло");
    }
}