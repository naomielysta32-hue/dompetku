package id.dompetku.ui;

import id.dompetku.dao.KeuanganDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.*;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Panel Swing khusus untuk menggambar diagram donat rincian kategori.
 */
// [OOP - Inheritance]: CategoryPiePanel mewarisi JPanel untuk membuat komponen diagram donat kustom.
public class CategoryPiePanel extends JPanel {

    private static final NumberFormat RP = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID"));
    static { RP.setMinimumFractionDigits(0); RP.setMaximumFractionDigits(0); }

    private static final Color[] PALETTE = {
            new Color(91, 108, 214), new Color(32, 163, 158), new Color(238, 108, 88),
            new Color(150, 104, 205), new Color(62, 170, 110), new Color(70, 160, 225),
            new Color(222, 92, 140), new Color(100, 120, 145), new Color(110, 190, 170),
            new Color(120, 80, 130)
    };

    private static final int RING_TOP = 72, MAX_DIAMETER = 220, ROW_H = 40;
    private static final double INNER_RATIO = 0.60;

    private final String title;
    // [OOP - Encapsulation]: State data grafik, progres animasi, dan indeks hover disembunyikan sebagai field private.
    // [OOP - Association]: Panel ini menampilkan koleksi DTO KategoriLaporan dari KeuanganDAO.
    private List<KeuanganDAO.KategoriLaporan> data = new ArrayList<>();
    private BigDecimal total = BigDecimal.ZERO;
    private double progress = 1.0;
    private int hover = -1;
    private final List<Rectangle> legendRows = new ArrayList<>();
    private Timer animator;

