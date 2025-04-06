package me.markoutte.deviewer.jfr;

import java.util.List;

public record StackFrame(
        String className,
        String methodName,
        List<String> parameters,
        String returnValue,
        StackFrameType type
) { }
