# Stage None Example

Demonstrates the simplest setup -- the OpenAPI UI is **always enabled** with no project-stage gating.

## Dependency

```xml
<dependency>
    <groupId>org.os890.mp-ext</groupId>
    <artifactId>openapi-gui-addon</artifactId>
    <version>1.11.0</version>
</dependency>
```

## How it works

The addon auto-discovers via JAX-RS. To keep the UI always enabled regardless of `project.stage`, set `openapi.ui.enabled=true` in `microprofile-config.properties`:

```properties
openapi.ui.enabled=true
```

## Build and Deploy with WildFly 39

```bash
# Build the WAR
mvn clean package

# Run via Podman
podman build -t openapi-demos .
podman run --rm -p 8080:8080 openapi-demos
```

Alternatively, use the interactive launcher from the `examples/` directory:
```bash
./build_and_start.sh
```

## Endpoints

- REST: `http://localhost:8080/info-api/info`
- OpenAPI UI: `http://localhost:8080/info-api/openapi-ui/`
- OpenAPI document: `http://localhost:8080/info-api/openapi`
