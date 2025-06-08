/*
 * Copyright 2025 Maksim Pelevin and contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.markoutte.deviewer;

import me.markoutte.deviewer.jfr.StackFrame;
import me.markoutte.deviewer.utils.StackFrames;
import me.markoutte.deviewer.utils.Trie;
import org.jetbrains.annotations.Nls;

import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

public class MethodList extends JTable {

    private final long totalCalls;
    private final Method[] methods;

    public MethodList(Trie<StackFrame, StackFrame> trie) {
        var totalCalls = new AtomicLong(0);
        var frames = new HashMap<StackFrame, Method>();
        trie.forEach(stackFrames -> {
            for (StackFrame stackFrame : stackFrames) {
                frames.computeIfAbsent(stackFrame, Method::new).hit();
                totalCalls.incrementAndGet();
            }
        });
        this.totalCalls = totalCalls.get();
        this.methods = new Method[frames.size()];
        int i = 0;
        for (var value : frames.values()) {
            methods[i] = value;
            int targetIndexToSet = i; //
            for (int j = i - 1; j >= 0 && methods[j].getCalls() < value.getCalls(); j--) {
//                methods[j + 1] = methods[j];
//                methods[j] = value;
                targetIndexToSet = j;
            }
            if (targetIndexToSet != i) {
                System.arraycopy(methods, targetIndexToSet, methods, targetIndexToSet + 1, i - targetIndexToSet);
                methods[targetIndexToSet] = value;
            }
            i++;
        }

        setModel(new Model());
    }

    private static class Method {
        private StackFrame frame;
        private long calls = 0;

        public Method(StackFrame frame) {
            this.frame = frame;
        }

        public StackFrame getFrame() {
            return frame;
        }

        public void setFrame(StackFrame frame) {
            this.frame = frame;
        }

        public long getCalls() {
            return calls;
        }

        public void hit() {
            calls++;
        }
    }

    private class Model implements TableModel {

        @Override
        public int getRowCount() {
            return methods.length;
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public @Nls String getColumnName(int columnIndex) {
            return switch (columnIndex) {
                case 0 -> "Name";
                case 1 -> "Count";
                case 2 -> "Ratio";
                default -> throw new IllegalArgumentException("Too many columns expected");
            };
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return switch (columnIndex) {
                case 0 -> String.class;
                case 1 -> Long.class;
                case 2 -> Double.class;
                default -> throw new IllegalArgumentException("Too many columns expected");
            };
        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return false;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Method method = methods[rowIndex];
            return switch (columnIndex) {
                case 0 -> StackFrames.format(method.frame);
                case 1 -> method.getCalls();
                case 2 -> method.getCalls() * 1.0 / totalCalls * 100;
                default -> throw new IllegalArgumentException("Too many columns expected");
            };
        }

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addTableModelListener(TableModelListener l) {

        }

        @Override
        public void removeTableModelListener(TableModelListener l) {

        }
    }
}
