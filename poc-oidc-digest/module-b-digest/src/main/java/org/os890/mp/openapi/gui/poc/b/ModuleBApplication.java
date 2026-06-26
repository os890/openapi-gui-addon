/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.os890.mp.openapi.gui.poc.b;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

/**
 * Module B is protected by HTTP Digest (WildFly Elytron digest mechanism, realm "poc-digest").
 *
 * The OpenAPI document declares an {@code http} / {@code digest} security scheme for
 * documentation. Swagger UI has no native digest flow, so the shared GUI's addon supplies a
 * context-root-aware requestInterceptor (enabled via openapi.ui.digestPaths=/module-b) that
 * performs the digest challenge/response only for /module-b/** requests.
 */
@ApplicationPath("/api")
@OpenAPIDefinition(info = @Info(title = "Module B (Digest)", version = "1.0.0"))
@SecurityScheme(
        securitySchemeName = "digest",
        type = SecuritySchemeType.HTTP,
        scheme = "digest"
)
public class ModuleBApplication extends Application {
}
