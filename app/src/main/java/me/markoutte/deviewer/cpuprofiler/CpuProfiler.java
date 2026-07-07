package me.markoutte.deviewer.cpuprofiler;

public class CpuProfiler {

    private Node[] nodes = new Node[0];
    private long startTime;
    private long endTime;
    private int[] samples = new int[0];
    private int[] timeDeltas = new int[0];

    public Node[] getNodes() {
        return nodes;
    }

    public void setNodes(Node[] nodes) {
        this.nodes = nodes;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int[] getSamples() {
        return samples;
    }

    public void setSamples(int[] samples) {
        this.samples = samples;
    }

    public int[] getTimeDeltas() {
        return timeDeltas;
    }

    public void setTimeDeltas(int[] timeDeltas) {
        this.timeDeltas = timeDeltas;
    }
}
