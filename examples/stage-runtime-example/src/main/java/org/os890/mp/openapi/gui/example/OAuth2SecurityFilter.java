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

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HEAD;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

/**
 * Injects the OAuth2 security scheme at runtime (authorization/token URLs
 * from MP Config) AND auto-derives per-operation SecurityRequirement from
 * plain Jakarta Security {@code @RolesAllowed} annotations on JAX-RS
 * resources — so REST classes don't need any OpenAPI-specific annotation.
 */
public class OAuth2SecurityFilter implements OASFilter {

    private static final Logger log = Logger.getLogger(OAuth2SecurityFilter.class.getName());
    private static final String SCHEME_NAME = "keycloak";

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        injectSecurityScheme(openAPI);
        attachRequirementsForRolesAllowed(openAPI);
    }

    private void injectSecurityScheme(OpenAPI openAPI) {
        Config cfg = ConfigProvider.getConfig(getClass().getClassLoader());

        SecurityScheme scheme = OASFactory.createSecurityScheme()
                .type(SecurityScheme.Type.OAUTH2)
                .description("Keycloak OIDC — authorization code flow")
                .flows(OASFactory.createOAuthFlows()
                        .authorizationCode(OASFactory.createOAuthFlow()
                                .authorizationUrl(cfg.getValue("app.oauth2.authorizationUrl", String.class))
                                .tokenUrl(cfg.getValue("app.oauth2.tokenUrl", String.class))
                                .addScope("openid", "OIDC ID token")));

        if (openAPI.getComponents() == null) {
            openAPI.setComponents(OASFactory.createComponents());
        }
        openAPI.getComponents().addSecurityScheme(SCHEME_NAME, scheme);
    }

    /**
     * Scans CDI beans for JAX-RS resources, builds a map of (path, HTTP verb)
     * entries that carry {@code @RolesAllowed}, then attaches a
     * SecurityRequirement to the matching OpenAPI operations.
     */
    private void attachRequirementsForRolesAllowed(OpenAPI openAPI) {
        Map<String, Set<PathItem.HttpMethod>> protectedOps;
        try {
            protectedOps = scanProtectedOperations();
        } catch (Exception e) {
            log.warning("Could not auto-derive SecurityRequirement from @RolesAllowed: " + e);
            return;
        }
        if (protectedOps.isEmpty() || openAPI.getPaths() == null) return;

        Map<String, PathItem> pathItems = openAPI.getPaths().getPathItems();
        if (pathItems == null) return;

        pathItems.forEach((openApiPath, item) -> {
            String normalized = normalizePath(openApiPath);
            // OpenAPI paths can include the WAR context root (e.g. /hello-api/hello)
            // while JAX-RS @Path values do not. Match by suffix so we tolerate that.
            Set<PathItem.HttpMethod> verbs = protectedOps.entrySet().stream()
                    .filter(e -> normalized.equals(e.getKey()) || normalized.endsWith(e.getKey()))
                    .findFirst().map(Map.Entry::getValue).orElse(null);
            if (verbs == null || item.getOperations() == null) return;
            item.getOperations().forEach((method, op) -> {
                if (verbs.contains(method)) {
                    op.addSecurityRequirement(
                            OASFactory.createSecurityRequirement().addScheme(SCHEME_NAME));
                }
            });
        });
    }

    private Map<String, Set<PathItem.HttpMethod>> scanProtectedOperations() throws IOException {
        Map<String, Set<PathItem.HttpMethod>> result = new HashMap<>();
        String scanPackage = OAuth2SecurityFilter.class.getPackageName();

        for (Class<?> cls : findClassesInPackage(scanPackage, getClass().getClassLoader())) {
            Path classPath = cls.getAnnotation(Path.class);
            if (classPath == null) continue;

            String basePath = normalizePath(classPath.value());

            for (Method m : cls.getDeclaredMethods()) {
                PathItem.HttpMethod verb = httpVerbOf(m);
                if (verb == null) continue;

                if (resolveAccess(cls, m) != Access.ROLES_ALLOWED) continue;

                Path mp = m.getAnnotation(Path.class);
                String fullPath = normalizePath(basePath + (mp != null ? "/" + mp.value() : ""));
                result.computeIfAbsent(fullPath, k -> new HashSet<>()).add(verb);
            }
        }
        return result;
    }

    /**
     * Resolves the effective Jakarta Security access rule for a JAX-RS method.
     * Method-level annotations take precedence over class-level, as per the
     * Jakarta Security spec. If none of the three annotations are present
     * anywhere the method is considered unconstrained.
     */
    private Access resolveAccess(Class<?> cls, Method m) {
        if (m.isAnnotationPresent(DenyAll.class))      return Access.DENY_ALL;
        if (m.isAnnotationPresent(PermitAll.class))    return Access.PERMIT_ALL;
        if (m.isAnnotationPresent(RolesAllowed.class)) return Access.ROLES_ALLOWED;
        if (cls.isAnnotationPresent(DenyAll.class))      return Access.DENY_ALL;
        if (cls.isAnnotationPresent(PermitAll.class))    return Access.PERMIT_ALL;
        if (cls.isAnnotationPresent(RolesAllowed.class)) return Access.ROLES_ALLOWED;
        return Access.NONE;
    }

    private enum Access { ROLES_ALLOWED, PERMIT_ALL, DENY_ALL, NONE }

    /**
     * Classpath scan for {@code .class} files in a package, handling the three
     * URL schemes WildFly / Tomcat / plain Java produce: {@code file:},
     * {@code jar:} and WildFly VFS ({@code vfs:}). VFS support is via reflection
     * so we don't pull a WildFly compile-time dependency.
     */
    private Set<Class<?>> findClassesInPackage(String pkg, ClassLoader cl) throws IOException {
        Set<Class<?>> found = new LinkedHashSet<>();
        String path = pkg.replace('.', '/');
        Enumeration<URL> urls = cl.getResources(path);
        while (urls.hasMoreElements()) {
            URL url = urls.nextElement();
            String protocol = url.getProtocol();
            try {
                if ("file".equals(protocol)) {
                    scanFileSystem(pkg, Paths.get(url.toURI()).toFile(), cl, found);
                } else if ("jar".equals(protocol)) {
                    scanJar(url, path, cl, found);
                } else if ("vfs".equals(protocol)) {
                    scanVfs(url, pkg, cl, found);
                }
            } catch (Exception e) {
                log.warning("Skipping unreadable package URL " + url + ": " + e);
            }
        }
        return found;
    }

    private void scanFileSystem(String pkg, java.io.File dir, ClassLoader cl, Set<Class<?>> out) {
        if (!dir.isDirectory()) return;
        for (java.io.File f : dir.listFiles()) {
            if (f.getName().endsWith(".class")) {
                tryLoad(pkg + "." + f.getName().substring(0, f.getName().length() - 6), cl, out);
            }
        }
    }

    private void scanJar(URL url, String path, ClassLoader cl, Set<Class<?>> out) throws IOException {
        String jarPath = url.getPath().substring(5, url.getPath().indexOf('!'));
        try (JarFile jf = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jf.entries();
            while (entries.hasMoreElements()) {
                JarEntry e = entries.nextElement();
                String n = e.getName();
                if (n.startsWith(path + "/") && n.endsWith(".class") && n.indexOf('/', path.length() + 1) == -1) {
                    tryLoad(n.substring(0, n.length() - 6).replace('/', '.'), cl, out);
                }
            }
        }
    }

    /** WildFly VFS: content is exposed as a VirtualFile via url.getContent(). */
    private void scanVfs(URL url, String pkg, ClassLoader cl, Set<Class<?>> out) throws Exception {
        Object virtualFile = url.getContent();  // org.jboss.vfs.VirtualFile
        Object children = virtualFile.getClass().getMethod("getChildren").invoke(virtualFile);
        for (Object child : (Iterable<?>) children) {
            String name = (String) child.getClass().getMethod("getName").invoke(child);
            if (name.endsWith(".class")) {
                tryLoad(pkg + "." + name.substring(0, name.length() - 6), cl, out);
            }
        }
    }

    private void tryLoad(String fqcn, ClassLoader cl, Set<Class<?>> out) {
        try { out.add(Class.forName(fqcn, false, cl)); }
        catch (Throwable ignored) { /* anonymous, module-info, etc. */ }
    }

    private static PathItem.HttpMethod httpVerbOf(Method m) {
        if (m.isAnnotationPresent(GET.class))     return PathItem.HttpMethod.GET;
        if (m.isAnnotationPresent(POST.class))    return PathItem.HttpMethod.POST;
        if (m.isAnnotationPresent(PUT.class))     return PathItem.HttpMethod.PUT;
        if (m.isAnnotationPresent(DELETE.class))  return PathItem.HttpMethod.DELETE;
        if (m.isAnnotationPresent(PATCH.class))   return PathItem.HttpMethod.PATCH;
        if (m.isAnnotationPresent(HEAD.class))    return PathItem.HttpMethod.HEAD;
        if (m.isAnnotationPresent(OPTIONS.class)) return PathItem.HttpMethod.OPTIONS;
        return null;
    }

    private static String normalizePath(String p) {
        if (p == null || p.isEmpty()) return "/";
        String s = ("/" + p).replaceAll("/+", "/");
        if (s.length() > 1 && s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }

}
