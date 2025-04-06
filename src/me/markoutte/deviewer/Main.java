package me.markoutte.deviewer;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.util.SystemInfo;
import me.markoutte.deviewer.jfr.StackFrame;
import me.markoutte.deviewer.jfr.StackFrameType;
import me.markoutte.deviewer.utils.Trie;
import one.jfr.JfrReader;
import one.jfr.event.Event;
import one.jfr.event.ExecutionSample;

import javax.swing.*;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class Main {

    public static void main(String[] args) throws IOException {
        var reader = new JfrReader("/Users/markoutte/Snapshots/Main_2025_02_20_191719.jfr");
        Event event;
        var eventsByGroup = new HashMap<Class<? extends Event>, List<Event>>();
        var stackTraces = new Trie<StackFrame, StackFrame>(input -> input);
        StackFrame allFrame = new StackFrame("Everything", StackFrameType.First);
        while ((event = reader.readEvent()) != null) {
            eventsByGroup.computeIfAbsent(event.getClass(), eventClass -> new ArrayList<>()).add(event);
            List<StackFrame> chain = new ArrayList<>();
            chain.add(allFrame);
            if (event instanceof ExecutionSample sample) {
                var stacktrace = reader.stackTraces.get(sample.stackTraceId);
                for (int i = stacktrace.methods.length - 1; i >= 0; i--) {
                    chain.add(new StackFrame(
                            methodString(reader, stacktrace.methods[i]),
                            StackFrameType.byId(stacktrace.types[i])
                    ));
                }
            }
            stackTraces.add(chain);
        };

        System.setProperty( "apple.laf.useScreenMenuBar", "true" );
        System.setProperty( "apple.awt.application.appearance", "system" );

        FlatLightLaf.setup();
        var frame = new JFrame("Univier");
        if( SystemInfo.isMacFullWindowContentSupported ) {
//            frame.getRootPane().putClientProperty( "apple.awt.fullWindowContent", true );
            frame.getRootPane().putClientProperty( "apple.awt.transparentTitleBar", true );
        }

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));
        panel.add(new JScrollPane(getJTree(allFrame, stackTraces)), BorderLayout.CENTER);

        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setBounds(0, 0, 640, 480);
        frame.setVisible(true);
    }

    private static JTree getJTree(StackFrame root, Trie<StackFrame, StackFrame> trie) {
        var jTree = new JTree();
        jTree.setLargeModel(true);
        jTree.setShowsRootHandles(true);
        jTree.setRootVisible(true);
        jTree.setRowHeight(24);
        jTree.setModel(new TreeModel() {

            private final List<TreeModelListener> listeners = new ArrayList<>();

            @Override
            public Object getRoot() {
                return trie.get(Collections.singleton(root));
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
        jTree.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(JTree tree1, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
                Component component = super.getTreeCellRendererComponent(tree1, value, sel, expanded, leaf, row, hasFocus);
                if (component instanceof JLabel label) {
                    label.setText("%s (samples: %d)".formatted(
                            ((Trie.Node<StackFrame>) value).getData().method(),
                            ((Trie.Node<StackFrame>) value).getCount()
                    ));
                }
                return component;
            }
        });
        return jTree;
    }

    private static String methodString(JfrReader reader, long method) {
        var cls = reader.classes.get(reader.methods.get(method).cls);
        var methodRef = reader.methods.get(method);
        return "%s.%s(%s)".formatted(
            new String(Objects.requireNonNullElse(reader.symbols.get(cls.name), new byte[0])),
            new String(reader.symbols.get(methodRef.name))
            , new String(reader.symbols.get(methodRef.sig))
        ).intern();
    }
}
