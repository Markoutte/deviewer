package me.markoutte.deviewer.jfr;

public enum StackFrameType {

    Zero(0), First(1), Second(2), Third(3), Fourth(4), Fifth(5), Sixth(6);

    private final int id;

    StackFrameType(int id) {
        this.id = id;
    }

    public static StackFrameType byId(byte id) {
        for (StackFrameType value : values()) {
            if (value.id == id) {
                return value;
            }
        }
        throw new RuntimeException("Cannot find value with id = " + id);
    }
}
