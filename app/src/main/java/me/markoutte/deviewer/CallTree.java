package me.markoutte.deviewer;

import me.markoutte.deviewer.jfr.StackFrame;
import me.markoutte.deviewer.utils.Trie;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CallTree extends JTree {

    public CallTree(StackFrame root, Trie<StackFrame, StackFrame> trie) {
        setLargeModel(true);
        setShowsRootHandles(true);
//        jTree.setRootVisible(true);
        setModel(new TreeModel() {

            private final List<TreeModelListener> listeners = new ArrayList<>();

            @Override
            public Object getRoot() {
                return trie.getImpl(Collections.singleton(root));
            }

            @Override
            public Object getChild(Object parent, int index) {
                return trie.children((Trie.Node<StackFrame>) parent).get(index);
            }

            @Override
            public int getChildCount(Object parent) {
                return trie.children((Trie.Node<StackFrame>) parent).size();
            }

            @Override
            public boolean isLeaf(Object node) {
                return getChildCount(node) == 0;
            }

            @Override
            public void valueForPathChanged(TreePath path, Object newValue) {
                for (TreeModelListener listener : listeners) {
                    listener.treeStructureChanged(new TreeModelEvent(newValue, path));
                }
            }

            @Override
            public int getIndexOfChild(Object parent, Object child) {
                return trie.children((Trie.Node<StackFrame>) parent).indexOf(child);
            }

            @Override
            public void addTreeModelListener(TreeModelListener l) {
                listeners.add(l);
            }

            @Override
            public void removeTreeModelListener(TreeModelListener l) {
                listeners.remove(l);
            }
        });
        setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree1, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                Component component = super.getTreeCellRendererComponent(tree1, value, sel, expanded, leaf, row, hasFocus);
                if (component instanceof JLabel label) {
                    Trie.Node<StackFrame> frameNode = (Trie.Node<StackFrame>) value;
                    StackFrame frame = frameNode.getData();
                    label.setText("<html><body><b>%d</b> %s <span color=gray>%s</span>".formatted(
                            frameNode.getHit(),
                            "%s(%s)".formatted(
                                    frame.methodName(),
                                    String.join(",", frame.parameters())
                            ),
                            frame.className(),
                            "frame.method()"
                    ));
                }
                return component;
            }
        });
    }
}
