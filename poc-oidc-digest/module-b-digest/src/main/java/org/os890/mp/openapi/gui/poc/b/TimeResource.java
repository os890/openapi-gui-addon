/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.os890.mp.openapi.gui.poc.b;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;

import java.time.OffsetDateTime;
import java.util.Map;

@Path("/time")
@SecurityRequirement(name = "digest")
public class TimeResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> time(@Context SecurityContext securityContext) {
        String name = securityContext.getUserPrincipal() != null
                ? securityContext.getUserPrincipal().getName()
                : "anonymous";
        return Map.of(
                "greeting", "Hello " + name + ", welcome to module-b!",
                "module", "module-b",
                "secured-by", "http digest",
                "authenticated-user", name,
                "server-time", OffsetDateTime.now().toString());
    }
}
