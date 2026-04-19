# Stage Runtime Example

Demonstrates two things at once:

1. Enabling/disabling the OpenAPI UI based on a **runtime** system property
   (`project.stage`).
2. **Protecting the whole WAR with Keycloak OIDC via server-side session
   cookies.** The initial request redirects to Keycloak, the user logs in,
   and every subsequent request (including Swagger UI's "Try it out"
   calls) is authenticated via the session cookie. No OAuth2 popup in
   Swagger UI — bypasses browser COOP restrictions entirely. See
   [../OAUTH2.md](../OAUTH2.md) for the full story and the
   `build_and_start_oauth2.sh` launcher.

## Dependency

```xml
<dependency>
    <groupId>org.os890.mp-ext</groupId>
    <artifactId>openapi-gui-addon</artifactId>
    <version>1.11.0</version>
</dependency>
```

## How it works

The addon includes a `@PreMatching` JAX-RS filter that checks the `project.stage` config property at startup. If set to `production` (or not set at all), the UI is disabled. No application code needed.

## Configuration

To enable the UI at runtime, pass `-Dproject.stage=development` to WildFly.

Without any configuration, the UI is disabled (production default).

## Build and Deploy with WildFly 39

```bash
# Build the WAR
mvn clean package

# Run via Podman with UI disabled (default)
podman build -t openapi-demos .
podman run --rm -p 8080:8080 openapi-demos

# Run via Podman with UI enabled
podman run --rm -p 8080:8080 -e PROJECT_STAGE=development openapi-demos
```

Alternatively, use the interactive launcher from the `examples/` directory:
```bash
./build_and_start.sh
```

## Endpoints (all protected by the WAR-wide security-constraint)

- REST: `http://localhost:8080/hello-api/hello`
- OpenAPI UI: `http://localhost:8080/hello-api/openapi-ui/`
- OpenAPI document: `http://localhost:8080/hello-api/openapi`

Launch the demo via `../build_and_start_oauth2.sh`. First visit redirects
to Keycloak; log in as `demo`/`demo`; Swagger UI then loads and
**Try it out** works without any additional Authorize step — the browser
carries the session cookie automatically.
