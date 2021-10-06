package com.miteksystems.misnaphybridcontroller;

public class MiSnapHybridControllerResult {

    private final byte[] finalFrame;
    private final int[][] fourCorners;
    private final String extractedBarcode;

    public MiSnapHybridControllerResult(byte[] frame, int[][] corners, String barcode) {
        finalFrame = frame;
        fourCorners = corners;
        extractedBarcode = barcode;
    }

    public byte[] getFinalFrame() {
        return finalFrame;
    }

    public int[][] getFourCorners() {
        return fourCorners;
    }

    public String getExtractedBarcode() {
        return extractedBarcode;
    }
}
