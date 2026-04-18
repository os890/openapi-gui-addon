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

`DemoApplication.getClasses()` unconditionally registers the UI classes:

```java
import org.os890.mp.openapi.gui.OpenApiUiService;
import org.os890.mp.openapi.gui.StaticResourcesService;

classes.add(OpenApiUiService.class);
classes.add(StaticResourcesService.class);
```

## Build and Deploy with WildFly 39

```bash
# Build the WAR
mvn clean package

# Build and run all examples via Podman (from the examples/ directory)
podman build -t openapi-gui-examples .
podman run --rm -p 8080:8080 openapi-gui-examples
```

Alternatively, use the interactive launcher from the `examples/` directory:
```bash
./build_and_start.sh
```

## Endpoints

- REST: `http://localhost:8080/info-api/info`
- OpenAPI UI: `http://localhost:8080/info-api/openapi-ui/`
- OpenAPI document: `http://localhost:8080/info-api/openapi`
