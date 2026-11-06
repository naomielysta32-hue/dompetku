package id.dompetku.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class Theme {

    private Theme() {
    }

    // Palet Warna Konsisten Sesuai Halaman Login
    public static final Color BG = new Color(0xED, 0xE7, 0xC7);         // #EDE7C7 (Krim lembut)
    public static final Color CHARCOAL = new Color(0x20, 0x0E, 0x00);   // #200E00 (Hitam Kecokelatan)
    public static final Color GOLD = new Color(0xC5, 0x9B, 0x27);       // #C59B27 (Aksen Emas)
    public static final Color PRIMARY = new Color(0xC5, 0x9B, 0x27);    // Disamakan dengan warna emas login
    public static final Color NAVY = new Color(0x5B, 0x03, 0x02);       
    public static final Color UBE = PRIMARY;
    
    public static final Color TEXT = new Color(0x20, 0x0E, 0x00);       // #200E00
    public static final Color MUTED = new Color(0x5B, 0x03, 0x02);      
    public static final Color WHITE = new Color(0xFF, 0xFC, 0xF7);      // Putih susu
    public static final Color BORDER = new Color(0xD0, 0xC2, 0xA0);     // Garis pembatas

    public static final Color INCOME = new Color(0x2E, 0x7D, 0x32);     // Hijau Matcha
    public static final Color EXPENSE = new Color(0xC6, 0x28, 0x28);    // Merah Strawberry

    public static void setup() {
        try {
            UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName()
            );
        } catch (Exception ignored) {
        }

        UIManager.put("Label.foreground", TEXT);
        UIManager.put("Panel.background", BG);
        
        UIManager.put("TabbedPane.background", BG);
        UIManager.put("TabbedPane.foreground", TEXT);
        UIManager.put("TabbedPane.selected", WHITE);
        UIManager.put("TabbedPane.contentAreaColor", WHITE);
        
        UIManager.put("TextArea.foreground", TEXT);
        UIManager.put("TextArea.background", WHITE);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.background", WHITE);
        UIManager.put("Table.foreground", TEXT);
        UIManager.put("Table.background", WHITE);
        UIManager.put("Table.selectionBackground", PRIMARY);
        UIManager.put("Table.selectionForeground", WHITE);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("TableHeader.background", CHARCOAL);
        UIManager.put("TableHeader.foreground", WHITE);
        UIManager.put("OptionPane.background", BG);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("ComboBox.background", WHITE);
        UIManager.put("ComboBox.foreground", TEXT);
    }

    public static JPanel page() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(20, 16, 16, 16));
        return panel;
    }

    public static JLabel heading(String title) {
        JLabel label = new JLabel(title);
        label.setForeground(CHARCOAL);
        label.setFont(new Font("SansSerif", Font.BOLD, 22));
        label.setBorder(new EmptyBorder(0, 0, 10, 0));
        return label;
    }

    public static JButton button(String title) {
        JButton button = new JButton(title);
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setForeground(WHITE);
        button.setBackground(PRIMARY); // Menggunakan warna emas selaras dengan halaman login
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(10, 16, 10, 16));
        return button;
    }
}