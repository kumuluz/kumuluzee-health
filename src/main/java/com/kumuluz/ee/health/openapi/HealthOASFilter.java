/*
 *  Copyright (c) 2014-2017 Kumuluz and/or its affiliates
 *  and other contributors as indicated by the @author tags and
 *  the contributor list.
 *
 *  Licensed under the MIT License (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  https://opensource.org/licenses/MIT
 *
 *  The software is provided "AS IS", WITHOUT WARRANTY OF ANY KIND, express or
 *  implied, including but not limited to the warranties of merchantability,
 *  fitness for a particular purpose and noninfringement. in no event shall the
 *  authors or copyright holders be liable for any claim, damages or other
 *  liability, whether in an action of contract, tort or otherwise, arising from,
 *  out of or in connection with the software or the use or other dealings in the
 *  software. See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.kumuluz.ee.health.openapi;

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.health.checks.DataSourceHealthCheck;
import com.kumuluz.ee.health.utils.HealthServletMappingUtil;
import io.smallrye.openapi.api.models.ComponentsImpl;
import io.smallrye.openapi.api.models.OperationImpl;
import io.smallrye.openapi.api.models.PathItemImpl;
import io.smallrye.openapi.api.models.PathsImpl;
import io.smallrye.openapi.api.models.media.ContentImpl;
import io.smallrye.openapi.api.models.media.MediaTypeImpl;
import io.smallrye.openapi.api.models.media.SchemaImpl;
import io.smallrye.openapi.api.models.responses.APIResponseImpl;
import io.smallrye.openapi.api.models.responses.APIResponsesImpl;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;

import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * If enabled through configuration key, adds /health, /health/live and /health/ready endpoints to OpenAPI.
 *
 * @author Urban Malc
 * @since 2.3.0
 */
public class HealthOASFilter implements OASFilter {

    private String servletMapping;

    public HealthOASFilter() {
        this.servletMapping = HealthServletMappingUtil.getMapping();

        // remove trailing "/*"
        this.servletMapping = this.servletMapping.substring(0, this.servletMapping.length() - 2);
    }

    private boolean isEnabled() {
        return ConfigurationUtil.getInstance().getBoolean("kumuluzee.health.openapi-mp.enabled").orElse(false);
    }

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {

        if (!isEnabled()) {
            return;
        }

        if (openAPI.getComponents() == null) {
            openAPI.components(new ComponentsImpl());
        }
        if (openAPI.getComponents().getSchemas() == null) {
            openAPI.getComponents().schemas(new HashMap<>());
        }
        if (openAPI.getPaths() == null) {
            openAPI.paths(new PathsImpl());
        }

        Schema healthStatusSchema = new SchemaImpl();
        healthStatusSchema.setType(Schema.SchemaType.STRING);
        healthStatusSchema.setEnumeration(Arrays.asList(new String[]{"UP", "DOWN"}));
        healthStatusSchema.setExample("UP/DOWN"); // looks better in examples
        openAPI.getComponents().getSchemas().put("HealthStatus", healthStatusSchema);

        Schema healthCheckSchema = new SchemaImpl();
        healthCheckSchema.setType(Schema.SchemaType.OBJECT);
        Map<String, Schema> healthCheckSchemaProperties = new HashMap<>();
        healthCheckSchemaProperties.put("name", new SchemaImpl()
                .type(Schema.SchemaType.STRING)
                .example(DataSourceHealthCheck.class.getSimpleName()));
        healthCheckSchemaProperties.put("status", new SchemaImpl()
                .ref("#/components/schemas/HealthStatus"));
        healthCheckSchemaProperties.put("data", new SchemaImpl()
                .type(Schema.SchemaType.OBJECT)
                .nullable(true));
        healthCheckSchema.setProperties(healthCheckSchemaProperties);


        Schema healthResponseSchema = new SchemaImpl();
        healthResponseSchema.setType(Schema.SchemaType.OBJECT);
        Map<String, Schema> healthResponseSchemaProperties = new HashMap<>();
        healthResponseSchemaProperties.put("status", new SchemaImpl()
                .ref("#/components/schemas/HealthStatus"));
        healthResponseSchemaProperties.put("checks", new SchemaImpl()
                .type(Schema.SchemaType.ARRAY)
                .items(healthCheckSchema));
        healthResponseSchema.setProperties(healthResponseSchemaProperties);
        openAPI.getComponents().getSchemas().put("HealthResponse", healthResponseSchema);

        PathItem healthPath = createHealthPath("Get information about the health of this service",
                "Contains both readiness and liveness checks.");
        openAPI.getPaths().addPathItem(this.servletMapping, healthPath);
        PathItem healthReadyPath = createHealthPath("Get information about the readiness of this service",
                null);
        openAPI.getPaths().addPathItem(this.servletMapping + "/ready", healthReadyPath);
        PathItem healthLivePath = createHealthPath("Get information about the liveness of this service",
                null);
        openAPI.getPaths().addPathItem(this.servletMapping + "/live", healthLivePath);
    }

    private PathItem createHealthPath(String summary, String description) {
        Content healthResponseContent = new ContentImpl();
        healthResponseContent.addMediaType(MediaType.APPLICATION_JSON, new MediaTypeImpl().schema(new SchemaImpl()
                .ref("#/components/schemas/HealthResponse")));

        PathItem healthPath = new PathItemImpl();
        Operation healthGet = new OperationImpl();
        healthGet.addTag("health");
        healthGet.summary(summary);
        healthGet.description(description);
        APIResponses healthResponses = new APIResponsesImpl();
        APIResponse health200 = new APIResponseImpl();
        health200.description("The service is healthy");
        health200.content(healthResponseContent);
        healthResponses.addAPIResponse("200", health200);
        APIResponse health503 = new APIResponseImpl();
        health503.description("The service is not healthy");
        health503.content(healthResponseContent);
        healthResponses.addAPIResponse("503", health503);
        APIResponse health500 = new APIResponseImpl();
        health500.description("Server error while evaluating health checks");
        healthResponses.addAPIResponse("500", health500);
        healthGet.responses(healthResponses);
        healthPath.setGET(healthGet);

        return healthPath;
    }
}
