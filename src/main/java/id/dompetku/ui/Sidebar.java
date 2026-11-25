package id.dompetku.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Komponen menu samping DompetKu: daftar halaman, kartu profil, dan tombol keluar.
 */
// [OOP - Inheritance]: Sidebar mewarisi JPanel sebagai komponen navigasi sisi kiri aplikasi.
// [OOP - Composition]: Sidebar tersusun dari objek NavButton, UserChip, Theme.Avatar, dan callback Consumer/Runnable.
public class Sidebar extends JPanel {

    public static final String PROFILE_KEY = "profil";
    private static final int WIDE = 236, NARROW = 76;
    private static final Color IDLE = new Color(0xE6, 0xDD, 0xC3);
    private static final Color SECTION = new Color(0xA8, 0x98, 0x78);

    private final Consumer<String> onNavigate;
    private final Map<String, NavButton> items = new LinkedHashMap<>();
    private final List<JLabel> sectionLabels = new ArrayList<>();

    private final JPanel nav = new JPanel();
    private final JLabel brand = Theme.label("DompetKu", Font.BOLD, 20, Theme.WHITE);
    private final NavButton toggle = new NavButton("Ciutkan menu", "menu", true);
    private final UserChip chip;
    private final NavButton logout = new NavButton("Keluar", "logout", false);
    private boolean collapsed;

