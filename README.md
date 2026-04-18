# OpenAPI GUI Addon

A self-contained OpenAPI UI addon for Jakarta EE 8 / MicroProfile applications.
Built on top of [microprofile-extensions/openapi-ext](https://github.com/microprofile-extensions/openapi-ext) with added multi-API dropdown support and availability checking.

## Features

- Swagger UI 4.15.5 bundled (no external requests, fully self-contained)
- Multi-API dropdown: configure multiple OpenAPI endpoints, switch between them in the UI
- Availability check: unreachable APIs are shown as disabled in the dropdown (checked on every page refresh)
- Configurable via MicroProfile Config
- No `web-fragment.xml` — register UI classes explicitly via `Application.getClasses()` for full control (e.g. project-stage gating)

## Maven Dependency

```xml
<dependency>
    <groupId>org.os890.mp-ext</groupId>
    <artifactId>openapi-gui-addon</artifactId>
    <version>1.8.0</version>
</dependency>
```

## Usage

### Basic Setup

Register the UI classes in your JAX-RS Application:

```java
import org.os890.mp.openapi.gui.OpenApiUiService;
import org.os890.mp.openapi.gui.StaticResourcesService;

@ApplicationPath("/")
public class MyApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(MyResource.class);
        classes.add(OpenApiUiService.class);
        classes.add(StaticResourcesService.class);
        return classes;
    }
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

The addon has no `web-fragment.xml`, so the UI is only active when explicitly registered in `getClasses()`.
This enables conditional registration based on project stage.

**Runtime config check:**

```java
@Override
public Set<Class<?>> getClasses() {
    Set<Class<?>> classes = new HashSet<>();
    classes.add(MyResource.class);

    String stage = ConfigProvider.getConfig()
            .getOptionalValue("project.stage", String.class)
            .orElse("production");

    if (!"production".equals(stage)) {
        classes.add(OpenApiUiService.class);
        classes.add(StaticResourcesService.class);
    }
    return classes;
}
```

**Build-time Maven profile:**

In `pom.xml`:
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

In `microprofile-config.properties` (with Maven resource filtering enabled):
```properties
openapi.ui.enabled=${openapi.ui.enabled}
```

In `DemoApplication.java`:
```java
if (Boolean.parseBoolean(
        ConfigProvider.getConfig()
                .getOptionalValue("openapi.ui.enabled", String.class)
                .orElse("false"))) {
    classes.add(OpenApiUiService.class);
    classes.add(StaticResourcesService.class);
}
```

Build with `mvn clean package -Pdevelopment` to enable the UI.

## Project Structure

```
openapi-gui-addon/
├── addon/          The addon JAR (org.os890.mp-ext:openapi-gui-addon)
└── examples/       Three demo apps showing different project-stage approaches
    ├── openapi-config/             Shared openapi.ui.urls config
    ├── stage-runtime-example/      UI gated by runtime config (project.stage)
    ├── stage-buildtime-example/    UI gated by Maven profile (-Pproduction)
    ├── stage-none-example/         UI always enabled
    └── Dockerfile                  Deploys all three on WildFly 25
```

## White-Labeling

Override the default branding by placing files on the classpath:
- `openapi.png` — custom logo
- `openapi.css` — custom stylesheet
- `openapi.html` — custom HTML template

## License

Apache License, Version 2.0
