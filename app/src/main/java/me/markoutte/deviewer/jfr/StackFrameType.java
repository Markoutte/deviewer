package me.markoutte.deviewer.jfr;

public enum StackFrameType {

    INTERPRETED,
    JIT_COMPILED,
    INLINED,
    NATIVE,
    CPP,
    KERNEL,
    C1_COMPILED,
    UNDEFINED;
}
