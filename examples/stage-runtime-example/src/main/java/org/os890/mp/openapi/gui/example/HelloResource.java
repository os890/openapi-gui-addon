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

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;

/**
 * All three endpoints live behind the WAR-wide security-constraint in
 * web.xml, so any unauthenticated request is bounced through Keycloak
 * first. Once logged in, the browser's session cookie carries auth on
 * every subsequent request.
 *
 * <p>Role-level access is enforced by Jakarta Security at runtime via
 * {@code @RolesAllowed}. WildFly Elytron OIDC maps the token's
 * {@code realm_access.roles} claim into the Jakarta security context
 * automatically, so {@code @RolesAllowed("user")} simply works — no
 * custom role-mapping adapter needed. Method-level annotations override
 * class-level ones (standard Jakarta Security precedence).
 */
@Path("/hello")
@RolesAllowed("user")
public class HelloResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Say hello",
            description = "Inherits class-level @RolesAllowed(\"user\"). The demo user has this role.")
    @APIResponse(responseCode = "200", description = "Hello greeting",
            content = @Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(implementation = String.class)))
    public String hello(@Context SecurityContext securityContext) {
        String caller = securityContext.getUserPrincipal() != null
                ? securityContext.getUserPrincipal().getName()
                : "anonymous";
        return "Hello " + caller + " from the Hello API!";
    }

    /**
     * Method-level {@code @PermitAll} overrides the class-level
     * {@code @RolesAllowed}. Any authenticated user (the web.xml
     * constraint still requires login) can call this regardless of
     * realm roles.
     */
    @GET
    @Path("/ping")
    @PermitAll
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Liveness ping",
            description = "@PermitAll — no Jakarta role required.")
    @APIResponse(responseCode = "200", description = "pong",
            content = @Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(implementation = String.class)))
    public String ping() {
        return "pong";
    }

    /**
     * Requires the {@code admin} role — which the demo user does not
     * have. Authenticated but unauthorized calls return HTTP 403.
     */
    @GET
    @Path("/admin")
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Admin-only greeting",
            description = "@RolesAllowed(\"admin\") — demo user lacks this role so the call always 403s.")
    @APIResponse(responseCode = "200", description = "Admin greeting",
            content = @Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(implementation = String.class)))
    @APIResponse(responseCode = "403", description = "Caller lacks the 'admin' role")
    public String admin(@Context SecurityContext securityContext) {
        return "Hello admin " + securityContext.getUserPrincipal().getName() + "!";
    }
}
