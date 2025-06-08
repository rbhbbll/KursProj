package org.example;

import com.github.weisj.darklaf.LafManager;

import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        DatabaseManager dbManager = new DatabaseManager();
        LafManager.install();
        SwingUtilities.invokeLater(() -> {
            new LoginWindow(dbManager).setVisible(true);
        });
    }
}