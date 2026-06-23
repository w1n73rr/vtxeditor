package org.example;

import org.example.database.DatabaseManager;
import org.example.database.SqliteVtxDao;
import org.example.database.VtxDao;
import org.example.service.ConfigParser;
import org.example.service.SerialConnector;
import org.example.ui.MainFrame;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        // Инициализация базы данных
        DatabaseManager dbManager = new DatabaseManager();
        dbManager.initializeDatabase();

        // Создание слоя данных
        VtxDao dao = new SqliteVtxDao();

        // Создание сервисного слоя
        ConfigParser parser = new ConfigParser();
        SerialConnector serialConnector = new SerialConnector(dao, parser);

        // Запуск UI
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.err.println("Ошибка установки LookAndFeel: " + e.getMessage());
            }
            new MainFrame(dao, serialConnector).setVisible(true);
        });
    }
}