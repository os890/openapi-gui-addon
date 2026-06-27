# PoC — Per-module OIDC + Digest auth in one shared OpenAPI GUI

One WildFly server, **one app made of two WAR modules** with different context roots, both
exposed through the **single shared Swagger UI** of this project (the `openapi-gui-addon`
multi-module dropdown — *not* a separate Swagger setup per module).

The new part: the two modules use **different** authentication schemes, and the Swagger UI
**"Authorize" / "Try it out" flow uses the correct scheme per module** — never one for both.

| Module | Context root | OpenAPI endpoint | Secured by | How Swagger UI authenticates |
|--------|--------------|------------------|------------|------------------------------|
| Module A | `/module-a` | `/module-a/openapi` | **OIDC** (Keycloak) | Swagger UI "Authorize" → OAuth2 password grant (inline user/pass) → bearer token |
| Module B | `/module-b` | `/module-b/openapi` | **HTTP Digest** | addon's context-root-aware `requestInterceptor` does the digest challenge/response |
| GUI | `/gui` | — | none | hosts the one shared Swagger UI |

## Why this works — the key idea

In multi-spec (`urls`) mode, Swagger UI loads **one spec at a time** and rebuilds the
"Authorize" dialog from *that* spec's `securitySchemes`. So per-module auth is already the
default — **as long as each module's OpenAPI document declares its own scheme**:

- **Module A** declares an `oauth2` **password**-grant scheme (`ModuleAApplication`). Swagger UI's
  Authorize dialog collects username + password + client_id inline and POSTs `grant_type=password`
  straight to Keycloak's token endpoint — **no popup, no redirect**. This deliberately avoids the
  authorization-code popup flow, which breaks against Keycloak 24+ (`Cross-Origin-Opener-Policy:
  same-origin` severs `window.opener`). `openapi.ui.oauth2ClientId=swagger-ui` presets the client id
  (public client — leave the client_secret field blank).
- **Module B** declares `http`/`digest` (`ModuleBApplication`). Swagger UI has **no** native
  digest flow, so the addon adds one: a **single, context-root-aware `requestInterceptor`**
  (config `openapi.ui.digestPaths=/module-b`). It runs the digest challenge/response **only**
  for `/module-b/**` requests and leaves module A's OIDC bearer token untouched.

Both pieces are generic, config-gated features of the addon (no behavior change when the new
properties are unset). See `addon/src/main/webapp/templates/template.html` and `Templates.java`.

## Requirements

- `podman` (with a started machine) + `podman compose`
- JDK 17+ and Maven (to build the WARs)

## Run

```bash
./build_and_run.sh
```

This builds & installs the addon, builds the three WARs, bakes them into a WildFly 39 image
(with the Elytron DIGEST + elytron-oidc-client configuration applied via `wildfly/configure.cli`),
and starts WildFly + Keycloak via `compose.yml`.

Then open the **shared Swagger UI**:

> http://localhost:8090/gui/openapi-ui/

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
│   ├── Dockerfile              WildFly 39 + config + the 3 WARs
│   ├── configure.cli           Elytron DIGEST + elytron-oidc-client setup
│   ├── poc-users.properties    digest user (clear-text realm)
│   └── poc-groups.properties   digest user -> "user" role
├── gui/                        the shared Swagger UI host (bundles the addon)
├── module-a-oidc/              OIDC-secured module (bearer-only resource server)
└── module-b-digest/            Digest-secured module
```

## Stop

```bash
podman compose -f compose.yml down
```
