package me.markoutte.deviewer.jfr;

public record StackFrame(
        String method,
        StackFrameType type
) { }
