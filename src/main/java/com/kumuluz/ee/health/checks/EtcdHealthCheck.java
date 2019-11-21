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
import com.kumuluz.ee.health.annotations.BuiltInHealthCheck;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Etcd health check.
 *
 * @author Benjamin Kastelic
 * @since 1.0.0
 */
@ApplicationScoped
@BuiltInHealthCheck
public class EtcdHealthCheck extends KumuluzHealthCheck implements HealthCheck {

    private static final Logger LOG = Logger.getLogger(EtcdHealthCheck.class.getName());

    private static final String HEALTHY = "{\"health\":\"true\"}";

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder healthCheckResponseBuilder = HealthCheckResponse.named(EtcdHealthCheck.class.getSimpleName()).up();
        Optional<Integer> connectionUrls = ConfigurationUtil.getInstance().getListSize(name());

        if (connectionUrls.isPresent()) {
            for (int i = 0; i < connectionUrls.get(); i++) {
                String connectionUrl =
                        ConfigurationUtil.getInstance().get(name() + "[" + i + "].connection-url").orElse("");
                checkEtcdStatus(connectionUrl, healthCheckResponseBuilder);
            }
        } else {
            String connectionUrl = ConfigurationUtil.getInstance().get(name() + ".connection-url").orElse("");
            checkEtcdStatus(connectionUrl, healthCheckResponseBuilder);
        }

        return healthCheckResponseBuilder.build();
    }

    /**
     * Helper method for checking if etcd is online.
     */
    private void checkEtcdStatus(String connectionUrl, HealthCheckResponseBuilder healthCheckResponseBuilder) {
        WebTarget webTarget = ClientBuilder.newClient().target(connectionUrl);
        Response response = null;

        try {
            response = webTarget.request().get();

            if (response.getStatus() == 200) {
                String result = response.readEntity(String.class).replaceAll("\\s+", "");

                if (result != null && result.equals(HEALTHY)) {
                    healthCheckResponseBuilder.withData(connectionUrl, HealthCheckResponse.State.UP.toString());
                    return;
                }
            }
        } catch (Exception exception) {
            LOG.log(Level.SEVERE, "An exception occurred when trying to get etcd status.", exception);
        } finally {
            if (response != null) {
                response.close();
            }
        }

        healthCheckResponseBuilder.withData(connectionUrl, HealthCheckResponse.State.DOWN.toString());
        healthCheckResponseBuilder.down();
    }

    @Override
    public String name() {
        return kumuluzBaseHealthConfigPath + "etcd-health-check";
    }

    @Override
    public boolean initSuccess() {
        return true;
    }
}
