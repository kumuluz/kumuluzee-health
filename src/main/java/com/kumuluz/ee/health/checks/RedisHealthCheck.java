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
import redis.clients.jedis.JedisPool;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Marko Škrjanec
 * @since 1.0.0
 */
public class RedisHealthCheck implements HealthCheck {

    private Logger LOG = Logger.getLogger(RedisHealthCheck.class.getName());

    // Default redis connection url
    private static final String DEFAULT_REDIS_URL = "redis://localhost:6379/0";

    @Override
    public HealthCheckResponse call() {
        String connectionUrl = ConfigurationUtil.getInstance()
                .get("kumuluzee.health.checks.redis-health-check.connection-url")
                .orElse(DEFAULT_REDIS_URL);

        JedisPool pool = null;
        try {
            pool = new JedisPool(connectionUrl);
            pool.getResource();
            return HealthCheckResponse.named(RedisHealthCheck.class.getSimpleName()).up().build();
        } catch (Exception exception) {
            LOG.log(Level.SEVERE, "Connection to Redis couldn't be established.", exception);
            return HealthCheckResponse.named(RedisHealthCheck.class.getSimpleName()).down().build();
        } finally {
            if (pool != null) {
                pool.close();
            }
        }
    }
}
