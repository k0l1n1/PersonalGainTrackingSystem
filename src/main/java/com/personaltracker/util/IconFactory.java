package com.personaltracker.util;

import com.personaltracker.config.ThemeConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;

/**
 * High-DPI scalable vector icon generator for Swing components.
 * Renders crisp anti-aliased vector glyphs in application theme colors.
 */
public class IconFactory {

    public enum IconType {
        DASHBOARD,
        WORKOUT,
        GOAL,
        MEASUREMENT,
        NUTRITION,
        WATER,
        SLEEP,
        ANALYTICS,
        REMINDERS,
        BADGES,
        SETTINGS,
        STUDENTS,
        USER,
        LOGOUT,
        PLUS,
        CHECK,
        FIRE,
        TRASH,
        EDIT,
        REFRESH,
        TIMER,
        COPY,
        CLOSE,
        INFO,
        STAR,
        BELL,
        SUN,
        MOON
    }

    public static Icon createIcon(IconType type, int size, Color color) {
        return new VectorIcon(type, size, color);
    }

    public static Icon createIcon(IconType type, int size) {
        return new VectorIcon(type, size, ThemeConfig.TEXT_SECONDARY);
    }

    private static class VectorIcon implements Icon {
        private final IconType type;
        private final int size;
        private final Color color;

