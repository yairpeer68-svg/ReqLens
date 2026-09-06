package com.reqlens.app;

public final class FeatureCatalog {
    public static final String[] NAMES = {
        "HTTP/2 Inspector","HTTP/3 & QUIC Analyzer","gRPC Inspector","Protobuf Schema Decoder","GraphQL Operation Explorer","WebSocket Frame Inspector","Server-Sent Events Viewer","HTTP/2 Stream Events","DNS Intelligence Panel","TLS Handshake Explorer",
        "App Security Profile","APK Static Analysis Import","Exported Components Explorer","Deep Link Mapper","App Links Verification Analyzer","Network Security Config Analyzer","Certificate Inventory","SDK & Tracker Inventory","Permission Risk Map","App Version Comparison",
        "Automatic API Inventory","OpenAPI Generator","OpenAPI Import & Validation","Endpoint Parameter Discovery","JSON Schema Inference","API Version Diff","Undocumented Endpoint Finder","API Dependency Graph","Endpoint Coverage Map","API Change Detection",
        "Security Test Case Builder","Authorization Matrix","IDOR Test Assistant","JWT Inspector","OAuth/OIDC Flow Analyzer","Session Lifecycle Tester","CSRF Protection Analyzer","CORS Policy Analyzer","Security Headers Auditor","Rate-Limit Behavior Lab",
        "Visual Workflow Builder","Environment Variables Vault","Dynamic Value Extraction","Conditional Workflow Branches","Loop & Dataset Runner","Scheduled Test Runs","Regression Test Suite","Assertion Library","Mock API Server","Traffic Replay Lab",
        "AI Traffic Analyst","Anomaly Detection","Endpoint Risk Prioritization","Sensitive Data Flow Map","Third-party Data Sharing Report","Authentication Flow Graph","Error Pattern Clustering","Root Cause Assistant","Natural Language Traffic Search","AI Test Plan Generator",
        "Evidence Vault","Finding Management","Reproduction Steps Generator","Professional Report Builder","SARIF Export","HAR Import/Export","Postman Collection Export","Evidence Redaction Review","Encrypted Team Project Sharing","Audit Trail",
        "Complete Design System","Bottom Navigation Workspace","Split-pane Tablet Layout","Command Palette","Customizable Workspace","Live Session Health Monitor","One-tap Diagnostic Bundle","Guided First-run Setup","Offline Documentation & Tutorials","Plugin SDK"
    };
    public static final String[] GROUPS={"Protocols","App Research","API Discovery","Security Lab","Automation","Intelligence","Evidence","Experience"};
    private FeatureCatalog(){}
    public static String group(int id){int i=Math.max(1,Math.min(80,id));return GROUPS[(i-1)/10];}
}
