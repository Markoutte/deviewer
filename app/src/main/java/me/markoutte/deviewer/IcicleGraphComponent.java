package me.markoutte.deviewer;

import com.formdev.flatlaf.util.Animator;
import com.intellij.ui.scroll.LatchingScroll;
import com.intellij.util.animation.Animations;
import com.intellij.util.animation.Easing;
import com.intellij.util.animation.JBAnimator;
import me.markoutte.deviewer.jfr.StackFrame;
import me.markoutte.deviewer.utils.Trie;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

public class IcicleGraphComponent extends JComponent {

    private final List<Rectangle> rectangles = new ArrayList<>();
    private int maxDepth = 0;
    private double scale = 1.0;
    private final double FACTOR = 1.05;
    private Point point = null;
    private Rectangle hoveredRectangle = null;
    private JBAnimator animator = new JBAnimator();

    public IcicleGraphComponent(StackFrame root, Trie<StackFrame, StackFrame> trie) {
        traverse(trie, trie.getImpl(Collections.singleton(root)), 0.0, 1.0, 0);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        JViewport viewport = (JViewport) getParent();
        viewport.addMouseWheelListener(new MouseWheelListener() {
            LatchingScroll ls = new LatchingScroll();
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (ls.shouldBeIgnored(e)) {
                    e.consume();
                }
            }
        });
        viewport.addMouseWheelListener(e -> {
            Component comp = e.getComponent();
            if (e.isControlDown()) {
                e.consume();
                double factor = FACTOR;
                if (e.getPreciseWheelRotation() >= 0) {
                    factor = 1 / FACTOR;
                }
                resizeComponent(e.getX(), e.getY(), factor);
            } else {
                comp.getParent().dispatchEvent(e);
            }
        });
        viewport.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point position = viewport.getViewPosition();
                point = new Point(
                        position.x + e.getX(),
                        position.y + e.getY()
                );
                repaint();
            }
        });
        viewport.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && hoveredRectangle != null) {
                    java.awt.Rectangle rect = viewport.getViewRect();
                    int oldWidth = (int) Math.round(rect.width * scale);
                    scale = 1 / (hoveredRectangle.end - hoveredRectangle.start);
                    int newWidth = (int) Math.round(rect.width * scale);
                    int newX = (int) Math.round(newWidth * hoveredRectangle.start);
                    int newY = rect.y;
                    point = null;
                    hoveredRectangle = null;
                    animator.animate(Animations.animation(
                            new java.awt.Rectangle(rect.x, rect.y, oldWidth, maxDepth * 24),
                            new java.awt.Rectangle(newX, newY, newWidth, maxDepth * 24),
                            value -> {
                        try {
                            SwingUtilities.invokeAndWait(() -> {
                                viewport.setViewSize(value.getSize());
                                viewport.setViewPosition(value.getLocation());
                                viewport.invalidate();
                                viewport.repaint();
                            });
                        } catch (InterruptedException | InvocationTargetException ex) {
                            throw new RuntimeException(ex);
                        }
                    }).setDuration(500).setEasing(Easing.EASE_IN_OUT).setDelay(8));
                }
            }
        });
        // force resizing
        resizeComponent(0, 0, 1.0);
    }

    private void resizeComponent(int mx, int my, double scale) {
        this.scale = Math.max(this.scale * scale, 1.0);
        JViewport viewport = (JViewport) getParent();
        java.awt.Rectangle viewRect = viewport.getViewRect();
        int nx = (int) Math.round(scale * (mx + viewRect.x) - mx);
        viewport.setViewSize(new Dimension((int) Math.round(this.scale * viewport.getWidth()), maxDepth * 24));
        viewport.setViewPosition(new Point(Math.max(nx, 0), viewRect.y));
        viewport.revalidate();
        viewport.repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return super.getSize();
    }

    private void traverse(Trie<StackFrame, StackFrame> trie, Trie.Node<StackFrame> node, double start, double end, int depth) {
        maxDepth = Math.max(depth, maxDepth);
        rectangles.add(new Rectangle(start, end, depth, node.getData()));
        List<Trie.Node<StackFrame>> children = trie.children(node);
        double s = start;
        double sc = end - start;
        for (Trie.Node<StackFrame> child : children) {
            double w = sc * (child.getHit() * 1.0 / node.getHit());
            traverse(trie, child, s, s + w, depth + 1);
            s += w;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        java.awt.Rectangle bounds = getBounds();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, bounds.width, bounds.height);
        Color green = new Color(68, 236, 190);
        Color greenh = new Color(31, 209, 160);
        Color green2 = new Color(25, 131, 104);
        java.awt.Rectangle rect = getVisibleRect();
        for (Rectangle rectangle : rectangles) {
            int x = (int) (rectangle.start * bounds.width);
            int width = (int) ((rectangle.end - rectangle.start) * bounds.width);
            int y = rectangle.depth * 24;
            int height = 24;
            if (!rect.intersects(x, y, width, height) || width <= 1) {
                continue;
            }
            boolean hovered = point != null && point.x > x && point.y > y && point.x < x + width && point.y < y + height;
            Graphics2D g2 = (Graphics2D) g.create(x, y, width, height);
            g2.setColor(!hovered ? green : greenh);
            g2.fillRect(0, 0, width, height);
            g2.setColor(green2);
            g2.drawRect(0, 0, width, height);
            g2.setColor(Color.BLACK);
            int max = Math.max(0, rect.x - x);
            g2.drawString(rectangle.frame.methodName(), max + 5, 16);
            g2.dispose();
            if (hovered) {
                hoveredRectangle = rectangle;
            }
        }
    }

    private static record Rectangle(
            double start,
            double end,
            int depth,
            StackFrame frame
    ) {}
}
