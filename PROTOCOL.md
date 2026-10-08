# Urja Meter Ops — Portal Protocol (Observations)

Reverse-engineered by observing the portal's network traffic. This document
explains how the portal actually works, so that another engineer could build
a client without ever opening a browser.

## Base URL
https://urja-ops.flockenergy.tech

## Technology stack (inferred)
- Frontend: SvelteKit — responses return `{"type":"redirect",...}` from form
  actions, unknown routes fall through to the SPA HTML shell, and CSRF is
  enforced via `Origin` header matching.
- Auth library: `better-auth` — recognisable from the cookie name
  `__Secure-better-auth.session_token`.
- Server: standard HTTPS behind a reverse proxy (Alt-Svc: h3=":443").

## Authentication

### Flow
1. Client POSTs form-encoded credentials to `/login`.
2. Portal responds 200 OK with Set-Cookie + a JSON body describing a soft 303.
3. Client replays the cookie on every /portal/* request.
4. Cookie TTL is 1 hour; on 401/403 the client re-logs in.

### Login request
POST /login
Content-Type: application/x-www-form-urlencoded
Origin: https://urja-ops.flockenergy.tech        <-- REQUIRED
Referer: https://urja-ops.flockenergy.tech/login <-- recommended
Body: email=operator@urja.local&password=urja-ops-2026

### Login response
HTTP/1.1 200 OK
Set-Cookie: __Secure-better-auth.session_token=<opaque>.<signature>;
Max-Age=3600; Path=/; HttpOnly; Secure; SameSite=Lax
Content-Type: application/json

{"type":"redirect","status":303,"location":"/meters"}

### Working curl (verified against the live portal)
curl -i -c cookies.txt \
-X POST 'https://urja-ops.flockenergy.tech/login' \
-H 'Content-Type: application/x-www-form-urlencoded' \
-H 'Origin: https://urja-ops.flockenergy.tech' \
-H 'Referer: https://urja-ops.flockenergy.tech/login' \
--data 'email=operator@urja.local&password=urja-ops-2026'

## Data endpoints

### List meters
GET /portal/meters/search?q=<search>&page=<n>

Response:
{
"data": [{"meterId":"J100000","serialNo":"SE33962","make":"HPL",
"phaseType":"single","installStatus":"Decommissioned","dtCode":"DT-001"}],
"page": 1, "pageSize": 20, "total": 403
}

Quirks:
- Pagination is 1-indexed.
- pageSize is fixed at 20.
- q filters serial number only (q=HPL returns 0 results).

### Meter geolocations
GET /portal/meters/{meterId}/geo
Response: {"data":{"latitude":"26.93...","longitude":"75.83..."}}
Coordinates are strings, not numbers.

### Meter energy readings
GET /portal/meters/{meterId}/energy
Response:
{"data":[{"timestamp":"23/06/2026 23:30","kwh":"48438.74",
"kvah":"52313.84","voltR":"226"}, ...]}

Quirks:
- ~100 readings, roughly last 24h in 15-min intervals.
- from, to, limit params are IGNORED.
- All numeric fields are string-encoded.
- Timestamp is dd/MM/yyyy HH:mm with NO timezone.

### Distribution transformers (partial)
GET /portal/dts?page=1
Returns a single {data:{lat,lng}} object, not a list. List endpoint not located.

## Quirks & surprises

1. CSRF via Origin header. A POST to /login without the Origin header
   returns: 403 Forbidden — "Cross-site POST form submissions are forbidden".
   Any non-browser client MUST set Origin and Referer.

2. Unknown /portal/* paths return the SPA HTML shell with HTTP 200.
   Response Content-Type is text/html. Clients must check Content-Type —
   an HTML response with 200 means "route doesn't exist", not success.

3. Login returns HTTP 200 even though the body declares a 303. It's a
   SvelteKit form-action convention, not a real HTTP redirect.

4. Timestamps are dd/MM/yyyy HH:mm with no timezone. Assumed IST
   (Asia/Kolkata). If wrong, all energy timestamps shift by ±5.5h.

5. Search q= filters serial number, not make/status/phase.

## What I chose not to use
- Write endpoints (portal is read-only for us).
- Static assets / HTML pages.
- /portal/dts single-object endpoint — semantics unclear.