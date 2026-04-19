# OAuth2 / Keycloak Demo (session-cookie variant, plain openapi-ui)

Shows how to keep the OpenAPI UI working when the underlying REST API is
protected by Keycloak — **without a popup-based OAuth2 flow in Swagger
UI, and without this repo's `openapi-gui-addon`**. Authentication runs
entirely on the server side through WildFly Elytron OIDC, and "Try it
out" rides on the resulting JSESSIONID cookie.

This branch depends only on the upstream
`org.microprofile-ext.openapi-ext:openapi-ui` artifact. Everything
interesting lives outside the addon — so dropping the addon changes
nothing in the user-visible flow.

The demo protects the **Hello API** (`stage-runtime-example`). The other
two demos stay open so you can compare.

## Run it

```bash
examples/build_and_start_oauth2.sh
```

Starts a Podman pod with **WildFly** (port 8080) and **Keycloak** (port
8081), imports the `demo` realm and creates a test user `demo` / `demo`.

Then open <http://localhost:8080/hello-api/openapi-ui/>. You will be
redirected to the Keycloak login page, sign in as `demo` / `demo`, and
land back on Swagger UI — **already authenticated**. Expand `GET /hello`,
click **Try it out** → **Execute** and you get `200 OK` with your
principal name. No Authorize button, no Bearer token handling, nothing.

## Why cookies, not the popup OAuth2 flow

Popup-based OAuth2 in Swagger UI breaks against any modern Keycloak that
sends `Cross-Origin-Opener-Policy: same-origin` (default since Keycloak
24+): when the popup navigates from the app's origin to Keycloak and
back, the browser severs the `window.opener` link, so Swagger UI's
`oauth2-redirect.html` throws *"Cannot read properties of null (reading
'swaggerUIRedirectOauth2')"*. If you can't change Keycloak's headers,
the popup flow cannot be made to work.

This branch sidesteps the popup entirely. The browser obtains a session
cookie during the initial OIDC redirect chain (which happens same-tab,
not in a popup, so COOP never matters), and every subsequent same-origin
request — including every "Try it out" fetch — automatically carries
that cookie. Elytron OIDC sees the existing session and lets the call
through. Classic server-side OIDC — the same pattern the old Keycloak
Java adapter used to provide.

## What changed compared to the popup-based variant

| Piece | Popup variant (`oauth2-support_simple`) | This variant |
| --- | --- | --- |
| UI dependency | custom `org.os890.mp-ext:openapi-gui-addon` | plain `org.microprofile-ext.openapi-ext:openapi-ui:2.1.1` |
| `oidc.json` | `"bearer-only": true` | `"public-client": true` (no bearer-only) — Elytron does the full OIDC flow |
| `web.xml` constraint | `/hello/*` only, UI stays public | `/*` — the whole WAR is gated by Keycloak, login happens before the UI loads |
| OpenAPI doc | declares `oauth2` security scheme via OASFilter, operations tagged `security:[{keycloak:[]}]` | no security scheme declared at all — Swagger UI has no Authorize button |
| `HelloResource` | `@SecurityRequirement(name = "keycloak")` | plain `@Path` + `@RolesAllowed` (pure Jakarta) |
| `microprofile-config.properties` | `mp.openapi.filter=...` + `app.oauth2.*` URLs | OAuth2 keys removed — nothing to configure, Elytron reads `oidc.json` only |
| `OAuth2SecurityFilter.java` | present, builds the OAuth2 scheme | deleted |
| Role declarations | `<security-role>` / `@DeclareRoles` listing each role | **none** — `auth-constraint` is `**` (any authenticated user); `@RolesAllowed` resolves role names on the fly via `SecurityContext.isUserInRole()` |
| User interaction | click Authorize → popup → Keycloak → back → Try it out | visit UI → redirect to Keycloak → login → Try it out (no extra clicks) |

## Key files

