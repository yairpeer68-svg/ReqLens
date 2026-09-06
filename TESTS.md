# ReqLens 0.5-alpha validation

## Passed in the packaging environment
- Local SOCKS5 TCP CONNECT smoke test against a real loopback TCP echo server.
- Local SOCKS5 UDP ASSOCIATE smoke test against a real loopback UDP echo server.
- SOCKS observer callbacks verified for TCP-open and outbound UDP events.
- TLS ClientHello metadata smoke test: SNI `api.example.com`, ALPN `h2`, TLS 1.3 supported-version parsing.
- DNS question-name parser smoke test.
- Android backend source compiled against targeted Java Android stubs, including VpnService.Builder, Network, ParcelFileDescriptor and HEV JNI signatures.
- Existing PacketParser DNS smoke test.
- Existing SecretRedactor/CapturePolicy privacy smoke test.
- AndroidManifest XML parse and ZIP integrity are part of final packaging checks.

## Adversarial pre-build review
Mitigated failure modes:
- VPN recursion: every SOCKS upstream TCP/UDP socket uses `VpnService.protect()`.
- TUN without forwarder: HEV class/native load is checked before `establish()`.
- Failed HEV startup: closes TUN + local SOCKS resources.
- SOCKS TCP connection failure: returns an error only before a success reply; it does not inject a second SOCKS reply into an established stream.
- Service stop with blocked SOCKS clients: active local client sockets are closed.
- IPv4/IPv6: both VPN routes are attempted; IPv6 setup failure does not prevent IPv4 capture.
- DNS choice: underlying network DNS servers are copied to the VPN when available.
- Underlying network: captured VPN declares the pre-VPN active network as underlying.
- App identity ambiguity after tun2socks: multi-app flows are grouped rather than falsely attributed.
- Sensitive payload collection: Capture Mode stores metadata, not TLS plaintext.

## Still not validated here
- Real Gradle Android build with SDK 36 resolving the Maven AAR.
- APK install on a physical Android 16 device.
- Native HEV startup on arm64-v8a in this container.
- Real LTE/Wi-Fi TCP, UDP, IPv6, DNS, QUIC and network handover behavior.
- Long-duration battery/memory testing.
- Exact per-app attribution when multiple apps are selected simultaneously.

Do not call this production-ready until the Android build/device gates pass.

## Packaging result
Pure-Java/runtime proxy smoke tests passed in the packaging environment. This archive does not claim an Android APK build or physical-device validation because those gates were not available here.

## 0.6-alpha additions
- PASS: TextDiff pure-Java smoke test.
- PASS: AndroidManifest XML parse after registering Repeater/Decoder/Comparer activities.
- PASS: string/comment-aware delimiter sanity on all newly added Java files.
- NOT VERIFIED HERE: full Gradle assembleDebug/assembleRelease because this environment does not provide a complete Android SDK/Gradle toolchain.
- NOT VERIFIED HERE: physical Android 16 installation/runtime of the new activities.

## 0.7-alpha additions
- PASS: Suite07 pure-Java pipeline smoke test using temporary Android/JSON-independent stubs: Rewrite Rule -> RequestPipeline -> Intercept Queue.
- PASS: MainActivity suite wiring includes Proxy History, Intercept, Rules, Projects, Variations and WebSocket entry points.
- PASS: Manifest component references resolve to source classes.
- PASS: AndroidManifest.xml and resource XML parse successfully.
- FIXED: Variations/Intruder no longer reads EditText widgets from the worker thread; request inputs are snapshotted on the UI thread before network execution.
- FIXED: Variations validates supported HTTP methods before starting a run.
- SAFETY/LOAD: Variations remains single-worker, max 50 payloads and 4 requests/sec.
- NOT VERIFIED HERE: full Gradle Android build because this container still lacks Android SDK/Gradle.
- NOT VERIFIED HERE: install/runtime on a physical Android 16 device.
