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

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/")
@OpenAPIDefinition(info = @Info(title = "Hello API", version = "1.0",
        description = "Bearer-token variant with OAuth2 password flow. WildFly Elytron "
                + "OIDC runs in bearer-only mode, so REST endpoints require an "
                + "Authorization: Bearer <jwt> header. The security scheme is injected "
                + "at runtime by OAuth2SecurityFilter so the Keycloak token URL comes "
                + "from microprofile-config.properties rather than being hardcoded in "
                + "annotations."))
public class DemoApplication extends Application {
}
