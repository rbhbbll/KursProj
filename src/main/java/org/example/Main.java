package org.example;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        DatabaseManager dbManager = new DatabaseManager();
        SwingUtilities.invokeLater(() -> {
            new LoginWindow(dbManager).setVisible(true);
        });
        // test commit from master branch
    }
}