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
import redis.clients.jedis.JedisPool;

import javax.enterprise.context.ApplicationScoped;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Redis health check.
 *
 * @author Marko Škrjanec
 * @since 1.0.0
 */
@ApplicationScoped
@BuiltInHealthCheck
public class RedisHealthCheck extends KumuluzHealthCheck implements HealthCheck {

    private static final Logger LOG = Logger.getLogger(RedisHealthCheck.class.getName());

    // Default redis connection url
    private static final String DEFAULT_REDIS_URL = "redis://localhost:6379/0";

    @Override
    public HealthCheckResponse call() {
        String connectionUrl = ConfigurationUtil.getInstance().get(name() + ".connection-url")
                .orElse(DEFAULT_REDIS_URL);

        JedisPool pool = null;
        try {
            pool = new JedisPool(connectionUrl);
            pool.getResource();
            return HealthCheckResponse.up(RedisHealthCheck.class.getSimpleName());
        } catch (Exception exception) {
            LOG.log(Level.SEVERE, "An exception occurred when trying to establish connection to Redis.", exception);
            return HealthCheckResponse.down(RedisHealthCheck.class.getSimpleName());
        } finally {
            if (pool != null) {
                pool.close();
            }
        }
    }

    @Override
    public String name() {
        return kumuluzBaseHealthConfigPath + "redis-health-check";
    }

    @Override
    public boolean initSuccess() {
        try {
            Class.forName("redis.clients.jedis.JedisPool");
            return true;
        } catch (ClassNotFoundException e) {
            LOG.severe("The required jedis library appears to be missing.");
            return false;
        }
    }
}
