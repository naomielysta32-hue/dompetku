package id.dompetku.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Pengganti JOptionPane dengan tampilan modern: kartu membulat, bayangan lembut,
 * ikon vektor (bukan ikon bawaan sistem), input membulat, dan tombol datar.
 * Nilai kembalian sama dengan JOptionPane (OK_OPTION = 0, CANCEL_OPTION = 2, CLOSED_OPTION = -1)
 * sehingga kode pemanggil cukup mengganti nama kelas.
 */
public final class Dialogs {

    private Dialogs() {
    }

    private static final int RADIUS = 18;
    private static final int SHADOW = 14;

    private enum Kind { INFO, WARNING, ERROR, QUESTION, FORM }

    // ------------------------------------------------------------------ API publik

    public static int showConfirmDialog(Component parent, Object message, String title, int optionType) {
        return showConfirmDialog(parent, message, title, optionType, JOptionPane.QUESTION_MESSAGE);
    }

    public static int showConfirmDialog(Component parent, Object message, String title,
                                        int optionType, int messageType) {
        Kind kind = kindOf(message, messageType);
        if (optionType == JOptionPane.OK_OPTION || optionType == JOptionPane.DEFAULT_OPTION) {
            int r = show(parent, message, title, kind, new String[]{"OK"}, 0, false);
            return r < 0 ? JOptionPane.CLOSED_OPTION : JOptionPane.OK_OPTION;
        }
        if (optionType == JOptionPane.YES_NO_OPTION) {
            int r = show(parent, message, title, kind, new String[]{"Ya", "Tidak"}, 0, kind == Kind.WARNING);
            return r == 0 ? JOptionPane.YES_OPTION : r == 1 ? JOptionPane.NO_OPTION : JOptionPane.CLOSED_OPTION;
        }
        int r = show(parent, message, title, kind, new String[]{"Simpan", "Batal"}, 0, false);
        return r == 0 ? JOptionPane.OK_OPTION : r == 1 ? JOptionPane.CANCEL_OPTION : JOptionPane.CLOSED_OPTION;
    }

    public static void showMessageDialog(Component parent, Object message) {
        showMessageDialog(parent, message, "Informasi", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showMessageDialog(Component parent, Object message, String title, int messageType) {
        show(parent, message, title, kindOf(message, messageType), new String[]{"OK"}, 0, false);
    }

    /** Setara JOptionPane.showOptionDialog; mengembalikan indeks tombol yang dipilih. */
    public static int showOptionDialog(Component parent, Object message, String title,
                                       int optionType, int messageType, Icon icon,
                                       Object[] options, Object initial) {
        String[] labels;
        if (options == null || options.length == 0) {
            labels = new String[]{"OK"};
        } else {
            labels = new String[options.length];
            for (int i = 0; i < options.length; i++) labels[i] = String.valueOf(options[i]);
        }
        int def = 0;
        for (int i = 0; i < labels.length; i++) {
            if (initial != null && labels[i].equals(String.valueOf(initial))) def = i;
        }
        boolean danger = messageType == JOptionPane.WARNING_MESSAGE || messageType == JOptionPane.ERROR_MESSAGE;
        // Tombol pertama pada dialog peringatan dianggap aksi berbahaya (mis. "Hapus").
        return show(parent, message, title, kindOf(message, messageType), labels, def, danger);
    }

    // ------------------------------------------------------------------ inti dialog

    private static Kind kindOf(Object message, int messageType) {
        switch (messageType) {
            case JOptionPane.ERROR_MESSAGE:
                return Kind.ERROR;
            case JOptionPane.WARNING_MESSAGE:
                return Kind.WARNING;
            case JOptionPane.INFORMATION_MESSAGE:
                return Kind.INFO;
            case JOptionPane.PLAIN_MESSAGE:
                return Kind.FORM;
            default:
                // QUESTION_MESSAGE: formulir -> ikon dompet/plus, pesan biasa -> ikon tanya
                return message instanceof Object[] ? Kind.FORM : Kind.QUESTION;
        }
    }

    private static int show(Component parent, Object message, String title, Kind kind,
                            String[] buttons, int defaultIndex, boolean firstIsDanger) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        if (owner == null && parent instanceof Window) owner = (Window) parent;

        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        boolean translucent = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT);
        if (translucent) {
            dialog.setBackground(new Color(0, 0, 0, 0));
        }

        final int[] result = {-1};

        Card card = new Card(translucent);
        card.setLayout(new BorderLayout());

        // ---- kepala: ikon + judul + tombol tutup
        JPanel header = new JPanel(new BorderLayout(14, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(22, 24, 6, 16));

        header.add(new IconBadge(kind), BorderLayout.WEST);

        JLabel lblTitle = new JLabel(title == null ? "" : title);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 17));
        lblTitle.setForeground(Theme.CHARCOAL);
        header.add(lblTitle, BorderLayout.CENTER);