    public CategoryPiePanel(String title) {
        this.title = title;
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER), new EmptyBorder(15, 12, 15, 12)));
        Theme.onMouseMove(this, this::updateHover, () -> setHover(-1));
    }

    public void setData(List<KeuanganDAO.KategoriLaporan> newData) {
        data = new ArrayList<>(newData);
        total = data.stream().map(KeuanganDAO.KategoriLaporan::total).reduce(BigDecimal.ZERO, BigDecimal::add);
        hover = -1;
        revalidate();
        startAnimation();
    }

    private void startAnimation() {
        if (animator != null) animator.stop();
        if (data.isEmpty()) { progress = 1; repaint(); return; }
        progress = 0;
        final long begin = System.nanoTime();
        animator = new Timer(15, e -> {
            double t = (System.nanoTime() - begin) / 650_000_000.0;
            if (t >= 1) { t = 1; ((Timer) e.getSource()).stop(); }
            progress = 1 - Math.pow(1 - t, 3);
            repaint();
        });
        animator.start();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(430, RING_TOP + MAX_DIAMETER + 30 + data.size() * ROW_H + 24);
    }

    private void setHover(int index) {
        if (index != hover) {
            hover = index;
            setCursor(index >= 0 ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR) : Cursor.getDefaultCursor());
            repaint();
        }
    }

    private void updateHover(Point p) {
        int found = -1;
        for (int i = 0; i < legendRows.size(); i++) {
            if (legendRows.get(i).contains(p)) { found = i; break; }
        }
        if (found < 0 && !data.isEmpty() && total.signum() > 0) {
            double d = diameter(), dx = p.x - getWidth() / 2.0, dy = p.y - (RING_TOP + d / 2.0);
            double dist = Math.hypot(dx, dy), outer = d / 2.0, inner = outer * INNER_RATIO;
            if (dist >= inner && dist <= outer + 8) {
                double angle = Math.toDegrees(Math.atan2(dx, -dy));
                if (angle < 0) angle += 360;
                double acc = 0;
                for (int i = 0; i < data.size(); i++) {
                    double sweep = share(i) * 360;
                    if (angle >= acc && angle < acc + sweep) { found = i; break; }
                    acc += sweep;
                }
            }
        }
        setHover(found);
    }

    private int diameter() { return Math.max(120, Math.min(MAX_DIAMETER, getWidth() - 90)); }
    private double share(int i) { return data.get(i).total().doubleValue() / total.doubleValue(); }
    private Color colorOf(int i) { return PALETTE[i % PALETTE.length]; }

    private static Color mix(Color a, Color b, double t) {
        return new Color((int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    private static String ellipsize(String s, FontMetrics fm, int maxWidth) {
        if (fm.stringWidth(s) <= maxWidth) return s;
        String t = s;
        while (t.length() > 1 && fm.stringWidth(t + "…") > maxWidth) t = t.substring(0, t.length() - 1);
        return t + "…";
    }

    private static Font fitFont(Graphics2D g2, String text, int maxWidth, int startSize, int style) {
        int size = startSize;
        Font f = Theme.font(style, size);
        while (size > 9 && g2.getFontMetrics(f).stringWidth(text) > maxWidth) f = Theme.font(style, --size);
        return f;
    }

    private static void drawCentered(Graphics2D g2, String text, double cx, double baselineY) {
        g2.drawString(text, (float) (cx - g2.getFontMetrics().stringWidth(text) / 2.0), (float) baselineY);
    }

    // [OOP - Polymorphism]: Method overriding paintComponent menggambar grafik donat, label tengah, dan legenda.
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        int w = getWidth();
        drawTitle(g2, w);
        legendRows.clear();

        if (data.isEmpty() || total.signum() <= 0) {
            drawEmpty(g2, w);
        } else {
            drawDonut(g2, w);
            drawCenterText(g2, w);
            drawLegend(g2, w);
        }
        g2.dispose();
    }

    private void drawTitle(Graphics2D g2, int w) {
        g2.setColor(Theme.TEXT);
        g2.setFont(Theme.font(Font.BOLD, 17));
        g2.drawString(title, (w - g2.getFontMetrics().stringWidth(title)) / 2, 30);
        g2.setColor(new Color(225, 220, 205));
        g2.drawLine(20, 45, w - 20, 45);
    }

    private void drawEmpty(Graphics2D g2, int w) {
        double d = diameter(), x0 = (w - d) / 2.0, hole = d * INNER_RATIO;
        Area ring = new Area(new Ellipse2D.Double(x0, RING_TOP, d, d));
        ring.subtract(new Area(new Ellipse2D.Double(x0 + (d - hole) / 2.0, RING_TOP + (d - hole) / 2.0, hole, hole)));
        g2.setColor(new Color(238, 234, 225));
        g2.fill(ring);
        g2.setColor(Color.GRAY);
        g2.setFont(Theme.font(Font.PLAIN, 12));
        double cy = RING_TOP + d / 2.0;
        drawCentered(g2, "Belum ada transaksi", w / 2.0, cy - 2);
        drawCentered(g2, "pada periode ini", w / 2.0, cy + 15);
    }

    private void drawDonut(Graphics2D g2, int w) {
        double d = diameter(), x0 = (w - d) / 2.0, y0 = RING_TOP, hole = d * INNER_RATIO;
        Ellipse2D holeShape = new Ellipse2D.Double(x0 + (d - hole) / 2.0, y0 + (d - hole) / 2.0, hole, hole);

        for (int i = 8; i >= 1; i--) {
            double grow = i * 1.5;
            g2.setColor(new Color(60, 45, 20, 7));
            g2.fill(new Ellipse2D.Double(x0 - grow, y0 + 9 - grow, d + 2 * grow, d + 2 * grow));
        }

        int n = data.size();
        double gap = n > 1 ? 2.5 : 0, start = 90;
        for (int i = 0; i < n; i++) {
            double sweep = share(i) * 360 * progress, extent = Math.max(sweep - gap, sweep * 0.5);
            if (extent <= 0.01) { start -= sweep; continue; }

            Area seg = extent >= 359.9
                    ? new Area(new Ellipse2D.Double(x0, y0, d, d))
                    : new Area(new Arc2D.Double(x0, y0, d, d, start - (sweep - extent) / 2.0, -extent, Arc2D.PIE));
            seg.subtract(new Area(holeShape));
            if (i == hover) {
                double mid = Math.toRadians(start - sweep / 2.0);
                seg.transform(AffineTransform.getTranslateInstance(Math.cos(mid) * 7, -Math.sin(mid) * 7));
            }
            Color base = (hover >= 0 && hover != i) ? mix(colorOf(i), Color.WHITE, 0.45) : colorOf(i);
            g2.setPaint(new GradientPaint(0f, (float) y0, mix(base, Color.WHITE, 0.22),
                    0f, (float) (y0 + d), mix(base, Color.BLACK, 0.12)));
            g2.fill(seg);
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(new Color(255, 255, 255, 90));
            g2.draw(seg);
            start -= sweep;
        }

        g2.setColor(Color.WHITE);
        g2.fill(holeShape);
        g2.setPaint(new RadialGradientPaint(new Point2D.Double(w / 2.0, y0 + d / 2.0), (float) (hole / 2.0),
                new float[]{0f, 0.82f, 1f},
                new Color[]{new Color(255, 255, 255, 0), new Color(255, 255, 255, 0), new Color(60, 45, 20, 38)}));
        g2.fill(holeShape);
        g2.setStroke(new BasicStroke(1f));
        g2.setColor(new Color(0, 0, 0, 18));
        g2.draw(holeShape);
    }

    private void drawCenterText(Graphics2D g2, int w) {
        double d = diameter(), cx = w / 2.0, cy = RING_TOP + d / 2.0;
        int maxWidth = (int) (d * INNER_RATIO * 0.80);
        boolean hovered = hover >= 0 && hover < data.size();
        var item = hovered ? data.get(hover) : null;
        String line1 = hovered ? item.nama() : "TOTAL";
        String line2 = RP.format(hovered ? item.total() : total);
        String line3 = hovered ? item.persentase().toPlainString() + "%" : data.size() + " kategori";

        g2.setFont(Theme.font(Font.BOLD, 11));
        g2.setColor(hovered ? colorOf(hover) : Color.GRAY);
        drawCentered(g2, ellipsize(line1, g2.getFontMetrics(), maxWidth), cx, cy - 14);
        g2.setFont(fitFont(g2, line2, maxWidth, 17, Font.BOLD));
        g2.setColor(Theme.TEXT);
        drawCentered(g2, line2, cx, cy + 8);
        g2.setFont(Theme.font(Font.PLAIN, 11));
        g2.setColor(Color.GRAY);
        drawCentered(g2, line3, cx, cy + 26);
    }

    private void drawLegend(Graphics2D g2, int w) {
        int top = RING_TOP + diameter() + 30, right = w - 16;
        Font bold12 = Theme.font(Font.BOLD, 12), plain12 = Theme.font(Font.PLAIN, 12);
        FontMetrics amtFM = g2.getFontMetrics(bold12), pctFM = g2.getFontMetrics(plain12);

        int amtW = 0, pctW = 0;
        for (var item : data) {
            amtW = Math.max(amtW, amtFM.stringWidth(RP.format(item.total())));
            pctW = Math.max(pctW, pctFM.stringWidth(item.persentase().toPlainString() + "%"));
        }
        int pctRight = right - amtW - 14, nameX = 54, nameMax = pctRight - pctW - 12 - nameX;

        for (int i = 0; i < data.size(); i++) {
            var item = data.get(i);
            Rectangle row = new Rectangle(8, top + i * ROW_H, w - 16, ROW_H - 2);
            legendRows.add(row);
            if (i == hover) {
                g2.setColor(new Color(248, 244, 234));
                g2.fillRoundRect(row.x, row.y, row.width, row.height, 12, 12);
            }
            int midY = row.y + row.height / 2;
            Color c = colorOf(i);
            g2.setColor(mix(c, Color.WHITE, 0.80));
            g2.fillOval(row.x + 10, midY - 13, 26, 26);
            g2.setColor(c);
            g2.fillOval(row.x + 18, midY - 5, 10, 10);

            g2.setFont(bold12);
            g2.setColor(Theme.TEXT);
            g2.drawString(ellipsize(item.nama(), amtFM, nameMax), nameX, midY + (amtFM.getAscent() - amtFM.getDescent()) / 2);

            String pct = item.persentase().toPlainString() + "%";
            g2.setFont(plain12);
            g2.setColor(Color.GRAY);
            g2.drawString(pct, pctRight - pctFM.stringWidth(pct), midY + (pctFM.getAscent() - pctFM.getDescent()) / 2);

            String amt = RP.format(item.total());
            g2.setFont(bold12);
            g2.setColor(Theme.TEXT);
            g2.drawString(amt, right - amtFM.stringWidth(amt), midY + (amtFM.getAscent() - amtFM.getDescent()) / 2);

            if (i < data.size() - 1) {
                g2.setColor(new Color(240, 236, 226));
                g2.drawLine(nameX, row.y + ROW_H - 1, right, row.y + ROW_H - 1);
            }
        }
    }
}
