# Stage Runtime Example

Demonstrates enabling/disabling the OpenAPI UI based on a **runtime** MicroProfile Config property.

## Dependency

```xml
<dependency>
    <groupId>org.os890.mp-ext</groupId>
    <artifactId>openapi-gui-addon</artifactId>
    <version>1.11.0</version>
</dependency>
```

## How it works

`DemoApplication.getClasses()` reads `project.stage` via `ConfigProvider.getConfig()` at startup.
If no value is set, it defaults to `production` (UI disabled). The UI is only enabled
when `project.stage` is explicitly set to a non-production value (e.g. `development`).

```java
import org.os890.mp.openapi.gui.OpenApiUiService;
import org.os890.mp.openapi.gui.StaticResourcesService;

String stage = ConfigProvider.getConfig()
        .getOptionalValue("project.stage", String.class)
        .orElse("production");

if (!"production".equals(stage)) {
    classes.add(OpenApiUiService.class);
    classes.add(StaticResourcesService.class);
}
```

## Configuration

To enable the UI at runtime, pass `-Dproject.stage=development` as a JVM system property.

Without any configuration, the UI is disabled (production default).

## Build and Deploy with WildFly 39

```bash
# Build the WAR
mvn clean package

# Build and run all examples via Podman (from the examples/ directory)
podman build -t openapi-gui-examples .
podman run --rm -p 8080:8080 openapi-gui-examples

# To enable the UI, pass the system property to WildFly
podman run --rm -p 8080:8080 openapi-gui-examples \
    /opt/jboss/wildfly/bin/standalone.sh -b 0.0.0.0 \
    -c standalone-microprofile.xml \
    -Dproject.stage=development
```

Alternatively, use the interactive launcher from the `examples/` directory:
```bash
./build_and_start.sh
```

## Endpoints

- REST: `http://localhost:8080/hello-api/hello`
- OpenAPI UI: `http://localhost:8080/hello-api/openapi-ui/` (only when enabled)
- OpenAPI document: `http://localhost:8080/hello-api/openapi`
