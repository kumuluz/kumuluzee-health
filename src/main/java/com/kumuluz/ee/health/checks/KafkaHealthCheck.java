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
package com.kumuluz.ee.health.checks;

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.health.annotations.BuiltInHealthCheck;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.common.Node;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;

import javax.enterprise.context.ApplicationScoped;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Kafka health check.
 *
 * @author Urban Malc
 * @since 2.3.0
 */
@ApplicationScoped
@BuiltInHealthCheck
public class KafkaHealthCheck extends KumuluzHealthCheck implements HealthCheck {

    private static final Logger LOG = Logger.getLogger(KafkaHealthCheck.class.getName());

    private static final String DEFAULT_KAFKA_BOOTSTRAP_SERVERS = "localhost:9092";
    private static final int DEFAULT_KAFKA_REQUEST_TIMEOUT_MS = 5000;
    private static final int DEFAULT_KAFKA_MIN_AVAILABLE_NODES = 1;

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder healthCheckResponseBuilder = HealthCheckResponse.named(KafkaHealthCheck.class.getSimpleName()).up();

        ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();

        String bootstrapServers = configurationUtil.get(name() + ".bootstrap-servers")
                .orElse(DEFAULT_KAFKA_BOOTSTRAP_SERVERS);
        int requestTimeout = configurationUtil.getInteger(name() + ".request-timeout-ms")
                .orElse(DEFAULT_KAFKA_REQUEST_TIMEOUT_MS);
        int minimumAvailableNodes = configurationUtil.getInteger(name() + ".minimum-available-nodes")
                .orElse(DEFAULT_KAFKA_MIN_AVAILABLE_NODES);

        Properties kafkaProperties = new Properties();
        kafkaProperties.put("bootstrap.servers", bootstrapServers);
        kafkaProperties.put("request.timeout.ms", requestTimeout);
        kafkaProperties.put("connections.max.idle.ms", requestTimeout * 3);

        try (AdminClient adminClient = KafkaAdminClient.create(kafkaProperties)) {

            // timeout on get serves only as fallback, the timeout in properties gives more descriptive exceptions
            Collection<Node> nodes = adminClient.describeCluster().nodes()
                    .get(DEFAULT_KAFKA_REQUEST_TIMEOUT_MS * 2, TimeUnit.MILLISECONDS);

            if (nodes.size() < minimumAvailableNodes) {
                healthCheckResponseBuilder.down();
            }

            healthCheckResponseBuilder.withData("available-nodes", nodes.size());

        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Could not get available nodes from Kafka cluster.", e);
            healthCheckResponseBuilder.down();
        }

        return healthCheckResponseBuilder.build();
    }

    @Override
    public String name() {
        return kumuluzBaseHealthConfigPath + "kafka-health-check";
    }

    @Override
    public boolean initSuccess() {
        try {
            Class.forName("org.apache.kafka.clients.admin.AdminClient");
            return true;
        } catch (ClassNotFoundException e) {
            LOG.severe("The required kafka-clients library appears to be missing or outdated.");
            return false;
        }
    }
}
