package me.markoutte.deviewer;

import me.markoutte.deviewer.jfr.StackFrame;
import me.markoutte.deviewer.utils.Trie;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

public class IcicleGraphComponent extends JComponent {

    private List<Rectangle> rectangles = new ArrayList<>();
    private int maxDepth = 0;
    private double scale = 1.0;
    private final double FACTOR = 1.01;
    private Point point = null;
    private Rectangle hoveredRectangle = null;

    public IcicleGraphComponent(StackFrame root, Trie<StackFrame, StackFrame> trie) {
        traverse(trie, trie.getImpl(Collections.singleton(root)), 0.0, 1.0, 0);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        JViewport container = (JViewport) getParent();
        container.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                JViewport component = (JViewport) e.getComponent();
                java.awt.Rectangle viewRect = component.getViewRect();
                resizeComponent(0, 0, 1.0);
                container.removeComponentListener(this);
            }
        });
        container.addMouseWheelListener(e -> {
            Component comp = e.getComponent();
            if (e.isControlDown()) {
                e.consume();
                double factor = FACTOR;
                if (e.getPreciseWheelRotation() < 0) {
                    factor = 1 / FACTOR;
                }
                scale *= factor;
                scale = Math.max(1.0, scale);
                resizeComponent(e.getX(), e.getY(), factor);
            } else {
                comp.getParent().dispatchEvent(e);
            }
        });
        container.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point position = container.getViewPosition();
                point = new Point(
                        position.x + e.getX(),
                        position.y + e.getY()
                );
                repaint();
            }
        });
        container.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && hoveredRectangle != null) {
                    java.awt.Rectangle rect = container.getViewRect();
                    scale = 1 / (hoveredRectangle.end - hoveredRectangle.start);
                    int newWidth = (int) Math.round(rect.width * scale);
                    int newHeight = 24 * maxDepth;
                    int newX = (int) Math.round(newWidth * hoveredRectangle.start);
                    int newY = rect.y;
                    point = null;
                    hoveredRectangle = null;
                    IcicleGraphComponent.this.setPreferredSize(new Dimension(newWidth, newHeight));
                    container.revalidate();
                    container.repaint();
                    SwingUtilities.invokeLater(() -> {
                        container.setViewPosition(new Point(newX, newY));
                    });
                }
            }
        });
    }

    private void resizeComponent(int x, int y, double scale) {
        JViewport parent = (JViewport) getParent();
        java.awt.Rectangle viewRect = parent.getViewRect();
        IcicleGraphComponent.this.setPreferredSize(
                new Dimension((int) (this.scale * parent.getWidth()), maxDepth * 24)
        );
        int nx = (int) Math.round(scale * (x + viewRect.x) - x);
        parent.setViewPosition(new Point(Math.max(nx, 0), viewRect.y));
        parent.revalidate();
        parent.repaint();
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
