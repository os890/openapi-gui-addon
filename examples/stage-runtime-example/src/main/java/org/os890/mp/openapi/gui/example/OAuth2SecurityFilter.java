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

public class OAuth2SecurityFilter implements OASFilter {

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        // Use this class's ClassLoader explicitly — when the filter runs during
        // deployment, the TCCL can point at the deployer and miss the WAR's
        // microprofile-config.properties.
        Config cfg = ConfigProvider.getConfig(getClass().getClassLoader());

        SecurityScheme scheme = OASFactory.createSecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .description("Keycloak OIDC — authorization code flow")
                .flows(OASFactory.createOAuthFlows()
                        .authorizationCode(OASFactory.createOAuthFlow()
                                .authorizationUrl(cfg.getValue("app.oauth2.authorizationUrl", String.class))
                                .tokenUrl(cfg.getValue("app.oauth2.tokenUrl", String.class))
                                .addScope("openid", "OIDC ID token")));

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(OASFactory.createComponents());
        }
        openAPI.getComponents().addSecurityScheme("keycloak", scheme);
    }
}
