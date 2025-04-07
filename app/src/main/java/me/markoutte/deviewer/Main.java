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
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

    public static void main(String[] args) {
        System.setProperty( "apple.laf.useScreenMenuBar", "true" );
        System.setProperty( "apple.awt.application.appearance", "system" );

        FlatLightLaf.setup();
        var frame = new JFrame("");
        if( SystemInfo.isMacFullWindowContentSupported ) {
            frame.getRootPane().putClientProperty( "apple.awt.fullWindowContent", true );
            frame.getRootPane().putClientProperty( "apple.awt.transparentTitleBar", true );
        }

        var panel = new JComponent() {};
        panel.setLayout(new BorderLayout());
        {
            JButton openFileButton = new JButton("Open...");
            openFileButton.addActionListener(e -> {
                JFileChooser fc = new JFileChooser();
                if( fc.showOpenDialog( openFileButton ) == JFileChooser.APPROVE_OPTION ) {
                    File file = fc.getSelectedFile();
                    reload(panel, file);
                }
            });
            JPanel box = new JPanel();
            box.setLayout(new BoxLayout(box, BoxLayout.PAGE_AXIS));
            box.setBorder(new EmptyBorder(5, 5, 5, 10));
            box.add(openFileButton);
            openFileButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
            panel.add(box, BorderLayout.NORTH);
        }
        {
            JPanel box = new JPanel();
            box.setLayout(new BorderLayout());
            box.add(new JLabel("No opened file", SwingConstants.CENTER), BorderLayout.CENTER);
            panel.add(box, BorderLayout.CENTER);
        }

        frame.setContentPane(panel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setBounds(0, 0, 640, 480);
        frame.setVisible(true);
    }

    private static void reload(JComponent panel, File file) {
        try (var reader = new JfrReader(file.getAbsolutePath())) {
            Event event;
            var eventsByGroup = new HashMap<Class<? extends Event>, List<Event>>();
            var stackTraces = new Trie<StackFrame, StackFrame>(input -> input);
            StackFrame allFrame = new StackFrame(null, "Everything", Collections.emptyList(), null, StackFrameType.First);
            while ((event = reader.readEvent()) != null) {
                eventsByGroup.computeIfAbsent(event.getClass(), eventClass -> new ArrayList<>()).add(event);
                if (event instanceof ExecutionSample sample) {
                    final List<StackFrame> chain = new ArrayList<>();
                    chain.add(allFrame);
                    var stacktrace = reader.stackTraces.get(sample.stackTraceId);
                    for (int i = stacktrace.methods.length - 1; i >= 0; i--) {
                        chain.add(methodString(
                                reader,
                                stacktrace.methods[i],
                                StackFrameType.byId(stacktrace.types[i])
                        ));
                    }
                    stackTraces.add(chain);
                }
            }
            panel.remove(1);
            var tabbed = new JTabbedPane();
            IcicleGraphComponent icicleGraphComponent = new IcicleGraphComponent(allFrame, stackTraces);
            JScrollPane scrollPane = new JScrollPane(icicleGraphComponent);
            scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
            scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            tabbed.addTab("Flame Graph", scrollPane);
            tabbed.addTab("Call Tree", new JScrollPane(getJTree(allFrame, stackTraces)));
            panel.add(tabbed, BorderLayout.CENTER);
            panel.revalidate();
            panel.repaint();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static JTree getJTree(StackFrame root, Trie<StackFrame, StackFrame> trie) {
        var jTree = new JTree();
        jTree.setLargeModel(true);
        jTree.setShowsRootHandles(true);
//        jTree.setRootVisible(true);
        jTree.setModel(new TreeModel() {

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
        jTree.setCellRenderer(new DefaultTreeCellRenderer() {
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
        return jTree;
    }

    private static StackFrame methodString(JfrReader reader, long method, StackFrameType type) {
        var cls = reader.classes.get(reader.methods.get(method).cls);
        var methodRef = reader.methods.get(method);
        List<String> parameters = jvmNameToCanonical(new String(reader.symbols.get(methodRef.sig)));
        String returnValue = parameters.removeLast();
        return new StackFrame(
                Optional.ofNullable(reader.symbols.get(cls.name))
                        .filter(bytes -> bytes.length > 0)
                        .map(String::new)
                        .map(s -> s.replace('/', '.'))
                        .orElse(null),
                new String(reader.symbols.get(methodRef.name)),
                parameters,
                returnValue,
                type
        );
    }

    private static final Pattern pattern = Pattern.compile("\\((L.+;|V|Z|B|C|S|I|J|F|D)*\\)(L.+;|V|Z|B|C|S|I|J|F|D)");

    private static List<String> jvmNameToCanonical(String name) {
        Matcher matcher = pattern.matcher(name);
        if (matcher.matches()) {
            var parameters = name.toCharArray();
            List<String> list = new ArrayList<>();
            for (int i = 0; i < parameters.length; i++) {
                switch (parameters[i]) {
                    case 'V': list.add("void"); break;
                    case 'Z': list.add("boolean"); break;
                    case 'B': list.add("byte"); break;
                    case 'C': list.add("char"); break;
                    case 'S': list.add("short"); break;
                    case 'I': list.add("int"); break;
                    case 'J': list.add("long"); break;
                    case 'F': list.add("float"); break;
                    case 'D': list.add("double"); break;
                    case 'L': {
                        var j = i + 1;
                        while (parameters[j] != ';') {
                            j++;
                        }
                        char[] className = Arrays.copyOfRange(parameters, i + 1, j);
                        for (int c = 0; c < className.length; c++) {
                            if (className[c] == '/') {
                                className[c] = '.';
                            }
                        }
                        list.add(new String(className));
                        i = j;
                    }
                }
            }
            return list;
        }
        var list = new ArrayList<String>(1);
        list.add(name);
        return list;
    }
}
