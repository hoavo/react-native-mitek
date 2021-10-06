package com.miteksystems.misnapcontroller;

public class MiSnapControllerResult {
    private final int[][] fourCorners;
    private final byte[] finalFrame;

    public MiSnapControllerResult(byte[] finalFrame, int[][] fourCorners) {
        this.fourCorners = fourCorners;
        this.finalFrame = finalFrame;
    }

    public byte[] getFinalFrame() {
        return finalFrame;
    }

    public int[][] getFourCorners() {
        return fourCorners;
    }
}
