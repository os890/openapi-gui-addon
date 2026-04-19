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

@Path("/hello")
@RolesAllowed("user")   // default for all methods unless overridden
public class HelloResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Say hello", description = "Returns a hello greeting for the authenticated caller")
    @APIResponse(responseCode = "200", description = "Hello greeting",
            content = @Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(implementation = String.class)))
    public String hello(@Context SecurityContext securityContext) {
        String caller = securityContext.getUserPrincipal() != null
                ? securityContext.getUserPrincipal().getName()
                : "anonymous";
        return "Hello " + caller + " from the Hello API!";
    }

    /**
     * Method-level {@code @PermitAll} overrides the class-level {@code @RolesAllowed}.
     * The OAS filter must therefore NOT add a security requirement to this
     * operation — it should appear without a padlock in Swagger UI, even
     * though the URL is still under the web.xml security-constraint.
     */
    @GET
    @Path("/ping")
    @PermitAll
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Liveness ping", description = "Public probe — no Jakarta role required.")
    @APIResponse(responseCode = "200", description = "pong",
            content = @Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(implementation = String.class)))
    public String ping() {
        return "pong";
    }

    /**
     * Requires the {@code admin} role — which the demo user does not have.
     * Any call made after logging in as {@code demo} should return HTTP 403
     * (authenticated but not authorized). The OpenAPI doc only shows a
     * padlock (auth required) — role specifics are enforced at runtime.
     */
    @GET
    @Path("/admin")
    @RolesAllowed("admin")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Admin-only greeting",
            description = "Requires the 'admin' Jakarta role — demo user lacks it, so this always 403s.")
    @APIResponse(responseCode = "200", description = "Admin greeting",
            content = @Content(mediaType = MediaType.TEXT_PLAIN, schema = @Schema(implementation = String.class)))
    @APIResponse(responseCode = "403", description = "Caller lacks the 'admin' role")
    public String admin(@Context SecurityContext securityContext) {
        return "Hello admin " + securityContext.getUserPrincipal().getName() + "!";
    }
}
