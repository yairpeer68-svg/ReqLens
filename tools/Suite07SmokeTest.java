package com.reqlens.app;
public final class Suite07SmokeTest {
    public static void main(String[] args) {
        RequestRecord r = new RequestRecord(); r.method = "POST"; r.url = "https://api.example.com/x"; r.requestBody = "old";
        InterceptRule x = new InterceptRule(); x.hostContains = "example.com"; x.method = "POST"; x.action = "SET_HEADER"; x.name = "X-Test"; x.value = "1";
        RuleRepository.add(x); RequestRecord q = RuleRepository.apply(r);
        if (!"1".equals(q.requestHeaders.get("X-Test"))) throw new AssertionError("rule failed");
        InterceptController.setEnabled(true); RequestPipeline.Prepared p = RequestPipeline.prepare(r, true);
        if (p.decision != RequestPipeline.Decision.QUEUED || InterceptController.snapshot().size() != 1) throw new AssertionError("intercept failed");
        System.out.println("Suite07SmokeTest PASS");
    }
}
