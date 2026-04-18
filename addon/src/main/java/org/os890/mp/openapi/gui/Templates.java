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
package org.os890.mp.openapi.gui;

// Based on https://github.com/microprofile-extensions/openapi-ext
// Original: org.microprofileext.openapi.swaggerui.Templates
// Changes: added openapi.ui.urls multi-API dropdown with availability check,
//          auto-visible explore form when urls configured, removed Lombok

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.microprofileext.openapi.swaggerui.RequestInfo;
import org.microprofileext.openapi.swaggerui.WhiteLabel;

@ApplicationScoped
public class Templates {

    private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(Templates.class.getName());

    private byte[] originalLogo = null;
    private String style = null;

    public byte[] getOriginalLogo() { return originalLogo; }
    public String getStyle() { return style; }

    @Inject
    private WhiteLabel whiteLabel;

    private String swaggerUIHtml = null;

    @PostConstruct
    public void afterCreate() {
        this.originalLogo = getLogo();
        log.finest("OpenApi UI: Created logo");
        this.style = getCss();
    }

    public String getSwaggerUIHtml(RequestInfo requestInfo){
        if(this.swaggerUIHtml==null){
            this.swaggerUIHtml = parseHtmlTemplate(requestInfo);
        }
        return this.swaggerUIHtml;
    }

    private String parseHtmlTemplate(RequestInfo requestInfo){
        String html = getHTMLTemplate();

        html = html.replaceAll(VAR_CONTEXT_ROOT, getContextRoot(requestInfo));
        html = html.replaceAll(VAR_YAML_URL, yamlUrl);
        html = html.replaceAll(VAR_URLS_DATA, getUrlsData());
        html = html.replaceAll(VAR_CURRENT_YEAR, getCopyrightYear());
        html = html.replaceAll(VAR_OAUTH2_REDIRECT_URI, oauth2RedirectUri);

        try {
            Iterable<String> propertyNames = config.getPropertyNames();
            for(String key: propertyNames){
                if(key.startsWith(KEY_IDENTIFIER) && !isKnownProperty(key)){
                    String htmlKey = PERSENTAGE + key + PERSENTAGE;
                    html = html.replaceAll(htmlKey, config.getValue(key,String.class));
                }
            }
        }catch(UnsupportedOperationException uoe){
            log.log(Level.WARNING, "Can not replace dynamic properties in the Open API Swagger template. {0}", uoe.getMessage());
        }
        html = html.replaceAll(VAR_COPYRIGHT_BY, getCopyrightBy());
        html = html.replaceAll(VAR_TITLE, title);
        html = html.replaceAll(VAR_SWAGGER_THEME, swaggerUiTheme);

        if ("hidden".equalsIgnoreCase(modelsVisibility)) {
            String domId = "dom_id: '#swagger-ui',";
            html = html.replaceAll(domId, domId + "\n                    defaultModelsExpandDepth: -1,");
        }
        return html;
    }

    private String getUrlsData() {
        if (urls.isPresent() && !urls.get().isEmpty()) {
            StringBuilder sb = new StringBuilder("[");
            String[] entries = urls.get().split(",");
            for (int i = 0; i < entries.length; i++) {
                String entry = entries[i].trim();
                int eqIdx = entry.indexOf('=');
                String name;
                String entryUrl;
                if (eqIdx > 0) {
                    name = entry.substring(0, eqIdx).trim();
                    entryUrl = entry.substring(eqIdx + 1).trim();
                } else {
                    name = entry;
                    entryUrl = entry;
                }
                if (i > 0) sb.append(",");
                sb.append("{\"url\":\"").append(entryUrl).append("\",\"name\":\"").append(name).append("\"}");
            }
            sb.append("]");
            return sb.toString();
        }
        return "null";
    }

    private String getOriginalContextPath(RequestInfo requestInfo){
        String xRequestUriHeader = requestInfo.getHttpHeaders().getHeaderString(X_REQUEST_URI);
        if(xRequestUriHeader!=null && !xRequestUriHeader.isEmpty()){
            return getContextPathPart(requestInfo,xRequestUriHeader);
        }
        return requestInfo.getContextPath();
    }

    private String getContextPathPart(RequestInfo requestInfo,String xRequestUriHeader){
        String restBase = requestInfo.getRestPath();
        String restUrl = restBase + requestInfo.getUriInfo().getPath();
        int restUrlStart = xRequestUriHeader.indexOf(restUrl);
        if(restUrlStart>0){
            return xRequestUriHeader.substring(0, restUrlStart);
        }else{
            return xRequestUriHeader;
        }
    }

