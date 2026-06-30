package org.example.ui;

import org.example.database.VtxDao;
import org.example.model.VtxBand;
import org.example.model.VtxChannel;
import org.example.model.VtxConfig;
import org.example.service.ConfigParser;
import org.example.service.SerialConnector;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class MainFrame extends JFrame implements ActionListener {

    private static final Color COLOR_BG = new Color(240, 242, 245);
    private static final Color COLOR_CARD_BG = Color.WHITE;
    private static final Color COLOR_TEXT = Color.BLACK;
    private static final Color COLOR_BORDER = new Color(180, 180, 190);
    private static final Color COLOR_SUCCESS = new Color(0, 150, 80);
    private static final Color COLOR_DANGER = new Color(200, 40, 40);
    private static final Color COLOR_WARNING = new Color(200, 150, 0);

    private final VtxDao dao;
    private final SerialConnector serialConnector;
    private final ConfigParser parser;

    private JComboBox<String> portComboBox;
    private JButton btnRefreshPorts;
    private JButton btnConnect;
    private JLabel lblStatusIndicator;
    private JLabel lblStatusText;

    private JPanel centerPanel;
    private JPanel powerGridPanel;
    private JToggleButton btnPitmode;
    private JButton btnSync;
    private JButton btnWriteChanges;
    private JButton btnAddBand;
    private JButton btnAddChannel;
    private JButton btnRemoveChannel;
    private JButton btnAddPower;

    private final Map<Integer, List<JTextField>> channelFieldsMap = new HashMap<>();
    private final Map<Integer, List<VtxChannel>> channelObjectsMap = new HashMap<>();
    private final Map<Integer, JTextField> powerValueFieldsMap = new HashMap<>();
    private final Map<Integer, JTextField> powerLabelFieldsMap = new HashMap<>();

    private List<VtxBand> currentBands = new ArrayList<>();
    private List<VtxConfig> currentPowerConfigs = new ArrayList<>();

    private boolean isConnected = false;
    private String activePort = null;
    private boolean writeInProgress = false;

    /** Снимок того, что реально записано в контроллере (после sync или успешной записи). */
    private VtxBaseline vtxBaseline = new VtxBaseline();

    public MainFrame(VtxDao dao, SerialConnector serialConnector) {
        this.dao = dao;
        this.serialConnector = serialConnector;
        this.parser = new ConfigParser();

        setTitle("VTX Config Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 750);
        setMinimumSize(new Dimension(900, 550));
        setLocationRelativeTo(null);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        initUI();
        refreshPorts();
        loadDataFromDatabase(true);
    }

    // --- UI ---

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(COLOR_BG);
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        mainPanel.add(createPortPanel(), BorderLayout.NORTH);
        mainPanel.add(createCenterPanel(), BorderLayout.CENTER);
        mainPanel.add(createSouthPanel(), BorderLayout.SOUTH);
        setContentPane(mainPanel);
    }

    private JPanel createPortPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(COLOR_CARD_BG);
        panel.setBorder(new LineBorder(COLOR_BORDER, 1));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.weightx = 0.0;
        JLabel lblPort = new JLabel("COM-порт:");
        lblPort.setFont(lblPort.getFont().deriveFont(Font.BOLD));
        lblPort.setForeground(COLOR_TEXT);
        panel.add(lblPort, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.2;
        portComboBox = new JComboBox<>();
        portComboBox.setPreferredSize(new Dimension(150, 28));
        portComboBox.setBackground(Color.WHITE);
        portComboBox.setForeground(COLOR_TEXT);
        panel.add(portComboBox, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0.0;
        btnRefreshPorts = new JButton("Обновить");
        btnRefreshPorts.setForeground(COLOR_TEXT);
        btnRefreshPorts.addActionListener(this);
        panel.add(btnRefreshPorts, gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.05;
        panel.add(Box.createHorizontalStrut(10), gbc);

        gbc.gridx = 4;
        gbc.weightx = 0.0;
        lblStatusIndicator = new JLabel("●");
        lblStatusIndicator.setForeground(COLOR_DANGER);
        lblStatusIndicator.setFont(new Font("Segoe UI", Font.BOLD, 18));
        panel.add(lblStatusIndicator, gbc);

        gbc.gridx = 5;
        gbc.weightx = 0.5;
        lblStatusText = new JLabel("Отключено");
        lblStatusText.setForeground(COLOR_TEXT);
        lblStatusText.setFont(lblStatusText.getFont().deriveFont(Font.BOLD));
        lblStatusText.setPreferredSize(new Dimension(300, 20));
        panel.add(lblStatusText, gbc);

        gbc.gridx = 6;
        gbc.weightx = 0.0;
        gbc.anchor = GridBagConstraints.EAST;
        btnConnect = new JButton("Подключиться");
        btnConnect.setForeground(COLOR_TEXT);
        btnConnect.setBackground(new Color(220, 220, 220));
        btnConnect.setPreferredSize(new Dimension(130, 30));
        btnConnect.addActionListener(this);
        panel.add(btnConnect, gbc);

        return panel;
    }

    private JPanel createCenterPanel() {
        centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(COLOR_BG);

        JScrollPane scrollPane = new JScrollPane(centerPanel);
        scrollPane.setBorder(BorderFactory.createTitledBorder(
                new LineBorder(COLOR_BORDER, 1), " СЕТКИ ЧАСТОТ (МГц) ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12), COLOR_TEXT));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(COLOR_BG);
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createSouthPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(COLOR_BG);

        JPanel powerPanel = new JPanel(new BorderLayout(10, 5));
        powerPanel.setBackground(COLOR_CARD_BG);
        powerPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_BORDER, 1), new EmptyBorder(10, 15, 10, 15)));

        JLabel powerTitle = new JLabel("Уровни мощности (метка / мВт):");
        powerTitle.setFont(powerTitle.getFont().deriveFont(Font.BOLD));
        powerTitle.setForeground(COLOR_TEXT);
        powerPanel.add(powerTitle, BorderLayout.NORTH);

        powerGridPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        powerGridPanel.setBackground(COLOR_CARD_BG);
        powerPanel.add(powerGridPanel, BorderLayout.CENTER);

        btnPitmode = new JToggleButton("Pitmode: ВЫКЛ");
        btnPitmode.setForeground(COLOR_TEXT);
        btnPitmode.setBackground(new Color(200, 200, 200));
        btnPitmode.setFont(btnPitmode.getFont().deriveFont(Font.BOLD));
        btnPitmode.addActionListener(this);
        powerPanel.add(btnPitmode, BorderLayout.SOUTH);

        panel.add(powerPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setBackground(COLOR_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        btnAddBand = new JButton("+ Добавить бэнд");
        btnAddBand.setForeground(COLOR_TEXT);
        btnAddBand.setBackground(new Color(220, 220, 220));
        btnAddBand.addActionListener(this);
        buttonPanel.add(btnAddBand, gbc);

        gbc.gridx = 1;
        btnAddChannel = new JButton("+ Добавить канал");
        btnAddChannel.setForeground(COLOR_TEXT);
        btnAddChannel.setBackground(new Color(220, 220, 220));
        btnAddChannel.addActionListener(this);
        buttonPanel.add(btnAddChannel, gbc);

        gbc.gridx = 2;
        btnRemoveChannel = new JButton("- Убрать канал");
        btnRemoveChannel.setForeground(COLOR_TEXT);
        btnRemoveChannel.setBackground(new Color(220, 220, 220));
        btnRemoveChannel.addActionListener(this);
        buttonPanel.add(btnRemoveChannel, gbc);

        gbc.gridx = 3;
        btnAddPower = new JButton("+ Добавить уровень мощности");
        btnAddPower.setForeground(COLOR_TEXT);
        btnAddPower.setBackground(new Color(220, 220, 220));
        btnAddPower.addActionListener(this);
        buttonPanel.add(btnAddPower, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 4;
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        actionPanel.setBackground(COLOR_BG);

        btnSync = new JButton("Синхронизировать");
        btnSync.setForeground(COLOR_TEXT);
        btnSync.setBackground(new Color(200, 200, 200));
        btnSync.setEnabled(false);
        btnSync.addActionListener(this);
        actionPanel.add(btnSync);

        btnWriteChanges = new JButton("Записать в VTX");
        btnWriteChanges.setForeground(COLOR_TEXT);
        btnWriteChanges.setBackground(new Color(200, 200, 200));
        btnWriteChanges.setEnabled(false);
        btnWriteChanges.addActionListener(this);
        actionPanel.add(btnWriteChanges);

        buttonPanel.add(actionPanel, gbc);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        return panel;
    }

    // --- Загрузка и отрисовка ---

    private void loadDataFromDatabase(boolean resetBaseline) {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            List<VtxBand> bands;
            List<VtxConfig> powerConfigs;

            @Override
            protected Void doInBackground() {
                bands = dao.getAllBands();
                if (!bands.isEmpty()) {
                    powerConfigs = dao.getConfigByBandId(bands.get(0).getIdBands());
                } else {
                    powerConfigs = new ArrayList<>();
                }
                return null;
            }

            @Override
            protected void done() {
                currentBands = bands;
                currentPowerConfigs = powerConfigs;
                rebuildUI();
                if (resetBaseline) {
                    vtxBaseline = captureBaselineFromCurrentState();
                }
                refreshChangeStatus();
            }
        };
        worker.execute();
    }

    private void rebuildUI() {
        centerPanel.removeAll();
        channelFieldsMap.clear();
        channelObjectsMap.clear();
        powerGridPanel.removeAll();
        powerValueFieldsMap.clear();
        powerLabelFieldsMap.clear();

        if (currentBands.isEmpty()) {
            JLabel emptyLabel = new JLabel("Нет сеток частот. Нажмите «+ Добавить бэнд» для создания.");
            emptyLabel.setForeground(COLOR_TEXT);
            emptyLabel.setFont(emptyLabel.getFont().deriveFont(Font.BOLD, 14f));
            emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
            JPanel emptyPanel = new JPanel(new BorderLayout());
            emptyPanel.setBackground(COLOR_CARD_BG);
            emptyPanel.setBorder(new EmptyBorder(30, 0, 30, 0));
            emptyPanel.add(emptyLabel, BorderLayout.CENTER);
            centerPanel.add(emptyPanel);
        } else {
            for (VtxBand band : currentBands) {
                List<VtxChannel> channels = dao.getChannelsByBandId(band.getIdBands());
                centerPanel.add(createBandCard(band, channels));
                centerPanel.add(Box.createVerticalStrut(8));
            }
        }

        buildPowerPanel();
        updatePitmodeState();
        updateWriteButton();

        centerPanel.revalidate();
        centerPanel.repaint();
        powerGridPanel.revalidate();
        powerGridPanel.repaint();
    }

    private JPanel createBandCard(VtxBand band, List<VtxChannel> channels) {
        JPanel card = new JPanel(new BorderLayout(10, 5));
        card.setBackground(COLOR_CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_BORDER, 1), new EmptyBorder(8, 12, 8, 12)));

        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        namePanel.setBackground(COLOR_CARD_BG);

        JLabel nameLabel = new JLabel(String.format("%s (%c)", band.getBandName(), band.getBandLetter()));
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 14f));
        nameLabel.setForeground(COLOR_TEXT);
        nameLabel.setPreferredSize(new Dimension(130, 30));
        namePanel.add(nameLabel);

        JButton btnDeleteBand = createDeleteButton();
        btnDeleteBand.addActionListener(e -> deleteBand(band));
        namePanel.add(btnDeleteBand);

        card.add(namePanel, BorderLayout.WEST);

        JPanel channelsPanel = new JPanel(new GridLayout(1, Math.max(channels.size(), 1), 6, 0));
        channelsPanel.setBackground(COLOR_CARD_BG);

        List<JTextField> fields = new ArrayList<>();
        List<VtxChannel> channelObjects = new ArrayList<>();

        for (VtxChannel channel : channels) {
            JPanel cell = new JPanel(new BorderLayout(0, 2));
            cell.setBackground(COLOR_CARD_BG);

            JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
            labelPanel.setBackground(COLOR_CARD_BG);

            JLabel chLabel = new JLabel("CH" + channel.getChannelNumber());
            chLabel.setFont(chLabel.getFont().deriveFont(10f));
            chLabel.setForeground(COLOR_TEXT);
            labelPanel.add(chLabel);

            JButton btnErase = createSmallDeleteButton();
            btnErase.setToolTipText("Стереть канал (установить 0)");
            final int chNum = channel.getChannelNumber();
            final int bId = band.getIdBands();
            btnErase.addActionListener(e -> eraseChannel(bId, chNum));
            labelPanel.add(btnErase);

            cell.add(labelPanel, BorderLayout.NORTH);

            JTextField freqField = new JTextField(String.valueOf(channel.getFrequencyMhz()));
            freqField.setHorizontalAlignment(JTextField.CENTER);
            freqField.setFont(new Font("Monospaced", Font.PLAIN, 12));
            freqField.setPreferredSize(new Dimension(70, 28));
            freqField.setBorder(new LineBorder(COLOR_BORDER, 1));
            freqField.setForeground(COLOR_TEXT);
            freqField.setBackground(Color.WHITE);
            setupFrequencyFieldListener(freqField);
            fields.add(freqField);
            channelObjects.add(channel);
            cell.add(freqField, BorderLayout.CENTER);
            channelsPanel.add(cell);
        }

        channelFieldsMap.put(band.getIdBands(), fields);
        channelObjectsMap.put(band.getIdBands(), channelObjects);
        card.add(channelsPanel, BorderLayout.CENTER);
        return card;
    }

    private void buildPowerPanel() {
        if (currentPowerConfigs.isEmpty()) {
            powerGridPanel.add(new JLabel("Нет данных об уровнях мощности"));
            return;
        }
        for (VtxConfig config : currentPowerConfigs) {
            JPanel cell = new JPanel(new GridBagLayout());
            cell.setBackground(COLOR_CARD_BG);
            cell.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(COLOR_BORDER, 1), new EmptyBorder(4, 6, 4, 6)));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(1, 2, 1, 2);

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            JTextField labelField = new JTextField(config.getLabel(), 6);
            labelField.setHorizontalAlignment(JTextField.CENTER);
            labelField.setFont(new Font("Monospaced", Font.BOLD, 11));
            labelField.setBorder(new LineBorder(COLOR_BORDER, 1));
            labelField.setBackground(new Color(245, 245, 255));
            setupPowerLabelFieldListener(labelField);
            powerLabelFieldsMap.put(config.getIdConfig(), labelField);
            cell.add(labelField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            gbc.gridwidth = 1;
            JTextField valueField = new JTextField(String.valueOf(config.getValueMw()), 5);
            valueField.setHorizontalAlignment(JTextField.CENTER);
            valueField.setFont(new Font("Monospaced", Font.PLAIN, 11));
            valueField.setBorder(new LineBorder(COLOR_BORDER, 1));
            valueField.setBackground(Color.WHITE);
            setupPowerValueFieldListener(valueField);
            powerValueFieldsMap.put(config.getIdConfig(), valueField);
            cell.add(valueField, gbc);

            gbc.gridx = 1;
            JLabel unit = new JLabel("мВт");
            unit.setFont(unit.getFont().deriveFont(10f));
            cell.add(unit, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.gridwidth = 2;
            JButton btnDel = new JButton("Удалить");
            btnDel.setFont(new Font("Arial", Font.PLAIN, 9));
            btnDel.setBackground(new Color(220, 200, 200));
            btnDel.setPreferredSize(new Dimension(70, 18));
            btnDel.setFocusPainted(false);
            btnDel.addActionListener(e -> deletePowerLevel(config));
            cell.add(btnDel, gbc);

            powerGridPanel.add(cell);
        }
    }

    // --- Слушатели полей ---

    private void setupFrequencyFieldListener(JTextField field) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            private void validate() {
                SwingUtilities.invokeLater(() -> {
                    String text = field.getText().trim();
                    if (text.isEmpty()) {
                        field.setBorder(new LineBorder(COLOR_WARNING, 1));
                        return;
                    }
                    try {
                        int freq = Integer.parseInt(text);
                        if (freq >= 0 && freq <= 12000) {
                            field.setBorder(new LineBorder(COLOR_BORDER, 1));
                            field.setBackground(Color.WHITE);
                            refreshChangeStatus();
                        } else {
                            field.setBorder(new LineBorder(COLOR_DANGER, 1));
                            field.setBackground(new Color(255, 230, 230));
                        }
                    } catch (NumberFormatException e) {
                        field.setBorder(new LineBorder(COLOR_DANGER, 1));
                        field.setBackground(new Color(255, 230, 230));
                    }
                });
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                validate();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                validate();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                validate();
            }
        });
    }

    private void setupPowerValueFieldListener(JTextField field) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            private void validate() {
                SwingUtilities.invokeLater(() -> {
                    String text = field.getText().trim();
                    if (text.isEmpty()) {
                        field.setBorder(new LineBorder(COLOR_WARNING, 1));
                        return;
                    }
                    try {
                        int val = Integer.parseInt(text);
                        if (val >= 0 && val <= 2000) {
                            field.setBorder(new LineBorder(COLOR_BORDER, 1));
                            field.setBackground(Color.WHITE);
                            refreshChangeStatus();
                        } else {
                            field.setBorder(new LineBorder(COLOR_DANGER, 1));
                            field.setBackground(new Color(255, 230, 230));
                        }
                    } catch (NumberFormatException e) {
                        field.setBorder(new LineBorder(COLOR_DANGER, 1));
                        field.setBackground(new Color(255, 230, 230));
                    }
                });
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                validate();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                validate();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                validate();
            }
        });
    }

    private void setupPowerLabelFieldListener(JTextField field) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            private void check() {
                SwingUtilities.invokeLater(() -> {
                    if (!field.getText().trim().isEmpty()) {
                        field.setBorder(new LineBorder(COLOR_BORDER, 1));
                        refreshChangeStatus();
                    } else {
                        field.setBorder(new LineBorder(COLOR_WARNING, 1));
                    }
                });
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                check();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                check();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                check();
            }
        });
    }

    // --- Вспомогательные методы UI ---

    private JButton createDeleteButton() {
        JButton b = new JButton("X");
        b.setFont(new Font("Arial", Font.BOLD, 13));
        b.setForeground(COLOR_TEXT);
        b.setBackground(new Color(200, 200, 200));
        b.setPreferredSize(new Dimension(24, 24));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(new Color(160, 160, 160), 1));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setBackground(new Color(180, 180, 180));
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(new Color(200, 200, 200));
            }
        });
        return b;
    }

    private JButton createSmallDeleteButton() {
        JButton b = new JButton("0");
        b.setFont(new Font("Arial", Font.BOLD, 9));
        b.setForeground(COLOR_TEXT);
        b.setBackground(new Color(200, 200, 200));
        b.setPreferredSize(new Dimension(18, 18));
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(new Color(160, 160, 160), 1));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setBackground(new Color(180, 180, 180));
            }

            public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(new Color(200, 200, 200));
            }
        });
        return b;
    }

    private void refreshChangeStatus() {
        if (hasVtxChanges()) {
            setStatus("Есть несохранённые изменения для VTX");
        } else {
            setStatus(isConnected ? "Подключено к " + activePort : "Готово");
        }
        updateWriteButton();
    }

    private void updateWriteButton() {
        boolean canWrite = isConnected && hasVtxChanges() && !writeInProgress;
        btnWriteChanges.setEnabled(canWrite);
    }

    private void updatePitmodeState() {
        if (currentPowerConfigs.isEmpty()) {
            btnPitmode.setText("Pitmode: НЕТ ДАННЫХ");
            btnPitmode.setSelected(false);
            return;
        }
        boolean pitmode = currentPowerConfigs.get(0).isPitMode();
        btnPitmode.setSelected(pitmode);
        btnPitmode.setText(pitmode ? "Pitmode: ВКЛ" : "Pitmode: ВЫКЛ");
        btnPitmode.setBackground(pitmode ? new Color(150, 220, 150) : new Color(200, 200, 200));
    }

    private void setStatus(String message) {
        lblStatusText.setText(message);
        lblStatusText.setForeground(COLOR_TEXT);
    }

    private void setStatus(String message, Color color) {
        lblStatusText.setText(message);
        lblStatusText.setForeground(color);
    }

    // --- События ---

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == btnRefreshPorts) refreshPorts();
        else if (src == btnConnect) toggleConnection();
        else if (src == btnSync) onSync();
        else if (src == btnWriteChanges) onSave();
        else if (src == btnPitmode) onPitmodeChanged();
        else if (src == btnAddBand) onAddBand();
        else if (src == btnAddChannel) onAddColumnToAllBands();
        else if (src == btnRemoveChannel) onRemoveColumnFromAllBands();
        else if (src == btnAddPower) onAddPowerLevel();
    }

    // --- Подключение ---

    private void refreshPorts() {
        setStatus("Сканирование портов...");
        SwingWorker<String[], Void> worker = new SwingWorker<>() {
            @Override
            protected String[] doInBackground() {
                return serialConnector.getAvailablePorts().stream()
                        .map(p -> p.getSystemPortName()).toArray(String[]::new);
            }

            @Override
            protected void done() {
                try {
                    String[] ports = get();
                    portComboBox.removeAllItems();
                    for (String p : ports) portComboBox.addItem(p);
                    setStatus(ports.length > 0 ? "Найдено " + ports.length + " портов" : "Порты не найдены");
                } catch (Exception e) {
                    setStatus("Ошибка сканирования", COLOR_DANGER);
                }
            }
        };
        worker.execute();
    }

    private void toggleConnection() {
        if (isConnected) disconnect();
        else connect();
    }

    private void connect() {
        String selectedPort = (String) portComboBox.getSelectedItem();
        if (selectedPort == null || selectedPort.isEmpty()) {
            setStatus("Выберите порт", COLOR_WARNING);
            return;
        }
        btnConnect.setEnabled(false);
        setStatus("Подключение...");
        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return serialConnector.connect(selectedPort);
            }

            @Override
            protected void done() {
                try {
                    if (get()) {
                        isConnected = true;
                        activePort = selectedPort;
                        updateConnectionUI();
                        setStatus("Подключено к " + activePort);
                        lblStatusIndicator.setForeground(COLOR_SUCCESS);
                    } else {
                        setStatus("Ошибка подключения", COLOR_DANGER);
                    }
                } catch (Exception e) {
                    setStatus("Ошибка: " + e.getMessage(), COLOR_DANGER);
                }
                btnConnect.setEnabled(true);
            }
        };
        worker.execute();
    }

    private void disconnect() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                serialConnector.disconnect();
                return null;
            }

            @Override
            protected void done() {
                isConnected = false;
                activePort = null;
                updateConnectionUI();
                setStatus("Отключено");
                lblStatusIndicator.setForeground(COLOR_DANGER);
            }
        };
        worker.execute();
    }

    private void updateConnectionUI() {
        if (isConnected) {
            btnConnect.setText("Отключиться");
            portComboBox.setEnabled(false);
            btnRefreshPorts.setEnabled(false);
            btnSync.setEnabled(true);
        } else {
            btnConnect.setText("Подключиться");
            portComboBox.setEnabled(true);
            btnRefreshPorts.setEnabled(true);
            btnSync.setEnabled(false);
        }
        updateWriteButton();
    }

    // --- Синхронизация ---

    private void onSync() {
        if (!isConnected) {
            setStatus("Устройство не подключено!", COLOR_DANGER);
            return;
        }
        btnSync.setEnabled(false);
        btnWriteChanges.setEnabled(false);
        setStatus("Синхронизация с контроллером...");
        SwingWorker<SerialConnector.SyncResult, Void> worker = new SwingWorker<>() {
            @Override
            protected SerialConnector.SyncResult doInBackground() {
                return serialConnector.syncVtxTable();
            }

            @Override
            protected void done() {
                try {
                    SerialConnector.SyncResult result = get();
                    if (result.success) {
                        loadDataFromDatabase(true);
                        setStatus("Синхронизация завершена — " + result.foundBands.size() + " бэндов", COLOR_SUCCESS);
                    } else {
                        setStatus("Ошибка синхронизации: " + result.message, COLOR_DANGER);
                        JOptionPane.showMessageDialog(MainFrame.this,
                                "Ошибка синхронизации:\n" + result.message, "Ошибка", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    setStatus("Ошибка: " + e.getMessage(), COLOR_DANGER);
                } finally {
                    btnSync.setEnabled(isConnected);
                    updateWriteButton();
                }
            }
        };
        worker.execute();
    }

    // --- Запись в VTX ---

    private void onSave() {
        if (!isConnected) {
            setStatus("Устройство не подключено!", COLOR_DANGER);
            return;
        }
        if (!hasVtxChanges()) {
            setStatus("Нет изменений для записи");
            return;
        }

        List<String> cmds = collectCommandsFromUI();
        if (cmds.isEmpty()) {
            setStatus("Нет изменений для записи в VTX");
            refreshChangeStatus();
            return;
        }

        DbSnapshot dbSnapshot = captureDbSnapshot();
        writeInProgress = true;
        btnWriteChanges.setEnabled(false);
        btnSync.setEnabled(false);
        setStatus("Запись " + cmds.size() + " команд в VTX...");

        SwingWorker<SerialConnector.WriteResult, Void> worker = new SwingWorker<>() {
            @Override
            protected SerialConnector.WriteResult doInBackground() {
                return serialConnector.writeIncrementalCommands(cmds, activePort);
            }

            @Override
            protected void done() {
                writeInProgress = false;
                try {
                    SerialConnector.WriteResult wr = get();
                    if (wr.success) {
                        VtxBaseline fromDevice = parseBaselineFromResponse(wr.vtxTableResponse);
                        VtxBaseline expected = captureBaselineFromUIState();
                        if (!fromDevice.matches(expected)) {
                            restoreDbSnapshot(dbSnapshot);
                            loadDataFromDatabase(false);
                            setStatus("Верификация не прошла", COLOR_DANGER);
                            JOptionPane.showMessageDialog(MainFrame.this,
                                    "Данные записаны, но vtxtable на контроллере не совпадает с UI.",
                                    "Ошибка", JOptionPane.ERROR_MESSAGE);
                            return;
                        }
                        saveChangesToDatabase();
                        vtxBaseline = captureBaselineFromUIState();
                        isConnected = serialConnector.isConnected();
                        setStatus("Успешно записано и верифицировано!", COLOR_SUCCESS);
                        JOptionPane.showMessageDialog(MainFrame.this,
                                "Изменения успешно записаны в VTX и проверены.", "Успех",
                                JOptionPane.INFORMATION_MESSAGE);
                        loadDataFromDatabase(false);
                    } else {
                        restoreDbSnapshot(dbSnapshot);
                        loadDataFromDatabase(false);
                        setStatus("Ошибка записи: " + wr.error, COLOR_DANGER);
                        JOptionPane.showMessageDialog(MainFrame.this,
                                "Ошибка при записи:\n" + wr.error, "Ошибка", JOptionPane.ERROR_MESSAGE);
                        isConnected = serialConnector.isConnected();
                    }
                } catch (Exception e) {
                    restoreDbSnapshot(dbSnapshot);
                    loadDataFromDatabase(false);
                    setStatus("Ошибка: " + e.getMessage(), COLOR_DANGER);
                } finally {
                    btnSync.setEnabled(isConnected);
                    refreshChangeStatus();
                }
            }
        };
        worker.execute();
    }

    /**
     * Собирает инкрементальные CLI-команды, сравнивая UI с последним подтверждённым состоянием VTX.
     */
    private List<String> collectCommandsFromUI() {
        List<String> commands = new ArrayList<>();
        UiState ui = readUiState();

        boolean bandsCountChanged = ui.bands.size() != vtxBaseline.bands.size();
        boolean channelsCountChanged = ui.channelCount != vtxBaseline.channelCount;

        if (bandsCountChanged) {
            commands.add("vtxtable bands " + ui.bands.size());
        }
        if (channelsCountChanged) {
            commands.add("vtxtable channels " + ui.channelCount);
        }

        if (bandsCountChanged && ui.bands.size() < vtxBaseline.bands.size()) {
            for (BandState band : ui.bands) {
                commands.add(buildBandCommand(band));
            }
        } else {
            for (BandState band : ui.bands) {
                BandState base = vtxBaseline.findBand(band.bandNumber);
                if (base == null || !band.sameFrequencies(base) || channelsCountChanged) {
                    commands.add(buildBandCommand(band));
                }
            }
        }

        if (!ui.powerLevels.equals(vtxBaseline.powerLevels)) {
            commands.add("vtxtable powerlevels " + ui.powerLevels.size());
            StringBuilder valCmd = new StringBuilder("vtxtable powervalues");
            StringBuilder lblCmd = new StringBuilder("vtxtable powerlabels");
            for (PowerState ps : ui.powerLevels) {
                valCmd.append(" ").append(ps.valueMw);
                lblCmd.append(" ").append(ps.label);
            }
            commands.add(valCmd.toString());
            commands.add(lblCmd.toString());
        }

        return commands;
    }

    private String buildBandCommand(BandState band) {
        StringBuilder cmd = new StringBuilder("vtxtable band ")
                .append(band.bandNumber).append(" ")
                .append(band.bandName).append(" ")
                .append(band.bandLetter).append(" CUSTOM");
        for (int freq : band.frequencies) {
            cmd.append(" ").append(freq);
        }
        return cmd.toString();
    }

    private void saveChangesToDatabase() {
        UiState ui = readUiState();

        for (BandState bandState : ui.bands) {
            VtxBand band = findBandByNumber(bandState.bandNumber);
            if (band == null) continue;
            List<VtxChannel> channels = channelObjectsMap.get(band.getIdBands());
            List<JTextField> fields = channelFieldsMap.get(band.getIdBands());
            if (channels == null || fields == null) continue;
            for (int i = 0; i < Math.min(channels.size(), fields.size()); i++) {
                try {
                    int nf = Integer.parseInt(fields.get(i).getText().trim());
                    VtxChannel ch = channels.get(i);
                    if (nf != ch.getFrequencyMhz()) {
                        dao.updateChannelFrequency(ch.getIdChannels(), nf);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        for (int i = 0; i < currentPowerConfigs.size() && i < ui.powerLevels.size(); i++) {
            VtxConfig cfg = currentPowerConfigs.get(i);
            PowerState ps = ui.powerLevels.get(i);
            if (ps.valueMw != cfg.getValueMw() || !ps.label.equals(cfg.getLabel())) {
                dao.updateConfigPower(cfg.getIdConfig(), ps.valueMw, ps.label);
            }
        }
    }

    // --- Pitmode ---

    private void onPitmodeChanged() {
        boolean pitmode = btnPitmode.isSelected();
        btnPitmode.setText(pitmode ? "Pitmode: ВКЛ" : "Pitmode: ВЫКЛ");
        btnPitmode.setBackground(pitmode ? new Color(150, 220, 150) : new Color(200, 200, 200));
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                for (VtxBand band : currentBands) dao.updatePitmode(band.getIdBands(), pitmode);
                return null;
            }

            @Override
            protected void done() {
                setStatus("Pitmode " + (pitmode ? "включён" : "выключен"));
            }
        };
        worker.execute();
    }

    // --- Структурные изменения ---

    private void onAddBand() {
        int channelsCount = vtxBaseline.channelCount > 0 ? vtxBaseline.channelCount : 8;
        if (!currentBands.isEmpty()) {
            List<VtxChannel> ex = dao.getChannelsByBandId(currentBands.get(0).getIdBands());
            if (!ex.isEmpty()) channelsCount = ex.size();
        }

        String name = JOptionPane.showInputDialog(this, "Имя новой сетки (например, BAND_X):");
        if (name == null || name.trim().isEmpty()) return;
        name = name.trim().toUpperCase();

        String letterStr = JOptionPane.showInputDialog(this, "Буква сетки (A-Z):");
        if (letterStr == null || letterStr.trim().isEmpty()) return;
        char letter = letterStr.trim().toUpperCase().charAt(0);
        if (letter < 'A' || letter > 'Z') {
            setStatus("Буква должна быть A-Z", COLOR_DANGER);
            return;
        }
        for (VtxBand b : currentBands) {
            if (b.getBandLetter() == letter) {
                setStatus("Буква '" + letter + "' уже используется", COLOR_DANGER);
                return;
            }
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Будет добавлен бэнд " + name + " (" + letter + ") с " + channelsCount + " каналами.\n" +
                        "Все частоты будут инициализированы нулями.\n\nПродолжить?",
                "Добавление бэнда", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        int bandNumber = currentBands.size() + 1;
        String finalName = name;
        char finalLetter = letter;
        int finalChannels = channelsCount;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                VtxBand newBand = new VtxBand(bandNumber, finalName, finalLetter);
                dao.saveBand(newBand);
                int bandId = newBand.getIdBands();
                for (int i = 1; i <= finalChannels; i++) dao.saveChannel(new VtxChannel(bandId, i, 0));
                if (!currentBands.isEmpty()) {
                    int refId = currentBands.get(0).getIdBands();
                    for (VtxConfig cfg : dao.getConfigByBandId(refId)) {
                        dao.saveConfig(new VtxConfig(bandId, cfg.getLevelIndex(), cfg.getLabel(), cfg.getValueMw(), cfg.isPitMode()));
                    }
                } else {
                    dao.saveConfig(new VtxConfig(bandId, 1, "MIN", 25, false));
                    dao.saveConfig(new VtxConfig(bandId, 2, "MAX", 800, false));
                }
                return null;
            }

            @Override
            protected void done() {
                loadDataFromDatabase(false);
                setStatus("Добавлен бэнд " + finalName + " — запишите в VTX");
                if (isConnected) onSave();
            }
        };
        worker.execute();
    }

    private void deleteBand(VtxBand band) {
        if (currentBands.size() <= 1) {
            setStatus("Нельзя удалить последний бэнд", COLOR_WARNING);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Удалить бэнд " + band.getBandName() + " (" + band.getBandLetter() + ")?\n" +
                        "Оставшиеся бэнды будут перенумерованы.",
                "Удаление бэнда", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                dao.deleteBand(band.getIdBands());
                List<VtxBand> remaining = dao.getAllBands();
                for (int i = 0; i < remaining.size(); i++) {
                    VtxBand b = remaining.get(i);
                    b.setBandNumber(i + 1);
                    dao.saveBand(b);
                }
                return null;
            }

            @Override
            protected void done() {
                loadDataFromDatabase(false);
                setStatus("Бэнд " + band.getBandName() + " удалён — запишите в VTX");
                if (isConnected) onSave();
            }
        };
        worker.execute();
    }

    private void eraseChannel(int bandId, int channelNumber) {
        List<JTextField> fields = channelFieldsMap.get(bandId);
        if (fields == null || channelNumber - 1 >= fields.size()) return;
        JTextField target = fields.get(channelNumber - 1);
        target.setText("0");
        target.setBorder(new LineBorder(COLOR_BORDER, 1));
        target.setBackground(Color.WHITE);
        refreshChangeStatus();
        setStatus("Канал CH" + channelNumber + " обнулён");
        if (isConnected) onSave();
    }

    private void onAddColumnToAllBands() {
        if (currentBands.isEmpty()) {
            setStatus("Сначала создайте бэнд", COLOR_WARNING);
            return;
        }

        int currentChannels = dao.getChannelsByBandId(currentBands.get(0).getIdBands()).size();
        int newCount = currentChannels + 1;

        int confirm = JOptionPane.showConfirmDialog(this,
                "Добавить канал CH" + newCount + " во все бэнды?\n" +
                        "Количество каналов: " + currentChannels + " → " + newCount + "\n" +
                        "Новый канал будет инициализирован нулём.",
                "Добавление канала", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                for (VtxBand band : currentBands) dao.saveChannel(new VtxChannel(band.getIdBands(), newCount, 0));
                return null;
            }

            @Override
            protected void done() {
                loadDataFromDatabase(false);
                setStatus("Добавлен CH" + newCount + " — запишите в VTX");
                if (isConnected) onSave();
            }
        };
        worker.execute();
    }

    private void onRemoveColumnFromAllBands() {
        if (currentBands.isEmpty()) {
            setStatus("Сначала создайте бэнд", COLOR_WARNING);
            return;
        }

        int currentChannels = dao.getChannelsByBandId(currentBands.get(0).getIdBands()).size();
        if (currentChannels <= 1) {
            setStatus("Нельзя убрать последний канал", COLOR_WARNING);
            return;
        }
        int newCount = currentChannels - 1;

        int confirm = JOptionPane.showConfirmDialog(this,
                "Убрать последний канал CH" + currentChannels + " из всех бэндов?\n" +
                        "Количество каналов: " + currentChannels + " → " + newCount,
                "Уменьшение каналов", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                for (VtxBand band : currentBands) {
                    List<VtxChannel> channels = dao.getChannelsByBandId(band.getIdBands());
                    if (!channels.isEmpty()) {
                        VtxChannel last = channels.get(channels.size() - 1);
                        dao.deleteChannel(last.getIdChannels());
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                loadDataFromDatabase(false);
                setStatus("Канал CH" + currentChannels + " убран — запишите в VTX");
                if (isConnected) onSave();
            }
        };
        worker.execute();
    }

    private void onAddPowerLevel() {
        if (currentBands.isEmpty()) {
            setStatus("Сначала создайте бэнд", COLOR_WARNING);
            return;
        }

        String label = JOptionPane.showInputDialog(this, "Метка уровня мощности (например, MED):");
        if (label == null || label.trim().isEmpty()) return;
        label = label.trim().toUpperCase();

        String valueStr = JOptionPane.showInputDialog(this, "Мощность (мВт, 0-2000):");
        if (valueStr == null || valueStr.trim().isEmpty()) return;

        int value;
        try {
            value = Integer.parseInt(valueStr.trim());
            if (value < 0 || value > 2000) {
                setStatus("Мощность должна быть 0-2000 мВт", COLOR_DANGER);
                return;
            }
        } catch (NumberFormatException e) {
            setStatus("Введите число", COLOR_DANGER);
            return;
        }

        int levelIndex = currentPowerConfigs.size() + 1;
        String finalLabel = label;
        int finalValue = value;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                for (VtxBand band : currentBands) {
                    dao.saveConfig(new VtxConfig(band.getIdBands(), levelIndex, finalLabel, finalValue, false));
                }
                return null;
            }

            @Override
            protected void done() {
                loadDataFromDatabase(false);
                setStatus("Добавлен уровень " + finalLabel + " — запишите в VTX");
                if (isConnected) onSave();
            }
        };
        worker.execute();
    }

    private void deletePowerLevel(VtxConfig config) {
        if (currentPowerConfigs.size() <= 1) {
            setStatus("Нельзя удалить последний уровень мощности", COLOR_WARNING);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Удалить уровень мощности " + config.getLabel() + " (" + config.getValueMw() + " мВт)?",
                "Удаление уровня мощности", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                int targetLevel = config.getLevelIndex();
                for (VtxBand band : currentBands) {
                    for (VtxConfig cfg : dao.getConfigByBandId(band.getIdBands())) {
                        if (cfg.getLevelIndex() == targetLevel) dao.deleteConfig(cfg.getIdConfig());
                    }
                }
                for (VtxBand band : currentBands) {
                    List<VtxConfig> remaining = dao.getConfigByBandId(band.getIdBands());
                    for (int i = 0; i < remaining.size(); i++) {
                        VtxConfig cfg = remaining.get(i);
                        cfg.setLevelIndex(i + 1);
                        dao.saveConfig(cfg);
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                loadDataFromDatabase(false);
                setStatus("Уровень " + config.getLabel() + " удалён — запишите в VTX");
                if (isConnected) onSave();
            }
        };
        worker.execute();
    }

    // --- Снимки состояния ---

    private boolean hasVtxChanges() {
        return !readUiState().matches(vtxBaseline);
    }

    private UiState readUiState() {
        UiState state = new UiState();
        for (VtxBand band : currentBands) {
            BandState bs = new BandState();
            bs.bandNumber = band.getBandNumber();
            bs.bandName = band.getBandName();
            bs.bandLetter = band.getBandLetter();
            List<JTextField> fields = channelFieldsMap.get(band.getIdBands());
            if (fields != null) {
                for (JTextField f : fields) {
                    try {
                        bs.frequencies.add(Integer.parseInt(f.getText().trim()));
                    } catch (NumberFormatException e) {
                        bs.frequencies.add(0);
                    }
                }
            }
            state.bands.add(bs);
            if (bs.frequencies.size() > state.channelCount) {
                state.channelCount = bs.frequencies.size();
            }
        }

        for (VtxConfig cfg : currentPowerConfigs) {
            PowerState ps = new PowerState();
            JTextField vf = powerValueFieldsMap.get(cfg.getIdConfig());
            JTextField lf = powerLabelFieldsMap.get(cfg.getIdConfig());
            ps.valueMw = cfg.getValueMw();
            ps.label = cfg.getLabel();
            if (vf != null) {
                try {
                    ps.valueMw = Integer.parseInt(vf.getText().trim());
                } catch (NumberFormatException ignored) {
                }
            }
            if (lf != null && !lf.getText().trim().isEmpty()) {
                ps.label = lf.getText().trim();
            }
            state.powerLevels.add(ps);
        }
        return state;
    }

    private VtxBaseline captureBaselineFromCurrentState() {
        VtxBaseline baseline = new VtxBaseline();
        for (VtxBand band : currentBands) {
            BandState bs = new BandState();
            bs.bandNumber = band.getBandNumber();
            bs.bandName = band.getBandName();
            bs.bandLetter = band.getBandLetter();
            for (VtxChannel ch : dao.getChannelsByBandId(band.getIdBands())) {
                bs.frequencies.add(ch.getFrequencyMhz());
            }
            baseline.bands.add(bs);
            if (bs.frequencies.size() > baseline.channelCount) {
                baseline.channelCount = bs.frequencies.size();
            }
        }
        if (!currentPowerConfigs.isEmpty()) {
            for (VtxConfig cfg : currentPowerConfigs) {
                baseline.powerLevels.add(new PowerState(cfg.getLabel(), cfg.getValueMw()));
            }
        }
        return baseline;
    }

    private VtxBaseline captureBaselineFromUIState() {
        UiState ui = readUiState();
        VtxBaseline baseline = new VtxBaseline();
        baseline.bands.addAll(ui.bands);
        baseline.channelCount = ui.channelCount;
        baseline.powerLevels.addAll(ui.powerLevels);
        return baseline;
    }

    private VtxBaseline parseBaselineFromResponse(String response) {
        ConfigParser.ParseResult parsed = parser.parseVtxTableResponse(response, dao.getAllBands());
        VtxBaseline baseline = new VtxBaseline();
        baseline.channelCount = parsed.totalChannels;
        for (VtxBand band : parsed.bands) {
            BandState bs = new BandState();
            bs.bandNumber = band.getBandNumber();
            bs.bandName = band.getBandName();
            bs.bandLetter = band.getBandLetter();
            List<VtxChannel> channels = parsed.channelsMap.get(band);
            if (channels != null) {
                for (VtxChannel ch : channels) {
                    bs.frequencies.add(ch.getFrequencyMhz());
                }
            }
            baseline.bands.add(bs);
        }
        if (!parsed.bands.isEmpty()) {
            List<VtxConfig> configs = parsed.configsMap.get(parsed.bands.get(0));
            if (configs != null) {
                for (VtxConfig cfg : configs) {
                    baseline.powerLevels.add(new PowerState(cfg.getLabel(), cfg.getValueMw()));
                }
            }
        }
        return baseline;
    }

    private VtxBand findBandByNumber(int bandNumber) {
        for (VtxBand b : currentBands) {
            if (b.getBandNumber() == bandNumber) return b;
        }
        return null;
    }

    private DbSnapshot captureDbSnapshot() {
        DbSnapshot snap = new DbSnapshot();
        snap.bands = new ArrayList<>(dao.getAllBands());
        for (VtxBand band : snap.bands) {
            snap.channels.put(band.getIdBands(), new ArrayList<>(dao.getChannelsByBandId(band.getIdBands())));
            snap.configs.put(band.getIdBands(), new ArrayList<>(dao.getConfigByBandId(band.getIdBands())));
        }
        return snap;
    }

    private void restoreDbSnapshot(DbSnapshot snap) {
        List<VtxBand> current = dao.getAllBands();
        for (VtxBand b : current) {
            if (snap.bands.stream().noneMatch(s -> s.getIdBands() == b.getIdBands())) {
                dao.deleteBand(b.getIdBands());
            }
        }
        for (VtxBand band : snap.bands) {
            dao.saveBand(band);
            dao.replaceChannelsForBand(band.getIdBands(), snap.channels.getOrDefault(band.getIdBands(), new ArrayList<>()));
            dao.replaceConfigsForBand(band.getIdBands(), snap.configs.getOrDefault(band.getIdBands(), new ArrayList<>()));
        }
    }

    // --- Внутренние модели состояния ---

    private static class DbSnapshot {
        List<VtxBand> bands = new ArrayList<>();
        Map<Integer, List<VtxChannel>> channels = new HashMap<>();
        Map<Integer, List<VtxConfig>> configs = new HashMap<>();
    }

    private static class PowerState {
        String label;
        int valueMw;

        PowerState() {
        }

        PowerState(String label, int valueMw) {
            this.label = label;
            this.valueMw = valueMw;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PowerState)) return false;
            PowerState that = (PowerState) o;
            return valueMw == that.valueMw && Objects.equals(label, that.label);
        }

        @Override
        public int hashCode() {
            return Objects.hash(label, valueMw);
        }
    }

    private static class BandState {
        int bandNumber;
        String bandName;
        char bandLetter;
        List<Integer> frequencies = new ArrayList<>();

        boolean sameFrequencies(BandState other) {
            return frequencies.equals(other.frequencies);
        }
    }

    private static class UiState {
        List<BandState> bands = new ArrayList<>();
        int channelCount;
        List<PowerState> powerLevels = new ArrayList<>();

        boolean matches(VtxBaseline baseline) {
            if (bands.size() != baseline.bands.size()) return false;
            if (channelCount != baseline.channelCount) return false;
            if (!powerLevels.equals(baseline.powerLevels)) return false;
            for (BandState a : bands) {
                BandState b = baseline.findBand(a.bandNumber);
                if (b == null) return false;
                if (!a.bandName.equals(b.bandName)) return false;
                if (a.bandLetter != b.bandLetter) return false;
                if (!a.frequencies.equals(b.frequencies)) return false;
            }
            return true;
        }
    }

    private static class VtxBaseline {
        List<BandState> bands = new ArrayList<>();
        int channelCount;
        List<PowerState> powerLevels = new ArrayList<>();

        BandState findBand(int bandNumber) {
            for (BandState b : bands) {
                if (b.bandNumber == bandNumber) return b;
            }
            return null;
        }

        boolean matches(VtxBaseline other) {
            if (bands.size() != other.bands.size()) return false;
            if (channelCount != other.channelCount) return false;
            if (!powerLevels.equals(other.powerLevels)) return false;
            for (BandState a : bands) {
                BandState b = other.findBand(a.bandNumber);
                if (b == null) return false;
                if (!a.bandName.equals(b.bandName)) return false;
                if (a.bandLetter != b.bandLetter) return false;
                if (!a.frequencies.equals(b.frequencies)) return false;
            }
            return true;
        }
    }
}
