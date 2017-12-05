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
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Marko Škrjanec
 * @since 1.0.0
 */
public class HttpHealthCheck implements HealthCheck {

    private static final Logger LOG = Logger.getLogger(HttpHealthCheck.class.getName());

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder healthCheckResponseBuilder = HealthCheckResponse.named(HttpHealthCheck.class
                .getSimpleName()).up();
        Optional<Integer> connectionUrls = ConfigurationUtil.getInstance().getListSize("kumuluzee.health.checks" +
                ".http-health-check");

        if (connectionUrls.isPresent()) {
            for (int i = 0; i < connectionUrls.get(); i++) {
                String connectionUrl = ConfigurationUtil.getInstance().get("kumuluzee.health.checks" +
                        ".http-health-check[" + i + "].connection-url").orElse("");
                checkHttpStatus(connectionUrl, healthCheckResponseBuilder);
            }
        } else {
            String connectionUrl = ConfigurationUtil.getInstance().get("kumuluzee.health.checks.http-health-check" +
                    ".connection-url").orElse("");
            checkHttpStatus(connectionUrl, healthCheckResponseBuilder);
        }

        return healthCheckResponseBuilder.build();
    }

    /**
     * Helper method for checking if url is accessible and status code is >= 200 and < 300.
     *
     * @param connectionUrl
     * @return
     */
    private void checkHttpStatus(String connectionUrl, HealthCheckResponseBuilder healthCheckResponseBuilder) {
        WebTarget webTarget = ClientBuilder.newClient().target(connectionUrl);
        Response response = null;

        try {
            response = webTarget.request().head();

            if (response.getStatus() >= 200 && response.getStatus() < 300) {
                healthCheckResponseBuilder.withData(connectionUrl, HealthCheckResponse.State.UP.toString());
                return;
            }
        } catch (Exception exception) {
            LOG.log(Level.SEVERE, "An exception occurred when trying to connect over HTTP.", exception);
        } finally {
            if (response != null) {
                response.close();
            }
        }
        healthCheckResponseBuilder.withData(connectionUrl, HealthCheckResponse.State.DOWN.toString());
        healthCheckResponseBuilder.down();
    }

}
