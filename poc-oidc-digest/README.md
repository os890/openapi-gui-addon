# PoC — Per-module OIDC + Digest auth in one shared OpenAPI GUI

One WildFly server, **one app made of two WAR modules** with different context roots. **Each WAR
ships the `openapi-gui-addon`** and therefore serves its own shared Swagger UI, and **both carry
the full dropdown config** (`openapi.ui.urls`) — so from either module's UI you can switch between
both APIs. There is no separate "GUI" WAR.

The new part: the two modules use **different** authentication schemes, and the Swagger UI
**"Authorize" / "Try it out" flow uses the correct scheme per module** — never one for both — and
the GUI decides **purely from each module's OpenAPI document** (no GUI-side per-module config).

| Module | Context root | Swagger UI | REST resource | Secured by | How the UI authenticates |
|--------|--------------|------------|---------------|------------|--------------------------|
| Module A | `/module-a` | `/module-a/openapi-ui/` | `/module-a/profile` | **OIDC** (Keycloak) | "Authorize" → OAuth2 password grant (inline user/pass) → bearer token |
| Module B | `/module-b` | `/module-b/openapi-ui/` | `/module-b/time` | **HTTP Digest** | "Authorize" (Basic dialog) → addon converts to a digest challenge/response |

## Why this works — the key idea

In multi-spec (`urls`) mode, Swagger UI loads **one spec at a time** and rebuilds the
"Authorize" dialog from *that* spec's `securitySchemes`. So per-module auth is already the
default — **as long as each module's OpenAPI document declares its own scheme**. Each module
declares its scheme at runtime via an **`OASFilter`** (so the scheme/URL lives in config, not
annotations, and REST resources stay pure Jakarta):

- **Module A** → `OAuth2SecurityFilter` injects an `oauth2` **password**-grant scheme. Swagger UI's
  Authorize dialog collects username + password + client_id inline and POSTs `grant_type=password`
  straight to Keycloak's token endpoint — **no popup, no redirect**. This deliberately avoids the
  authorization-code popup flow, which breaks against Keycloak 24+ (`Cross-Origin-Opener-Policy:
  same-origin` severs `window.opener`). `openapi.ui.oauth2ClientId=swagger-ui` presets the client id
  (public client — leave the client_secret field blank).
