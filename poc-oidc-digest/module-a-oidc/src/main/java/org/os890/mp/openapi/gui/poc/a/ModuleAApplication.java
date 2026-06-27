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
 * Module A is an OIDC-protected resource server.
 *
 * The scheme is declared as {@code oauth2} with an explicit {@code authorizationCode} flow
 * (rather than {@code openIdConnect}) so the Swagger UI "Authorize" dialog lists ONLY the
 * scope we actually need ({@code openid}) instead of every scope advertised by Keycloak's
 * discovery document. The flow runs authorization-code + PKCE against Keycloak (public client
 * "swagger-ui", preset via openapi.ui.oauth2ClientId) — no client secret is used; leave that
 * field blank. The resulting bearer token is validated by WildFly's elytron-oidc-client
 * (bearer-only, see WEB-INF/oidc.json).
 *
 * The authorization/token URLs point at the browser-reachable Keycloak (localhost:8081).
 */
@ApplicationPath("/api")
@OpenAPIDefinition(info = @Info(title = "Module A (OIDC)", version = "1.0.0"))
@SecurityScheme(
        securitySchemeName = "oidc",
        type = SecuritySchemeType.OAUTH2,
        flows = @OAuthFlows(
                authorizationCode = @OAuthFlow(
                        authorizationUrl = "http://localhost:8081/realms/poc/protocol/openid-connect/auth",
                        tokenUrl = "http://localhost:8081/realms/poc/protocol/openid-connect/token",
                        scopes = @OAuthScope(name = "openid", description = "OpenID Connect authentication")
                )
        )
)
public class ModuleAApplication extends Application {
}
