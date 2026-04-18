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
import org.os890.mp.openapi.gui.OpenApiUiService;
import org.os890.mp.openapi.gui.StaticResourcesService;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

@ApplicationPath("/")
@OpenAPIDefinition(info = @Info(title = "Hello API", version = "1.0",
        description = "Programmatic project-stage: OpenAPI UI enabled unless project.stage=production"))
public class DemoApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        classes.add(HelloResource.class);

        String stage = org.eclipse.microprofile.config.ConfigProvider.getConfig()
                .getOptionalValue("project.stage", String.class)
                .orElse("production");

        if (!"production".equals(stage)) {
            classes.add(OpenApiUiService.class);
            classes.add(StaticResourcesService.class);
        }
        return classes;
    }
}
