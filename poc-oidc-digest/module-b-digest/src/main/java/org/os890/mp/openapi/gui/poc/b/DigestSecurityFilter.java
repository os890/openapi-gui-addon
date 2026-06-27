/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.os890.mp.openapi.gui.poc.b;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

/**
 * Declares module B's security scheme in the OpenAPI document at runtime (registered via
 * {@code mp.openapi.filter}).
 *
 * Swagger UI has no native HTTP Digest support, so the scheme is published as {@code http}/{@code basic}
 * (a working username/password Authorize dialog) carrying the extension {@code x-auth-mode: digest}.
 * The shared GUI reads that extension straight from this document and, when this module is selected,
 * converts the captured Basic credentials into a real HTTP Digest challenge/response on the wire.
 * Behaviour is therefore driven entirely by the endpoint definition — no GUI-side configuration.
 */
public class DigestSecurityFilter implements OASFilter {

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        SecurityScheme scheme = OASFactory.createSecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("basic")
                .description("HTTP Digest. Enter your credentials here; Swagger UI captures them via a "
                        + "Basic dialog and the shared GUI performs the HTTP Digest challenge/response.")
                .addExtension("x-auth-mode", "digest");

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(OASFactory.createComponents());
        }
        openAPI.getComponents().addSecurityScheme("digest", scheme);
        openAPI.addSecurityRequirement(OASFactory.createSecurityRequirement().addScheme("digest"));
    }
}