        public VectorIcon(IconType type, int size, Color color) {
            this.type = type;
            this.size = size;
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            g2.translate(x, y);
            g2.setColor(color != null ? color : ThemeConfig.TEXT_SECONDARY);

            float s = size;
            float strokeW = Math.max(1.5f, s / 12.0f);
            g2.setStroke(new BasicStroke(strokeW, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            switch (type) {
                case DASHBOARD:
                    // 4-tile grid
                    float pad = s * 0.15f;
                    float tileSize = s * 0.3f;
                    float gap = s * 0.1f;
                    g2.draw(new RoundRectangle2D.Float(pad, pad, tileSize, tileSize, 3, 3));
                    g2.draw(new RoundRectangle2D.Float(pad + tileSize + gap, pad, tileSize, tileSize, 3, 3));
                    g2.draw(new RoundRectangle2D.Float(pad, pad + tileSize + gap, tileSize, tileSize, 3, 3));
                    g2.draw(new RoundRectangle2D.Float(pad + tileSize + gap, pad + tileSize + gap, tileSize, tileSize, 3, 3));
                    break;

                case WORKOUT:
                    // Dumbbell
                    float midY = s * 0.5f;
                    g2.drawLine((int)(s * 0.25f), (int)midY, (int)(s * 0.75f), (int)midY);
                    g2.draw(new RoundRectangle2D.Float(s * 0.15f, s * 0.3f, s * 0.1f, s * 0.4f, 2, 2));
                    g2.draw(new RoundRectangle2D.Float(s * 0.08f, s * 0.35f, s * 0.07f, s * 0.3f, 2, 2));
                    g2.draw(new RoundRectangle2D.Float(s * 0.75f, s * 0.3f, s * 0.1f, s * 0.4f, 2, 2));
                    g2.draw(new RoundRectangle2D.Float(s * 0.85f, s * 0.35f, s * 0.07f, s * 0.3f, 2, 2));
                    break;

                case GOAL:
                    // Target bullseye / flag
                    g2.draw(new Ellipse2D.Float(s * 0.15f, s * 0.15f, s * 0.7f, s * 0.7f));
                    g2.draw(new Ellipse2D.Float(s * 0.32f, s * 0.32f, s * 0.36f, s * 0.36f));
                    g2.fill(new Ellipse2D.Float(s * 0.43f, s * 0.43f, s * 0.14f, s * 0.14f));
                    break;

                case MEASUREMENT:
                    // Scale / tape
                    g2.draw(new RoundRectangle2D.Float(s * 0.15f, s * 0.2f, s * 0.7f, s * 0.6f, 6, 6));
                    g2.draw(new Arc2D.Float(s * 0.32f, s * 0.28f, s * 0.36f, s * 0.28f, 0, 180, Arc2D.OPEN));
                    g2.drawLine((int)(s * 0.5f), (int)(s * 0.42f), (int)(s * 0.56f), (int)(s * 0.32f));
                    break;

                case NUTRITION:
                    // Apple / leaf
                    g2.draw(new Ellipse2D.Float(s * 0.2f, s * 0.3f, s * 0.6f, s * 0.55f));
                    Path2D stem = new Path2D.Float();
                    stem.moveTo(s * 0.5f, s * 0.3f);
                    stem.curveTo(s * 0.52f, s * 0.15f, s * 0.65f, s * 0.15f, s * 0.68f, s * 0.2f);
                    g2.draw(stem);
                    break;

                case WATER:
                    // Water drop (solid filled teardrop)
                    Path2D drop = new Path2D.Float();
                    drop.moveTo(s * 0.5f, s * 0.10f);
                    drop.curveTo(s * 0.22f, s * 0.50f, s * 0.20f, s * 0.82f, s * 0.5f, s * 0.88f);
                    drop.curveTo(s * 0.80f, s * 0.82f, s * 0.78f, s * 0.50f, s * 0.5f, s * 0.10f);
                    drop.closePath();
                    g2.fill(drop);
                    break;

                case SLEEP:
                    // Crescent moon
                    Area moon = new Area(new Ellipse2D.Float(s * 0.2f, s * 0.18f, s * 0.6f, s * 0.64f));
                    moon.subtract(new Area(new Ellipse2D.Float(s * 0.36f, s * 0.14f, s * 0.52f, s * 0.56f)));
                    g2.fill(moon);
                    break;

                case ANALYTICS:
                    // Chart bars & trend
                    g2.drawLine((int)(s * 0.15f), (int)(s * 0.85f), (int)(s * 0.85f), (int)(s * 0.85f));
                    g2.drawLine((int)(s * 0.15f), (int)(s * 0.85f), (int)(s * 0.15f), (int)(s * 0.15f));
                    g2.fillRect((int)(s * 0.28f), (int)(s * 0.55f), (int)(s * 0.12f), (int)(s * 0.3f));
                    g2.fillRect((int)(s * 0.46f), (int)(s * 0.4f), (int)(s * 0.12f), (int)(s * 0.45f));
                    g2.fillRect((int)(s * 0.64f), (int)(s * 0.25f), (int)(s * 0.12f), (int)(s * 0.6f));
                    break;

                case REMINDERS:
                    // Bell
                    Path2D bell = new Path2D.Float();
                    bell.moveTo(s * 0.5f, s * 0.2f);
                    bell.curveTo(s * 0.35f, s * 0.25f, s * 0.25f, s * 0.55f, s * 0.2f, s * 0.7f);
                    bell.lineTo(s * 0.8f, s * 0.7f);
                    bell.curveTo(s * 0.75f, s * 0.55f, s * 0.65f, s * 0.25f, s * 0.5f, s * 0.2f);
                    g2.draw(bell);
                    g2.draw(new Arc2D.Float(s * 0.42f, s * 0.7f, s * 0.16f, s * 0.14f, 180, 180, Arc2D.OPEN));
                    break;

                case BADGES:
                    // Trophy / star
                    Path2D trophy = new Path2D.Float();
                    trophy.moveTo(s * 0.28f, s * 0.22f);
                    trophy.lineTo(s * 0.72f, s * 0.22f);
                    trophy.lineTo(s * 0.68f, s * 0.5f);
                    trophy.curveTo(s * 0.62f, s * 0.65f, s * 0.38f, s * 0.65f, s * 0.32f, s * 0.5f);
                    trophy.closePath();
                    g2.draw(trophy);
                    g2.drawLine((int)(s * 0.5f), (int)(s * 0.65f), (int)(s * 0.5f), (int)(s * 0.8f));
                    g2.drawLine((int)(s * 0.35f), (int)(s * 0.8f), (int)(s * 0.65f), (int)(s * 0.8f));
                    break;


                case SETTINGS:
                    // Cog
                    g2.draw(new Ellipse2D.Float(s * 0.25f, s * 0.25f, s * 0.5f, s * 0.5f));
                    g2.draw(new Ellipse2D.Float(s * 0.4f, s * 0.4f, s * 0.2f, s * 0.2f));
                    break;

                case STUDENTS:
                    // Multiple people silhouettes (Coach & student)
                    // Primary person
                    g2.draw(new Ellipse2D.Float(s * 0.22f, s * 0.2f, s * 0.26f, s * 0.26f));
                    Path2D body1 = new Path2D.Float();
                    body1.moveTo(s * 0.12f, s * 0.82f);
                    body1.curveTo(s * 0.14f, s * 0.62f, s * 0.56f, s * 0.62f, s * 0.58f, s * 0.82f);
                    g2.draw(body1);
                    // Secondary person in background
                    g2.draw(new Ellipse2D.Float(s * 0.58f, s * 0.24f, s * 0.22f, s * 0.22f));
                    Path2D body2 = new Path2D.Float();
                    body2.moveTo(s * 0.6f, s * 0.64f);
                    body2.curveTo(s * 0.72f, s * 0.62f, s * 0.86f, s * 0.66f, s * 0.88f, s * 0.82f);
                    g2.draw(body2);
                    break;

                case USER:
                    // Head & shoulders
                    g2.draw(new Ellipse2D.Float(s * 0.35f, s * 0.18f, s * 0.3f, s * 0.3f));
                    Path2D body = new Path2D.Float();
                    body.moveTo(s * 0.2f, s * 0.82f);
                    body.curveTo(s * 0.22f, s * 0.6f, s * 0.78f, s * 0.6f, s * 0.8f, s * 0.82f);
                    g2.draw(body);
                    break;

                case LOGOUT:
                    // Exit bracket & arrow
                    g2.draw(new Line2D.Float(s * 0.5f, s * 0.2f, s * 0.2f, s * 0.2f));
                    g2.draw(new Line2D.Float(s * 0.2f, s * 0.2f, s * 0.2f, s * 0.8f));
                    g2.draw(new Line2D.Float(s * 0.2f, s * 0.8f, s * 0.5f, s * 0.8f));
                    // Arrow
                    g2.draw(new Line2D.Float(s * 0.4f, s * 0.5f, s * 0.85f, s * 0.5f));
                    g2.draw(new Line2D.Float(s * 0.7f, s * 0.35f, s * 0.85f, s * 0.5f));
                    g2.draw(new Line2D.Float(s * 0.7f, s * 0.65f, s * 0.85f, s * 0.5f));
                    break;

                case PLUS:
                    g2.draw(new Line2D.Float(s * 0.5f, s * 0.2f, s * 0.5f, s * 0.8f));
                    g2.draw(new Line2D.Float(s * 0.2f, s * 0.5f, s * 0.8f, s * 0.5f));
                    break;

                case CHECK:
                    Path2D check = new Path2D.Float();
                    check.moveTo(s * 0.2f, s * 0.52f);
                    check.lineTo(s * 0.42f, s * 0.75f);
                    check.lineTo(s * 0.8f, s * 0.28f);
                    g2.draw(check);
                    break;

                case FIRE:
                    // Flame
                    Path2D flame = new Path2D.Float();
                    flame.moveTo(s * 0.5f, s * 0.15f);
                    flame.curveTo(s * 0.3f, s * 0.35f, s * 0.2f, s * 0.55f, s * 0.25f, s * 0.75f);
                    flame.curveTo(s * 0.32f, s * 0.9f, s * 0.68f, s * 0.9f, s * 0.75f, s * 0.75f);
                    flame.curveTo(s * 0.8f, s * 0.55f, s * 0.6f, s * 0.4f, s * 0.58f, s * 0.3f);
                    flame.closePath();
                    g2.fill(flame);
                    break;

                case TRASH:
                    // Bin
                    g2.draw(new RoundRectangle2D.Float(s * 0.25f, s * 0.32f, s * 0.5f, s * 0.52f, 3, 3));
                    g2.drawLine((int)(s * 0.2f), (int)(s * 0.32f), (int)(s * 0.8f), (int)(s * 0.32f));
                    g2.draw(new Arc2D.Float(s * 0.4f, s * 0.22f, s * 0.2f, s * 0.15f, 0, 180, Arc2D.OPEN));
                    break;

                case EDIT:
                    // Pencil
                    g2.drawLine((int)(s * 0.25f), (int)(s * 0.75f), (int)(s * 0.7f), (int)(s * 0.3f));
                    g2.drawLine((int)(s * 0.7f), (int)(s * 0.3f), (int)(s * 0.78f), (int)(s * 0.38f));
                    g2.drawLine((int)(s * 0.78f), (int)(s * 0.38f), (int)(s * 0.33f), (int)(s * 0.83f));
                    g2.drawLine((int)(s * 0.33f), (int)(s * 0.83f), (int)(s * 0.25f), (int)(s * 0.75f));
                    break;

                case REFRESH:
                    g2.draw(new Arc2D.Float(s * 0.2f, s * 0.2f, s * 0.6f, s * 0.6f, 45, 270, Arc2D.OPEN));
                    g2.drawLine((int)(s * 0.7f), (int)(s * 0.2f), (int)(s * 0.8f), (int)(s * 0.38f));
                    g2.drawLine((int)(s * 0.8f), (int)(s * 0.38f), (int)(s * 0.6f), (int)(s * 0.38f));
                    break;

                case TIMER:
                    g2.draw(new Ellipse2D.Float(s * 0.2f, s * 0.25f, s * 0.6f, s * 0.6f));
                    g2.drawLine((int)(s * 0.5f), (int)(s * 0.55f), (int)(s * 0.5f), (int)(s * 0.35f));
                    g2.drawLine((int)(s * 0.5f), (int)(s * 0.55f), (int)(s * 0.65f), (int)(s * 0.55f));
                    g2.drawLine((int)(s * 0.42f), (int)(s * 0.18f), (int)(s * 0.58f), (int)(s * 0.18f));
                    break;

                case COPY:
                    // Back page outline
                    g2.draw(new Line2D.Float(s * 0.36f, s * 0.16f, s * 0.82f, s * 0.16f));
                    g2.draw(new Line2D.Float(s * 0.82f, s * 0.16f, s * 0.82f, s * 0.62f));
                    // Front document
                    g2.draw(new RoundRectangle2D.Float(s * 0.18f, s * 0.32f, s * 0.48f, s * 0.54f, 2, 2));
                    break;

                case CLOSE:
                    float m = s * 0.25f;
                    g2.draw(new Line2D.Float(m, m, s - m, s - m));
                    g2.draw(new Line2D.Float(s - m, m, m, s - m));
                    break;

                case INFO:
                    g2.draw(new Ellipse2D.Float(s * 0.15f, s * 0.15f, s * 0.7f, s * 0.7f));
                    g2.fill(new Ellipse2D.Float(s * 0.45f, s * 0.28f, s * 0.1f, s * 0.1f));
                    g2.drawLine((int)(s * 0.5f), (int)(s * 0.45f), (int)(s * 0.5f), (int)(s * 0.7f));
                    break;

                case STAR:
                    Path2D star = new Path2D.Float();
                    float cx = s * 0.5f;
                    float cy = s * 0.52f;
                    float rOuter = s * 0.42f;
                    float rInner = s * 0.18f;
                    for (int i = 0; i < 10; i++) {
                        double angle = i * Math.PI / 5.0 - Math.PI / 2.0;
                        float r = (i % 2 == 0) ? rOuter : rInner;
                        float px = (float) (cx + Math.cos(angle) * r);
                        float py = (float) (cy + Math.sin(angle) * r);
                        if (i == 0) star.moveTo(px, py);
                        else star.lineTo(px, py);
                    }
                    star.closePath();
                    g2.fill(star);
                    break;

                case BELL:
                    Path2D bellGlyph = new Path2D.Float();
                    bellGlyph.moveTo(s * 0.5f, s * 0.16f);
                    bellGlyph.curveTo(s * 0.35f, s * 0.16f, s * 0.28f, s * 0.36f, s * 0.25f, s * 0.60f);
                    bellGlyph.lineTo(s * 0.16f, s * 0.72f);
                    bellGlyph.lineTo(s * 0.84f, s * 0.72f);
                    bellGlyph.lineTo(s * 0.75f, s * 0.60f);
                    bellGlyph.curveTo(s * 0.72f, s * 0.36f, s * 0.65f, s * 0.16f, s * 0.5f, s * 0.16f);
                    bellGlyph.closePath();
                    g2.draw(bellGlyph);
                    // Clapper
                    g2.draw(new Arc2D.Float(s * 0.42f, s * 0.72f, s * 0.16f, s * 0.16f, 180, 180, Arc2D.OPEN));
                    // Top loop
                    g2.fill(new Ellipse2D.Float(s * 0.45f, s * 0.08f, s * 0.1f, s * 0.1f));
                    break;

                case MOON:
                    Area moonArea = new Area(new Ellipse2D.Float(s * 0.2f, s * 0.2f, s * 0.6f, s * 0.6f));
                    moonArea.subtract(new Area(new Ellipse2D.Float(s * 0.38f, s * 0.12f, s * 0.52f, s * 0.52f)));
                    g2.fill(moonArea);
                    break;

                case SUN:
                    float sunR = s * 0.22f;
                    g2.fill(new Ellipse2D.Float(s * 0.5f - sunR, s * 0.5f - sunR, sunR * 2, sunR * 2));
                    for (int i = 0; i < 8; i++) {
                        double a = i * Math.PI / 4.0;
                        float x1 = (float) (s * 0.5f + Math.cos(a) * s * 0.32f);
                        float y1 = (float) (s * 0.5f + Math.sin(a) * s * 0.32f);
                        float x2 = (float) (s * 0.5f + Math.cos(a) * s * 0.44f);
                        float y2 = (float) (s * 0.5f + Math.sin(a) * s * 0.44f);
                        g2.draw(new Line2D.Float(x1, y1, x2, y2));
                    }
                    break;
            }

            g2.dispose();
        }

        @Override
        public int getIconWidth() { return size; }

        @Override
        public int getIconHeight() { return size; }
    }
}
