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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.h2.tools.Server;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.sql.SQLException;
import java.util.Objects;
import java.util.stream.Collectors;

public class DatasourceDownHealthCheckTest extends Arquillian {

    private static Server h2Server;

    @ArquillianResource
    private URI uri;

    @Deployment
    public static JavaArchive createDeployment() throws SQLException {

        h2Server = Server.createTcpServer("-ifNotExists").start();

        String config = new BufferedReader(new InputStreamReader(Objects.requireNonNull(
                DatasourceDownHealthCheckTest.class.getClassLoader().getResourceAsStream("down-datasource-hc.yml"),
                "Could not load config.yml"
        ))).lines().collect(Collectors.joining("\n"))
                .replace("<h2_port>", String.valueOf(h2Server.getPort()));

        return ShrinkWrap.create(JavaArchive.class)
                .addAsResource(new StringAsset(config), "config.yml");
    }

    @Test
    @RunAsClient
    public void healthApiShouldReturnDown() throws IOException {
        h2Server.stop();

        JsonObject healthApiResponse = getHealthApiResponse();
        Assert.assertNotNull(healthApiResponse);
        JsonArray checks = healthApiResponse.getJsonArray("checks");
        Assert.assertEquals(checks.size(), 1);
        Assert.assertEquals(((JsonObject) checks.get(0)).getString("name"), "DataSourceHealthCheck");
        Assert.assertEquals(((JsonObject) checks.get(0)).getString("status"), "DOWN");
        Assert.assertEquals(((JsonObject) checks.get(0)).getJsonObject("data").getString("jdbc/ds1"), "UP");
        Assert.assertEquals(((JsonObject) checks.get(0)).getJsonObject("data").getString("jdbc/ds2"), "DOWN");
    }

    private JsonObject getHealthApiResponse() throws IOException {

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse response = client.execute(new HttpGet(uri + "/health"));
            Assert.assertEquals(response.getStatusLine().getStatusCode(), 503);

            JsonReader jsonReader = Json.createReader(response.getEntity().getContent());
            return jsonReader.readObject();
        }
    }
}
