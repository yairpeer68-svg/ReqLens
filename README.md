# ReqLens Android 0.5-alpha

ReqLens is an Android HTTP/network inspection project for traffic you own or are explicitly authorized to test.

## Major change in 0.5-alpha: live forwarding backend
The previous fail-safe placeholder has been replaced with an embedded forwarding design:

`selected Android app -> VpnService TUN -> HEV tun2socks -> ReqLens local SOCKS5 -> protected upstream socket -> Internet`

The local SOCKS5 relay implements TCP CONNECT and UDP ASSOCIATE. Every upstream TCP/UDP socket is passed through `VpnService.protect()` before it connects, preventing the proxy from recursively entering its own VPN. The selected apps are applied with `VpnService.Builder.addAllowedApplication()`.

## Capture metadata
- TCP and UDP destination IP/port and byte counters.
- DNS query-name metadata for UDP/53 when visible.
- TLS ClientHello inspection for SNI, ALPN and negotiated-version hints without decrypting TLS.
- QUIC/HTTP3 candidate detection from UDP flows.
- IPv4 and IPv6 VPN routes.
- Live Connections, session stats, diagnostics, JSON/CSV export, encrypted local HTTP-client history and cURL/Bash generation are preserved.

## App attribution boundary
A single selected app can be attributed exactly from the selection itself. With several apps selected at once, HEV translates original app sockets into local SOCKS connections, so the SOCKS layer no longer carries the original Android UID. ReqLens therefore labels such flows as grouped selected-app traffic instead of inventing an app identity. Raw-packet UID attribution remains available to packet-observer backends.

## HTTPS / protections boundary
Capture Mode is passive metadata forwarding. It does not bypass certificate pinning, anti-debugging, mTLS or application-layer encryption, and it does not install a CA or perform TLS MITM. Full URL/headers/body capture is reserved for Authorized Debug Mode in apps you own or are allowed to instrument.

## Fail-safe behavior
- HEV native availability is checked before the TUN is established.
- No selected packages -> no VPN.
- No installed selected packages -> startup aborts.
- HEV start failure -> TUN and SOCKS resources are closed.
- SOCKS TCP/UDP upstream sockets must pass `VpnService.protect()` or that flow fails rather than loop.
- ReqLens itself cannot be selected for capture.

## Build target
- Android compile/target SDK: 36
- minSdk: 26
- Java: 17
- AGP: 8.13.2
- Gradle: 8.13
- Forwarder dependency: `com.wgtunnel:hevtunnel:1.0.4` from Maven Central

See `TESTS.md` for what was actually validated in this packaging environment. A physical Android 16 runtime test is still required before calling this production-ready.

## Third-party software
See `THIRD_PARTY_NOTICES.md`. HEV tun2socks is integrated through the WG Tunnel `hevtunnel` AAR; both the wrapper and upstream hev-socks5-tunnel are MIT-licensed.

## 0.6-alpha Mobile Proxy Suite layer
- Send any saved HTTP request directly to Repeater.
- Editable method, URL, headers and body with resend.
- Response status/headers/body viewer and line diff against the original response.
- Standalone Decoder/Encoder: URL encode/decode and Base64 encode/decode.
- Standalone Comparer for quick line-oriented text diffs.
- RewriteRule core for scoped request transformation in authorized testing workflows.
- Existing App Capture / tun2socks backend remains intact.

### Boundary
ReqLens does not include forced certificate-pinning bypass, anti-tamper defeat, or stealth interception of third-party apps. Full plaintext interception is intended for apps/systems you own or are explicitly authorized to test.