    private String toString(InputStream input) throws IOException {
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(input))) {
            return buffer.lines().collect(Collectors.joining(NL));
        }
    }

    private byte[] getLogo(){
        if(whiteLabel.hasLogo())return whiteLabel.getLogo();
        InputStream logoStream = this.getClass().getClassLoader().getResourceAsStream(FILE_LOGO);
        try{
            if(logoStream!=null){
                return logoStream.readAllBytes();
            }
        } catch (IOException ex) {
            log.warning(ex.getMessage());
        }
        return null;
    }

    private String getCss() {
        String rawcss;
        if(whiteLabel.hasCss()){
            rawcss = whiteLabel.getCss();
        }else {
            try(InputStream css = this.getClass().getClassLoader().getResourceAsStream(FILE_STYLE)){
                rawcss = toString(css);
            } catch (IOException ex) {
                return EMPTY;
            }
        }
        rawcss = rawcss.replaceAll(VAR_SWAGGER_HEADER_VISIBILITY, swaggerHeaderVisibility);
        String effectiveExploreFormVisibility = (urls.isPresent() && !urls.get().isEmpty()) ? "visible" : exploreFormVisibility;
        rawcss = rawcss.replaceAll(VAR_EXPLORE_FORM_VISIBILITY, effectiveExploreFormVisibility);
        rawcss = rawcss.replaceAll(VAR_SERVER_VISIBILITY, serverVisibility);
        rawcss = rawcss.replaceAll(VAR_SERVER_VISIBILITY_BLOCK_SIZE, getServerVisibilityBlockSize());
        rawcss = rawcss.replaceAll(VAR_CREATED_WITH_VISIBILITY, createdWithVisibility);
        return rawcss;
    }

    private String getServerVisibilityBlockSize() {
        if(serverVisibility.equals("hidden"))return "0px";
        return "auto";
    }

    private String getHTMLTemplate() {
        if(whiteLabel.hasHtml())return whiteLabel.getHtml();
        try(InputStream template = this.getClass().getClassLoader().getResourceAsStream(FILE_TEMPLATE)){
            return toString(template);
        } catch (IOException ex) {
            return EMPTY;
        }
    }

    private String getCopyrightYear(){
        if(copyrightYear.isPresent()) return copyrightYear.get();
        return String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
    }

    private String getCopyrightBy(){
        if(copyrightBy.isPresent()) return copyrightBy.get();
        return EMPTY;
    }

    private String getContextRoot(RequestInfo requestInfo){
        if(contextRoot.isPresent()) return contextRoot.get();
        return getOriginalContextPath(requestInfo);
    }

    private boolean isKnownProperty(String key){
        return KNOWN_PROPERTIES.contains(key);
    }

    private static final String X_REQUEST_URI = "x-request-uri";
    private static final List<String> KNOWN_PROPERTIES = Arrays.asList("openapi.ui.serverVisibility","openapi.ui.exploreFormVisibility","openapi.ui.swaggerHeaderVisibility","openapi.ui.copyrightBy","openapi.ui.copyrightYear","openapi.ui.title","openapi.ui.contextRoot","openapi.ui.yamlUrl","openapi.ui.swaggerUiTheme","openapi.ui.urls");

    @Inject @ConfigProperty(name = "openapi.ui.copyrightBy") private Optional<String> copyrightBy;
    @Inject @ConfigProperty(name = "openapi.ui.copyrightYear") private Optional<String> copyrightYear;
    @Inject @ConfigProperty(name = "openapi.ui.title", defaultValue = "MicroProfile - Open API") private String title;
    @Inject @ConfigProperty(name = "openapi.ui.contextRoot") private Optional<String> contextRoot;
    @Inject @ConfigProperty(name = "openapi.ui.yamlUrl", defaultValue = "/openapi") private String yamlUrl;
    @Inject @ConfigProperty(name = "openapi.ui.swaggerUiTheme", defaultValue = "flattop") private String swaggerUiTheme;
    @Inject @ConfigProperty(name = "openapi.ui.swaggerHeaderVisibility", defaultValue = "visible") private String swaggerHeaderVisibility;
    @Inject @ConfigProperty(name = "openapi.ui.exploreFormVisibility", defaultValue = "hidden") private String exploreFormVisibility;
    @Inject @ConfigProperty(name = "openapi.ui.serverVisibility", defaultValue = "hidden") private String serverVisibility;
    @Inject @ConfigProperty(name = "openapi.ui.createdWithVisibility", defaultValue = "visible") private String createdWithVisibility;
    @Inject @ConfigProperty(name = "openapi.ui.modelsVisibility", defaultValue = "visible") private String modelsVisibility;
    @Inject @ConfigProperty(name = "openapi.ui.oauth2RedirectUri", defaultValue = "/oauth2-redirect.html") private String oauth2RedirectUri;
    @Inject @ConfigProperty(name = "openapi.ui.urls") private Optional<String> urls;
    @Inject private Config config;

    private static final String VAR_COPYRIGHT_BY = "%copyrighBy%";
    private static final String VAR_TITLE = "%title%";
    private static final String VAR_CURRENT_YEAR = "%currentYear%";
    private static final String VAR_CONTEXT_ROOT = "%contextRoot%";
    private static final String VAR_YAML_URL = "%yamlUrl%";
    private static final String VAR_URLS_DATA = "%urlsData%";
    private static final String VAR_SWAGGER_THEME = "%swaggerUiTheme%";
    private static final String VAR_SWAGGER_HEADER_VISIBILITY = "%swaggerHeaderVisibility%";
    private static final String VAR_EXPLORE_FORM_VISIBILITY = "%exploreFormVisibility%";
    private static final String VAR_SERVER_VISIBILITY = "%serverVisibility%";
    private static final String VAR_SERVER_VISIBILITY_BLOCK_SIZE = "%serverVisibilityBlockSize%";
    private static final String VAR_CREATED_WITH_VISIBILITY = "%createdWithVisibility%";
    private static final String VAR_OAUTH2_REDIRECT_URI = "%oauth2RedirectUri%";
    private static final String PERSENTAGE = "%";
    private static final String NL = "\n";
    private static final String EMPTY = "";
    private static final String FILE_TEMPLATE = "META-INF/resources/templates/template.html";
    private static final String FILE_LOGO = "META-INF/resources/templates/logo.png";
    private static final String FILE_STYLE = "META-INF/resources/templates/style.css";
    private static final String KEY_IDENTIFIER = "openapi.ui.";
}
