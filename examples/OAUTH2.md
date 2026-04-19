# OAuth2 / Keycloak Demo (Bearer-token variant, OAuth2 password grant)

Shows how to keep the OpenAPI UI working when the REST API is protected by
Keycloak in **bearer-only** mode, **without any popup-based OAuth2 flow**
and **without this repo's `openapi-gui-addon`**. Swagger UI exchanges a
username+password for a Bearer token via Keycloak's direct-access-grant
endpoint and attaches the token to every "Try it out" call automatically
— no manual token paste, no popup, no redirect.

The demo protects the **Hello API** (`stage-runtime-example`). The other
two demos stay open so you can compare.

## Run it

```bash
examples/build_and_start_oauth2.sh
```

Starts a Podman pod with **WildFly** (port 8080) and **Keycloak** (port
8081), imports the `demo` realm (user `demo` / `demo`, assigned the `user`
realm role), with direct-access grants enabled on the `openapi-ui` client.

Then open <http://localhost:8080/hello-api/openapi-ui/>. The UI loads
without prompting for login. Click **Authorize**, fill in the Keycloak
credentials (username `demo`, password `demo`, client_id `openapi-ui`,
tick `openid`), click **Authorize** again. The dialog closes, Swagger UI
is marked *Authorized*, and every subsequent **Try it out** attaches an
`Authorization: Bearer <jwt>` header automatically.

## Why OAuth2 password grant, not the auth-code popup

Popup-based OAuth2 Authorization Code flow in Swagger UI breaks against
any modern Keycloak that sends `Cross-Origin-Opener-Policy: same-origin`
(default since Keycloak 24+): when the popup navigates from the app's
origin to Keycloak and back, the browser severs the `window.opener`
link, so Swagger UI's `oauth2-redirect.html` throws *"Cannot read
properties of null (reading 'swaggerUIRedirectOauth2')"*. If you can't
change Keycloak's headers, the popup flow cannot be made to work.

The password grant sidesteps the popup entirely — Swagger UI just POSTs
`grant_type=password` to Keycloak's `/token` endpoint, all in the main
tab, no `window.opener` involved. The tradeoff is that the user's
password passes through Swagger UI's JavaScript (same-origin) rather
than being entered directly at Keycloak's login page. Fine for internal
tooling and demos; OAuth 2.1 deprecates password grant for public-facing
applications.

If you prefer that users log in at Keycloak's own page, see the
`oauth2-support_simple-cookie_plain` branch — that uses the server-side
OIDC redirect flow with session cookies and requires no paste or dialog.

## What changed compared to the popup-based variant

| Piece | Popup variant (`oauth2-support_simple`) | This variant |
| --- | --- | --- |
| UI dependency | custom `org.os890.mp-ext:openapi-gui-addon` | plain `org.microprofile-ext.openapi-ext:openapi-ui:2.1.1` |
| OAuth2 flow in OpenAPI doc | `authorizationCode` (popup) | `password` (inline username / password dialog, no redirect) |
| Security-scheme source | `@SecurityScheme` annotation with hardcoded URLs | `OASFilter` reading `app.oauth2.tokenUrl` from MP Config |
| `oidc.json` | `"bearer-only": true` | same — trimmed to the five useful keys |
| `web.xml` constraint | `/hello/*` only, UI stays public | same |
| `HelloResource` | `@SecurityRequirement(name = "keycloak")` | pure Jakarta (`@Path`, `@RolesAllowed`, `@PermitAll`) — no OpenAPI annotation |
| Document-level security | per-operation `@SecurityRequirement` | injected globally by the `OASFilter` — scales to any number of endpoints without per-class annotations |
| Keycloak client | standard flow only | adds `directAccessGrantsEnabled: true` |
| Role declarations | `<security-role>` / `@DeclareRoles` listing each role | **none** — `auth-constraint` is `**`; `@RolesAllowed` resolves names on the fly via `SecurityContext.isUserInRole()` |
| User interaction | click Authorize → popup → Keycloak → back → Try it out | click Authorize → type creds in dialog → Try it out |

## Key files

