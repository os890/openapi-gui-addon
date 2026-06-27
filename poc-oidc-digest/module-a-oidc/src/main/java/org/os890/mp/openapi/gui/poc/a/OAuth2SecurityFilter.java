/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.os890.mp.openapi.gui.poc.a;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

/**
 * Declares module A's security scheme in the OpenAPI document at runtime (registered via
 * {@code mp.openapi.filter}). The scheme is OAuth2 with the {@code password} (direct-access-grant)
 * flow, so the shared Swagger UI renders an inline username/password Authorize dialog and POSTs
 * straight to Keycloak's token endpoint — no popup/redirect (which breaks against Keycloak 24+ COOP).
 *
 * Keeping the scheme in a filter (rather than a {@code @SecurityScheme} annotation) means the token
 * URL comes from MicroProfile Config ({@code app.oauth2.tokenUrl}, overridable per stage) and the
 * document-level {@code SecurityRequirement} applies to every operation — REST resources stay pure
 * Jakarta. The GUI then acts solely on what this document declares.
 */
public class OAuth2SecurityFilter implements OASFilter {

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        Config cfg = ConfigProvider.getConfig(getClass().getClassLoader());

        SecurityScheme scheme = OASFactory.createSecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .description("OAuth2 password grant — Swagger UI collects username/password/client_id "
                        + "and POSTs grant_type=password to Keycloak's token endpoint. No popup, no redirect.")
                .flows(OASFactory.createOAuthFlows()
                        .password(OASFactory.createOAuthFlow()
                                .tokenUrl(cfg.getValue("app.oauth2.tokenUrl", String.class))
                                .addScope("openid", "OpenID Connect authentication")));

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(OASFactory.createComponents());
        }
        openAPI.getComponents().addSecurityScheme("oidc", scheme);
        openAPI.addSecurityRequirement(OASFactory.createSecurityRequirement().addScheme("oidc"));
    }
}
