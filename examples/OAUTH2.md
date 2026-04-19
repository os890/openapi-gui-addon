# OAuth2 / Keycloak Demo (Bearer-token variant, inline login form)

Shows how to keep the OpenAPI UI working when the REST API is protected
by Keycloak in **bearer-only** mode, with a **custom in-page login form**
driving the flow. The Swagger UI page embeds ~80 lines of plain
JavaScript that: renders a username/password form, POSTs it to
Keycloak's `/token` endpoint (`grant_type=password`), and hands the
resulting JWT to Swagger UI via `preauthorizeApiKey`. Swagger UI's
default Authorize button is CSS-hidden so users can only sign in and
out through the custom UI — no popup, no redirect, no paste-a-token
dialog, no external JS dependency (no `keycloak-js`).

The demo protects the **Hello API** (`stage-runtime-example`). The other
two demos stay open so you can compare.

## Run it

```bash
examples/build_and_start_oauth2.sh
```

Starts a Podman pod with **WildFly** (port 8080) and **Keycloak** (port
8081), imports the `demo` realm (user `demo` / `demo`, assigned the
`user` realm role), with direct-access grants enabled on the
`openapi-ui` client.

Then open <http://localhost:8080/hello-api/openapi-ui/>. A small
"Sign in to Hello API" form renders. Enter `demo` / `demo` → click
**Sign in**. The form disappears, a status bar at the top shows
*"Signed in as demo. Sign out"*, and Swagger UI boots pre-authorized —
every **Try it out** carries `Authorization: Bearer <jwt>`. Clicking
**Sign out** clears the token and reloads to the login form.

## Why an inline form and not Swagger UI's built-in Authorize dialog

Swagger UI's stock OAuth2-password dialog works (see the
`oauth2-support_simple-bearer` branch), but three things push apps to
replace it:

1. **Branding and UX control.** The Authorize dialog is hard to style;
   a dedicated page-level form fits the app's look.
2. **Hide the "paste a token" fallback.** With a Bearer scheme in the
   OpenAPI doc, clicking Swagger UI's built-in Authorize button opens a
   generic `Value` input where anyone can paste any JWT. Users who
   click **Logout** and then **Authorize** again land there instead of
   the flow you want. The inline form plus a CSS rule that hides
   `.auth-wrapper` / `.scheme-container` closes that door.
3. **Direct control over the token request.** You can add extra
   parameters, use a different grant, swap endpoints, or change
   behaviour without depending on Swagger UI's internal request
   builder.

## What needed to change vs. the "bearer" branch

| Piece | `oauth2-support_simple-bearer` | This branch |
| --- | --- | --- |
| OpenAPI security scheme | OAuth2 `password` flow | HTTP `bearer` scheme |
| Swagger UI Authorize dialog | used (built-in OAuth2 password form) | **CSS-hidden** (`.auth-wrapper`, `.scheme-container`) |
| Login form | Swagger UI's own | custom `openapi.html` form |
| Token request | Swagger UI POSTs to `/token` | our JS POSTs to `/token` |
| Token handover to Swagger UI | implicit via the Authorize dialog | explicit `ui.preauthorizeApiKey('bearer', token)` |
| Logout | Swagger UI's Logout button | custom "Sign out" link → clears sessionStorage → reloads |
| External JS dependencies | none | none (no keycloak-js, no other libs) |
| Token shape | password grant (no `auth_time`/`nonce`) | **same** — still password grant |

## Key files

- [`src/main/resources/openapi.html`](stage-runtime-example/src/main/resources/openapi.html) — **white-label template** picked up by openapi-ui's `WhiteLabel`. Renders the login form, performs the token exchange, hides Swagger UI's built-in auth widgets, and provides the Sign-out flow.
- [`WEB-INF/oidc.json`](stage-runtime-example/src/main/webapp/WEB-INF/oidc.json) — Elytron OIDC in `bearer-only` mode. Five keys: `realm`, `auth-server-url`, `resource`, `bearer-only`, `principal-attribute`.
- [`WEB-INF/web.xml`](stage-runtime-example/src/main/webapp/WEB-INF/web.xml) — constrains only `/hello/*`; the UI stays publicly reachable so users can see the login form before authenticating. `role-name=**` so no `<security-role>` enumeration is required; `resteasy.role.based.security=true` so `@RolesAllowed` is enforced on JAX-RS methods.
- [`DemoApplication.java`](stage-runtime-example/src/main/java/org/os890/mp/openapi/gui/example/DemoApplication.java) — just `@OpenAPIDefinition.info`; no security scheme in annotations.
- [`OAuth2SecurityFilter.java`](stage-runtime-example/src/main/java/org/os890/mp/openapi/gui/example/OAuth2SecurityFilter.java) — injects a minimal HTTP Bearer scheme + a document-level `SecurityRequirement`, so every operation gets Swagger UI's padlock (hidden) and `preauthorizeApiKey` can attach the token.
- [`HelloResource.java`](stage-runtime-example/src/main/java/org/os890/mp/openapi/gui/example/HelloResource.java) — pure Jakarta (`@Path`, `@RolesAllowed`, `@PermitAll`), demonstrating the three access levels.
- [`microprofile-config.properties`](stage-runtime-example/src/main/resources/META-INF/microprofile-config.properties) — carries Keycloak URL / realm / client_id as `openapi.ui.*` keys, which the Templates class substitutes into `openapi.html`'s `%...%` placeholders. Overridable per stage via env vars or `-D` system properties.
- [`keycloak/realm-demo.json`](keycloak/realm-demo.json) — `openapi-ui` client with `directAccessGrantsEnabled: true`; `demo` user with the `user` realm role.

