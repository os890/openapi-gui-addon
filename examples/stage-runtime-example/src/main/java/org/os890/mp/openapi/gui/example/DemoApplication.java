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

import jakarta.annotation.security.DeclareRoles;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

@ApplicationPath("/")
@DeclareRoles({"user", "admin"})
@OpenAPIDefinition(info = @Info(title = "Hello API", version = "1.0",
        description = "Runtime project-stage demo. Keycloak OIDC is enforced by "
                + "WildFly Elytron server-side; authentication rides on a session "
                + "cookie established during the first OIDC redirect. Swagger UI "
                + "has no Authorize button — \"Try it out\" uses the cookie the "
                + "browser already holds. @RolesAllowed is enforced on JAX-RS "
                + "methods via Resteasy role-based security (enabled in web.xml)."))
public class DemoApplication extends Application {
}
