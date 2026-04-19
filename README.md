# OpenAPI GUI Addon

A self-contained OpenAPI UI addon for Jakarta EE 11 / MicroProfile applications.
Built on top of [microprofile-extensions/openapi-ext](https://github.com/microprofile-extensions/openapi-ext) with added multi-API dropdown support and availability checking.

## Features

- Swagger UI 5.18.2 bundled (no external requests, fully self-contained)
- Multi-API dropdown: configure multiple OpenAPI endpoints, switch between them in the UI
- Availability check: unreachable APIs are shown as disabled in the dropdown (checked on every page refresh)
- Configurable via MicroProfile Config
- No `web-fragment.xml` — register UI classes explicitly via `Application.getClasses()` for full control (e.g. project-stage gating)

## Maven Dependency

```xml
<dependency>
    <groupId>org.os890.mp-ext</groupId>
    <artifactId>openapi-gui-addon</artifactId>
    <version>1.11.0</version>
</dependency>
```

## Usage

### Basic Setup

Just add the dependency — the addon auto-discovers via JAX-RS. No code needed beyond your REST resources and a minimal `Application` class:

```java
@ApplicationPath("/")
public class MyApplication extends Application {
}
```

### MicroProfile Config Properties

Add to `META-INF/microprofile-config.properties`:

```properties
# Required: path to this app's OpenAPI document
openapi.ui.yamlUrl=/my-app/openapi

# Required for per-app OpenAPI endpoints on WildFly (SmallRye extension)
mp.openapi.extensions.path=/my-app/openapi

# Optional: page title
openapi.ui.title=My API

# Optional: multi-API dropdown (comma-separated name=url pairs)
openapi.ui.urls=My API=/my-app/openapi,Other API=/other-app/openapi

# Optional: explicit context root (auto-detected if omitted)
openapi.ui.contextRoot=/my-app

# Optional: Swagger UI theme (default: flattop)
openapi.ui.swaggerUiTheme=flattop

# Optional: visibility controls (default values shown)
openapi.ui.swaggerHeaderVisibility=visible
openapi.ui.exploreFormVisibility=hidden
openapi.ui.serverVisibility=hidden
openapi.ui.createdWithVisibility=visible
openapi.ui.modelsVisibility=visible
```

### Multi-API Dropdown

When `openapi.ui.urls` is configured, the UI shows a dropdown to switch between APIs.
The explore form is automatically made visible to display the dropdown.

Format: `Name1=url1,Name2=url2,...`

On every page refresh, each configured URL is checked via a HEAD request:
- Reachable APIs appear as selectable entries
- Unreachable APIs appear below a separator as disabled/greyed out

### Project-Stage Gating

The addon includes a `@PreMatching` JAX-RS filter that controls UI access based on two config properties:

- `openapi.ui.enabled` — explicit override (`true`/`false`). When set, takes precedence.
- `project.stage` — if set to `production` and `openapi.ui.enabled` is not set, the UI is disabled.

No code changes needed — just configure via properties.

**Runtime (JVM system property):**
```bash
# Enable UI (WildFly)
standalone.sh -Dproject.stage=development

# Disable UI (default when no config is set)
standalone.sh
```

**Build-time (Maven profile — addon only in non-production builds):**

The most secure approach: don't ship the addon JAR at all in production.

```xml
<profiles>
    <profile>
        <id>development</id>
        <dependencies>
            <dependency>
                <groupId>org.os890.mp-ext</groupId>
                <artifactId>openapi-gui-addon</artifactId>
                <version>1.11.0</version>
            </dependency>
        </dependencies>
    </profile>
</profiles>
```

Build with `mvn clean package -Pdevelopment` to include the UI. Default build has no UI code.

**Always enabled:**
```properties
openapi.ui.enabled=true
```

## Project Structure

```
openapi-gui-addon/
├── addon/          The addon JAR (org.os890.mp-ext:openapi-gui-addon)
└── examples/       Three demo apps showing different project-stage approaches
    ├── openapi-config/             Shared openapi.ui.urls config
    ├── stage-runtime-example/      UI gated by runtime config (project.stage)
    ├── stage-buildtime-example/    UI gated by Maven profile (-Pdevelopment)
    ├── stage-none-example/         UI always enabled
    ├── keycloak/realm-demo.json    OAuth2 demo realm (see examples/OAUTH2.md)
    ├── build_and_start.sh          Interactive launcher for all demos
    ├── build_and_start_oauth2.sh   OAuth2 demo (WildFly + Keycloak pod)
    ├── Dockerfile                  Deploys all three on WildFly 39
    └── Dockerfile.oauth2           WildFly image with elytron-oidc-client
```

## OAuth2 / Keycloak — session cookie variant

The Hello API demo on this branch runs behind Keycloak **without using
Swagger UI's OAuth2 popup flow**. Authentication is handled server-side
by WildFly Elytron OIDC; Swagger UI's "Try it out" rides on the session
cookie established during the first OIDC redirect.

Reason: the popup flow is broken by Keycloak's default
`Cross-Origin-Opener-Policy: same-origin` header, which severs
`window.opener` and makes `oauth2-redirect.html` throw. If you don't
control the Keycloak instance, session-cookie mode is the only
reliable path.

See [examples/OAUTH2.md](examples/OAUTH2.md) for the full rationale, the
exact differences versus the popup variant, and when to use each approach.

Minimal setup:
- `WEB-INF/oidc.json` without `bearer-only` — Elytron runs the full OIDC code flow.
- `web.xml` protects `/*` — the entire WAR; the first visit triggers login.
- No OpenAPI security scheme declared — Swagger UI shows no Authorize button.

Related branches:
- `oauth2-support_simple` — OAuth2 popup flow with manual client_id entry.
- `oauth2-support_simple_prefilled` — same plus `initOAuth()` prefill.
- `oauth2-rolesallowed` — auto-derives `SecurityRequirement` from `@RolesAllowed` / `@PermitAll` / `@DenyAll`.

## White-Labeling

Override the default branding by placing files on the classpath:
- `openapi.png` — custom logo
- `openapi.css` — custom stylesheet
- `openapi.html` — custom HTML template

## License

Apache License, Version 2.0
