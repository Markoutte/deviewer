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
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static me.markoutte.deviewer.utils.Jvm.jvmNameToCanonical;

public class Main {

    public static void main(String[] args) {
        if (SystemInfo.isMacOS) {
            System.setProperty( "apple.laf.useScreenMenuBar", "true" );
            System.setProperty( "apple.awt.application.appearance", "system" );
        }

        FlatLightLaf.setup();
        var frame = new JFrame("");

        var panel = new JComponent() {};
        ActionListener al = e -> {
            JFileChooser fc = new JFileChooser();
            if( fc.showOpenDialog(panel) == JFileChooser.APPROVE_OPTION ) {
                File file = fc.getSelectedFile();
                reload(panel, file);
            }
        };

        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("File");
        JMenuItem item = new JMenuItem("Open...");
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, SystemInfo.isMacOS ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK));
        item.addActionListener(al);
        menu.add(item);
        menuBar.add(menu);
        frame.setJMenuBar(menuBar);

        if (SystemInfo.isMacFullWindowContentSupported) {
            frame.getRootPane().putClientProperty( "apple.awt.fullWindowContent", true );
            frame.getRootPane().putClientProperty( "apple.awt.transparentTitleBar", true );
        }

        panel.setLayout(new BorderLayout());
        {
            JPanel box = new JPanel();
            box.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.weightx = 1.0;
            gbc.weighty = 1.0;
            gbc.anchor = GridBagConstraints.CENTER;
            JButton button = new JButton("Open file...");
            button.addActionListener(al);
            box.add(button, gbc);
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
            panel.removeAll();
            var tabbed = new JTabbedPane();
            tabbed.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
            JPanel emptyPane = new JPanel();
            emptyPane.setBorder(new EmptyBorder(0, 60, 0, 0));
            tabbed.putClientProperty("JTabbedPane.leadingComponent", emptyPane);
            IcicleGraphComponent icicleGraphComponent = new IcicleGraphComponent(allFrame, stackTraces);
            JScrollPane scrollPane1 = new JScrollPane(icicleGraphComponent);
            scrollPane1.putClientProperty("JScrollPane.smoothScrolling", true);
            scrollPane1.getVerticalScrollBar().setUnitIncrement(24);
            scrollPane1.getHorizontalScrollBar().setUnitIncrement(24);
            scrollPane1.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
            scrollPane1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
            tabbed.addTab("Flame Graph", scrollPane1);
            JScrollPane scrollPane2 = new JScrollPane(new CallTree(allFrame, stackTraces));
            scrollPane2.getViewport().setScrollMode(JViewport.BACKINGSTORE_SCROLL_MODE);
            tabbed.addTab("Call Tree", scrollPane2);
            panel.add(tabbed, BorderLayout.CENTER);
            panel.revalidate();
            panel.repaint();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

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


}
