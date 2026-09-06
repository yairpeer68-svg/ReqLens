import com.reqlens.app.SecretRedactor;
import com.reqlens.app.CapturePolicy;

public class CoreSmokeTest {
    private static void ok(boolean v, String m) { if (!v) throw new AssertionError(m); }
    public static void main(String[] args) {
        String x = "Authorization: Bearer abc.def\nCookie: sid=123\n{\"password\":\"hello\",\"token\":\"xyz\"}";
        String r = SecretRedactor.redact(x);
        ok(!r.contains("abc.def") && !r.contains("sid=123") && !r.contains("hello") && !r.contains("xyz"), "redaction leak");
        CapturePolicy p = CapturePolicy.safeDefault();
        ok(p.shouldRedactSecrets(), "safe default must redact");
        ok(!p.collectPayloadPreview, "metadata mode must disable payload preview");
        System.out.println("CoreSmokeTest PASS: " + p.summary());
    }
}
