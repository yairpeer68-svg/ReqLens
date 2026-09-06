package com.reqlens.app;

public final class RequestPipeline {
    public enum Decision { SEND, QUEUED }
    public static final class Prepared { public final Decision decision; public final RequestRecord request; Prepared(Decision d,RequestRecord r){decision=d;request=r;} }
    private RequestPipeline(){}
    public static Prepared prepare(RequestRecord input, boolean allowIntercept){RequestRecord r=RuleRepository.apply(input);if(allowIntercept&&InterceptController.shouldIntercept(r)){InterceptController.enqueue(r);return new Prepared(Decision.QUEUED,r);}return new Prepared(Decision.SEND,r);}
}
