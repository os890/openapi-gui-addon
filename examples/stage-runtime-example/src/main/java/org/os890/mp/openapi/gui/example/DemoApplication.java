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

import org.eclipse.microprofile.openapi.annotations.Components;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.security.OAuthFlow;
import org.eclipse.microprofile.openapi.annotations.security.OAuthFlows;
import org.eclipse.microprofile.openapi.annotations.security.OAuthScope;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/")
@OpenAPIDefinition(
        info = @Info(title = "Hello API", version = "1.0",
                description = "Runtime project-stage demo, protected by Keycloak OAuth2."),
        components = @Components(
                securitySchemes = @SecurityScheme(
                        securitySchemeName = "keycloak",
                        type = SecuritySchemeType.OAUTH2,
                        description = "Keycloak OIDC — authorization code flow with PKCE",
                        flows = @OAuthFlows(
                                authorizationCode = @OAuthFlow(
                                        authorizationUrl = "http://localhost:8081/realms/demo/protocol/openid-connect/auth",
                                        tokenUrl = "http://localhost:8081/realms/demo/protocol/openid-connect/token",
                                        scopes = @OAuthScope(name = "openid", description = "OIDC ID token")
                                )
                        )
                )
        )
)
public class DemoApplication extends Application {
}
