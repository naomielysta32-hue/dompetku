package id.dompetku.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Pusat tema, palet warna, factory komponen Swing, dan helper styling/event untuk seluruh UI.
 */
// [OOP - Encapsulation]: Seluruh aturan styling, pewarnaan, dan komponen kustom dibungkus dalam utility class Theme.
public final class Theme {

    private Theme() {}

    public static final Color BG = new Color(0xED, 0xE7, 0xC7);
    public static final Color CHARCOAL = new Color(0x20, 0x0E, 0x00);
    public static final Color GOLD = new Color(0xC5, 0x9B, 0x27);
    public static final Color PRIMARY = GOLD;
    public static final Color NAVY = new Color(0x5B, 0x03, 0x02);
    public static final Color UBE = PRIMARY;

    public static final Color TEXT = CHARCOAL;
    public static final Color MUTED = NAVY;
    public static final Color HINT = new Color(0x7A, 0x6A, 0x55);
    public static final Color WHITE = new Color(0xFF, 0xFC, 0xF7);
    public static final Color BORDER = new Color(0xD0, 0xC2, 0xA0);
    public static final Color SELECTION = new Color(0xF2, 0xE4, 0xB5);

    public static final Color INCOME = new Color(0x2E, 0x7D, 0x32);
    public static final Color EXPENSE = new Color(0xC6, 0x28, 0x28);
    public static final Color DANGER = EXPENSE;

