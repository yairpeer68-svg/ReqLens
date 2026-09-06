package com.reqlens.app;

import java.util.Locale;

public final class CapturePolicy {
    public enum PrivacyMode { METADATA_ONLY, SAFE_DEBUG, FULL_DEBUG }
    public enum Retention { SESSION_ONLY, ONE_DAY, SEVEN_DAYS }

    public final PrivacyMode privacyMode;
    public final Retention retention;
    public final boolean collectDns;
    public final boolean collectTlsMetadata;
    public final boolean collectPayloadPreview;
    public final int maxPayloadPreviewBytes;

    public CapturePolicy(PrivacyMode privacyMode, Retention retention, boolean collectDns,
                         boolean collectTlsMetadata, boolean collectPayloadPreview,
                         int maxPayloadPreviewBytes) {
        this.privacyMode = privacyMode == null ? PrivacyMode.METADATA_ONLY : privacyMode;
        this.retention = retention == null ? Retention.SESSION_ONLY : retention;
        this.collectDns = collectDns;
        this.collectTlsMetadata = collectTlsMetadata;
        this.collectPayloadPreview = collectPayloadPreview && this.privacyMode != PrivacyMode.METADATA_ONLY;
        this.maxPayloadPreviewBytes = Math.max(0, Math.min(64 * 1024, maxPayloadPreviewBytes));
    }

    public static CapturePolicy safeDefault() {
        return new CapturePolicy(PrivacyMode.METADATA_ONLY, Retention.SESSION_ONLY, true, true, false, 0);
    }

    public boolean shouldRedactSecrets() { return privacyMode != PrivacyMode.FULL_DEBUG; }
    public String summary() {
        return String.format(Locale.US, "%s · retention=%s · dns=%s · tls=%s · payloadPreview=%s",
                privacyMode, retention, collectDns, collectTlsMetadata,
                collectPayloadPreview ? maxPayloadPreviewBytes + "B" : "off");
    }
}
