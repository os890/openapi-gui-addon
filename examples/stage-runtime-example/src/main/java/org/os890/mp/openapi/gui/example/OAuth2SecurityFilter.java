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

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

/**
 * Injects a minimal HTTP Bearer security scheme and a document-level
 * SecurityRequirement so every operation gets a padlock and Swagger UI
 * attaches the "Authorization: Bearer ..." header on Try-it-out.
 *
 * <p>The token itself is obtained by the embedded PKCE OIDC client in
 * {@code openapi.html} (same-tab redirect to Keycloak, real authorization-
 * code flow) and handed to Swagger UI via {@code preauthorizeApiKey}. That
 * way the token carries {@code auth_time} and {@code nonce} — the shape
 * the backend's validator expects — rather than the pared-down password-
 * grant shape.
 */
public class OAuth2SecurityFilter implements OASFilter {

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        SecurityScheme scheme = OASFactory.createSecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Keycloak access token — obtained automatically by the "
                        + "embedded OIDC+PKCE client in the Swagger UI page.");

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
