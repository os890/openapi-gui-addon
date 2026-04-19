# Stage Runtime Example

Demonstrates two things at once:

1. Enabling/disabling the OpenAPI UI based on a **runtime** system property
   (`project.stage`).
2. **Protecting the REST endpoint with Keycloak OAuth2** while keeping the
   OpenAPI UI ("Try it out" included) fully functional. See
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

## Endpoints

- OpenAPI UI: `http://localhost:8080/hello-api/openapi-ui/` (open; only when `project.stage != production`)
- OpenAPI document: `http://localhost:8080/hello-api/openapi` (open)
- `GET /hello` — inherits class-level `@RolesAllowed("user")` → padlock + Bearer token required.
- `GET /hello/ping` — `@PermitAll` overrides class level → no padlock in the doc; still auth'd at the HTTP layer by `web.xml`.
- `GET /hello/admin` — `@RolesAllowed("admin")` → padlock; demo user lacks the role, so calls return **403**.

Launch via `../build_and_start_oauth2.sh`, click **Authorize** in the
Swagger UI, enter `openapi-ui` as client_id + tick `openid`, log in as
`demo`/`demo`, then **Try it out** on each endpoint.
