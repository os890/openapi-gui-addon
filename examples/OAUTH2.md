# OAuth2 / Keycloak Demo (auto-derive from `@RolesAllowed`)

Shows how to keep the OpenAPI UI addon working when the underlying REST API
is protected by Keycloak, **without writing any OpenAPI security
annotations on REST classes**. A small `OASFilter` classpath-scans the
JAX-RS resources and maps Jakarta Security annotations
(`@RolesAllowed`, `@PermitAll`, `@DenyAll`) onto per-operation
`SecurityRequirement`s in the generated OpenAPI document.

The demo protects the **Hello API** (`stage-runtime-example`) with Keycloak.
The other two demos stay open so you can compare.

## Run it

```bash
examples/build_and_start_oauth2.sh
```

Starts a Podman pod with **WildFly** (port 8080) and **Keycloak** (port
8081), imports the `demo` realm and creates a test user `demo` / `demo`
(assigned the realm role `user`).

Then open <http://localhost:8080/hello-api/openapi-ui/>, click **Authorize**,
type `openapi-ui` as *client_id*, tick the `openid` scope, click
**Authorize**, log in as `demo` / `demo`, and exercise the three demo
endpoints below.

## The three demo endpoints — and what the filter does with each

`HelloResource` is plain Jakarta, no `@SecurityRequirement` anywhere:

```java
@Path("/hello")
@RolesAllowed("user")                   // class-level default
public class HelloResource {

    @GET
    public String hello(...) { ... }    // inherits @RolesAllowed("user")

    @GET @Path("/ping")
    @PermitAll                          // overrides class level
    public String ping() { ... }

    @GET @Path("/admin")
    @RolesAllowed("admin")              // method-level override
    public String admin(...) { ... }
}
```

What the filter produces in the OpenAPI doc:

| Endpoint | Effective Jakarta access | OpenAPI `security` field | Swagger UI | Runtime |
| --- | --- | --- | --- | --- |
| `GET /hello` | inherited `@RolesAllowed("user")` | `[{ "keycloak": [] }]` | 🔒 padlock | 200 OK for `demo` (has `user` role) |
| `GET /hello/ping` | method `@PermitAll` wins | *(absent)* | no padlock | 200 OK; the web.xml constraint still requires authentication at the HTTP layer |
| `GET /hello/admin` | method `@RolesAllowed("admin")` wins | `[{ "keycloak": [] }]` | 🔒 padlock | **403 Forbidden** — `demo` user lacks `admin` |

The OpenAPI doc only encodes *"this endpoint requires auth"*; it cannot
say *"this endpoint requires role X"*. Role-specific enforcement happens
at runtime via Jakarta Security (Elytron OIDC validates the Bearer
token's `realm_access.roles`). The `admin` endpoint is the proof that
token-present-but-wrong-role cleanly returns 403.

## How the auto-derivation works

At deployment time `OAuth2SecurityFilter#filterOpenAPI(OpenAPI)`:

1. **Classpath-scans its own package** (`org.os890.mp.openapi.gui.example`)
   for every `.class` file via the WAR's ClassLoader. Supports `file:`,
   `jar:`, and WildFly VFS (`vfs:`) URL schemes — the VFS case uses
   reflection so no WildFly compile-time dependency is needed.
2. For each class annotated with JAX-RS `@Path`, iterates its declared
   methods. For methods carrying a JAX-RS verb annotation (`@GET`,
   `@POST`, …) it computes the **effective Jakarta access** using the
   standard precedence rule: method-level annotation wins over
   class-level, in the order `@DenyAll` → `@PermitAll` → `@RolesAllowed`.
3. For every operation whose effective access is `@RolesAllowed`, attaches
   a `SecurityRequirement` pointing at the scheme named `keycloak`.
4. Separately, injects the `keycloak` `SecurityScheme` itself into
   `components.securitySchemes`, populating the `authorizationUrl` and
   `tokenUrl` from MicroProfile Config
   (`app.oauth2.authorizationUrl`, `app.oauth2.tokenUrl`) — so Keycloak
   URLs are not hardcoded in Java annotations and can be overridden per
   stage via env vars or `-D` system properties.

Why a classpath scan and not `CDI.current().getBeanManager()`: CDI isn't
fully initialized when SmallRye OpenAPI runs filters during deployment.
A classloader-based scan avoids that timing issue.

## What needed to change versus an unprotected REST endpoint

### 1. `OAuth2SecurityFilter` (the only Java class the pattern actually requires)

See [`OAuth2SecurityFilter.java`](stage-runtime-example/src/main/java/org/os890/mp/openapi/gui/example/OAuth2SecurityFilter.java)
— registered via `mp.openapi.filter` in `microprofile-config.properties`.

### 2. Enforce auth on the REST path — but NOT on the UI

The addon UI, its CSS, and the OpenAPI document must stay reachable
without login. [`WEB-INF/web.xml`](stage-runtime-example/src/main/webapp/WEB-INF/web.xml)
constrains only `/hello/*`; the addon's JAX-RS routes (`/openapi-ui/*`,
`/webjars/*`) are deliberately excluded.

