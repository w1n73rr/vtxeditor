package org.example.ui;

import org.example.database.VtxDao;
import org.example.model.VtxBand;
import org.example.model.VtxChannel;
import org.example.model.VtxConfig;
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
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Главное окно приложения VTX Config Manager.
 * Реализовано вручную с использованием Swing.
 * Светлая тема с хорошей контрастностью.
 */
public class MainFrame extends JFrame implements ActionListener {

    // Цветовая палитра
    private static final Color COLOR_BG = new Color(240, 242, 245);
    private static final Color COLOR_CARD_BG = Color.WHITE;
    private static final Color COLOR_TEXT = Color.BLACK;
    private static final Color COLOR_BORDER = new Color(180, 180, 190);
    private static final Color COLOR_PRIMARY = new Color(0, 102, 204);
    private static final Color COLOR_SUCCESS = new Color(0, 150, 80);
    private static final Color COLOR_DANGER = new Color(200, 40, 40);
    private static final Color COLOR_WARNING = new Color(200, 150, 0);
    private static final Color COLOR_ERROR_BG = new Color(255, 230, 230);

    // Сервисы
    private final VtxDao dao;
    private final SerialConnector serialConnector;

    // Компоненты UI
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
    private JButton btnAddPower;

    // Хранилища для полей
    private final Map<Integer, List<JTextField>> channelFieldsMap = new HashMap<>();
    private final Map<Integer, JTextField> powerFieldsMap = new HashMap<>();

    // Данные
    private List<VtxBand> currentBands = new ArrayList<>();
    private Map<Integer, List<VtxChannel>> channelsByBand = new HashMap<>();
    private Map<Integer, List<VtxConfig>> configsByBand = new HashMap<>();

    // Состояние
    private boolean isConnected = false;
    private String activePort = null;

    public MainFrame(VtxDao dao, SerialConnector serialConnector) {
        this.dao = dao;
        this.serialConnector = serialConnector;

        setTitle("VTX Config Manager");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1150, 750);
        setMinimumSize(new Dimension(900, 550));
        setLocationRelativeTo(null);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        initUI();

        // Автоматическое сканирование портов при запуске
        refreshPorts();

