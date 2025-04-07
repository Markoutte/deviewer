package me.markoutte.deviewer;

import me.markoutte.deviewer.jfr.StackFrame;
import me.markoutte.deviewer.utils.Trie;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;

public class IcicleGraphComponent extends JComponent {

    private List<Rectangle> rectangles = new ArrayList<>();
    private int maxDepth = 0;
    private double scale = 1.0;
    private final double FACTOR = 1.01;

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
    }

    private void resizeComponent(int x, int y, double scale) {
        JViewport parent = (JViewport) getParent();
        java.awt.Rectangle viewRect = parent.getViewRect();
        IcicleGraphComponent.this.setPreferredSize(
                new Dimension((int) (this.scale * parent.getWidth()), maxDepth * 24)
        );
        int nx = (int) Math.round(scale * (x + viewRect.x) - x);
        parent.setViewPosition(new Point(Math.max(nx, 0), viewRect.y));
        IcicleGraphComponent.this.revalidate();
        IcicleGraphComponent.this.repaint();
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
            Graphics2D g2 = (Graphics2D) g.create(x, y, width, height);
            g2.setColor(green);
            g2.fillRect(0, 0, width, height);
            g2.setColor(green2);
            g2.drawRect(0, 0, width, height);
            g2.setColor(Color.BLACK);
            g2.drawString(rectangle.frame.methodName(), 5, 16);
            g2.dispose();
        }
    }

    private static record Rectangle(
            double start,
            double end,
            int depth,
            StackFrame frame
    ) {}
}
