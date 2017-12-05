package com.kumuluz.ee.health.checks;

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;

import javax.ws.rs.HttpMethod;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RestHealthCheck implements HealthCheck {

    private static final Logger LOG = Logger.getLogger(RestHealthCheck.class.getName());

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder response = HealthCheckResponse.named(RestHealthCheck.class.getSimpleName()).up();
        Optional<Integer> connectionUrls = ConfigurationUtil.getInstance().getListSize("kumuluzee.health.checks" +
                ".rest-health-check");

        if (connectionUrls.isPresent()) {
            for (int i = 0; i < connectionUrls.get(); i++) {
                String connectionUrl = ConfigurationUtil.getInstance().get("kumuluzee.health.checks" +
                        ".rest-health-check[" + i + "].connection-url").orElse("");
                checkHttpStatus(connectionUrl, response);
            }
        } else {
            String connectionUrl = ConfigurationUtil.getInstance().get("kumuluzee.health.checks.rest-health-check" +
                    ".connection-url").orElse("");
            checkHttpStatus(connectionUrl, response);
        }

        return response.build();
    }

    /**
     * Helper method for checking if url is accessible and status code is >= 200 and < 300.
     *
     * @param connectionUrl
     * @return
     */
    private void checkHttpStatus(String connectionUrl, HealthCheckResponseBuilder response) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(connectionUrl).openConnection();
            connection.setRequestMethod(HttpMethod.HEAD);

            if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 300) {
                response.withData(connectionUrl, HealthCheckResponse.State.UP.toString());
            } else {
                response.withData(connectionUrl, HealthCheckResponse.State.DOWN.toString());
                response.down();
            }
        } catch (Exception exception) {
            LOG.log(Level.SEVERE, "An exception occurred when trying to connect over HTTP to " + connectionUrl + ".",
                    exception);
            response.withData(connectionUrl, HealthCheckResponse.State.DOWN.toString());
            response.down();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

}
