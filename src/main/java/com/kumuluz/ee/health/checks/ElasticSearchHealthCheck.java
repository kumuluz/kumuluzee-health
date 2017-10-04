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
package com.kumuluz.ee.health.checks;

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Marko Škrjanec
 * @since 1.0.0
 */
public class ElasticSearchHealthCheck implements HealthCheck {

    private static final Logger LOG = Logger.getLogger(ElasticSearchHealthCheck.class.getName());
    private static final Client CLIENT = ClientBuilder.newClient();

    // Default elastic search cluster health check url
    private static final String DEFAULT_CLUSTER_HEALTH_URL = "http://localhost:9200/_cluster/health";

    private static final String GREEN = "green";
    private static final String YELLOW = "yellow";
    private static final String STATUS = "status";

    @Override
    public HealthCheckResponse call() {
        String connectionUrl = ConfigurationUtil.getInstance()
                .get("kumuluzee.health.checks.elastic-search-health-check.connection-url")
                .orElse(DEFAULT_CLUSTER_HEALTH_URL);

        WebTarget webTarget = CLIENT.target(connectionUrl);
        Response response = null;
        try {
            response = webTarget.request().get();

            if (response.getStatus() == 200) {
                HashMap result = response.readEntity(new GenericType<LinkedHashMap>() {
                });
                Object status = result.get(STATUS);

                if (status != null && (GREEN.equals(status.toString()) || YELLOW.equals(status.toString()))) {
                    return HealthCheckResponse.named(ElasticSearchHealthCheck.class.getSimpleName()).up().build();
                }
            }

            return HealthCheckResponse.named(ElasticSearchHealthCheck.class.getSimpleName()).down().build();
        } catch (Exception exception) {
            LOG.log(Level.SEVERE, "An exception occurred when trying to get Elasticsearch cluster status.",
                    exception);
            return HealthCheckResponse.named(ElasticSearchHealthCheck.class.getSimpleName()).down().build();
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }
}
