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
import org.h2.tools.Server;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Before;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.IOException;
import java.net.URI;
import java.sql.SQLException;

public class DatasourceDownHealthCheckTest extends Arquillian {

    private static Server h2Server;

    @ArquillianResource
    private URI uri;

    @Deployment
    public static JavaArchive createDeployment() throws SQLException {
        h2Server = Server.createTcpServer("-ifNotExists").start();

        JavaArchive javaArchive = ShrinkWrap.create(JavaArchive.class)
                .addAsResource(new StringAsset("kumuluzee:\n" +
                        "  datasources:\n" +
                        "    - jndi-name: jdbc/ds1\n" +
                        "      connection-url: jdbc:h2:~/test1\n" +
                        "    - jndi-name: jdbc/ds2\n" +
                        "      connection-url: jdbc:h2:tcp://localhost:" + h2Server.getPort() + "/~/test\n" +
                        "\n" +
                        "  health:\n" +
                        "    checks:\n" +
                        "      data-source-health-check:\n" +
                        "        - jndi-name: jdbc/ds1\n" +
                        "        - jndi-name: jdbc/ds2\n"), "config.yml");

        javaArchive.merge(Maven.resolver().resolve("com.h2database:h2:1.4.200").withoutTransitivity().asSingle(JavaArchive.class));

        return javaArchive;
    }

    @Test
    @RunAsClient
    public void healthApiShouldReturnDown() throws IOException {
        h2Server.stop();

        JsonObject healthApiResponse = getHealthApiResponse("/health");
        Assert.assertNotNull(healthApiResponse);
        JsonArray checks = healthApiResponse.getJsonArray("checks");
        Assert.assertEquals(checks.size(), 1);
        Assert.assertEquals(checks.get(0).asJsonObject().getString("name"), "DataSourceHealthCheck");
        Assert.assertEquals(checks.get(0).asJsonObject().getString("status"), "DOWN");
        Assert.assertEquals(checks.get(0).asJsonObject().getJsonObject("data").getString("jdbc:h2:~/test1"), "UP");
        Assert.assertEquals(checks.get(0).asJsonObject().getJsonObject("data").getString("jdbc:h2:tcp://localhost:" + h2Server.getPort() + "/~/test"), "DOWN");
    }

    private JsonObject getHealthApiResponse(String healthPath) throws IOException {
        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = client.execute(new HttpGet(uri + healthPath));
        Assert.assertEquals(response.getStatusLine().getStatusCode(), 503);

        JsonReader jsonReader = Json.createReader(response.getEntity().getContent());
        return jsonReader.readObject();
    }
}
