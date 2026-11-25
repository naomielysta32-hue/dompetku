package id.dompetku.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Pengganti JOptionPane dengan tampilan kartu modern, bayangan lembut, dan ikon vektor.
 */
// [OOP - Encapsulation]: Seluruh mekanisme pembuatan modal JDialog, styling konten, dan animasi dibungkus di balik API mirip JOptionPane.
public final class Dialogs {

    private Dialogs() {}

    private static final int RADIUS = 18;
    private static final int SHADOW = 14;
    private enum Kind { INFO, WARNING, ERROR, QUESTION, FORM }

    // ------------------------------------------------------------------ API publik

    // [OOP - Polymorphism]: Method overloading showConfirmDialog untuk variasi argumen optionType dan messageType.
    public static int showConfirmDialog(Component parent, Object message, String title, int optionType) {
        return showConfirmDialog(parent, message, title, optionType, JOptionPane.QUESTION_MESSAGE);
    }

    public static int showConfirmDialog(Component parent, Object message, String title, int optionType, int messageType) {
        Kind kind = kindOf(message, messageType);
        if (optionType == JOptionPane.OK_OPTION || optionType == JOptionPane.DEFAULT_OPTION) {
            return show(parent, message, title, kind, new String[]{"OK"}, 0, false) < 0
                    ? JOptionPane.CLOSED_OPTION : JOptionPane.OK_OPTION;
        }
        if (optionType == JOptionPane.YES_NO_OPTION) {
            int r = show(parent, message, title, kind, new String[]{"Ya", "Tidak"}, 0, kind == Kind.WARNING);
            return r == 0 ? JOptionPane.YES_OPTION : r == 1 ? JOptionPane.NO_OPTION : JOptionPane.CLOSED_OPTION;
        }
        int r = show(parent, message, title, kind, new String[]{"Simpan", "Batal"}, 0, false);
        return r == 0 ? JOptionPane.OK_OPTION : r == 1 ? JOptionPane.CANCEL_OPTION : JOptionPane.CLOSED_OPTION;
    }

