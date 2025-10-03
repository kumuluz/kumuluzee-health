/*
 *  Copyright (c) 2014-2020 Kumuluz and/or its affiliates
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
package com.kumuluz.ee.health.tests;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.IOException;
import java.net.URI;

/**
 * Test Datasource readiness liveness both
 *
 * @author Gregor Porocnik
 * @since 2.2.0
 */
public class DatasourceMultipleHealthCheckTest extends Arquillian {

    @ArquillianResource
    private URI uri;

    @Deployment
    public static JavaArchive createDeployment() {

        return ShrinkWrap.create(JavaArchive.class)
                .addAsResource("multiple-datasource-hc.yml", "config.yml");
    }

    @Test
    @RunAsClient
    public void healthApiShouldReturnUp() throws IOException {
        JsonObject healthApiResponse = getHealthApiResponse("/health");
        Assert.assertNotNull(healthApiResponse);
        JsonArray checks = healthApiResponse.getJsonArray("checks");
        Assert.assertEquals(checks.size(), 1);
        Assert.assertEquals("DataSourceHealthCheck", checks.get(0).asJsonObject().getString("name"));
        Assert.assertEquals("UP", checks.get(0).asJsonObject().getString("status"));
        Assert.assertEquals("UP", checks.get(0).asJsonObject().getJsonObject("data").getString("jdbc:h2:mem:test1"));
        Assert.assertEquals("UP", checks.get(0).asJsonObject().getJsonObject("data").getString("jdbc:h2:mem:test2"));
    }

    private JsonObject getHealthApiResponse(String healthPath) throws IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = client.execute(new HttpGet(uri + healthPath));
        Assert.assertEquals(response.getStatusLine().getStatusCode(), 200);

        JsonReader jsonReader = Json.createReader(response.getEntity().getContent());
        return jsonReader.readObject();
    }
}
