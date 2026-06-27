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

# Optional: OAuth2/OIDC Authorize-dialog presets (see "Per-module authentication" below)
openapi.ui.oauth2RedirectUri=/my-app/webjars/swagger-ui/5.18.2/oauth2-redirect.html
openapi.ui.oauth2ClientId=swagger-ui
```

### Per-module authentication

When the dropdown switches APIs, Swagger UI rebuilds its "Authorize" dialog from the
**currently selected** spec's `securitySchemes` — so each module can use its own auth scheme
through the one shared UI, driven entirely by what each OpenAPI document declares.

- `openapi.ui.oauth2ClientId` — pre-fills the client id in the OAuth2/OIDC Authorize dialog and
  enables PKCE (`ui.initOAuth`). Optional; unset = no change.
- `openapi.ui.oauth2RedirectUri` — Swagger UI OAuth2 redirect landing page (only relevant for the
  authorization-code flow). Default `/oauth2-redirect.html`; for an app under a context root point
  it at the served webjar copy.
- **HTTP Digest** — Swagger UI has no native digest support, so the addon adds a
  `requestInterceptor` that performs the digest challenge/response. It is **spec-driven**: a module
  signals digest in its own OpenAPI document via a security scheme of `type: http` with
  `scheme: digest`, or `scheme: basic` carrying the extension `x-auth-mode: digest` (basic gives a
  working credential dialog while the addon does digest on the wire). No GUI-side configuration —
  when the selected spec declares digest, the interceptor activates; otherwise it is inert.

A complete worked example — one server, two WARs each shipping this addon, OIDC on one and Digest
on the other, switched from either module's Swagger UI — lives in
[`poc-oidc-digest/`](poc-oidc-digest/).

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
    ├── build_and_start.sh          Interactive launcher for all demos
    └── Dockerfile                  Deploys all three on WildFly 39
```

## White-Labeling

Override the default branding by placing files on the classpath:
- `openapi.png` — custom logo
- `openapi.css` — custom stylesheet
- `openapi.html` — custom HTML template

## License

Apache License, Version 2.0
