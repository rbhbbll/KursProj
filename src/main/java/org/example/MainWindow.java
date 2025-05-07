package org.example;

import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame {
    private final DatabaseManager dbManager;
    private final User user;

    public MainWindow(DatabaseManager dbManager, User user) {
        this.dbManager = dbManager;
        this.user = user;
        setTitle("Тур-агентство");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Просмотр данных", new ViewDataPanel(dbManager, user));
        tabbedPane.addTab("Добавить клиента", new ClientWindow(dbManager));
        tabbedPane.addTab("Добавить тур", new TourWindow(dbManager));
        tabbedPane.addTab("Сделать бронирование", new BookingWindow(dbManager, user));

        add(tabbedPane);
    }
}