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
import org.eclipse.microprofile.openapi.annotations.security.OAuthFlow;
import org.eclipse.microprofile.openapi.annotations.security.OAuthFlows;
import org.eclipse.microprofile.openapi.annotations.security.OAuthScope;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

/**
 * Module A is an OIDC-protected resource server (bearer-only, validated by WildFly's
 * elytron-oidc-client — see WEB-INF/oidc.json).
 *
 * The scheme is declared as {@code oauth2} with the {@code password} (direct-access-grant)
 * flow. Swagger UI's "Authorize" dialog therefore collects username + password + client_id
 * inline and POSTs {@code grant_type=password} straight to Keycloak's token endpoint — no
 * popup, no redirect. This deliberately avoids the authorization-code popup flow, which breaks
 * against Keycloak 24+ (its {@code Cross-Origin-Opener-Policy: same-origin} header severs
 * {@code window.opener}, so Swagger UI's oauth2-redirect handler throws). The public client
 * "swagger-ui" needs no secret (that field is hidden via openapi.ui.oauth2HideClientSecret);
 * only the {@code openid} scope is requested.
 *
 * tokenUrl points at the browser-reachable Keycloak (localhost:8081).
 */
@ApplicationPath("/api")
@OpenAPIDefinition(info = @Info(title = "Module A (OIDC)", version = "1.0.0"))
@SecurityScheme(
        securitySchemeName = "oidc",
        type = SecuritySchemeType.OAUTH2,
        flows = @OAuthFlows(
                password = @OAuthFlow(
                        tokenUrl = "http://localhost:8081/realms/poc/protocol/openid-connect/token",
                        scopes = @OAuthScope(name = "openid", description = "OpenID Connect authentication")
                )
        )
)
public class ModuleAApplication extends Application {
}
