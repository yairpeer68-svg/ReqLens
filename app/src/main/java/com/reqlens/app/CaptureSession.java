package com.reqlens.app;

public final class CaptureSession {
    public final String id;
    public final long startedAt;
    public long stoppedAt;
    public String stopReason = "";

    public CaptureSession(String id, long startedAt) { this.id = id; this.startedAt = startedAt; }
    public boolean isRunning() { return stoppedAt == 0L; }
}
