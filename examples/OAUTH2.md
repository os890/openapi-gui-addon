# OAuth2 / Keycloak Demo

Shows how to keep the OpenAPI UI addon working when the underlying REST API
is protected by OAuth2. The goal: clicking **Authorize** in the UI, logging
in at Keycloak, and then using **Try it out** on any endpoint must result in
a successful authenticated call — not a 401.

The demo protects the **Hello API** (`stage-runtime-example`) with Keycloak.
The other two demos stay open so you can compare.

## Run it

```bash
examples/build_and_start_oauth2.sh
```

This builds the addon + WAR, starts a Podman pod with **WildFly** (port 8080)
and **Keycloak** (port 8081), imports the `demo` realm and creates a test
user `demo` / `demo`.

Then open <http://localhost:8080/hello-api/openapi-ui/>, click **Authorize**,
log in as `demo` / `demo`, and run `GET /hello` via **Try it out**.

## What needed to change

The base plugin already exposes `openapi.ui.oauth2RedirectUri` and passes
it to Swagger UI, so the **Authorize** button appears automatically once
the OpenAPI document declares an OAuth2 security scheme. The addon adds
one small extra — three optional `openapi.ui.oauth2.*` keys for prefilling
the dialog via `initOAuth()` — so the user doesn't have to type the
client_id each time. Everything else is on the REST app side.

### 1. OpenAPI document — declare the security scheme

Without this, Swagger UI shows no Authorize button and has no idea where to
send users. See [`DemoApplication.java`](stage-runtime-example/src/main/java/org/os890/mp/openapi/gui/example/DemoApplication.java):

```java
@OpenAPIDefinition(
    components = @Components(
        securitySchemes = @SecurityScheme(
            securitySchemeName = "keycloak",
            type = SecuritySchemeType.OAUTH2,
            flows = @OAuthFlows(
                authorizationCode = @OAuthFlow(
                    authorizationUrl = "http://localhost:8081/realms/demo/protocol/openid-connect/auth",
                    tokenUrl         = "http://localhost:8081/realms/demo/protocol/openid-connect/token",
                    scopes = @OAuthScope(name = "openid", description = "OIDC ID token")
                )
            )
        )
    )
)
```

And per-endpoint:

```java
@Path("/hello")
@SecurityRequirement(name = "keycloak")
public class HelloResource { ... }
```

### 2. Enforce auth on the REST path — but NOT on the UI

This is the subtle part. The addon UI lives inside the same WAR as the REST
API, at `/openapi-ui/*` and `/webjars/*`, and the OpenAPI document at
`/openapi`. If you drop a blanket `<security-constraint>` on `/*` you will
lock the UI itself out and the whole flow collapses.

[`WEB-INF/web.xml`](stage-runtime-example/src/main/webapp/WEB-INF/web.xml)
constrains **only** `/hello/*`:

```xml
<security-constraint>
    <web-resource-collection>
        <url-pattern>/hello/*</url-pattern>
    </web-resource-collection>
    <auth-constraint><role-name>*</role-name></auth-constraint>
</security-constraint>
<login-config><auth-method>OIDC</auth-method></login-config>
```

Token validation is performed by the **WildFly Elytron OIDC client** via
[`WEB-INF/oidc.json`](stage-runtime-example/src/main/webapp/WEB-INF/oidc.json).
`bearer-only: true` tells WildFly to validate incoming Bearer tokens without
initiating any browser redirect of its own — the UI handles the flow.

### 3. Point Swagger UI at a valid redirect URL + prefill the dialog

The base plugin's default for `openapi.ui.oauth2RedirectUri` is
`/oauth2-redirect.html`, which (a) doesn't exist at the WAR context root
and (b) is a bare path, which Keycloak rejects with *"Invalid parameter:
redirect_uri"*. Fix both by pointing at the `oauth2-redirect.html` the
addon already ships inside the swagger-ui webjar, as an **absolute URL**:

```properties
openapi.ui.oauth2RedirectUri=http://localhost:8080/hello-api/webjars/swagger-ui/5.18.2/oauth2-redirect.html

# Optional — prefill Swagger UI's Authorize dialog (one-click authorize).
openapi.ui.oauth2.clientId=openapi-ui
openapi.ui.oauth2.scopes=openid
```

The exact redirect URL must also appear in the Keycloak client's
`redirectUris` list (see [`keycloak/realm-demo.json`](keycloak/realm-demo.json)).

## The redirect-URI gotcha

Keycloak only accepts `redirect_uri` values that are **absolute URLs**
(scheme + host + port + path). A bare path is rejected even when a
matching path-only entry exists on the client. Swagger UI forwards
`oauth2RedirectUrl` to Keycloak verbatim, so don't set it to a relative
path — always use the full URL, and keep the same URL on the Keycloak
client's list.

## The issuer-URL gotcha

This trips up nearly everyone: **Authorize** works, the user logs in, a
token comes back, **Try it out** fires… and WildFly answers 401.

The cause is almost always that the token's `iss` claim does not match the
issuer URL WildFly uses when fetching Keycloak's JWKS. In a container
setup, the browser typically reaches Keycloak on `http://localhost:8081`
while the backend might reach it on `http://keycloak:8080` — same Keycloak,
different URLs, different issuer strings.

This demo sidesteps the problem by running both containers in a **single
Podman pod**, so they share a network namespace:

- Browser → `http://localhost:8081` (published port)
- WildFly → `http://localhost:8081` (same-pod localhost)
- Token `iss` → `http://localhost:8081/realms/demo` — matches on both sides.

If you deploy outside a shared-namespace setup, set Keycloak's
`KC_HOSTNAME` so the public URL matches what the backend sees, or configure
the backend to resolve the public hostname. There is no way around this:
the issuer claim in the token and the URL used to validate the token must
be byte-for-byte identical.

## Files touched

| File | Purpose |
| --- | --- |
| `addon/.../template.html` + `Templates.java` | `initOAuth()` prefill from `openapi.ui.oauth2.*` keys. |
| `stage-runtime-example/.../DemoApplication.java` | OpenAPI `@SecurityScheme` + Keycloak URLs. |
| `stage-runtime-example/.../HelloResource.java` | `@SecurityRequirement` + authenticated principal. |
| `stage-runtime-example/.../web.xml` | Security constraint on `/hello/*` only. |
| `stage-runtime-example/.../oidc.json` | Elytron OIDC client config (`bearer-only`). |
| `stage-runtime-example/.../microprofile-config.properties` | Absolute `oauth2RedirectUri` + prefill keys. |
| `keycloak/realm-demo.json` | Realm, `openapi-ui` public client, `demo` user. |
| `Dockerfile.oauth2` + `enable-oidc.cli` | WildFly with `elytron-oidc-client` subsystem. |
| `build_and_start_oauth2.sh` | Podman pod wiring. |