    public static void setup() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        Map.ofEntries(
                Map.entry("Label.foreground", TEXT), Map.entry("Panel.background", BG),
                Map.entry("TabbedPane.background", BG), Map.entry("TabbedPane.foreground", TEXT),
                Map.entry("TabbedPane.selected", WHITE), Map.entry("TabbedPane.contentAreaColor", WHITE),
                Map.entry("TextArea.foreground", TEXT), Map.entry("TextArea.background", WHITE),
                Map.entry("TextField.foreground", TEXT), Map.entry("TextField.background", WHITE),
                Map.entry("Table.foreground", TEXT), Map.entry("Table.background", WHITE),
                Map.entry("Table.selectionBackground", SELECTION), Map.entry("Table.selectionForeground", TEXT),
                Map.entry("Table.gridColor", BORDER), Map.entry("TableHeader.background", CHARCOAL),
                Map.entry("TableHeader.foreground", WHITE), Map.entry("OptionPane.background", BG),
                Map.entry("OptionPane.messageForeground", TEXT), Map.entry("ComboBox.background", WHITE),
                Map.entry("ComboBox.foreground", TEXT)
        ).forEach(UIManager::put);
    }

    // ------------------------------------------------------------------ factory tipografi & layout

    public static Font font(int style, int size) {
        return new Font("SansSerif", style, size);
    }

    // [OOP - Polymorphism]: Method overloading label() untuk variasi parameter styling teks yang ringkas.
    public static JLabel label(String text, int size, boolean bold) {
        return label(text, bold ? Font.BOLD : Font.PLAIN, size, TEXT);
    }

    public static JLabel label(String text, int style, int size, Color fg) {
        JLabel l = new JLabel(text);
        l.setFont(font(style, size));
        l.setForeground(fg);
        return l;
    }

    public static JPanel page() {
        JPanel panel = new JPanel(new BorderLayout(12, 12));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(24, 28, 20, 28));
        return panel;
    }

    public static JLabel heading(String title) {
        JLabel label = label(title, Font.BOLD, 22, CHARCOAL);
        label.setBorder(new EmptyBorder(0, 0, 10, 0));
        return label;
    }

    public static JPanel pageHeader(String title, JComponent actions) {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(0, 0, 6, 0));
        JLabel label = heading(title);
        label.setBorder(new EmptyBorder(0, 0, 0, 0));
        header.add(label, BorderLayout.WEST);
        if (actions != null) header.add(actions, BorderLayout.EAST);
        return header;
    }

    public static JPanel buttonRow(JComponent... buttons) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        row.setOpaque(false);
        for (JComponent b : buttons) row.add(b);
        return row;
    }

    public static JPanel card() {
        return borderedCard(WHITE, BORDER, 20, 22);
    }

    public static JPanel borderedCard(Color bg, Color border, int vPad, int hPad) {
        JPanel p = new JPanel();
        p.setBackground(bg);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border), new EmptyBorder(vPad, hPad, vPad, hPad)));
        return p;
    }

    public static JScrollPane scroll(Component view) {
        JScrollPane sp = new JScrollPane(view);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setOpaque(false);
        sp.getViewport().setOpaque(false);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    public static JScrollPane tableScroll(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.getViewport().setBackground(WHITE);
        sp.setBorder(BorderFactory.createLineBorder(BORDER));
        return sp;
    }

    // ------------------------------------------------------------------ styling input & tabel

    public static void styleInput(JTextField field) {
        styleField(field);
    }

    public static void styleTree(Component c) {
        if (c instanceof JComboBox<?> cb) styleCombo(cb);
        else if (c instanceof JTextField tf) styleField(tf);
        else if (c instanceof Container ct) {
            for (Component child : ct.getComponents()) styleTree(child);
        }
    }

    public static void styleField(JTextField f) {
        if (f.getClientProperty("dk.styled") != null) return;
        f.putClientProperty("dk.styled", Boolean.TRUE);
        f.setFont(font(Font.PLAIN, 14));
        f.setForeground(CHARCOAL);
        f.setBackground(Color.WHITE);
        f.setCaretColor(CHARCOAL);
        f.setSelectionColor(SELECTION);
        f.setOpaque(true);
        f.setBorder(new FieldBorder(f));
        onFocusRepaint(f);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void styleCombo(JComboBox<?> combo) {
        if (combo.getClientProperty("dk.styled") != null) return;
        combo.putClientProperty("dk.styled", Boolean.TRUE);
        combo.setFont(font(Font.PLAIN, 14));
        combo.setBackground(Color.WHITE);
        combo.setForeground(CHARCOAL);
        combo.setUI(new ModernComboUI());
        combo.setBorder(new FieldBorder(combo));
        combo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        ListCellRenderer<?> base = combo.getRenderer();
        // [OOP - Polymorphism]: Lambda menggantikan implementasi anonymous ListCellRenderer.
        combo.setRenderer((ListCellRenderer) (list, value, index, selected, focus) -> {
            Component c = ((ListCellRenderer) base).getListCellRendererComponent(list, value, index, selected, focus);
            if (c instanceof JLabel l) {
                l.setFont(font(Font.PLAIN, 14));
                l.setBorder(index < 0 ? new EmptyBorder(0, 0, 0, 0) : new EmptyBorder(7, 10, 7, 10));
                l.setForeground(CHARCOAL);
                l.setBackground(selected && index >= 0 ? SELECTION : Color.WHITE);
                l.setOpaque(true);
            }
            return c;
        });
        onFocusRepaint(combo);
    }

    public static void uniformSize(JComponent comp) {
        comp.setPreferredSize(new Dimension(360, 38));
        comp.setMinimumSize(new Dimension(120, 38));
        comp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
    }

    /** Membungkus JPasswordField dengan tombol ikon mata (tampilkan/sembunyikan password). */
    public static JPanel passwordWithEye(JPasswordField pf, boolean uniformHeight) {
        styleInput(pf);
        char defaultEcho = pf.getEchoChar() == 0 ? '•' : pf.getEchoChar();
        pf.setEchoChar(defaultEcho);

        JPanel wrapper = new JPanel(new BorderLayout(0, 0));
        wrapper.setBackground(pf.getBackground());
        wrapper.setBorder(pf.getBorder());
        if (uniformHeight) uniformSize(wrapper);

        pf.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 4));
        pf.setOpaque(false);

        JToggleButton btnMata = new JToggleButton("👁");
        btnMata.setToolTipText("Tampilkan / Sembunyikan Password");
        btnMata.setFocusPainted(false);
        btnMata.setBorderPainted(false);
        btnMata.setContentAreaFilled(false);
        btnMata.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnMata.setForeground(HINT);
        btnMata.setPreferredSize(new Dimension(36, 34));
        btnMata.addActionListener(e -> {
            pf.setEchoChar(btnMata.isSelected() ? (char) 0 : defaultEcho);
            btnMata.setForeground(btnMata.isSelected() ? CHARCOAL : HINT);
        });

        wrapper.add(pf, BorderLayout.CENTER);
        wrapper.add(btnMata, BorderLayout.EAST);
        return wrapper;
    }

    public static void filterNoDigits(JTextField field) {
        regexFilter(field, "\\d");
    }

    public static void filterLettersOnly(JTextField field) {
        regexFilter(field, "[^\\p{L} ]");
    }

    private static void regexFilter(JTextField field, String removeRegex) {
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int off, String s, AttributeSet a) throws BadLocationException {
                super.insertString(fb, off, s == null ? null : s.replaceAll(removeRegex, ""), a);
            }
            @Override
            public void replace(FilterBypass fb, int off, int len, String s, AttributeSet a) throws BadLocationException {
                super.replace(fb, off, len, s == null ? null : s.replaceAll(removeRegex, ""), a);
            }
        });
    }

    /** Membuat field nominal yang otomatis berformat ribuan (contoh: 3000000 -> 3.000.000). */
    public static JTextField rupiahField(String awal) {
        JTextField field = new JTextField();
        ((AbstractDocument) field.getDocument()).setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int off, String s, AttributeSet a) throws BadLocationException {
                replace(fb, off, 0, s, a);
            }
            @Override
            public void remove(FilterBypass fb, int off, int len) throws BadLocationException {
                String cur = fb.getDocument().getText(0, fb.getDocument().getLength());
                if (len == 1 && off < cur.length() && !Character.isDigit(cur.charAt(off))) {
                    int o = off;
                    while (o > 0 && !Character.isDigit(cur.charAt(o))) o--;
                    if (!Character.isDigit(cur.charAt(o))) return;
                    off = o;
                }
                replace(fb, off, len, "", null);
            }
            @Override
            public void replace(FilterBypass fb, int off, int len, String text, AttributeSet attrs) throws BadLocationException {
                Document doc = fb.getDocument();
                String cur = doc.getText(0, doc.getLength());
                String before = cur.substring(0, off) + (text == null ? "" : text);
                int digitsBefore = before.replaceAll("\\D", "").length();
                String digits = (before + cur.substring(off + len)).replaceAll("\\D", "").replaceFirst("^0+(?=\\d)", "");
                if (digits.length() > 15) return;
                String out = digits.isEmpty() ? "" : formatRibuan(digits);
                fb.replace(0, doc.getLength(), out, attrs);
                int pos = 0, seen = 0;
                while (pos < out.length() && seen < digitsBefore) {
                    if (Character.isDigit(out.charAt(pos))) seen++;
                    pos++;
                }
                final int caret = pos;
                SwingUtilities.invokeLater(() -> {
                    if (caret <= field.getDocument().getLength()) field.setCaretPosition(caret);
                });
            }
        });
        if (awal != null && !awal.isBlank()) field.setText(awal);
        return field;
    }

    private static String formatRibuan(String digits) {
        StringBuilder sb = new StringBuilder();
        for (int i = digits.length() - 1, c = 0; i >= 0; i--, c++) {
            if (c > 0 && c % 3 == 0) sb.append('.');
            sb.append(digits.charAt(i));
        }
        return sb.reverse().toString();
    }

    public static void styleTable(JTable table) {
        table.setFont(font(Font.PLAIN, 14));
        table.setRowHeight(38);
        table.setForeground(TEXT);
        table.setBackground(WHITE);
        table.setGridColor(BORDER);
        table.setSelectionBackground(SELECTION);
        table.setSelectionForeground(TEXT);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setOpaque(true);
        header.setBackground(CHARCOAL);
        header.setForeground(Color.WHITE);
        header.setFont(font(Font.BOLD, 13));
        header.setPreferredSize(new Dimension(0, 40));

        // [OOP - Polymorphism]: Custom TableCellRenderer meng-override rendering header dan sel tabel secara dinamis.
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tbl, val, sel, foc, r, c);
                l.setOpaque(true);
                l.setBackground(CHARCOAL);
                l.setForeground(Color.WHITE);
                l.setFont(font(Font.BOLD, 13));
                l.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 2, 0, GOLD),
                        BorderFactory.createEmptyBorder(6, 12, 6, 12)));
                l.setHorizontalAlignment(SwingConstants.LEFT);
                return l;
            }
        });

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel, boolean foc, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                l.setFont(font(Font.PLAIN, 14));
                l.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
                l.setToolTipText(val == null ? null : val.toString());
                if (sel) {
                    l.setBackground(SELECTION);
                    l.setForeground(TEXT);
                    return l;
                }
                l.setBackground(WHITE);
                l.setForeground(TEXT);
                if ("Penggunaan".equalsIgnoreCase(tbl.getColumnName(col)) && val != null && val.toString().contains("Lebih")) {
                    l.setForeground(EXPENSE);
                    l.setFont(font(Font.BOLD, 14));
                    return l;
                }
                for (int i = 0; i < tbl.getColumnCount(); i++) {
                    if ("Jenis".equalsIgnoreCase(tbl.getColumnName(i))) {
                        Object jenis = tbl.getValueAt(row, i);
                        if (jenis != null) {
                            String tipe = jenis.toString().toUpperCase(Locale.ROOT);
                            if (tipe.contains("PEMASUKAN")) {
                                l.setForeground(INCOME);
                                l.setFont(font(Font.BOLD, 14));
                            } else if (tipe.contains("PENGELUARAN")) {
                                l.setForeground(EXPENSE);
                                l.setFont(font(Font.BOLD, 14));
                            }
                        }
                        break;
                    }
                }
                return l;
            }
        });
    }

    // ------------------------------------------------------------------ helper event berbasis lambda

    public static void onHover(JComponent c, Consumer<Boolean> hoverCallback) {
        c.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { hoverCallback.accept(true); }
            @Override public void mouseExited(MouseEvent e) { hoverCallback.accept(false); }
        });
    }

    public static void onClick(JComponent c, Runnable action) {
        c.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { action.run(); }
        });
    }

    public static void onDoubleClick(JTable t, Runnable action) {
        t.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && t.getSelectedRow() >= 0) action.run();
            }
        });
    }

    public static void onMouseMove(JComponent c, Consumer<Point> onMove, Runnable onExit) {
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) { onMove.accept(e.getPoint()); }
            @Override public void mouseExited(MouseEvent e) { onExit.run(); }
        };
        c.addMouseMotionListener(ma);
        c.addMouseListener(ma);
    }

    public static void onFocusRepaint(JComponent c) {
        c.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { c.repaint(); }
            @Override public void focusLost(FocusEvent e) { c.repaint(); }
        });
    }

    public static void onWindowOpened(Window w, Runnable action) {
        w.addWindowListener(new WindowAdapter() {
            @Override public void windowOpened(WindowEvent e) { action.run(); }
        });
    }

    public static void enableDrag(Window w, JComponent... handles) {
        MouseAdapter drag = new MouseAdapter() {
            Point start;
            @Override public void mousePressed(MouseEvent e) { start = e.getPoint(); }
            @Override public void mouseDragged(MouseEvent e) {
                if (start == null) return;
                Point p = w.getLocation();
                w.setLocation(p.x + e.getX() - start.x, p.y + e.getY() - start.y);
            }
        };
        for (JComponent h : handles) {
            h.addMouseListener(drag);
            h.addMouseMotionListener(drag);
        }
    }

    // ------------------------------------------------------------------ tombol & komponen kustom

    public static JButton button(String title) { return new FlatButton(title, PRIMARY, CHARCOAL, false); }
    public static JButton buttonDark(String title) { return new FlatButton(title, CHARCOAL, WHITE, false); }
    public static JButton buttonGhost(String title) { return new FlatButton(title, WHITE, CHARCOAL, true); }
    public static JButton buttonDanger(String title) { return new FlatButton(title, DANGER, Color.WHITE, false); }

    // [OOP - Inheritance]: FlatButton mewarisi JButton untuk menggambar tombol datar kustom lintas sistem operasi.
    private static final class FlatButton extends JButton {
        private static final int ARC = 14;
        private final boolean ghost;
        private float hoverT, targetT;
        private final Timer anim;

        FlatButton(String text, Color bg, Color fg, boolean ghost) {
            super(text);
            this.ghost = ghost;
            setBackground(bg);
            setForeground(fg);
            setFont(font(Font.BOLD, 13));
            setOpaque(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(9, 20, 9, 20));

            anim = new Timer(15, e -> {
                float step = 0.22f;
                hoverT = hoverT < targetT ? Math.min(targetT, hoverT + step) : Math.max(targetT, hoverT - step);
                repaint();
                if (hoverT == targetT) ((Timer) e.getSource()).stop();
            });
            onHover(this, h -> { targetT = h ? 1f : 0f; anim.start(); });
            onFocusRepaint(this);
        }

        @Override public void removeNotify() { anim.stop(); super.removeNotify(); }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            Insets in = getInsets();
            return new Dimension(Math.max(76, fm.stringWidth(getText()) + in.left + in.right),
                    Math.max(38, fm.getHeight() + in.top + in.bottom));
        }

        // [OOP - Polymorphism]: Method overriding paintComponent menggantikan rendering standar JButton.
        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            boolean on = isEnabled(), pressed = on && getModel().isPressed();
            Color baseBg = getBackground(), bg, fg = getForeground(), edge = BORDER;
            if (!on) {
                bg = ghost ? new Color(0xF6, 0xF2, 0xE6) : new Color(0xE4, 0xDC, 0xC6);
                fg = new Color(0xAB, 0xA1, 0x8A);
            } else {
                Color hoverBg = ghost ? new Color(0xFA, 0xF1, 0xD6) : shade(baseBg, 0.90f);
                bg = mix(baseBg, hoverBg, hoverT);
                if (ghost) edge = mix(BORDER, GOLD, hoverT);
                if (pressed) bg = shade(bg, 0.92f);
            }

            int w = getWidth(), h = getHeight(), lift = pressed ? 1 : 0;
            if (on && !ghost && !pressed) {
                g.setColor(new Color(0x20, 0x0E, 0x00, 28));
                g.fill(new RoundRectangle2D.Float(1, 3, w - 2, h - 3, ARC, ARC));
            }
            RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f + lift, w - 1, h - (ghost || pressed ? 1 : 3), ARC, ARC);
            g.setColor(bg);
            g.fill(shape);
            if (ghost) { g.setColor(edge); g.draw(shape); }
            if (isFocusOwner() && on) {
                g.setColor(new Color(GOLD.getRed(), GOLD.getGreen(), GOLD.getBlue(), 150));
                g.setStroke(new BasicStroke(2f));
                g.draw(new RoundRectangle2D.Float(2f, 2f + lift, w - 4, h - (ghost || pressed ? 4 : 6), ARC - 3, ARC - 3));
            }
            g.setFont(getFont());
            FontMetrics fm = g.getFontMetrics();
            int bodyH = ghost || pressed ? h : h - 2;
            g.setColor(fg);
            g.drawString(getText(), (w - fm.stringWidth(getText())) / 2, (bodyH - fm.getHeight()) / 2 + fm.getAscent() + lift);
            g.dispose();
        }

        private static Color mix(Color a, Color b, float t) {
            return new Color(Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                    Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                    Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
        }

        private static Color shade(Color c, float factor) {
            int lum = (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 1000;
            if (lum < 70) {
                float t = (1f - factor) * 1.6f;
                return new Color(Math.min(255, Math.round(c.getRed() + (255 - c.getRed()) * t)),
                        Math.min(255, Math.round(c.getGreen() + (255 - c.getGreen()) * t)),
                        Math.min(255, Math.round(c.getBlue() + (255 - c.getBlue()) * t)));
            }
            return new Color(Math.round(c.getRed() * factor), Math.round(c.getGreen() * factor), Math.round(c.getBlue() * factor));
        }
    }

    private static final class FieldBorder implements Border {
        private final JComponent owner;
        FieldBorder(JComponent owner) { this.owner = owner; }

        @Override
        public void paintBorder(Component c, Graphics g0, int x, int y, int w, int h) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Component fo = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            boolean focus = owner.hasFocus() || (owner instanceof JComboBox<?> cb
                    && (cb.isPopupVisible() || (fo != null && SwingUtilities.isDescendingFrom(fo, owner))));
            g.setColor(focus ? GOLD : BORDER);
            g.setStroke(new BasicStroke(focus ? 2f : 1f));
            float o = focus ? 1f : 0.5f;
            g.draw(new RoundRectangle2D.Float(x + o, y + o, w - 2 * o - 1, h - 2 * o - 1, 10, 10));
            g.dispose();
        }

        @Override public Insets getBorderInsets(Component c) { return new Insets(9, 12, 9, 12); }
        @Override public boolean isBorderOpaque() { return false; }
    }

    private static final class ModernComboUI extends BasicComboBoxUI {
        @Override
        protected JButton createArrowButton() {
            JButton b = new JButton() {
                @Override protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                    int cx = getWidth() / 2, cy = getHeight() / 2;
                    Path2D chevron = new Path2D.Float();
                    chevron.moveTo(cx - 4, cy - 2);
                    chevron.lineTo(cx, cy + 2);
                    chevron.lineTo(cx + 4, cy - 2);
                    g.setColor(MUTED);
                    g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(chevron);
                    g.dispose();
                }
            };
            b.setBorder(new EmptyBorder(0, 0, 0, 6));
            b.setContentAreaFilled(false);
            b.setFocusPainted(false);
            b.setOpaque(false);
            b.setPreferredSize(new Dimension(28, 20));
            return b;
        }

        @Override public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {}
        @Override protected ComboPopup createPopup() {
            BasicComboPopup p = new BasicComboPopup(comboBox);
            p.setBorder(BorderFactory.createLineBorder(BORDER));
            return p;
        }
    }

    /** Lingkaran emas berisi inisial nama. */
    // [OOP - Inheritance]: Avatar mewarisi JComponent untuk menggambar avatar inisial vektor.
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
            g.setFont(font(Font.BOLD, Math.round(s * 0.45f)));
            FontMetrics fm = g.getFontMetrics();
            g.drawString(initial, (s - fm.stringWidth(initial)) / 2f, (s - fm.getHeight()) / 2f + fm.getAscent());
            g.dispose();
        }
    }
}