- [`WEB-INF/oidc.json`](stage-runtime-example/src/main/webapp/WEB-INF/oidc.json) — Elytron OIDC client config. No `bearer-only`, so Elytron behaves as a full relying party (code flow + server-side token storage + session cookie).
- [`WEB-INF/web.xml`](stage-runtime-example/src/main/webapp/WEB-INF/web.xml) — constrains `/*` so the whole WAR requires authentication. Uses `<role-name>**</role-name>` (any authenticated user) so no `<security-role>` enumeration is required; turns on `resteasy.role.based.security` so `@RolesAllowed` is honoured on JAX-RS methods.
- [`DemoApplication.java`](stage-runtime-example/src/main/java/org/os890/mp/openapi/gui/example/DemoApplication.java) — no `@OpenAPIDefinition.components` block: no security scheme is declared.
- [`HelloResource.java`](stage-runtime-example/src/main/java/org/os890/mp/openapi/gui/example/HelloResource.java) — no OpenAPI security annotation. Jakarta EE's runtime security (driven by `web.xml` + Elytron OIDC) does the enforcement.

## The issuer-URL gotcha (still relevant)

Unrelated to cookies, but worth re-stating: the token's `iss` claim must
byte-match the issuer URL WildFly resolves Keycloak at when fetching
JWKS. This demo runs both containers in a **single Podman pod**, sharing
a network namespace, so the browser and WildFly see Keycloak on the
exact same URL (`http://localhost:8081`). Deploying outside a
shared-namespace setup requires `KC_HOSTNAME` on Keycloak pointed at
whatever hostname the backend will resolve.

## When to pick which variant

| Variant | Best when | Downside |
| --- | --- | --- |
| `oauth2-support_simple` | Keycloak admin can set `Cross-Origin-Opener-Policy: same-origin-allow-popups`; you want the UI accessible without login. | Requires Keycloak tweak; popup flow still fragile against future browser changes. |
| `oauth2-support_simple_prefilled` | Same as above; you want a one-click Authorize button (client_id prefilled). | Same constraints. |
| `oauth2-rolesallowed` | You want Swagger UI security scheme auto-derived from `@RolesAllowed` / `@PermitAll` / `@DenyAll` so REST code stays pure Jakarta. | Still a popup-based flow underneath; COOP still applies. |
| `oauth2-support_simple-cookie` | You don't control Keycloak; you can't relax COOP; server-side OIDC is already part of your app's model. Uses the addon. | Swagger UI is no longer publicly reachable — every visitor must log in. |
| **This one** — `oauth2-support_simple-cookie_plain` | Same as the cookie variant but you want zero dependency on this repo's addon, and you have many application roles that you don't want to enumerate in `web.xml` / `@DeclareRoles`. | Same as above; also lose the addon's project-stage gate (shape that with a Maven profile instead). |

## Files touched (vs. plain `stage-runtime-example`)

| File | Purpose |
| --- | --- |
| `stage-runtime-example/.../DemoApplication.java` | Removed security-scheme annotation block. |
| `stage-runtime-example/.../HelloResource.java` | Removed `@SecurityRequirement`; principal still echoed in response. |
| `stage-runtime-example/.../web.xml` | `<url-pattern>/*</url-pattern>` — protect the whole WAR. |
| `stage-runtime-example/.../oidc.json` | Elytron OIDC with full code flow (no `bearer-only`). |
| `stage-runtime-example/.../microprofile-config.properties` | Dropped `mp.openapi.filter` + `app.oauth2.*` URLs. |
| `keycloak/realm-demo.json` | Realm, `openapi-ui` public client, `demo` user (unchanged). |
| `Dockerfile.oauth2` + `enable-oidc.cli` | WildFly with `elytron-oidc-client` subsystem (unchanged). |
| `build_and_start_oauth2.sh` | Podman pod wiring (unchanged). |

This variant drops the addon entirely — the `stage-runtime-example`
POM depends on `org.microprofile-ext.openapi-ext:openapi-ui:2.1.1`
directly. The upstream plugin ships the same Swagger UI 5.18.2 webjar,
so the runtime behaviour is identical.
