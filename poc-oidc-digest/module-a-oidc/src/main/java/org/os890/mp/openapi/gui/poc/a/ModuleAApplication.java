/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.os890.mp.openapi.gui.poc.a;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

/**
 * Module A is an OIDC-protected resource server.
 *
 * The OpenAPI document declares an {@code openIdConnect} security scheme so the shared
 * Swagger UI renders the native "Authorize" dialog and runs the authorization-code + PKCE
 * flow against Keycloak. The resulting bearer token is sent to {@code /module-a/api/*},
 * which WildFly's elytron-oidc-client validates (bearer-only, see WEB-INF/oidc.json).
 *
 * openIdConnectUrl points at the browser-reachable Keycloak issuer (localhost:8081).
 */
@ApplicationPath("/api")
@OpenAPIDefinition(info = @Info(title = "Module A (OIDC)", version = "1.0.0"))
@SecurityScheme(
        securitySchemeName = "oidc",
        type = SecuritySchemeType.OPENIDCONNECT,
        openIdConnectUrl = "http://localhost:8081/realms/poc/.well-known/openid-configuration"
)
public class ModuleAApplication extends Application {
}