```xml
<security-constraint>
    <web-resource-collection>
        <url-pattern>/hello/*</url-pattern>
    </web-resource-collection>
    <auth-constraint><role-name>*</role-name></auth-constraint>
</security-constraint>
<login-config><auth-method>OIDC</auth-method></login-config>
```

Token validation is performed by the **WildFly Elytron OIDC client**
via [`WEB-INF/oidc.json`](stage-runtime-example/src/main/webapp/WEB-INF/oidc.json).
`bearer-only: true` tells WildFly to validate incoming Bearer tokens
without initiating any browser redirect of its own — Swagger UI owns
the login flow on the front channel.

### 3. Point Swagger UI at a valid redirect URL

Base-plugin default is `/oauth2-redirect.html`, which (a) doesn't exist
at the WAR root and (b) is a bare path — Keycloak rejects bare paths
with *"Invalid parameter: redirect_uri"*. Fix:

```properties
openapi.ui.oauth2RedirectUri=http://localhost:8080/hello-api/webjars/swagger-ui/5.18.2/oauth2-redirect.html
```

Must also appear in the Keycloak client's `redirectUris` list (see
[`keycloak/realm-demo.json`](keycloak/realm-demo.json)).

## The redirect-URI gotcha

Keycloak accepts only **absolute URLs** (scheme + host + port + path).
A bare path is rejected even when the client has a path-only entry
registered. Swagger UI forwards `oauth2RedirectUrl` verbatim.

## The issuer-URL gotcha

Classic trap: **Authorize** succeeds, user logs in, token comes back,
**Try it out** fires… WildFly answers 401. Almost always an `iss` claim
mismatch — the token's issuer URL must byte-match whatever URL WildFly
uses to fetch JWKS. This demo sidesteps it by running both containers
in a single **Podman pod** so the browser and WildFly share a network
namespace and reach Keycloak on identical URLs.

## Files touched

| File | Purpose |
| --- | --- |
| `stage-runtime-example/.../OAuth2SecurityFilter.java` | Classpath scan + OAS security-scheme injection + `@RolesAllowed/@PermitAll/@DenyAll` → `SecurityRequirement`. |
| `stage-runtime-example/.../DemoApplication.java` | Plain `@OpenAPIDefinition.info` — no security scheme block. |
| `stage-runtime-example/.../HelloResource.java` | Pure Jakarta Security annotations (`@RolesAllowed`, `@PermitAll`). |
| `stage-runtime-example/.../web.xml` | Security-constraint on `/hello/*` only; OIDC login-config. |
| `stage-runtime-example/.../oidc.json` | Elytron OIDC (`bearer-only`). |
| `stage-runtime-example/.../microprofile-config.properties` | `mp.openapi.filter`, `app.oauth2.*` URLs, `openapi.ui.oauth2RedirectUri`. |
| `keycloak/realm-demo.json` | Realm, `openapi-ui` public client, `demo` user assigned the `user` role. |
| `Dockerfile.oauth2` + `enable-oidc.cli` | WildFly with `elytron-oidc-client` subsystem. |
| `build_and_start_oauth2.sh` | Podman pod wiring. |

The `addon/` directory is not touched.

## Related branches

- `oauth2-support_simple` — same popup flow but security scheme declared via annotation (`@SecurityRequirement`); no auto-derivation.
- `oauth2-support_simple_prefilled` — same plus `initOAuth()` prefill so Authorize is one click.
- `oauth2-support_simple-cookie` — replaces the popup flow entirely with a server-side OIDC session cookie; no OpenAPI security scheme at all. Useful when Keycloak's COOP headers break the popup flow and you can't change them.