    // [OOP - Polymorphism]: Method overloading showMessageDialog untuk pesan standar vs judul & tipe khusus.
    public static void showMessageDialog(Component parent, Object message) {
        showMessageDialog(parent, message, "Informasi", JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showMessageDialog(Component parent, Object message, String title, int messageType) {
        show(parent, message, title, kindOf(message, messageType), new String[]{"OK"}, 0, false);
    }

    public static int showOptionDialog(Component parent, Object message, String title, int optionType,
                                       int messageType, Icon icon, Object[] options, Object initial) {
        String[] labels = (options == null || options.length == 0) ? new String[]{"OK"}
                : java.util.Arrays.stream(options).map(String::valueOf).toArray(String[]::new);
        int def = 0;
        for (int i = 0; i < labels.length; i++) {
            if (initial != null && labels[i].equals(String.valueOf(initial))) def = i;
        }
        boolean danger = messageType == JOptionPane.WARNING_MESSAGE || messageType == JOptionPane.ERROR_MESSAGE;
        return show(parent, message, title, kindOf(message, messageType), labels, def, danger);
    }

    // ------------------------------------------------------------------ inti dialog

    private static Kind kindOf(Object message, int messageType) {
        return switch (messageType) {
            case JOptionPane.ERROR_MESSAGE -> Kind.ERROR;
            case JOptionPane.WARNING_MESSAGE -> Kind.WARNING;
            case JOptionPane.INFORMATION_MESSAGE -> Kind.INFO;
            case JOptionPane.PLAIN_MESSAGE -> Kind.FORM;
            default -> message instanceof Object[] ? Kind.FORM : Kind.QUESTION;
        };
    }

    private static int show(Component parent, Object message, String title, Kind kind,
                            String[] buttons, int defaultIndex, boolean firstIsDanger) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        if (owner == null && parent instanceof Window w) owner = w;

        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        boolean translucent = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT);
        if (translucent) dialog.setBackground(new Color(0, 0, 0, 0));

        final int[] result = {-1};
        Card card = new Card(translucent);
        card.setLayout(new BorderLayout());

        // Header: ikon + judul + tombol tutup
        JPanel header = new JPanel(new BorderLayout(14, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(22, 24, 6, 16));
        header.add(new IconBadge(kind), BorderLayout.WEST);

        JLabel lblTitle = Theme.label(title == null ? "" : title, Font.BOLD, 17, Theme.CHARCOAL);
        header.add(lblTitle, BorderLayout.CENTER);

        JButton close = new CloseButton();
        close.addActionListener(e -> dialog.dispose());
        JPanel closeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        closeWrap.setOpaque(false);
        closeWrap.add(close);
        header.add(closeWrap, BorderLayout.EAST);

        // Isi & Footer
        JComponent body = buildBody(message);
        body.setBorder(new EmptyBorder(8, 24, 4, 24));

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(14, 24, 22, 24));

        JButton defaultButton = null;
        for (int i = buttons.length - 1; i >= 0; i--) {
            final int idx = i;
            boolean primary = i == defaultIndex;
            JButton b = (primary && firstIsDanger && i == 0) ? Theme.buttonDanger(buttons[i])
                    : primary ? Theme.button(buttons[i]) : Theme.buttonGhost(buttons[i]);
            b.addActionListener(e -> { result[0] = idx; dialog.dispose(); });
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

        JRootPane rp = dialog.getRootPane();
        rp.registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        if (defaultButton != null) rp.setDefaultButton(defaultButton);

        Theme.enableDrag(dialog, header, lblTitle);
        dialog.pack();
        Dimension d = dialog.getSize();
        dialog.setSize(new Dimension(Math.max(d.width, 400 + pad * 2), d.height));
        dialog.setLocationRelativeTo(owner);

        JComponent first = firstFocusable(body);
        if (first != null) Theme.onWindowOpened(dialog, first::requestFocusInWindow);

        dialog.setVisible(true);
        return result[0];
    }

    // ------------------------------------------------------------------ isi dialog

    private static JComponent buildBody(Object message) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        Object[] items = message instanceof Object[] arr ? arr : new Object[]{message};
        boolean pairForm = items.length > 1 && items[0] instanceof String && !(items[0] instanceof Component);

        for (int i = 0; i < items.length; i++) {
            Object it = items[i];
            if (it == null) continue;
            JComponent c;
            if (it instanceof String s) {
                boolean fieldLabel = pairForm && i + 1 < items.length && items[i + 1] instanceof Component;
                c = fieldLabel ? fieldLabel(s) : textBlock(s);
            } else if (it instanceof JScrollPane sp) {
                c = sp;
                c.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
                if (sp.getViewport().getView() instanceof JTextArea ta) {
                    ta.setBackground(new Color(0xFA, 0xF6, 0xEA));
                    ta.setBorder(new EmptyBorder(10, 12, 10, 12));
                    ta.setForeground(Theme.CHARCOAL);
                }
                c.setPreferredSize(new Dimension(420, 210));
            } else if (it instanceof JComponent jc) {
                c = jc;
                Theme.styleTree(c);
                if (c instanceof JLabel l) {
                    l.setForeground(Theme.TEXT);
                    l.setFont(Theme.font(Font.PLAIN, 14));
                }
            } else {
                c = textBlock(String.valueOf(it));
            }

            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            if (c instanceof JTextField || c instanceof JComboBox) {
                c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
            }
            panel.add(c);
            boolean nextIsField = i + 1 < items.length && items[i + 1] instanceof Component && it instanceof String;
            panel.add(Box.createVerticalStrut(nextIsField ? 5 : 12));
        }
        return panel;
    }

    private static JLabel fieldLabel(String text) {
        String t = text.endsWith(":") ? text.substring(0, text.length() - 1) : text;
        return Theme.label(t, Font.BOLD, 12, new Color(0x5B, 0x4A, 0x36));
    }

    private static JComponent textBlock(String text) {
        String html = text.startsWith("<html>") ? text
                : "<html><div style='width:340px'>" + text.replace("&", "&amp;").replace("<", "&lt;").replace("\n", "<br>") + "</div></html>";
        return Theme.label(html, Font.PLAIN, 14, Theme.TEXT);
    }

