/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.os890.mp.openapi.gui.poc.a;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;

/**
 * Module A is an OIDC-protected resource server (bearer-only, validated by WildFly's
 * elytron-oidc-client — see WEB-INF/oidc.json).
 *
 * The security scheme is not declared here via annotation — it is injected at runtime by
 * {@link OAuth2SecurityFilter} (registered through {@code mp.openapi.filter}), so the token URL
 * comes from config and the requirement applies document-wide. The shared GUI reacts purely to
 * what the resulting OpenAPI document declares.
 */
@ApplicationPath("/api")
@OpenAPIDefinition(info = @Info(title = "Module A (OIDC)", version = "1.0.0"))
public class ModuleAApplication extends Application {
}