        loadDataFromDatabase();
    }

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

        // Метка "COM-порт:"
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.0;
        JLabel lblPort = new JLabel("COM-порт:");
        lblPort.setFont(lblPort.getFont().deriveFont(Font.BOLD));
        lblPort.setForeground(COLOR_TEXT);
        panel.add(lblPort, gbc);

        // Выпадающий список
        gbc.gridx = 1;
        gbc.weightx = 0.2;
        portComboBox = new JComboBox<>();
        portComboBox.setPreferredSize(new Dimension(150, 28));
        portComboBox.setBackground(Color.WHITE);
        portComboBox.setForeground(COLOR_TEXT);
        panel.add(portComboBox, gbc);

        // Кнопка "Обновить"
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        btnRefreshPorts = new JButton("Обновить");
        btnRefreshPorts.setForeground(COLOR_TEXT);
        btnRefreshPorts.addActionListener(this);
        panel.add(btnRefreshPorts, gbc);

        // Отступ
        gbc.gridx = 3;
        gbc.weightx = 0.05;
        panel.add(Box.createHorizontalStrut(10), gbc);

        // Индикатор статуса
        gbc.gridx = 4;
        gbc.weightx = 0.0;
        lblStatusIndicator = new JLabel("●");
        lblStatusIndicator.setForeground(COLOR_DANGER);
        lblStatusIndicator.setFont(new Font("Segoe UI", Font.BOLD, 18));
        panel.add(lblStatusIndicator, gbc);

        // Текст статуса
        gbc.gridx = 5;
        gbc.weightx = 0.5;
        lblStatusText = new JLabel("Отключено");
        lblStatusText.setForeground(COLOR_TEXT);
        lblStatusText.setFont(lblStatusText.getFont().deriveFont(Font.BOLD));
        lblStatusText.setPreferredSize(new Dimension(200, 20));
        panel.add(lblStatusText, gbc);

        // Кнопка подключения - справа с принудительным выравниванием
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
                new LineBorder(COLOR_BORDER, 1),
                " СЕТКИ ЧАСТОТ (МГц) ",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                COLOR_TEXT
        ));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(COLOR_BG);
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel createSouthPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(COLOR_BG);

        // Панель мощностей
        JPanel powerPanel = new JPanel(new BorderLayout(10, 5));
        powerPanel.setBackground(COLOR_CARD_BG);
        powerPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_BORDER, 1),
                new EmptyBorder(10, 15, 10, 15)
        ));

        JLabel powerTitle = new JLabel("Уровни мощности:");
        powerTitle.setFont(powerTitle.getFont().deriveFont(Font.BOLD));
        powerTitle.setForeground(COLOR_TEXT);
        powerPanel.add(powerTitle, BorderLayout.NORTH);

        powerGridPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 5));
        powerGridPanel.setBackground(COLOR_CARD_BG);
        powerPanel.add(powerGridPanel, BorderLayout.CENTER);

        // Pitmode - используем кнопку-переключатель
        btnPitmode = new JToggleButton("Pitmode: ВЫКЛ");
        btnPitmode.setForeground(COLOR_TEXT);
        btnPitmode.setBackground(new Color(200, 200, 200));
        btnPitmode.setFont(btnPitmode.getFont().deriveFont(Font.BOLD));
        btnPitmode.addActionListener(this);
        powerPanel.add(btnPitmode, BorderLayout.SOUTH);

        panel.add(powerPanel, BorderLayout.CENTER);

        // Кнопки управления
        JPanel buttonPanel = new JPanel(new GridBagLayout());
        buttonPanel.setBackground(COLOR_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Ряд 1: Кнопки добавления
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
        btnAddPower = new JButton("+ Добавить уровень мощности");
        btnAddPower.setForeground(COLOR_TEXT);
        btnAddPower.setBackground(new Color(220, 220, 220));
        btnAddPower.addActionListener(this);
        buttonPanel.add(btnAddPower, gbc);

        // Ряд 2: Кнопки синхронизации и записи
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        actionPanel.setBackground(COLOR_BG);

        btnSync = new JButton("Синхронизировать с устройством");
        btnSync.setForeground(COLOR_TEXT);
        btnSync.setBackground(new Color(200, 200, 200));
        btnSync.setEnabled(false);
        btnSync.addActionListener(this);
        actionPanel.add(btnSync);

        btnWriteChanges = new JButton("Записать в VTX и сохранить");
        btnWriteChanges.setForeground(COLOR_TEXT);
        btnWriteChanges.setBackground(new Color(200, 200, 200));
        btnWriteChanges.setEnabled(false);
        btnWriteChanges.addActionListener(this);
        actionPanel.add(btnWriteChanges);

        buttonPanel.add(actionPanel, gbc);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void loadDataFromDatabase() {
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                currentBands = dao.getAllBands();
                channelsByBand.clear();
                configsByBand.clear();

                for (VtxBand band : currentBands) {
                    int bandId = band.getIdBands();
                    channelsByBand.put(bandId, dao.getChannelsByBandId(bandId));
                    configsByBand.put(bandId, dao.getConfigByBandId(bandId));
                }
                return null;
            }

            @Override
            protected void done() {
                rebuildUI();
            }
        };
        worker.execute();
    }

    private void rebuildUI() {
        centerPanel.removeAll();
        channelFieldsMap.clear();
        powerGridPanel.removeAll();
        powerFieldsMap.clear();

        // Строим сетки частот
        if (currentBands.isEmpty()) {
            JLabel emptyLabel = new JLabel("Нет сеток частот. Нажмите '+ Добавить бэнд' для создания.");
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
                JPanel bandCard = createBandCard(band);
                centerPanel.add(bandCard);
                centerPanel.add(Box.createVerticalStrut(8));
            }
        }

        // Строим мощности
        buildPowerPanel();

        // Обновляем Pitmode
        updatePitmodeState();

        centerPanel.revalidate();
        centerPanel.repaint();
        powerGridPanel.revalidate();
        powerGridPanel.repaint();
    }

    private JPanel createBandCard(VtxBand band) {
        JPanel card = new JPanel(new BorderLayout(10, 5));
        card.setBackground(COLOR_CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(COLOR_BORDER, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));

        // Название сетки с кнопкой удаления
        JPanel namePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        namePanel.setBackground(COLOR_CARD_BG);

        String bandLabel = String.format("%s (%c)", band.getBandName(), band.getBandLetter());
        JLabel nameLabel = new JLabel(bandLabel);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 14f));
        nameLabel.setForeground(COLOR_TEXT);
        nameLabel.setPreferredSize(new Dimension(130, 30));
        namePanel.add(nameLabel);

        // Кнопка удаления бэнда
        JButton btnDeleteBand = createDeleteButton();
        btnDeleteBand.addActionListener(e -> deleteBand(band));
        namePanel.add(btnDeleteBand);

        card.add(namePanel, BorderLayout.WEST);

        // Каналы
        List<VtxChannel> channels = channelsByBand.get(band.getIdBands());
        if (channels == null) channels = new ArrayList<>();

        JPanel channelsPanel = new JPanel(new GridLayout(1, Math.max(channels.size(), 1), 6, 0));
        channelsPanel.setBackground(COLOR_CARD_BG);

        List<JTextField> fields = new ArrayList<>();

        for (VtxChannel channel : channels) {
            JPanel cell = new JPanel(new BorderLayout(0, 2));
            cell.setBackground(COLOR_CARD_BG);

            // Метка канала с кнопкой удаления
            JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
            labelPanel.setBackground(COLOR_CARD_BG);

            JLabel channelLabel = new JLabel("CH" + channel.getChannelNumber());
            channelLabel.setFont(channelLabel.getFont().deriveFont(10f));
            channelLabel.setForeground(COLOR_TEXT);
            labelPanel.add(channelLabel);

            // Маленькая кнопка удаления канала
            JButton btnDeleteChannel = createSmallDeleteButton();
            btnDeleteChannel.addActionListener(e -> deleteChannel(channel));
            labelPanel.add(btnDeleteChannel);

            cell.add(labelPanel, BorderLayout.NORTH);

            JTextField freqField = new JTextField(String.valueOf(channel.getFrequencyMhz()));
            freqField.setHorizontalAlignment(JTextField.CENTER);
            freqField.setFont(new Font("Monospaced", Font.PLAIN, 12));
            freqField.setPreferredSize(new Dimension(70, 28));
            freqField.setBorder(new LineBorder(COLOR_BORDER, 1));
            freqField.setForeground(COLOR_TEXT);
            freqField.setBackground(Color.WHITE);

            setupFrequencyField(freqField, channel);
            fields.add(freqField);

            cell.add(freqField, BorderLayout.CENTER);
            channelsPanel.add(cell);
        }

        channelFieldsMap.put(band.getIdBands(), fields);
        card.add(channelsPanel, BorderLayout.CENTER);

        return card;
    }

    private JButton createDeleteButton() {
        JButton button = new JButton("X");
        button.setFont(new Font("Arial", Font.BOLD, 13));
        button.setForeground(COLOR_TEXT);
        button.setBackground(new Color(200, 200, 200));
        button.setPreferredSize(new Dimension(24, 24));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(new Color(160, 160, 160), 1));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(180, 180, 180));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(200, 200, 200));
            }
        });

        return button;
    }

    private JButton createSmallDeleteButton() {
        JButton button = new JButton("X");
        button.setFont(new Font("Arial", Font.BOLD, 9));
        button.setForeground(COLOR_TEXT);
        button.setBackground(new Color(200, 200, 200));
        button.setPreferredSize(new Dimension(18, 18));
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(new Color(160, 160, 160), 1));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(180, 180, 180));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(200, 200, 200));
            }
        });

        return button;
    }

    private void setupFrequencyField(JTextField field, VtxChannel channel) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            private void validate() {
                SwingUtilities.invokeLater(() -> {
                    String text = field.getText().trim();
                    if (text.isEmpty()) {
                        field.setBorder(new LineBorder(COLOR_WARNING, 1));
                        field.setBackground(Color.WHITE);
                        return;
                    }
                    try {
                        int freq = Integer.parseInt(text);
                        if (freq >= 1000 && freq <= 12000) {
                            field.setBorder(new LineBorder(COLOR_BORDER, 1));
                            field.setBackground(Color.WHITE);
                        } else {
                            field.setBorder(new LineBorder(COLOR_DANGER, 1));
                            field.setBackground(COLOR_ERROR_BG);
                        }
                    } catch (NumberFormatException e) {
                        field.setBorder(new LineBorder(COLOR_DANGER, 1));
                        field.setBackground(COLOR_ERROR_BG);
                    }
                });
            }

            @Override public void insertUpdate(DocumentEvent e) { validate(); }
            @Override public void removeUpdate(DocumentEvent e) { validate(); }
            @Override public void changedUpdate(DocumentEvent e) { validate(); }
        });

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                saveFrequency(field, channel);
            }
        });

        field.addActionListener(e -> {
            saveFrequency(field, channel);
            field.transferFocus();
        });
    }

    private void saveFrequency(JTextField field, VtxChannel channel) {
        String text = field.getText().trim();
        if (text.isEmpty()) {
            field.setText(String.valueOf(channel.getFrequencyMhz()));
            return;
        }

        try {
            int freq = Integer.parseInt(text);
            if (freq >= 1000 && freq <= 12000) {
                if (freq != channel.getFrequencyMhz()) {
                    SwingWorker<Void, Void> worker = new SwingWorker<>() {
                        @Override
                        protected Void doInBackground() {
                            dao.updateChannelFrequency(channel.getIdChannels(), freq);
                            channel.setFrequencyMhz(freq);
                            return null;
                        }

                        @Override
                        protected void done() {
                            field.setBorder(new LineBorder(COLOR_BORDER, 1));
                            field.setBackground(Color.WHITE);
                            setStatus("Частота обновлена: " + freq + " МГц", COLOR_TEXT);
                        }
                    };
                    worker.execute();
                }
            } else {
                field.setText(String.valueOf(channel.getFrequencyMhz()));
                field.setBorder(new LineBorder(COLOR_BORDER, 1));
                field.setBackground(Color.WHITE);
                setStatus("Ошибка: частота должна быть 1000-12000 МГц", COLOR_DANGER);
            }
        } catch (NumberFormatException e) {
            field.setText(String.valueOf(channel.getFrequencyMhz()));
            field.setBorder(new LineBorder(COLOR_BORDER, 1));
            field.setBackground(Color.WHITE);
            setStatus("Ошибка: введите целое число", COLOR_DANGER);
        }
    }

    private void buildPowerPanel() {
        if (currentBands.isEmpty()) {
            JLabel emptyLabel = new JLabel("Нет данных о мощности");
            emptyLabel.setForeground(COLOR_TEXT);
            powerGridPanel.add(emptyLabel);
            return;
        }

        int firstBandId = currentBands.get(0).getIdBands();
        List<VtxConfig> configs = configsByBand.get(firstBandId);

        if (configs != null) {
            for (VtxConfig config : configs) {
                JPanel powerCell = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
                powerCell.setBackground(COLOR_CARD_BG);

                JLabel label = new JLabel(config.getLabel() + ":");
                label.setFont(label.getFont().deriveFont(Font.BOLD));
                label.setForeground(COLOR_TEXT);
                powerCell.add(label);

                JTextField field = new JTextField(String.valueOf(config.getValueMw()));
                field.setHorizontalAlignment(JTextField.RIGHT);
                field.setPreferredSize(new Dimension(70, 26));
                field.setFont(new Font("Monospaced", Font.PLAIN, 12));
                field.setBorder(new LineBorder(COLOR_BORDER, 1));
                field.setForeground(COLOR_TEXT);
                field.setBackground(Color.WHITE);
                powerCell.add(field);

                JLabel unit = new JLabel("мВт");
                unit.setForeground(COLOR_TEXT);
                powerCell.add(unit);

                // Кнопка удаления уровня мощности
                JButton btnDeletePower = createSmallDeleteButton();
                btnDeletePower.addActionListener(e -> deletePowerLevel(config));
                powerCell.add(btnDeletePower);

                setupPowerField(field, config);
                powerFieldsMap.put(config.getIdConfig(), field);

                powerGridPanel.add(powerCell);
            }
        }
    }

    private void setupPowerField(JTextField field, VtxConfig config) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            private void validate() {
                SwingUtilities.invokeLater(() -> {
                    String text = field.getText().trim();
                    if (text.isEmpty()) {
                        field.setBorder(new LineBorder(COLOR_WARNING, 1));
                        field.setBackground(Color.WHITE);
                        return;
                    }
                    try {
                        int val = Integer.parseInt(text);
                        if (val >= 0 && val <= 2000) {
                            field.setBorder(new LineBorder(COLOR_BORDER, 1));
                            field.setBackground(Color.WHITE);
                        } else {
                            field.setBorder(new LineBorder(COLOR_DANGER, 1));
                            field.setBackground(COLOR_ERROR_BG);
                        }
                    } catch (NumberFormatException e) {
                        field.setBorder(new LineBorder(COLOR_DANGER, 1));
                        field.setBackground(COLOR_ERROR_BG);
                    }
                });
            }

            @Override public void insertUpdate(DocumentEvent e) { validate(); }
            @Override public void removeUpdate(DocumentEvent e) { validate(); }
            @Override public void changedUpdate(DocumentEvent e) { validate(); }
        });

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                savePower(field, config);
            }
        });

        field.addActionListener(e -> {
            savePower(field, config);
            field.transferFocus();
        });
    }

    private void savePower(JTextField field, VtxConfig config) {
        String text = field.getText().trim();
        if (text.isEmpty()) {
            field.setText(String.valueOf(config.getValueMw()));
            return;
        }

        try {
            int val = Integer.parseInt(text);
            if (val >= 0 && val <= 2000) {
                if (val != config.getValueMw()) {
                    SwingWorker<Void, Void> worker = new SwingWorker<>() {
                        @Override
                        protected Void doInBackground() {
                            dao.updateConfigPower(config.getIdConfig(), val, config.getLabel());
                            config.setValueMw(val);
                            return null;
                        }

                        @Override
                        protected void done() {
                            field.setBorder(new LineBorder(COLOR_BORDER, 1));
                            field.setBackground(Color.WHITE);
                            setStatus("Мощность обновлена: " + val + " мВт", COLOR_TEXT);
                        }
                    };
                    worker.execute();
                }
            } else {
                field.setText(String.valueOf(config.getValueMw()));
                field.setBorder(new LineBorder(COLOR_BORDER, 1));
                field.setBackground(Color.WHITE);
                setStatus("Ошибка: мощность должна быть 0-2000 мВт", COLOR_DANGER);
            }
        } catch (NumberFormatException e) {
            field.setText(String.valueOf(config.getValueMw()));
            field.setBorder(new LineBorder(COLOR_BORDER, 1));
            field.setBackground(Color.WHITE);
            setStatus("Ошибка: введите целое число", COLOR_DANGER);
        }
    }

    private void updatePitmodeState() {
        if (currentBands.isEmpty()) {
            btnPitmode.setText("Pitmode: НЕТ ДАННЫХ");
            btnPitmode.setSelected(false);
            return;
        }

        int firstBandId = currentBands.get(0).getIdBands();
        List<VtxConfig> configs = configsByBand.get(firstBandId);
        if (configs != null && !configs.isEmpty()) {
            boolean pitmode = configs.get(0).isPitMode();
            btnPitmode.setSelected(pitmode);
            btnPitmode.setText(pitmode ? "Pitmode: ВКЛ" : "Pitmode: ВЫКЛ");
            btnPitmode.setBackground(pitmode ? new Color(150, 220, 150) : new Color(200, 200, 200));
        }
    }

    private void setStatus(String message, Color color) {
        lblStatusText.setText(message);
        lblStatusText.setForeground(color);
    }

    // === ОБРАБОТЧИКИ СОБЫТИЙ ===

    @Override
    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();

        if (source == btnRefreshPorts) {
            refreshPorts();
        } else if (source == btnConnect) {
            toggleConnection();
        } else if (source == btnSync) {
            onSync();
        } else if (source == btnWriteChanges) {
            onSave();
        } else if (source == btnPitmode) {
            onPitmodeChanged();
        } else if (source == btnAddBand) {
            onAddBand();
        } else if (source == btnAddChannel) {
            onAddChannel();
        } else if (source == btnAddPower) {
            onAddPowerLevel();
        }
    }

    private void refreshPorts() {
        setStatus("Сканирование портов...", COLOR_TEXT);

        SwingWorker<String[], Void> worker = new SwingWorker<>() {
            @Override
            protected String[] doInBackground() {
                var ports = serialConnector.getAvailablePorts();
                String[] portNames = new String[ports.size()];
                for (int i = 0; i < ports.size(); i++) {
                    portNames[i] = ports.get(i).getSystemPortName();
                }
                return portNames;
            }

            @Override
            protected void done() {
                try {
                    String[] ports = get();
                    portComboBox.removeAllItems();
                    for (String port : ports) {
                        portComboBox.addItem(port);
                    }
                    if (ports.length > 0) {
                        setStatus("Найдено " + ports.length + " портов", COLOR_TEXT);
                    } else {
                        setStatus("Порты не найдены", COLOR_WARNING);
                    }
                } catch (Exception e) {
                    setStatus("Ошибка сканирования: " + e.getMessage(), COLOR_DANGER);
                }
            }
        };
        worker.execute();
    }

    private void toggleConnection() {
        if (isConnected) {
            disconnect();
        } else {
            connect();
        }
    }

    private void connect() {
        String selectedPort = (String) portComboBox.getSelectedItem();
        if (selectedPort == null || selectedPort.isEmpty()) {
            setStatus("Выберите порт", COLOR_WARNING);
            return;
        }

        btnConnect.setEnabled(false);
        setStatus("Подключение к " + selectedPort + "...", COLOR_TEXT);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return serialConnector.connect(selectedPort);
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        isConnected = true;
                        activePort = selectedPort;
                        updateConnectionUI();
                        setStatus("Подключено к " + activePort, COLOR_TEXT);
                        lblStatusIndicator.setForeground(COLOR_SUCCESS);
                    } else {
                        setStatus("Не удалось подключиться к " + selectedPort, COLOR_DANGER);
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
                setStatus("Отключено", COLOR_TEXT);
                lblStatusIndicator.setForeground(COLOR_DANGER);
            }
        };
        worker.execute();
    }

    private void updateConnectionUI() {
        if (isConnected) {
            btnConnect.setText("Отключиться");
            btnConnect.setBackground(new Color(200, 200, 200));
            portComboBox.setEnabled(false);
            btnRefreshPorts.setEnabled(false);
            btnSync.setEnabled(true);
            btnSync.setBackground(new Color(220, 220, 220));
            btnWriteChanges.setEnabled(true);
            btnWriteChanges.setBackground(new Color(220, 220, 220));
        } else {
            btnConnect.setText("Подключиться");
            btnConnect.setBackground(new Color(200, 200, 200));
            portComboBox.setEnabled(true);
            btnRefreshPorts.setEnabled(true);
            btnSync.setEnabled(false);
            btnSync.setBackground(new Color(200, 200, 200));
            btnWriteChanges.setEnabled(false);
            btnWriteChanges.setBackground(new Color(200, 200, 200));
        }
    }

    private void onSync() {
        if (serialConnector == null || !serialConnector.isConnected()) {
            setStatus("Устройство не подключено!", COLOR_DANGER);
            return;
        }

        btnSync.setEnabled(false);
        btnWriteChanges.setEnabled(false);
        setStatus("Синхронизация с устройством...", COLOR_TEXT);

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
                        setStatus("Синхронизация завершена. Найдено: " +
                                result.foundBands.size() + " бэндов, " +
                                result.foundChannels.size() + " каналов", COLOR_TEXT);
                        loadDataFromDatabase();
                    } else {
                        setStatus("Ошибка синхронизации: " + result.message, COLOR_DANGER);
                        JOptionPane.showMessageDialog(MainFrame.this,
                                "Ошибка синхронизации:\n" + result.message,
                                "Ошибка", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    setStatus("Ошибка: " + e.getMessage(), COLOR_DANGER);
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Ошибка синхронизации:\n" + e.getMessage(),
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                } finally {
                    btnSync.setEnabled(true);
                    btnSync.setBackground(new Color(220, 220, 220));
                    btnWriteChanges.setEnabled(true);
                    btnWriteChanges.setBackground(new Color(220, 220, 220));
                    if (isConnected && activePort != null) {
                        setStatus("Подключено к " + activePort, COLOR_TEXT);
                    }
                }
            }
        };
        worker.execute();
    }

    private void onSave() {
        if (!isConnected) {
            setStatus("Устройство не подключено!", COLOR_DANGER);
            return;
        }

        // Проверяем, отвечает ли контроллер
        setStatus("Проверка связи с контроллером", COLOR_TEXT);
        boolean pingOk = serialConnector.pingController();
        if (!pingOk) {
            // Если пинг не прошел - пробуем восстановить порт
            setStatus("Порт не отвечает, пытаемся восстановить", COLOR_WARNING);

            // Переподключаемся
            String currentPort = (String) portComboBox.getSelectedItem();
            if (currentPort != null && !currentPort.isEmpty()) {
                serialConnector.disconnect();
                isConnected = false;
                updateConnectionUI();
                lblStatusIndicator.setForeground(COLOR_DANGER);

                // Пауза
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }

                // Подключаемся заново
                boolean reconnectOk = serialConnector.connect(currentPort);
                if (reconnectOk) {
                    isConnected = true;
                    updateConnectionUI();
                    lblStatusIndicator.setForeground(COLOR_SUCCESS);
                    setStatus("Порт восстановлен", COLOR_SUCCESS);
                } else {
                    setStatus("Не удалось восстановить порт", COLOR_DANGER);
                    return;
                }
            } else {
                setStatus("Порт не выбран", COLOR_WARNING);
                return;
            }
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Записать текущую конфигурацию в VTX и сохранить в EEPROM?\n" +
                        "Это перезагрузит полетный контроллер.",
                "Подтверждение записи",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        btnWriteChanges.setEnabled(false);
        btnSync.setEnabled(false);
        setStatus("Запись конфигурации на устройство...", COLOR_TEXT);

        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() {
                return serialConnector.writeVtxTable();
            }

            @Override
            protected void done() {
                try {
                    boolean success = get();
                    if (success) {
                        setStatus("Запись успешно завершена! Контроллер перезагружен.", COLOR_SUCCESS);
                        JOptionPane.showMessageDialog(MainFrame.this,
                                "Данные успешно записаны в VTX!\n" +
                                        "Полетный контроллер перезагружен.",
                                "Успех",
                                JOptionPane.INFORMATION_MESSAGE);

                        // После сохранения устройство перезагружается
                        isConnected = false;
                        updateConnectionUI();
                        lblStatusIndicator.setForeground(COLOR_DANGER);
                    } else {
                        setStatus("Ошибка записи конфигурации", COLOR_DANGER);
                        JOptionPane.showMessageDialog(MainFrame.this,
                                "Не удалось записать конфигурацию в VTX.",
                                "Ошибка", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    setStatus("Ошибка: " + e.getMessage(), COLOR_DANGER);
                    JOptionPane.showMessageDialog(MainFrame.this,
                            "Ошибка записи:\n" + e.getMessage(),
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                } finally {
                    btnWriteChanges.setEnabled(true);
                    btnWriteChanges.setBackground(new Color(220, 220, 220));
                    btnSync.setEnabled(true);
                    btnSync.setBackground(new Color(220, 220, 220));
                }
            }
        };
        worker.execute();
    }

    private void onPitmodeChanged() {
        boolean pitmode = btnPitmode.isSelected();
        btnPitmode.setText(pitmode ? "Pitmode: ВКЛ" : "Pitmode: ВЫКЛ");
        btnPitmode.setBackground(pitmode ? new Color(150, 220, 150) : new Color(200, 200, 200));

        if (currentBands.isEmpty()) return;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                for (VtxBand band : currentBands) {
                    dao.updatePitmode(band.getIdBands(), pitmode);
                }
                return null;
            }

            @Override
            protected void done() {
                setStatus("Pitmode " + (pitmode ? "включен" : "выключен"), COLOR_TEXT);
            }
        };
        worker.execute();
    }

    // === УПРАВЛЕНИЕ ДАННЫМИ ===

    private void onAddBand() {
        String name = JOptionPane.showInputDialog(this, "Введите имя новой сетки (например, BAND_X):");
        if (name == null) return;
        name = name.trim();
        if (name.isEmpty()) {
            setStatus("Имя не может быть пустым", COLOR_DANGER);
            return;
        }

        String letterStr = JOptionPane.showInputDialog(this, "Введите букву сетки (например, X):");
        if (letterStr == null) return;
        letterStr = letterStr.trim().toUpperCase();
        if (letterStr.isEmpty() || letterStr.length() != 1) {
            setStatus("Введите одну букву", COLOR_DANGER);
            return;
        }

        char letter = letterStr.charAt(0);
        if (letter < 'A' || letter > 'Z') {
            setStatus("Буква должна быть A-Z", COLOR_DANGER);
            return;
        }

        // Проверяем, нет ли уже такой буквы
        for (VtxBand band : currentBands) {
            if (band.getBandLetter() == letter) {
                setStatus("Буква '" + letter + "' уже используется", COLOR_DANGER);
                return;
            }
        }

        int bandNumber = currentBands.size() + 1;

        String finalName = name;
        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                VtxBand newBand = new VtxBand(bandNumber, finalName, letter);
                dao.saveBand(newBand);

                // Создаем дефолтные конфиги для нового бэнда
                int bandId = newBand.getIdBands();

                // Проверяем, есть ли уже конфиги у первого бэнда
                if (!currentBands.isEmpty()) {
                    int firstBandId = currentBands.get(0).getIdBands();
                    List<VtxConfig> existingConfigs = dao.getConfigByBandId(firstBandId);
                    if (existingConfigs != null && !existingConfigs.isEmpty()) {
                        for (VtxConfig cfg : existingConfigs) {
                            VtxConfig newConfig = new VtxConfig(
                                    bandId,
                                    cfg.getLevelIndex(),
                                    cfg.getLabel(),
                                    cfg.getValueMw(),
                                    cfg.isPitMode()
                            );
                            dao.saveConfig(newConfig);
                        }
                    }
                } else {
                    // Если бэндов нет - создаем дефолтные
                    VtxConfig minConfig = new VtxConfig(bandId, 1, "MIN", 25, false);
                    VtxConfig maxConfig = new VtxConfig(bandId, 2, "MAX", 800, false);
                    dao.saveConfig(minConfig);
                    dao.saveConfig(maxConfig);
                }
                return null;
            }

            @Override
            protected void done() {
                loadDataFromDatabase();
                setStatus("Добавлена сетка: " + finalName, COLOR_TEXT);
            }
        };
        worker.execute();
    }

    private void deleteBand(VtxBand band) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Удалить сетку " + band.getBandName() + " и все её каналы?",
                "Подтверждение удаления",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                dao.deleteBand(band.getIdBands());
                return null;
            }

            @Override
            protected void done() {
                loadDataFromDatabase();
                setStatus("Сетка " + band.getBandName() + " удалена", COLOR_TEXT);
            }
        };
        worker.execute();
    }

    private void onAddChannel() {
        if (currentBands.isEmpty()) {
            setStatus("Сначала создайте сетку", COLOR_WARNING);
            return;
        }

        String[] bandNames = currentBands.stream()
                .map(VtxBand::getBandName)
                .toArray(String[]::new);
        String selected = (String) JOptionPane.showInputDialog(this,
                "Выберите сетку для добавления канала:",
                "Добавление канала",
                JOptionPane.QUESTION_MESSAGE,
                null,
                bandNames,
                bandNames[0]);

        if (selected == null) return;

        VtxBand selectedBand = currentBands.stream()
                .filter(b -> b.getBandName().equals(selected))
                .findFirst()
                .orElse(null);
        if (selectedBand == null) return;

        String freqStr = JOptionPane.showInputDialog(this, "Введите частоту канала (МГц):");
        if (freqStr == null) return;
        freqStr = freqStr.trim();
        if (freqStr.isEmpty()) {
            setStatus("Частота не может быть пустой", COLOR_DANGER);
            return;
        }

        try {
            int freq = Integer.parseInt(freqStr);
            if (freq < 1000 || freq > 12000) {
                setStatus("Частота должна быть 1000-12000 МГц", COLOR_DANGER);
                return;
            }

            List<VtxChannel> channels = channelsByBand.get(selectedBand.getIdBands());
            int channelNumber = channels != null ? channels.size() + 1 : 1;

            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    VtxChannel newChannel = new VtxChannel(selectedBand.getIdBands(), channelNumber, freq);
                    dao.saveChannel(newChannel);
                    return null;
                }

                @Override
                protected void done() {
                    loadDataFromDatabase();
                    setStatus("Добавлен канал CH" + channelNumber + " (" + freq + " МГц)", COLOR_TEXT);
                }
            };
            worker.execute();
        } catch (NumberFormatException e) {
            setStatus("Введите корректное число", COLOR_DANGER);
        }
    }

    private void deleteChannel(VtxChannel channel) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Удалить канал CH" + channel.getChannelNumber() +
                        " (частота: " + channel.getFrequencyMhz() + " МГц)?",
                "Подтверждение удаления",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                dao.deleteChannel(channel.getIdChannels());
                return null;
            }

            @Override
            protected void done() {
                loadDataFromDatabase();
                setStatus("Канал удален", COLOR_TEXT);
            }
        };
        worker.execute();
    }

    private void onAddPowerLevel() {
        if (currentBands.isEmpty()) {
            setStatus("Сначала создайте сетку", COLOR_WARNING);
            return;
        }

        String label = JOptionPane.showInputDialog(this, "Введите метку уровня мощности (например, MED):");
        if (label == null) return;
        label = label.trim();
        if (label.isEmpty()) {
            setStatus("Метка не может быть пустой", COLOR_DANGER);
            return;
        }

        String valueStr = JOptionPane.showInputDialog(this, "Введите мощность (мВт):");
        if (valueStr == null) return;
        valueStr = valueStr.trim();
        if (valueStr.isEmpty()) {
            setStatus("Мощность не может быть пустой", COLOR_DANGER);
            return;
        }

        try {
            int value = Integer.parseInt(valueStr);
            if (value < 0 || value > 2000) {
                setStatus("Мощность должна быть 0-2000 мВт", COLOR_DANGER);
                return;
            }

            int firstBandId = currentBands.get(0).getIdBands();
            List<VtxConfig> configs = configsByBand.get(firstBandId);
            int levelIndex = configs != null ? configs.size() + 1 : 1;

            String finalLabel = label;
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {
                    for (VtxBand band : currentBands) {
                        VtxConfig newConfig = new VtxConfig(
                                band.getIdBands(),
                                levelIndex,
                                finalLabel,
                                value,
                                false
                        );
                        dao.saveConfig(newConfig);
                    }
                    return null;
                }

                @Override
                protected void done() {
                    loadDataFromDatabase();
                    setStatus("Добавлен уровень мощности: " + finalLabel + " (" + value + " мВт)", COLOR_TEXT);
                }
            };
            worker.execute();
        } catch (NumberFormatException e) {
            setStatus("Введите корректное число", COLOR_DANGER);
        }
    }

    private void deletePowerLevel(VtxConfig config) {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Удалить уровень мощности " + config.getLabel() +
                        " (" + config.getValueMw() + " мВт)?",
                "Подтверждение удаления",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                dao.deleteConfig(config.getIdConfig());
                return null;
            }

            @Override
            protected void done() {
                loadDataFromDatabase();
                setStatus("Уровень мощности удален", COLOR_TEXT);
            }
        };
        worker.execute();
    }
}