    private static JComponent firstFocusable(Container c) {
        for (Component child : c.getComponents()) {
            if (child instanceof JTextField || child instanceof JComboBox) return (JComponent) child;
            if (child instanceof Container ct) {
                JComponent f = firstFocusable(ct);
                if (f != null) return f;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ komponen visual

    // [OOP - Inheritance]: Card mewarisi JPanel untuk menggambar panel dialog membulat berbayangan.
    private static final class Card extends JPanel {
        private final boolean shadow;
        Card(boolean shadow) { this.shadow = shadow; setOpaque(false); }

        @Override
        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            if (shadow) {
                for (int i = SHADOW; i > 0; i--) {
                    float a = 0.018f * (SHADOW - i + 1) / 2f;
                    g.setColor(new Color(0x20, 0x0E, 0x00, Math.min(255, Math.round(a * 255))));
                    g.fill(new RoundRectangle2D.Float(-i + 2, -i + 5, w + 2 * i - 4, h + 2 * i - 4, RADIUS + i, RADIUS + i));
                }
            }
            RoundRectangle2D rr = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, RADIUS, RADIUS);
            g.setColor(Theme.WHITE);
            g.fill(rr);
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

    // [OOP - Inheritance]: IconBadge mewarisi JComponent untuk merender ikon vektor status dialog.
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

            Color tint = switch (kind) {
                case ERROR -> new Color(0xFB, 0xE3, 0xE1);
                case WARNING -> new Color(0xFD, 0xEF, 0xD2);
                case INFO -> new Color(0xE0, 0xF0, 0xE1);
                default -> new Color(0xF8, 0xEC, 0xC8);
            };
            Color fg = switch (kind) {
                case ERROR -> Theme.EXPENSE;
                case WARNING -> new Color(0xB9, 0x77, 0x0E);
                case INFO -> Theme.INCOME;
                default -> new Color(0xA6, 0x7F, 0x12);
            };

            int s = Math.min(getWidth(), getHeight());
            g.setColor(tint);
            g.fill(new Ellipse2D.Float(0, 0, s, s));
            g.setColor(fg);
            g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float c = s / 2f;

            switch (kind) {
                case ERROR -> {
                    g.draw(new Line2D.Float(c - 6, c - 6, c + 6, c + 6));
                    g.draw(new Line2D.Float(c + 6, c - 6, c - 6, c + 6));
                }
                case WARNING -> {
                    Path2D tri = new Path2D.Float();
                    tri.moveTo(c, c - 9); tri.lineTo(c + 9, c + 7); tri.lineTo(c - 9, c + 7); tri.closePath();
                    g.draw(tri);
                    g.draw(new Line2D.Float(c, c - 3, c, c + 1));
                    g.fill(new Ellipse2D.Float(c - 1.3f, c + 3.2f, 2.6f, 2.6f));
                }
                case INFO -> {
                    g.fill(new Ellipse2D.Float(c - 1.8f, c - 9, 3.6f, 3.6f));
                    g.draw(new Line2D.Float(c, c - 2, c, c + 8));
                }
                case FORM -> {
                    g.draw(new RoundRectangle2D.Float(c - 9, c - 6, 18, 13, 5, 5));
                    g.draw(new Line2D.Float(c - 6, c - 6, c + 2, c - 9));
                    g.fill(new Ellipse2D.Float(c + 3f, c - 0.5f, 3.4f, 3.4f));
                }
                default -> {
                    Path2D q = new Path2D.Float();
                    q.moveTo(c - 5, c - 5);
                    q.curveTo(c - 5, c - 12, c + 6, c - 12, c + 5, c - 5);
                    q.curveTo(c + 4.5f, c - 1, c, c - 1, c, c + 3);
                    g.draw(q);
                    g.fill(new Ellipse2D.Float(c - 1.4f, c + 6, 2.8f, 2.8f));
                }
            }
            g.dispose();
        }
    }

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
            Theme.onHover(this, h -> { hover = h; repaint(); });
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
            g.draw(new Line2D.Float(c - 4.5f, c - 4.5f, c + 4.5f, c + 4.5f));
            g.draw(new Line2D.Float(c + 4.5f, c - 4.5f, c - 4.5f, c + 4.5f));
            g.dispose();
        }
    }
}
