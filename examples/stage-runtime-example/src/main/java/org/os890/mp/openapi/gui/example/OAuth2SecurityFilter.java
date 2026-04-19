/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.os890.mp.openapi.gui.example;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

/**
 * Injects the OAuth2 "password"-flow security scheme at runtime, reading the
 * Keycloak token URL from MicroProfile Config. Keeps the URL out of Java
 * annotations so it can be overridden per stage via env vars or
 * -D system properties.
 */
public class OAuth2SecurityFilter implements OASFilter {

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        // Use this class's ClassLoader explicitly — during deployment the
        // TCCL can point at the deployer and miss the WAR's config source.
        Config cfg = ConfigProvider.getConfig(getClass().getClassLoader());

        SecurityScheme scheme = OASFactory.createSecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .description("OAuth2 password grant (direct access) — Swagger UI asks for "
                        + "username / password / client_id and POSTs directly to Keycloak's "
                        + "token endpoint. No popup, no redirect, no COOP interaction.")
                .flows(OASFactory.createOAuthFlows()
                        .password(OASFactory.createOAuthFlow()
                                .tokenUrl(cfg.getValue("app.oauth2.tokenUrl", String.class))
                                .addScope("openid", "OIDC ID token")));

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(OASFactory.createComponents());
        }
        openAPI.getComponents().addSecurityScheme("bearer", scheme);

        // Global default: apply the "bearer" scheme to every operation so no
        // per-resource @SecurityRequirement annotation is needed. Operations
        // that need to be publicly reachable can still opt out by declaring
        // @SecurityRequirements({}) on the method.
        openAPI.addSecurityRequirement(
                OASFactory.createSecurityRequirement().addScheme("bearer"));
    }
}