- **Module B** → `DigestSecurityFilter` injects an `http`/`basic` scheme carrying the extension
  **`x-auth-mode: digest`** (basic, because Swagger UI can't render a digest dialog). The addon
  reads that marker from the **currently selected** spec (`activeSpecRequiresDigest()`); when set,
  its `requestInterceptor` converts the captured Basic credentials into a real HTTP Digest
  challenge/response on the wire. No path/`digestPaths` config — behaviour is driven by the spec.

Both addon pieces are generic and inert unless a spec opts in. See
`addon/src/main/webapp/templates/template.html` and `Templates.java`.

### What the addon adds for mixed mode

Everything required for per-module OIDC + Digest lives in
[`addon/src/main/webapp/templates/template.html`](../addon/src/main/webapp/templates/template.html)
— these are the only additions over the base addon (the rest of the template, e.g. the multi-API
dropdown, is unchanged):

| Addition (in `template.html`) | Purpose |
|---|---|
| `var md5 = …` (self-contained MD5, no external request) | compute the HTTP Digest response hash in the browser |
| `getActiveSecuritySchemes()` + `activeSpecRequiresDigest()` | spec-driven detection — inspect the *currently selected* spec's security schemes for `scheme: digest` or `x-auth-mode: digest` |
| `parseDigestChallenge()` | parse the `WWW-Authenticate: Digest …` challenge (realm/nonce/qop/opaque) |
| `ensureDigestChallenge()` | pre-flight fetch to obtain the challenge; sends a dummy `Authorization` so the browser doesn't pop its native dialog |
| `applyDigestAuth()` | read the credentials from Swagger UI's Authorize store (the `Basic` header) and emit `Authorization: Digest …`; no Basic header ⇒ unauthenticated (so Logout works) |
| `perModuleRequestInterceptor()` + `requestInterceptor:` wiring | single interceptor: digest when the active spec requires it, otherwise leave the request untouched (native bearer/OAuth2 survives) |
| `var oauth2ClientId` + `buildUi()` → `ui.initOAuth(...)` | preset the OIDC Authorize dialog (client id + PKCE) from `openapi.ui.oauth2ClientId` |

The matching `Templates.java` change is just the `%oauth2ClientId%` config plumbing — the digest
side needs no new config because detection is spec-driven.

## Requirements

- `podman` (with a started machine) + `podman compose`
- JDK 17+ and Maven (to build the WARs)

## Run

```bash
./build_and_run.sh
```

This builds & installs the addon, builds the two module WARs, bakes them into a WildFly 39 image
(with the Elytron DIGEST + elytron-oidc-client configuration applied via `wildfly/configure.cli`),
and starts WildFly + Keycloak via `compose.yml`.

Then open **either module's** Swagger UI (both list both APIs in the dropdown):

> http://localhost:8090/module-a/openapi-ui/
> http://localhost:8090/module-b/openapi-ui/

## Try it

Use the dropdown (top-right) to switch modules.

**Module A (OIDC)**
1. Select *Module A (OIDC)*.
2. Click **Authorize**. In the dialog enter username **`alice`**, password **`alice`**
   (client_id is pre-filled as `swagger-ui`; leave client_secret blank), tick **`openid`**,
   and click **Authorize** again. No popup or redirect — Swagger UI POSTs the password grant to
   Keycloak and stores the bearer token. Close the dialog.
3. Expand `GET /profile` → **Try it out** → **Execute**. The request carries the bearer token;
   the response greets you by name: `"greeting": "Hello alice, welcome to module-a!"`.

**Module B (Digest)**
1. Select *Module B (Digest)*.
2. Click **Authorize**, enter **`bob`** / **`bob`** (a Basic-style dialog — Swagger UI
   has no digest dialog), click **Authorize**, close.
3. Expand `GET /time` → **Try it out** → **Execute**. The interceptor reads the credentials Swagger
   UI captured, performs the digest challenge/response, and sends `Authorization: Digest …`; the
   response greets you: `"greeting": "Hello bob, welcome to module-b!"`.

Credentials come solely from the Authorize dialog, so **Logout** genuinely de-authenticates: after
Logout, Execute returns `401` and you must Authorize again — digest follows the same authorize/logout
lifecycle as the bearer scheme.

Switching back and forth shows each module using **its own** scheme — and each greets its own
authenticated user (`alice` vs `bob`), so a correct end-to-end run is obvious from the response.

## How the Keycloak hostname is handled

`compose.yml` runs Keycloak with `KC_HOSTNAME=http://localhost:8081` and
`KC_HOSTNAME_BACKCHANNEL_DYNAMIC=true`. The issuer/authorization endpoints are fixed to the
browser-reachable `localhost:8081`, while WildFly (bearer-only validation) reaches the
token/JWKS endpoints over the internal `http://keycloak:8080` — avoiding an issuer mismatch.

## Layout

```
poc-oidc-digest/
├── compose.yml                 podman/docker compose: keycloak + wildfly
├── build_and_run.sh            build everything + start the stack
├── keycloak/poc-realm.json     realm "poc": swagger-ui (public, password grant) client + user alice
├── wildfly/
│   ├── Dockerfile              WildFly 39 + config + the 2 module WARs
│   ├── configure.cli           Elytron DIGEST + elytron-oidc-client setup
│   ├── poc-users.properties    digest user (clear-text realm)
│   └── poc-groups.properties   digest user -> "user" role
├── module-a-oidc/              OIDC module: bundles the addon + OAuth2SecurityFilter
└── module-b-digest/            Digest module: bundles the addon + DigestSecurityFilter
```

Each module WAR bundles the `openapi-gui-addon` (so it serves its own `/<module>/openapi-ui/`)
and carries the full `openapi.ui.urls` dropdown config pointing at both modules.

## Stop

```bash
podman compose -f compose.yml down
```