    public Sidebar(String nama, String username, Consumer<String> onNavigate, Runnable onLogout) {
        this.onNavigate = onNavigate;
        setLayout(new BorderLayout());
        setBackground(Theme.CHARCOAL);
        setOpaque(true);
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(0x3A, 0x24, 0x10)));
        setPreferredSize(new Dimension(WIDE, 0));

        JPanel header = new JPanel(new BorderLayout(6, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(16, 12, 14, 12));
        toggle.addActionListener(e -> toggleCollapsed());
        header.add(toggle, BorderLayout.WEST);
        header.add(brand, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setOpaque(false);
        add(nav, BorderLayout.CENTER);

        chip = new UserChip(nama, username);
        chip.addActionListener(() -> this.onNavigate.accept(PROFILE_KEY));
        logout.addActionListener(e -> onLogout.run());

        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(8, 0, 14, 0));
        footer.add(chip);
        footer.add(Box.createVerticalStrut(4));
        footer.add(logout);
        add(footer, BorderLayout.SOUTH);
    }

    public void section(String title) {
        nav.add(Box.createVerticalStrut(14));
        JLabel l = Theme.label(title, Font.BOLD, 10, SECTION);
        l.setBorder(new EmptyBorder(0, 26, 6, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        l.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        nav.add(l);
        sectionLabels.add(l);
    }

    public void item(String key, String label, String icon) {
        NavButton b = new NavButton(label, icon, false);
        b.addActionListener(e -> onNavigate.accept(key));
        b.setCollapsed(collapsed);
        items.put(key, b);
        nav.add(b);
    }

    public void select(String key) {
        items.forEach((k, b) -> b.setActive(k.equals(key)));
        chip.setActive(PROFILE_KEY.equals(key));
    }

    public void setUser(String nama, String username) {
        chip.setUser(nama, username);
    }

    public void toggleCollapsed() {
        collapsed = !collapsed;
        setPreferredSize(new Dimension(collapsed ? NARROW : WIDE, 0));
        brand.setVisible(!collapsed);
        toggle.setToolTipText(collapsed ? "Perluas menu" : null);
        items.values().forEach(b -> b.setCollapsed(collapsed));
        logout.setCollapsed(collapsed);
        chip.setCollapsed(collapsed);
        sectionLabels.forEach(l -> l.setVisible(!collapsed));
        revalidate();
        repaint();
        if (getParent() != null) {
            getParent().revalidate();
            getParent().repaint();
        }
    }

    // ------------------------------------------------------------------ tombol menu

    private static final class NavButton extends JButton {
        private final String label, icon;
        private final boolean iconOnly;
        private boolean active, hover, collapsed;

        NavButton(String label, String icon, boolean iconOnly) {
            super(label);
            this.label = label;
            this.icon = icon;
            this.iconOnly = iconOnly;
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setFont(Theme.font(Font.BOLD, 13));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            Theme.onHover(this, h -> { hover = h; repaint(); });
        }

        void setActive(boolean a) { active = a; repaint(); }
        void setCollapsed(boolean c) { collapsed = c; setToolTipText(c ? label : null); repaint(); }

        @Override public Dimension getPreferredSize() { return new Dimension(iconOnly ? 52 : 100, iconOnly ? 44 : 42); }
        @Override public Dimension getMaximumSize() { return iconOnly ? getPreferredSize() : new Dimension(Integer.MAX_VALUE, 42); }

        // [OOP - Polymorphism]: Method overriding paintComponent menggambar ikon vektor dan status aktif tombol navigasi.
        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight(), pad = iconOnly ? 4 : 12;
            if (active || hover) {
                g.setColor(active ? Theme.GOLD : new Color(255, 255, 255, 28));
                g.fill(new RoundRectangle2D.Float(pad, 2, w - 2 * pad, h - 4, 12, 12));
            }
            Color fg = active ? Theme.CHARCOAL : (hover ? Color.WHITE : IDLE);
            int iconX = (iconOnly || collapsed) ? (w - 20) / 2 : pad + 14;
            paintIcon(g, icon, iconX, (h - 20) / 2, fg);
            if (!iconOnly && !collapsed) {
                g.setFont(getFont());
                FontMetrics fm = g.getFontMetrics();
                g.setColor(fg);
                g.drawString(label, iconX + 32, (h - fm.getHeight()) / 2 + fm.getAscent());
            }
            g.dispose();
        }
    }

    // ------------------------------------------------------------------ kartu profil

    private static final class UserChip extends JPanel {
        // [OOP - Composition]: UserChip memiliki komponen Theme.Avatar dan daftar listener klik.
        private final Theme.Avatar avatar;
        private final JLabel name = Theme.label("", Font.BOLD, 13, Color.WHITE);
        private final JPanel text = new JPanel();
        private boolean active, hover;
        private final List<Runnable> listeners = new ArrayList<>();

        UserChip(String nama, String username) {
            super(new BorderLayout(10, 0));
            setOpaque(false);
            setBorder(new EmptyBorder(8, 18, 8, 12));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
            setPreferredSize(new Dimension(100, 56));

            avatar = new Theme.Avatar(nama, 34);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
            text.setOpaque(false);
            text.add(name);
            text.add(Theme.label("Lihat profil", Font.PLAIN, 11, SECTION));

            JPanel avatarBox = new JPanel(new GridBagLayout());
            avatarBox.setOpaque(false);
            avatarBox.add(avatar);
            add(avatarBox, BorderLayout.WEST);
            add(text, BorderLayout.CENTER);
            setUser(nama, username);

            Theme.onClick(this, () -> listeners.forEach(Runnable::run));
            Theme.onHover(this, h -> { hover = h; repaint(); });
        }

        void addActionListener(Runnable r) { listeners.add(r); }
        void setActive(boolean a) { active = a; repaint(); }

        void setUser(String nama, String username) {
            avatar.setPerson(nama);
            name.setText(nama);
            setToolTipText(nama + (username == null ? "" : "  (@" + username + ")"));
        }

        void setCollapsed(boolean c) {
            text.setVisible(!c);
            setBorder(c ? new EmptyBorder(8, (NARROW - 1 - 34) / 2, 8, 0) : new EmptyBorder(8, 18, 8, 12));
            revalidate();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (active || hover) {
                g.setColor(active ? new Color(255, 255, 255, 40) : new Color(255, 255, 255, 22));
                g.fill(new RoundRectangle2D.Float(12, 2, getWidth() - 24, getHeight() - 4, 12, 12));
            }
            if (active) {
                g.setColor(Theme.GOLD);
                g.setStroke(new BasicStroke(1.5f));
                g.draw(new RoundRectangle2D.Float(12.75f, 2.75f, getWidth() - 25.5f, getHeight() - 5.5f, 12, 12));
            }
            g.dispose();
        }
    }

    // ------------------------------------------------------------------ ikon vektor (20x20)

    private static void paintIcon(Graphics2D g0, String key, int x, int y, Color c) {
        Graphics2D g = (Graphics2D) g0.create();
        g.translate(x, y);
        g.setColor(c);
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (key) {
            case "home" -> {
                line(g, 2, 10, 10, 3, 18, 10);
                line(g, 4.5f, 8.5f, 4.5f, 17, 15.5f, 17, 15.5f, 8.5f);
                line(g, 8.5f, 17, 8.5f, 12, 11.5f, 12, 11.5f, 17);
            }
            case "wallet" -> {
                g.draw(new RoundRectangle2D.Float(2, 4.5f, 16, 12, 4, 4));
                g.draw(new RoundRectangle2D.Float(11.5f, 9.5f, 6.5f, 4, 2, 2));
                line(g, 5, 4.5f, 13, 2.5f);
            }
            case "list" -> {
                for (float yy : new float[]{5, 10, 15}) {
                    line(g, 7.5f, yy, 17.5f, yy);
                    g.fill(new Ellipse2D.Float(2.2f, yy - 1.4f, 2.8f, 2.8f));
                }
            }
            case "pie" -> {
                g.draw(new Ellipse2D.Float(2.5f, 2.5f, 15, 15));
                g.fill(new Arc2D.Float(2.5f, 2.5f, 15, 15, 90, -90, Arc2D.PIE));
            }
            case "target" -> {
                g.draw(new Ellipse2D.Float(2, 2, 16, 16));
                g.draw(new Ellipse2D.Float(5.5f, 5.5f, 9, 9));
                g.fill(new Ellipse2D.Float(8.6f, 8.6f, 2.8f, 2.8f));
            }
            case "swap" -> {
                line(g, 3, 7, 16, 7); line(g, 12.5f, 3.5f, 16, 7, 12.5f, 10.5f);
                line(g, 17, 13, 4, 13); line(g, 7.5f, 9.5f, 4, 13, 7.5f, 16.5f);
            }
            case "chart" -> {
                g.fill(new RoundRectangle2D.Float(3, 10, 3.6f, 7, 1.2f, 1.2f));
                g.fill(new RoundRectangle2D.Float(8.2f, 4, 3.6f, 13, 1.2f, 1.2f));
                g.fill(new RoundRectangle2D.Float(13.4f, 7.5f, 3.6f, 9.5f, 1.2f, 1.2f));
            }
            case "user" -> {
                g.draw(new Ellipse2D.Float(6.5f, 2.5f, 7, 7));
                g.draw(new Arc2D.Float(3, 11.5f, 14, 14, 0, 180, Arc2D.OPEN));
            }
            case "logout" -> {
                line(g, 8, 3, 3, 3, 3, 17, 8, 17);
                line(g, 7, 10, 17, 10);
                line(g, 13.5f, 6.5f, 17, 10, 13.5f, 13.5f);
            }
            case "menu" -> { line(g, 3, 5, 17, 5); line(g, 3, 10, 17, 10); line(g, 3, 15, 17, 15); }
            default -> g.draw(new Ellipse2D.Float(4, 4, 12, 12));
        }
        g.dispose();
    }

    private static void line(Graphics2D g, float... p) {
        Path2D.Float path = new Path2D.Float();
        path.moveTo(p[0], p[1]);
        for (int i = 2; i < p.length; i += 2) path.lineTo(p[i], p[i + 1]);
        g.draw(path);
    }
}
