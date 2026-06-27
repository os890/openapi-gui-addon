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

/**
 * Module B is protected by HTTP Digest (WildFly Elytron digest mechanism, realm "poc-digest").
 *
 * The security scheme is injected at runtime by {@link DigestSecurityFilter} (registered through
 * {@code mp.openapi.filter}) — published as http/basic + {@code x-auth-mode: digest}. The shared GUI
 * detects that marker in this document and performs the digest challenge/response. Nothing about the
 * scheme is declared by annotation here, and the GUI needs no per-module configuration.
 */
// ApplicationPath "/" so the bundled Swagger UI mounts at /module-b/openapi-ui (outside the
// secured REST path). web.xml protects only /time, leaving the UI/webjars/openapi doc open.
@ApplicationPath("/")
@OpenAPIDefinition(info = @Info(title = "Module B (Digest)", version = "1.0.0"))
public class ModuleBApplication extends Application {
}
