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

public class RestHealthCheck implements HealthCheck {

    private static final Logger LOG = Logger.getLogger(RestHealthCheck.class.getName());

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder healthCheckResponseBuilder = HealthCheckResponse.named(RestHealthCheck.class
                .getSimpleName()).up();
        Optional<Integer> connectionUrls = ConfigurationUtil.getInstance().getListSize("kumuluzee.health.checks" +
                ".rest-health-check");

        if (connectionUrls.isPresent()) {
            for (int i = 0; i < connectionUrls.get(); i++) {
                String connectionUrl = ConfigurationUtil.getInstance().get("kumuluzee.health.checks" +
                        ".rest-health-check[" + i + "].connection-url").orElse("");
                checkHttpStatus(connectionUrl, healthCheckResponseBuilder);
            }
        } else {
            String connectionUrl = ConfigurationUtil.getInstance().get("kumuluzee.health.checks.rest-health-check" +
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
