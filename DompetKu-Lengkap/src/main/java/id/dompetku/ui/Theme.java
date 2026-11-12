package id.dompetku.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;

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
    public static final Color HINT = new Color(0x7A, 0x6A, 0x55);       // teks bantuan / keterangan kecil
    public static final Color WHITE = new Color(0xFF, 0xFC, 0xF7);      // Putih susu
    public static final Color BORDER = new Color(0xD0, 0xC2, 0xA0);     // Garis pembatas
    public static final Color SELECTION = new Color(0xF2, 0xE4, 0xB5);  // Baris tabel terpilih (emas lembut)

    public static final Color INCOME = new Color(0x2E, 0x7D, 0x32);     // Hijau Matcha
    public static final Color EXPENSE = new Color(0xC6, 0x28, 0x28);    // Merah Strawberry
    public static final Color DANGER = EXPENSE;

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
        UIManager.put("Table.selectionBackground", SELECTION);
        UIManager.put("Table.selectionForeground", TEXT);
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
        panel.setBorder(new EmptyBorder(24, 28, 20, 28));
        return panel;
    }

    public static JLabel heading(String title) {
        JLabel label = new JLabel(title);
        label.setForeground(CHARCOAL);
        label.setFont(new Font("SansSerif", Font.BOLD, 22));
        label.setBorder(new EmptyBorder(0, 0, 10, 0));
        return label;
    }

    /** Judul halaman di kiri, tombol aksi utama di kanan (selalu terlihat tanpa scroll). */
    public static JPanel pageHeader(String title, JComponent actions) {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 6, 0));

        JLabel label = heading(title);
        label.setBorder(new EmptyBorder(0, 0, 0, 0));
        header.add(label, BorderLayout.WEST);
        if (actions != null) {
            header.add(actions, BorderLayout.EAST);
        }
        return header;
    }

    /** Baris tombol rata kanan dengan jarak seragam. */
    public static JPanel buttonRow(JComponent... buttons) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        row.setOpaque(false);
        for (JComponent b : buttons) {
            row.add(b);
        }
        return row;
    }

    /** Panel putih berbingkai tipis, dipakai sebagai "kartu". */
    public static JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(WHITE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(20, 22, 20, 22)));
        return p;
    }

    public static void styleInput(JTextField field) {
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
        field.setForeground(CHARCOAL);
        field.setBackground(Color.WHITE);
        field.setCaretColor(CHARCOAL);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                new EmptyBorder(9, 10, 9, 10)));
    }

    // ------------------------------------------------------------------ tombol

    /** Tombol utama (emas). */
    public static JButton button(String title) {
        return new FlatButton(title, PRIMARY, CHARCOAL, false);
    }

    /** Tombol gelap. */
    public static JButton buttonDark(String title) {
        return new FlatButton(title, CHARCOAL, WHITE, false);
    }

    /** Tombol sekunder: putih dengan garis tepi. */
    public static JButton buttonGhost(String title) {
        return new FlatButton(title, WHITE, CHARCOAL, true);
    }

    /** Tombol untuk aksi berbahaya (hapus). */
    public static JButton buttonDanger(String title) {
        return new FlatButton(title, DANGER, Color.WHITE, false);
    }

    /**
     * Tombol yang digambar sendiri supaya warnanya tampil sama di Windows, macOS, maupun Linux
     * (look-and-feel sistem sering mengabaikan setBackground pada JButton).
     */
    private static final class FlatButton extends JButton {
        private final boolean ghost;
        private boolean hover;

        FlatButton(String text, Color bg, Color fg, boolean ghost) {
            super(text);
            this.ghost = ghost;
            setBackground(bg);
            setForeground(fg);
            setFont(new Font("SansSerif", Font.BOLD, 13));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(9, 18, 9, 18));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            Insets in = getInsets();
            return new Dimension(
                    fm.stringWidth(getText()) + in.left + in.right,
                    Math.max(36, fm.getHeight() + in.top + in.bottom));
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            Color bg = getBackground();
            Color fg = getForeground();
            Color edge = BORDER;

            if (!isEnabled()) {
                bg = ghost ? new Color(0xF6, 0xF2, 0xE6) : new Color(0xDD, 0xD5, 0xBF);
                fg = new Color(0xA0, 0x96, 0x80);
            } else if (getModel().isPressed()) {
                bg = shade(bg, 0.82f);
            } else if (hover) {
                bg = shade(bg, 0.92f);
                if (ghost) edge = GOLD;
            }

            RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 10, 10);
            g.setColor(bg);
            g.fill(shape);
            if (ghost) {
                g.setColor(edge);
                g.draw(shape);
            }

            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            int x = (getWidth() - fm.stringWidth(getText())) / 2;
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            g.setColor(fg);
            g.drawString(getText(), x, y);
            g.dispose();
        }

        /** Gelapkan warna terang, terangkan warna gelap, agar efek hover selalu terlihat. */
        private static Color shade(Color c, float factor) {
            int lum = (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 1000;
            if (lum < 70) {
                float t = 1f - factor; // campur dengan putih
                return new Color(
                        Math.min(255, Math.round(c.getRed() + (255 - c.getRed()) * t * 1.6f)),
                        Math.min(255, Math.round(c.getGreen() + (255 - c.getGreen()) * t * 1.6f)),
                        Math.min(255, Math.round(c.getBlue() + (255 - c.getBlue()) * t * 1.6f)));
            }
            return new Color(Math.round(c.getRed() * factor), Math.round(c.getGreen() * factor), Math.round(c.getBlue() * factor));
        }
    }

    // ------------------------------------------------------------------ avatar

    /** Lingkaran emas berisi inisial nama. */
    public static final class Avatar extends JComponent {
        private String initial = "?";

        public Avatar(String name, int size) {
            setPerson(name);
            Dimension d = new Dimension(size, size);
            setPreferredSize(d);
            setMinimumSize(d);
            setMaximumSize(d);
        }

        public void setPerson(String name) {
            String n = name == null ? "" : name.trim();
            initial = n.isEmpty() ? "?" : n.substring(0, 1).toUpperCase();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int s = Math.min(getWidth(), getHeight());
            g.setColor(GOLD);
            g.fill(new Ellipse2D.Float(0, 0, s, s));
            g.setColor(CHARCOAL);
            g.setFont(new Font("SansSerif", Font.BOLD, Math.round(s * 0.45f)));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(initial, (s - fm.stringWidth(initial)) / 2f, (s - fm.getHeight()) / 2f + fm.getAscent());
            g.dispose();
        }
    }
}
