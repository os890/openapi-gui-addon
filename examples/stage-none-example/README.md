# Stage None Example

Demonstrates the simplest setup — the OpenAPI UI is **always enabled** with no project-stage gating.

## Dependency

```xml
<dependency>
    <groupId>org.os890.mp-ext</groupId>
    <artifactId>openapi-gui-addon</artifactId>
    <version>1.8.0</version>
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

## Endpoints

- REST: `http://localhost:8080/info-api/info`
- OpenAPI UI: `http://localhost:8080/info-api/openapi-ui/`
- OpenAPI document: `http://localhost:8080/info-api/openapi`
