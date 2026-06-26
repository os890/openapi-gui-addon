/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package org.os890.mp.openapi.gui.poc.gui;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * Unsecured host for the single shared Swagger UI (served at /gui/openapi-ui).
 * No REST resources of its own — the dropdown (openapi.ui.urls) points at the two
 * secured modules' OpenAPI documents. The addon auto-discovers via JAX-RS.
 */
@ApplicationPath("/")
public class GuiApplication extends Application {
}
