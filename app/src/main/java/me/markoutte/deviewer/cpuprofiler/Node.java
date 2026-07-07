package me.markoutte.deviewer.cpuprofiler;

public class Node {

    private int id;
    private CallFrame callFrame;
    private int[] children = new int[0];

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public CallFrame getCallFrame() {
        return callFrame;
    }

    public void setCallFrame(CallFrame callFrame) {
        this.callFrame = callFrame;
    }

    public int[] getChildren() {
        return children;
    }

    public void setChildren(int[] children) {
        this.children = children;
    }
}
