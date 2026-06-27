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
3. Expand `GET /time` → **Try it out** → **Execute**. The interceptor reads those credentials,
   performs the digest challenge/response, and sends `Authorization: Digest …`; the response greets
   you: `"greeting": "Hello bob, welcome to module-b!"`. (If you skip step 2, the interceptor
   instead prompts on first Execute.)

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
