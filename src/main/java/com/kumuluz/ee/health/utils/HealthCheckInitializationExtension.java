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
package com.kumuluz.ee.health.utils;

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.health.HealthRegistry;
import com.kumuluz.ee.health.checks.*;
import com.kumuluz.ee.health.enums.HealthCheckType;
import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.util.AnnotationLiteral;
import java.sql.DriverManager;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Health check registration class.
 *
 * @author Marko Škrjanec
 * @since 1.0.0
 */
public class HealthCheckInitializationExtension implements Extension {

    private static final Logger LOG = Logger.getLogger(HealthCheckInitializationExtension.class.getName());

    public void registerHealthChecks(@Observes @Initialized(ApplicationScoped.class) Object init, BeanManager
            beanManager) {

        // register classes that implement health checks

        registerHealthCheck("kumuluzee.health.checks.data-source-health-check",
                DataSourceHealthCheck.class,
                () -> {
                    if(!DriverManager.getDrivers().hasMoreElements()){
                        LOG.severe("No database driver library appears to be provided.");
                        return false;
                    }

                    return true;
                });

        registerHealthCheck("kumuluzee.health.checks.disk-space-health-check",
                DiskSpaceHealthCheck.class,
                () -> true);

        registerHealthCheck("kumuluzee.health.checks.elastic-search-health-check",
                ElasticSearchHealthCheck.class,
                () -> true);

        registerHealthCheck("kumuluzee.health.checks.etcd-health-check",
                EtcdHealthCheck.class,
                () -> true);

        registerHealthCheck("kumuluzee.health.checks.http-health-check",
                HttpHealthCheck.class,
                () -> true);

        registerHealthCheck("kumuluzee.health.checks.mongo-health-check",
                MongoHealthCheck.class,
                () -> {
                    try {
                        Class.forName( "com.mongodb.MongoClient" );
                        return true;
                    } catch( ClassNotFoundException e ) {
                        LOG.severe("The required mongo-java-driver library appears to be missing.");
                        return false;
                    }
                });

        registerHealthCheck("kumuluzee.health.checks.rabbit-health-check",
                RabbitHealthCheck.class,
                () -> {
                    try {
                        Class.forName( "com.rabbitmq.client.Connection" );
                        return true;
                    } catch( ClassNotFoundException e ) {
                        LOG.severe("The required amqp-client library appears to be missing.");
                        return false;
                    }
                });

        registerHealthCheck("kumuluzee.health.checks.redis-health-check",
                RedisHealthCheck.class,
                () -> {
                    try {
                        Class.forName( "redis.clients.jedis.JedisPool" );
                        return true;
                    } catch( ClassNotFoundException e ) {
                        LOG.severe("The required jedis library appears to be missing.");
                        return false;
                    }
                });

        // register beans that implement health checks
        registerHealthCheckBeans(beanManager, new AnnotationLiteral<Liveness>() {}, HealthCheckType.LIVENESS);
        registerHealthCheckBeans(beanManager, new AnnotationLiteral<Readiness>() {}, HealthCheckType.READINESS);
        registerHealthCheckBeans(beanManager, new AnnotationLiteral<Health>() {}, HealthCheckType.BOTH);
    }

    private void registerHealthCheckBeans(BeanManager beanManager, AnnotationLiteral qualifier, HealthCheckType type) {
        // register beans that implement health checks
        Set<Bean<?>> beans = beanManager.getBeans(HealthCheck.class, qualifier);

        for (Bean<?> bean : beans) {
            HealthCheck healthCheckBean = (HealthCheck) beanManager.getReference(bean, HealthCheck.class,
                    beanManager.createCreationalContext(bean));
            HealthRegistry.getInstance().register(bean.getBeanClass().getSimpleName(), healthCheckBean, type);
        }
    }

    private void registerHealthCheck(String configKeyPrefix, Class<? extends HealthCheck> healthCheckClass,
                                     Supplier<Boolean> check) {
        if (ConfigurationUtil.getInstance().get(configKeyPrefix).isPresent()) {

            if (check.get()) {

                try {
                    HealthRegistry.getInstance().register(
                            healthCheckClass.getSimpleName(),
                            healthCheckClass.newInstance(),
                            getHealthCheckType(configKeyPrefix));
                } catch (InstantiationException | IllegalAccessException e) {
                    LOG.log(Level.SEVERE, "Could not instantiate " + configKeyPrefix, e);
                }

            }
        }
    }

    private HealthCheckType getHealthCheckType(String configKeyPrefix) {
        String type = ConfigurationUtil.getInstance().get(configKeyPrefix + ".type").orElse("readiness");

        if (type.equalsIgnoreCase("liveness")) {
            return HealthCheckType.LIVENESS;
        }

        return HealthCheckType.READINESS;
    }
}