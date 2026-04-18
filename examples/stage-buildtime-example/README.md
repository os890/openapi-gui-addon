# Stage Build-Time Example

Demonstrates enabling/disabling the OpenAPI UI by including or excluding the addon JAR at build time via a **Maven profile**.

## How it works

The addon dependency is only present in the `development` profile. The default build produces a WAR with no addon code at all -- the most secure approach since there is nothing to exploit.

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

No config properties or code changes needed. The addon auto-discovers via JAX-RS when present.

## Build and Deploy with WildFly 39

```bash
# Default build (production -- no addon, no UI)
mvn clean package

# Development build (addon included, UI available)
mvn clean package -Pdevelopment
```

Alternatively, use the interactive launcher from the `examples/` directory:
```bash
./build_and_start.sh
```

## Endpoints

- REST: `http://localhost:8080/greeting-api/greeting`
- OpenAPI UI: `http://localhost:8080/greeting-api/openapi-ui/` (only when built with `-Pdevelopment`)
- OpenAPI document: `http://localhost:8080/greeting-api/openapi`