- [`WEB-INF/oidc.json`](stage-runtime-example/src/main/webapp/WEB-INF/oidc.json) — Elytron OIDC in `bearer-only` mode. Five keys: `realm`, `auth-server-url`, `resource`, `bearer-only`, `principal-attribute`. That's the functional minimum that still returns readable principal names.
- [`WEB-INF/web.xml`](stage-runtime-example/src/main/webapp/WEB-INF/web.xml) — constrains only `/hello/*`; the UI stays publicly reachable so users can open it and paste / fill in credentials. Uses `<role-name>**</role-name>` (any authenticated user) so no `<security-role>` enumeration is required; turns on `resteasy.role.based.security` so `@RolesAllowed` is honoured on JAX-RS methods.
- [`DemoApplication.java`](stage-runtime-example/src/main/java/org/os890/mp/openapi/gui/example/DemoApplication.java) — just `@OpenAPIDefinition.info`; **no security-scheme annotation**. The scheme and the document-level security requirement are both produced at runtime by [`OAuth2SecurityFilter`](stage-runtime-example/src/main/java/org/os890/mp/openapi/gui/example/OAuth2SecurityFilter.java), registered via `mp.openapi.filter` in `microprofile-config.properties`. Keycloak's token URL lives in the config property `app.oauth2.tokenUrl` and is overridable per stage via env var / `-D` system property.
- [`HelloResource.java`](stage-runtime-example/src/main/java/org/os890/mp/openapi/gui/example/HelloResource.java) — **no OpenAPI security annotation at all**. Just `@Path` + Jakarta Security (`@RolesAllowed("user")`, per-method `@PermitAll` / `@RolesAllowed("admin")`). The global security requirement injected by the filter applies the `bearer` scheme to every operation.
- [`keycloak/realm-demo.json`](keycloak/realm-demo.json) — the `openapi-ui` client has `directAccessGrantsEnabled: true`; the demo user has the `user` realm role.

## The issuer-URL gotcha (still relevant)

Unrelated to the grant type, but worth re-stating: the token's `iss`
claim must byte-match the issuer URL WildFly resolves Keycloak at when
fetching JWKS. This demo runs both containers in a **single Podman
pod**, sharing a network namespace, so the browser and WildFly see
Keycloak on the exact same URL (`http://localhost:8081`). Deploying
outside a shared-namespace setup requires `KC_HOSTNAME` on Keycloak
pointed at whatever hostname the backend will resolve.

## When to pick which variant

| Variant | Best when | Downside |
| --- | --- | --- |
| `oauth2-support_simple` | Keycloak admin can set `Cross-Origin-Opener-Policy: same-origin-allow-popups`; you want the UI accessible without login. | Requires Keycloak tweak; popup flow still fragile against future browser changes. |
| `oauth2-support_simple_prefilled` | Same as above; you want a one-click Authorize button (client_id prefilled). | Same constraints. |
| `oauth2-rolesallowed` | You want Swagger UI security scheme auto-derived from `@RolesAllowed` / `@PermitAll` / `@DenyAll` so REST code stays pure Jakarta. | Still a popup-based flow underneath; COOP still applies. |
| `oauth2-support_simple-cookie` | You don't control Keycloak; you can't relax COOP; server-side OIDC is already part of your app's model. Uses the addon. | Swagger UI is no longer publicly reachable — every visitor must log in. |
| `oauth2-support_simple-cookie_plain` | Same as the cookie variant but you want zero dependency on this repo's addon, and you have many application roles that you don't want to enumerate in `web.xml` / `@DeclareRoles`. | Same as above; also lose the addon's project-stage gate (shape that with a Maven profile instead). |
| **This one** — `oauth2-support_simple-bearer` | You don't control Keycloak; you want the UI publicly accessible; you want no manual token paste but also no redirect to Keycloak. | Password grant is OAuth 2.1 deprecated; user's password passes through Swagger UI JS. Acceptable for internal demos / trusted networks. |

## Files touched (vs. plain `stage-runtime-example`)

| File | Purpose |
| --- | --- |
| `stage-runtime-example/.../DemoApplication.java` | `@OpenAPIDefinition.info` only — the security scheme lives in the filter. |
| `stage-runtime-example/.../OAuth2SecurityFilter.java` | `OASFilter` that injects the OAuth2 `password`-flow scheme (reading the Keycloak token URL from MP Config `app.oauth2.tokenUrl`) **and** attaches a document-level `SecurityRequirement` so every operation inherits it — no per-resource `@SecurityRequirement` needed. |
| `stage-runtime-example/.../microprofile-config.properties` | `mp.openapi.filter` registration + `app.oauth2.tokenUrl`. |
| `stage-runtime-example/.../HelloResource.java` | Pure Jakarta: `@Path`, `@RolesAllowed`, `@PermitAll`. No OpenAPI security annotation. |
| `stage-runtime-example/.../web.xml` | `/hello/*` constraint, `role-name=**`, `resteasy.role.based.security=true`, no role enumeration. |
| `stage-runtime-example/.../oidc.json` | Elytron OIDC `bearer-only` — five keys, trimmed. |
| `stage-runtime-example/.../pom.xml` | Depends on upstream `openapi-ui`, not the addon. |
| `keycloak/realm-demo.json` | `openapi-ui` client with `directAccessGrantsEnabled: true`; `demo` user with the `user` role. |
| `Dockerfile.oauth2` + `enable-oidc.cli` | WildFly with `elytron-oidc-client` subsystem (unchanged). |
| `build_and_start_oauth2.sh` | Podman pod wiring (unchanged). |

This variant drops the addon entirely — the `stage-runtime-example`
POM depends on `org.microprofile-ext.openapi-ext:openapi-ui:2.1.1`
directly. The upstream plugin ships the same Swagger UI 5.18.2 webjar,
so the runtime behaviour is identical.
