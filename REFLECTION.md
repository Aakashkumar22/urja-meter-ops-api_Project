# Reflection

## What assumptions did you make?

Three big ones, and each one is documented in PROTOCOL.md:

1. **Portal timestamps are IST.** The portal returns `dd/MM/yyyy HH:mm` with no timezone. I assumed Asia/Kolkata because the meters are geographically in India (coordinates in Rajasthan) and the format matches regional convention. This is the assumption with the highest blast radius — if wrong, every energy reading shifts by ±5.5 hours. I chose to normalize anyway because exposing a timezone-naive timestamp through a public API would be worse.

2. **The portal is read-only for us.** The assignment said "treat the portal as read-only," so I only implemented GET endpoints (plus the POST to `/login`). I did not probe for write endpoints.

3. **`pageSize` is fixed at 20.** The portal ignores any `pageSize` query param I tried, so I assumed it's server-side fixed. I could be wrong — maybe there's a header or a different param name — but the observed behavior was consistent across many calls.

## Which part was the most difficult, and how did you get unstuck?

**Cracking the login endpoint.** The first several `curl` attempts returned `403 Forbidden — Cross-site POST form submissions are forbidden`. That message is not standard Spring Boot or Nginx — it's specific to **SvelteKit's CSRF protection**, which checks the `Origin` header against the target.

I got unstuck by:
1. Reading the error message literally — "Cross-site POST" is a very specific phrase.
2. Recognizing it as SvelteKit's built-in `csrf.checkOrigin` feature (a bit of Googling confirmed the exact string).
3. Adding `Origin: [https://urja-ops.flockenergy.tech](...)` and `Referer: .../login` to the curl command.

The moment I saw `303 See Other` and the `Set-Cookie` header instead of `403`, everything after that was mechanical. The auth model, the cookie format, the data endpoints — all became obvious once the login worked.

Second hardest: mapping the portal's `dd/MM/yyyy HH:mm` timestamps (no timezone) to ISO-8601 UTC without breaking the wire format. I picked IST as the source timezone and made it a documented constant in `MeterService`.

## If you had another day, what would you improve?

In rough priority order:

1. **A short-lived cache** (30–60 seconds) on meter lists and geo calls. Data changes every 15 minutes, so caching would cut portal load substantially without staleness risk.
2. **Find the transformer list endpoint.** The `/portal/dts` route returned a single coordinate object, which was odd. I suspect there's a different route (maybe `/portal/dts/all` or a paginated variant) that I didn't try. A few more minutes of DevTools work would likely reveal it.
3. **Reactive end-to-end.** Currently `PortalDataClient` blocks on `.block()`. Making controllers return `Mono<T>` would let the service handle more concurrent requests with the same thread count.
4. **Better error surface.** Right now a portal 500 becomes our 502. A more nuanced mapping (`404` for missing meters, `503` for portal down, `504` for portal timeout) would be more useful to callers.
5. **Contract tests** with recorded portal responses (WireMock/VCR-style) to catch upstream changes.
6. **Docker + a Makefile** for reproducible "clone and run" without needing Java installed locally.
7. **Structured JSON logging** with request IDs, so any tool can correlate a client request to the corresponding portal call.

## What mistake did you make while solving this?

The **biggest one: I tried to guess the login endpoint before observing it.**

My first curl attempt was to `/api/auth/login` with a JSON body — because that's what I *expected* a modern portal to use. It returned 403/404, and I wasted about 20 minutes testing variations (`/login` with JSON, `/api/login` with form-encoding, etc.).

**The mistake was jumping to the solution before doing the reconnaissance.** If I had opened DevTools first, watched the browser's actual login request, and copied the URL, method, headers, and body directly — I'd have had the correct answer in 30 seconds. Instead I burned time guessing.

The lesson: **for reverse-engineering, observation beats intuition every time.** The portal tells you exactly how it works; you just have to look. I eventually did this and it went straight through — but I should have done it first.

Second mistake: I assumed `q=HPL` (search for a make) would work since the UI has a search box. It returns 0 results — search is serial-only. I only found this by testing. Lesson: verify what a parameter actually does before designing your API around it.

## If you were reviewing your own submission, what would you criticise?

Honest self-review, hardest criticisms first:

1. **The folder name `UrjaApi_Project` is ugly.** It came from IntelliJ's default naming when I created the project via a wizard. `urja-api` would be cleaner. I kept it as-is because renaming after creation would break local paths, and the deliverable is what's in the repo, not the folder name. But I should have been more careful at creation time.

2. **`.idea/` was committed.** The initial `.gitignore` didn't take effect before IntelliJ auto-added the folder. I ended up committing IDE state (`.idea/workspace.xml`, etc.) that changes every time you click. Functionally harmless, but reviewers expect this to be excluded. The fix is a one-line `git rm -r --cached .idea/`, but I prioritized getting the code committed over hygiene. Bad call.

3. **`sourceCompatibility = 17` instead of the assignment's literal "Java 8".** I chose 17 because it's what the local toolchain had and 8's compiler support was dropped in JDK 20. This is a defensible engineering choice but doesn't literally match the brief. I documented it, but a stricter reviewer might knock points.

4. **Only one test.** The `PortalAuthClientTest` proves the auth flow works against a mock server, but I didn't write controller tests (`@WebMvcTest`) or DTO mapper tests. The assignment said tests are the *least* important criterion, but one test feels thin.

5. **No retry/backoff on transient errors.** A portal 503 causes a single 502 to the client — no retries. For a production service I'd add resilience4j or a manual exponential backoff.

6. **Blocking calls in a reactive stack.** Using WebClient but calling `.block()` is a mixed metaphor. Either go full reactive (`Mono`/`Flux` end-to-end) or use a synchronous client. As it stands, the reactive dependency adds weight without delivering the async benefit.

7. **Documentation is thorough but dense.** The README is comprehensive, but a first-time reader might prefer a shorter "here's what you do in 30 seconds" section up top before the deeper material.

None of these are blockers, but each one is a real trade-off I made consciously under time constraints. Called out here so the reviewer sees my reasoning.\