        JButton close = new CloseButton();
        close.addActionListener(e -> dialog.dispose());
        JPanel closeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        closeWrap.setOpaque(false);
        closeWrap.add(close);
        header.add(closeWrap, BorderLayout.EAST);

        // ---- isi
        JComponent body = buildBody(message);
        body.setBorder(new EmptyBorder(8, 24, 4, 24));

        // ---- tombol
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(14, 24, 22, 24));

        JButton defaultButton = null;
        // Urutan tampilan: tombol batal di kiri, aksi utama di kanan (gaya modern)
        for (int i = buttons.length - 1; i >= 0; i--) {
            final int idx = i;
            boolean primary = i == defaultIndex;
            JButton b;
            if (primary && firstIsDanger && i == 0) {
                b = Theme.buttonDanger(buttons[i]);
            } else if (primary) {
                b = Theme.button(buttons[i]);
            } else {
                b = Theme.buttonGhost(buttons[i]);
            }
            b.addActionListener(e -> {
                result[0] = idx;
                dialog.dispose();
            });
            footer.add(b);
            if (primary) defaultButton = b;
        }

        card.add(header, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);

        int pad = translucent ? SHADOW : 0;
        JPanel root = new JPanel(new BorderLayout());
        root.setOpaque(false);
        root.setBorder(new EmptyBorder(pad, pad, pad, pad));
        root.add(card, BorderLayout.CENTER);
        dialog.setContentPane(root);

        // ESC = batal, Enter = tombol utama
        JRootPane rp = dialog.getRootPane();
        rp.registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        if (defaultButton != null) rp.setDefaultButton(defaultButton);

        // Geser dialog dengan menyeret kepala
        MouseAdapter drag = new MouseAdapter() {
            Point start;

            @Override
            public void mousePressed(MouseEvent e) {
                start = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (start == null) return;
                Point p = dialog.getLocation();
                dialog.setLocation(p.x + e.getX() - start.x, p.y + e.getY() - start.y);
            }
        };
        header.addMouseListener(drag);
        header.addMouseMotionListener(drag);
        lblTitle.addMouseListener(drag);
        lblTitle.addMouseMotionListener(drag);

        dialog.pack();
        Dimension d = dialog.getSize();
        dialog.setSize(new Dimension(Math.max(d.width, 400 + pad * 2), d.height));
        dialog.setLocationRelativeTo(owner);

        // Fokus langsung ke isian pertama supaya bisa mengetik tanpa klik dulu
        JComponent first = firstFocusable(body);
        if (first != null) {
            dialog.addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) {
                    first.requestFocusInWindow();
                }
            });
        }

        dialog.setVisible(true);
        return result[0];
    }

    // ------------------------------------------------------------------ isi dialog

    /** Ubah message (String, Component, atau Object[] campuran) menjadi panel. */
    private static JComponent buildBody(Object message) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        Object[] items = message instanceof Object[] ? (Object[]) message : new Object[]{message};
        boolean pairForm = items.length > 1 && items[0] instanceof String && !(items[0] instanceof Component);

        for (int i = 0; i < items.length; i++) {
            Object it = items[i];
            if (it == null) continue;
            JComponent c;
            if (it instanceof String) {
                String s = (String) it;
                boolean fieldLabel = pairForm && i + 1 < items.length && items[i + 1] instanceof Component;
                c = fieldLabel ? fieldLabel(s) : textBlock(s);
            } else if (it instanceof JScrollPane) {
                c = (JComponent) it;
                c.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
                c.setAlignmentX(Component.LEFT_ALIGNMENT);
                Component view = ((JScrollPane) it).getViewport().getView();
                if (view instanceof JTextArea) {
                    JTextArea ta = (JTextArea) view;
                    ta.setBackground(new Color(0xFA, 0xF6, 0xEA));
                    ta.setBorder(new EmptyBorder(10, 12, 10, 12));
                    ta.setForeground(Theme.CHARCOAL);
                }
                c.setPreferredSize(new Dimension(420, 210));
            } else if (it instanceof JComponent) {
                c = (JComponent) it;
                styleTree(c);
                if (c instanceof JLabel) {
                    c.setAlignmentX(Component.LEFT_ALIGNMENT);
                    ((JLabel) c).setForeground(Theme.TEXT);
                    c.setFont(new Font("SansSerif", Font.PLAIN, 14));
                }
            } else {
                c = textBlock(String.valueOf(it));
            }

            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            if (c instanceof JTextField || c instanceof JComboBox) {
                Dimension pref = c.getPreferredSize();
                c.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));
            }
            panel.add(c);

            // jarak: rapat antara label & isian, longgar antar pasangan
            boolean nextIsField = i + 1 < items.length && items[i + 1] instanceof Component
                    && it instanceof String;
            panel.add(Box.createVerticalStrut(nextIsField ? 5 : 12));
        }
        return panel;
    }

    private static JLabel fieldLabel(String text) {
        String t = text.endsWith(":") ? text.substring(0, text.length() - 1) : text;
        JLabel l = new JLabel(t);
        l.setFont(new Font("SansSerif", Font.BOLD, 12));
        l.setForeground(new Color(0x5B, 0x4A, 0x36));
        return l;
    }

    private static JComponent textBlock(String text) {
        String html = text.startsWith("<html>") ? text
                : "<html><div style='width:340px'>" + text.replace("&", "&amp;")
                .replace("<", "&lt;").replace("\n", "<br>") + "</div></html>";
        JLabel l = new JLabel(html);
        l.setFont(new Font("SansSerif", Font.PLAIN, 14));
        l.setForeground(Theme.TEXT);
        return l;
    }

    private static JComponent firstFocusable(Container c) {
        for (Component child : c.getComponents()) {
            if (child instanceof JTextField || child instanceof JComboBox) return (JComponent) child;
            if (child instanceof Container) {
                JComponent f = firstFocusable((Container) child);
                if (f != null) return f;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ gaya input

    /** Terapkan gaya modern ke seluruh isian di dalam komponen. */
    static void styleTree(Component c) {
        if (c instanceof JComboBox) {
            styleCombo((JComboBox<?>) c);
            return;
        }
        if (c instanceof JTextField) {
            styleField((JTextField) c);
            return;
        }
        if (c instanceof Container) {
            for (Component child : ((Container) c).getComponents()) styleTree(child);
        }
    }

    static void styleField(JTextField f) {
        if (f.getClientProperty("dk.styled") != null) return;
        f.putClientProperty("dk.styled", Boolean.TRUE);
        f.setFont(new Font("SansSerif", Font.PLAIN, 14));
        f.setForeground(Theme.CHARCOAL);
        f.setBackground(Color.WHITE);
        f.setCaretColor(Theme.CHARCOAL);
        f.setSelectionColor(Theme.SELECTION);
        f.setOpaque(true);
        f.setBorder(new FieldBorder(f));
        f.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                f.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                f.repaint();
            }
        });
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static void styleCombo(JComboBox<?> combo) {
        if (combo.getClientProperty("dk.styled") != null) return;
        combo.putClientProperty("dk.styled", Boolean.TRUE);
        combo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        combo.setBackground(Color.WHITE);
        combo.setForeground(Theme.CHARCOAL);
        combo.setUI(new ModernComboUI());
        combo.setBorder(new FieldBorder(combo));
        combo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        ListCellRenderer<?> base = combo.getRenderer();
        combo.setRenderer((ListCellRenderer) (list, value, index, selected, focus) -> {
            Component c = ((ListCellRenderer) base).getListCellRendererComponent(
                    list, value, index, selected, focus);
            if (c instanceof JLabel) {
                JLabel l = (JLabel) c;
                l.setFont(new Font("SansSerif", Font.PLAIN, 14));
                l.setBorder(index < 0 ? new EmptyBorder(0, 0, 0, 0) : new EmptyBorder(7, 10, 7, 10));
                l.setForeground(Theme.CHARCOAL);
                l.setBackground(selected && index >= 0 ? Theme.SELECTION : Color.WHITE);
                l.setOpaque(true);
            }
            return c;
        });
        combo.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                combo.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                combo.repaint();
            }
        });
    }

    /** Border membulat; emas saat fokus. */
    private static final class FieldBorder implements Border {
        private final JComponent owner;

        FieldBorder(JComponent owner) {
            this.owner = owner;
        }

        @Override
        public void paintBorder(Component c, Graphics g0, int x, int y, int w, int h) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Component fo = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            boolean focus = owner.hasFocus()
                    || (owner instanceof JComboBox
                    && (((JComboBox<?>) owner).isPopupVisible()
                    || (fo != null && SwingUtilities.isDescendingFrom(fo, owner))));
            g.setColor(focus ? Theme.GOLD : Theme.BORDER);
            g.setStroke(new BasicStroke(focus ? 2f : 1f));
            float o = focus ? 1f : 0.5f;
            g.draw(new RoundRectangle2D.Float(x + o, y + o, w - 2 * o - 1, h - 2 * o - 1, 10, 10));
            g.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(9, 12, 9, 12);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    private static final class ModernComboUI extends BasicComboBoxUI {
        @Override
        protected JButton createArrowButton() {
            JButton b = new JButton() {
                @Override
                protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                    int cx = getWidth() / 2, cy = getHeight() / 2;
                    Path2D chevron = new Path2D.Float();
                    chevron.moveTo(cx - 4, cy - 2);
                    chevron.lineTo(cx, cy + 2);
                    chevron.lineTo(cx + 4, cy - 2);
                    g.setColor(Theme.MUTED);
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

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
            // dibiarkan kosong: latar putih sudah dari komponen
        }

        @Override
        protected javax.swing.plaf.basic.ComboPopup createPopup() {
            javax.swing.plaf.basic.BasicComboPopup p = new javax.swing.plaf.basic.BasicComboPopup(comboBox);
            p.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
            return p;
        }
    }

    // ------------------------------------------------------------------ komponen visual

    /** Kartu membulat dengan bayangan lembut. */
    private static final class Card extends JPanel {
        private final boolean shadow;

        Card(boolean shadow) {
            this.shadow = shadow;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            if (shadow) {
                // bayangan: beberapa lapis bulat transparan
                for (int i = SHADOW; i > 0; i--) {
                    float a = 0.018f * (SHADOW - i + 1) / 2f;
                    g.setColor(new Color(0x20, 0x0E, 0x00, Math.min(255, Math.round(a * 255))));
                    g.fill(new RoundRectangle2D.Float(-i + 2, -i + 5, w + 2 * i - 4, h + 2 * i - 4, RADIUS + i, RADIUS + i));
                }
            }

            RoundRectangle2D rr = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, RADIUS, RADIUS);
            g.setColor(Theme.WHITE);
            g.fill(rr);

            // garis aksen emas tipis di atas kartu
            Shape oldClip = g.getClip();
            g.setClip(rr);
            g.setColor(Theme.GOLD);
            g.fillRect(0, 0, w, 4);
            g.setClip(oldClip);

            g.setColor(new Color(0xE3, 0xD8, 0xB8));
            g.draw(rr);
            g.dispose();
        }
    }

    /** Ikon bulat vektor sesuai jenis dialog. */
    private static final class IconBadge extends JComponent {
        private final Kind kind;

        IconBadge(Kind kind) {
            this.kind = kind;
            Dimension d = new Dimension(40, 40);
            setPreferredSize(d);
            setMinimumSize(d);
        }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            Color tint, fg;
            switch (kind) {
                case ERROR:
                    tint = new Color(0xFB, 0xE3, 0xE1);
                    fg = Theme.EXPENSE;
                    break;
                case WARNING:
                    tint = new Color(0xFD, 0xEF, 0xD2);
                    fg = new Color(0xB9, 0x77, 0x0E);
                    break;
                case INFO:
                    tint = new Color(0xE0, 0xF0, 0xE1);
                    fg = Theme.INCOME;
                    break;
                default:
                    tint = new Color(0xF8, 0xEC, 0xC8);
                    fg = new Color(0xA6, 0x7F, 0x12);
            }

            int s = Math.min(getWidth(), getHeight());
            g.setColor(tint);
            g.fill(new Ellipse2D.Float(0, 0, s, s));

            g.setColor(fg);
            g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float c = s / 2f;

            switch (kind) {
                case ERROR:
                    g.draw(new java.awt.geom.Line2D.Float(c - 6, c - 6, c + 6, c + 6));
                    g.draw(new java.awt.geom.Line2D.Float(c + 6, c - 6, c - 6, c + 6));
                    break;
                case WARNING: {
                    Path2D tri = new Path2D.Float();
                    tri.moveTo(c, c - 9);
                    tri.lineTo(c + 9, c + 7);
                    tri.lineTo(c - 9, c + 7);
                    tri.closePath();
                    g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(tri);
                    g.draw(new java.awt.geom.Line2D.Float(c, c - 3, c, c + 1));
                    g.fill(new Ellipse2D.Float(c - 1.3f, c + 3.2f, 2.6f, 2.6f));
                    break;
                }
                case INFO:
                    g.fill(new Ellipse2D.Float(c - 1.8f, c - 9, 3.6f, 3.6f));
                    g.draw(new java.awt.geom.Line2D.Float(c, c - 2, c, c + 8));
                    break;
                case FORM: {
                    // dompet: badan membulat + tab + titik kancing
                    g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.draw(new RoundRectangle2D.Float(c - 9, c - 6, 18, 13, 5, 5));
                    g.draw(new java.awt.geom.Line2D.Float(c - 6, c - 6, c + 2, c - 9));
                    g.fill(new Ellipse2D.Float(c + 3f, c - 0.5f, 3.4f, 3.4f));
                    break;
                }
                default:
                    // tanda tanya
                    g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    Path2D q = new Path2D.Float();
                    q.moveTo(c - 5, c - 5);
                    q.curveTo(c - 5, c - 12, c + 6, c - 12, c + 5, c - 5);
                    q.curveTo(c + 4.5f, c - 1, c, c - 1, c, c + 3);
                    g.draw(q);
                    g.fill(new Ellipse2D.Float(c - 1.4f, c + 6, 2.8f, 2.8f));
            }
            g.dispose();
        }
    }

    /** Tombol tutup (x) kecil dengan hover lembut. */
    private static final class CloseButton extends JButton {
        private boolean hover;

        CloseButton() {
            setPreferredSize(new Dimension(30, 30));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFocusable(false);
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (hover) {
                g.setColor(new Color(0xF1, 0xE9, 0xD2));
                g.fill(new Ellipse2D.Float(0, 0, getWidth(), getHeight()));
            }
            g.setColor(hover ? Theme.CHARCOAL : Theme.HINT);
            g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float c = getWidth() / 2f;
            g.draw(new java.awt.geom.Line2D.Float(c - 4.5f, c - 4.5f, c + 4.5f, c + 4.5f));
            g.draw(new java.awt.geom.Line2D.Float(c + 4.5f, c - 4.5f, c - 4.5f, c + 4.5f));
            g.dispose();
        }
    }
}