## The issuer-URL gotcha (still relevant)

Unrelated to the form, but worth re-stating: the token's `iss` claim
must byte-match the issuer URL WildFly resolves Keycloak at when
fetching JWKS. This demo runs both containers in a **single Podman
pod**, sharing a network namespace, so the browser and WildFly see
Keycloak on the exact same URL (`http://localhost:8081`). Deploying
outside a shared-namespace setup requires `KC_HOSTNAME` on Keycloak
pointed at whatever hostname the backend will resolve.

## Password-grant caveat

Tokens minted via the `/token` endpoint with `grant_type=password`
do **not** carry the `auth_time` or `nonce` claims that an interactive
authorization-code login produces. Some backend validators require
those; if yours does, the password-grant token will be rejected
regardless of what UI drives the request. In that case you need a
flow that actually redirects the user to Keycloak's login page (same-
tab redirect is fine, popup isn't under COOP). See
`oauth2-support_simple-cookie_redirect` for the server-side variant.

## When to pick which variant

| Variant | Best when | Downside |
| --- | --- | --- |
| `oauth2-support_simple` | Keycloak admin can set `Cross-Origin-Opener-Policy: same-origin-allow-popups`. | Fragile against future browser changes. |
| `oauth2-support_simple_prefilled` | Same as above; you want a one-click Authorize button. | Same constraints. |
| `oauth2-rolesallowed` | You want Swagger UI security scheme auto-derived from Jakarta annotations. | Still a popup flow. |
| `oauth2-support_simple-cookie` / `_plain` / `_redirect` | You don't control Keycloak and you want the classic same-tab-redirect OIDC flow with server-side session cookies. Matches legacy Keycloak Java-adapter apps. | The Swagger UI itself is gated by login. |
| `oauth2-support_simple-bearer` | Paste-free Bearer UX via Swagger UI's own OAuth2 password dialog. | Can't hide the "paste a token" fallback; Swagger UI's logout UX differs from the app's. |
| **This one** — `oauth2-support_simple-inline-login` | Same as `bearer` but you want full UX control (branded login form, explicit Sign-out flow) and to guarantee users never see Swagger UI's generic Authorize dialog. | Still password grant — no `auth_time`/`nonce` in the token. |

## Files touched (vs. plain `stage-runtime-example`)

| File | Purpose |
| --- | --- |
| `stage-runtime-example/.../openapi.html` | **New.** White-label template with the embedded login form + token exchange + Sign-out. |
| `stage-runtime-example/.../OAuth2SecurityFilter.java` | Injects an HTTP Bearer scheme + document-level security requirement. |
| `stage-runtime-example/.../DemoApplication.java` | `@OpenAPIDefinition.info` only. |
| `stage-runtime-example/.../HelloResource.java` | Pure Jakarta `@Path` + `@RolesAllowed` / `@PermitAll`. |
| `stage-runtime-example/.../web.xml` | Security-constraint on `/hello/*` only; `role-name=**`; `resteasy.role.based.security=true`. |
| `stage-runtime-example/.../oidc.json` | Elytron OIDC `bearer-only`, five keys. |
| `stage-runtime-example/.../microprofile-config.properties` | `openapi.ui.keycloakUrl` / `.keycloakRealm` / `.keycloakClientId` for the template substitution. |
| `keycloak/realm-demo.json` | `directAccessGrantsEnabled: true`, `demo` user with `user` role. |
| `Dockerfile.oauth2` + `enable-oidc.cli` | WildFly with `elytron-oidc-client` subsystem (unchanged). |
| `build_and_start_oauth2.sh` | Podman pod wiring (unchanged). |

This variant drops the addon entirely — the `stage-runtime-example`
POM depends on `org.microprofile-ext.openapi-ext:openapi-ui:2.1.1`
directly. The upstream plugin ships the same Swagger UI 5.18.2 webjar,
so the runtime behaviour is identical.
