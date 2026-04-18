# Stage Build-Time Example

Demonstrates enabling/disabling the OpenAPI UI based on a **Maven profile** at build time.

## Dependency

```xml
<dependency>
    <groupId>org.os890.mp-ext</groupId>
    <artifactId>openapi-gui-addon</artifactId>
    <version>1.11.0</version>
</dependency>
```

## How it works

The default build has `openapi.ui.enabled=false` (UI disabled). The Maven profile
`development` sets `openapi.ui.enabled=true` which is baked into
`microprofile-config.properties` via resource filtering. `DemoApplication.getClasses()`
reads this property at startup to decide whether to register the UI classes.

```java
import org.os890.mp.openapi.gui.OpenApiUiService;
import org.os890.mp.openapi.gui.StaticResourcesService;

if (Boolean.parseBoolean(
        ConfigProvider.getConfig()
                .getOptionalValue("openapi.ui.enabled", String.class)
                .orElse("false"))) {
    classes.add(OpenApiUiService.class);
    classes.add(StaticResourcesService.class);
}
```

## Configuration

`pom.xml` defines the default (disabled) and development profile (enabled):
```xml
<properties>
    <openapi.ui.enabled>false</openapi.ui.enabled>
</properties>

<profiles>
    <profile>
        <id>development</id>
        <properties>
            <openapi.ui.enabled>true</openapi.ui.enabled>
        </properties>
    </profile>
</profiles>
```

## Build and Deploy with WildFly 39

```bash
# Default build (UI disabled)
mvn clean package

# Development build (UI enabled)
mvn clean package -Pdevelopment

# Build and run all examples via Podman (from the examples/ directory)
podman build -t openapi-gui-examples .
podman run --rm -p 8080:8080 openapi-gui-examples
```

Alternatively, use the interactive launcher from the `examples/` directory:
```bash
./build_and_start.sh
```

## Endpoints

- REST: `http://localhost:8080/greeting-api/greeting`
- OpenAPI UI: `http://localhost:8080/greeting-api/openapi-ui/` (only when enabled)
- OpenAPI document: `http://localhost:8080/greeting-api/openapi`
