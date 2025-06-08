package me.markoutte.deviewer.utils;

import me.markoutte.deviewer.jfr.StackFrame;

public class StackFrames {

    public static String format(StackFrame frame) {
        return "%s(%s)".formatted(
                frame.methodName(),
                String.join(",", frame.parameters())
        );
    }

    private StackFrames() {

    }
}
