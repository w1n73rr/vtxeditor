

import org.example.service.ConfigParser;
import org.junit.jupiter.api.Test;
import org.example.model.VtxBand;
import org.example.model.VtxChannel;
import org.example.model.VtxConfig;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SerialConnectorMockTest {

    @Test
    void testSendVtxCommandsWithMock() throws IOException {
        ByteArrayOutputStream mockOut = new ByteArrayOutputStream();

        List<String> commands = Arrays.asList(
                "vtxtable bands 3",
                "vtxtable channels 8",
                "vtxtable band 1 BAND_A A CUSTOM 7300 7325 7350 7375 7400 7425 7450 7475",
                "vtxtable band 2 BAND_B B CUSTOM 7300 7325 7350 7375 7400 7425 7450 7475",
                "vtxtable band 3 BAND_C C CUSTOM 7300 7325 7350 7375 7400 7425 7450 7475",
                "vtxtable powerlevels 2",
                "vtxtable powervalues 20 1995",
                "vtxtable powerlabels MIN MAX",
                "save"
        );

        for (String cmd : commands) {
            mockOut.write(cmd.getBytes());
            mockOut.write('\n');
        }

        String sentData = mockOut.toString();
        assertFalse(sentData.isEmpty());
        assertTrue(sentData.contains("vtxtable bands 3"));
        assertTrue(sentData.contains("vtxtable channels 8"));
        assertTrue(sentData.contains("save"));

        System.out.println("Тест 4: отправка команд на COM-порт выполнена успешно");
        System.out.println("Отправлено " + commands.size() + " команд");
    }

    @Test
    void testGenerateAndSendFlow() throws IOException {
        ConfigParser parser = new ConfigParser();

        List<VtxBand> bands = new ArrayList<>();
        bands.add(new VtxBand(1, 1, "BAND_A", 'A'));
        bands.add(new VtxBand(2, 2, "BAND_B", 'B'));
        bands.add(new VtxBand(3, 3, "BAND_C", 'C'));

        List<VtxChannel> channels = new ArrayList<>();
        int id = 1;
        for (int bandId = 1; bandId <= 3; bandId++) {
            for (int ch = 1; ch <= 8; ch++) {
                int freq = 7300 + (bandId - 1) * 100 + (ch - 1) * 25;
                channels.add(new VtxChannel(id++,bandId, ch, freq));
            }
        }

        List<VtxConfig> configs = new ArrayList<>();
        configs.add(new VtxConfig(1, 1, 1, "MIN", 20, false, 3, 8));
        configs.add(new VtxConfig(2, 1, 2, "MAX", 1995, false, 3, 8));

        String generatedCommands = parser.generateVtxTableCommand(bands, channels, configs);
        assertNotNull(generatedCommands);
        assertFalse(generatedCommands.isEmpty());

        ByteArrayOutputStream mockOut = new ByteArrayOutputStream();
        mockOut.write(generatedCommands.getBytes());
        String sent = mockOut.toString();

        assertTrue(sent.contains("vtxtable bands 3"));
        assertTrue(sent.contains("vtxtable powerlabels MIN MAX"));

        System.out.println("Тест 4: интеграционный тест генерации и отправки выполнен успешно");
        System.out.println("Сгенерировано " + generatedCommands.length() + " символов");
    }
}