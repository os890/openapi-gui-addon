/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.os890.mp.openapi.gui.poc.b;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;

/**
 * Module B is protected by HTTP Digest (WildFly Elytron digest mechanism, realm "poc-digest").
 *
 * Swagger UI has no native HTTP Digest support — its Authorize dialog rejects scheme "digest"
 * ("unsupported scheme"). So the scheme is declared as {@code http} / {@code basic}: that gives a
 * working username/password Authorize dialog. The shared GUI's addon interceptor (enabled via
 * openapi.ui.digestPaths=/module-b) then reads those captured credentials and performs the real
 * HTTP Digest challenge/response on the wire for /module-b/** requests. (If the user skips the
 * dialog, the interceptor falls back to prompting on first "Try it out".)
 */
@ApplicationPath("/api")
@OpenAPIDefinition(info = @Info(title = "Module B (Digest)", version = "1.0.0"))
@SecurityScheme(
        securitySchemeName = "digest",
        type = SecuritySchemeType.HTTP,
        scheme = "basic",
        description = "Enter your HTTP Digest credentials here. Swagger UI captures them via a Basic "
                + "dialog; the shared GUI performs the actual HTTP Digest challenge/response on the wire."
)
public class ModuleBApplication extends Application {
}
