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
import com.kumuluz.ee.health.annotations.BuiltInHealthCheck;
import com.kumuluz.ee.health.checks.KumuluzHealthCheck;
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
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Health check registration class.
 *
 * @author Marko Škrjanec
 * @since 1.0.0
 */
public class HealthCheckInitializationExtension implements Extension {

    private static final Logger LOG = Logger.getLogger(HealthCheckInitializationExtension.class.getName());

    protected final String MP_HEALTH_DISABLE_DEFAULT_HEALTH = "mp.health.disable-default-procedures";

    public void registerHealthChecks(@Observes @Initialized(ApplicationScoped.class) Object init, BeanManager
            beanManager) {

        // register beans that implement health checks
        registerHealthCheckBeans(beanManager, new AnnotationLiteral<BuiltInHealthCheck>() {
        }, HealthCheckType.READINESS);
        registerHealthCheckBeans(beanManager, new AnnotationLiteral<Liveness>() {
        }, HealthCheckType.LIVENESS);
        registerHealthCheckBeans(beanManager, new AnnotationLiteral<Readiness>() {
        }, HealthCheckType.READINESS);
        //backwards compatible TBR
        registerHealthCheckBeans(beanManager, new AnnotationLiteral<Health>() {
        }, null);
    }

    private void registerHealthCheckBeans(BeanManager beanManager, AnnotationLiteral qualifier, HealthCheckType type) {
        // register beans that implement health checks
        Set<Bean<?>> beans = beanManager.getBeans(HealthCheck.class, qualifier);

        boolean disableDefaultHealthChecks = ConfigurationUtil.getInstance().getBoolean(MP_HEALTH_DISABLE_DEFAULT_HEALTH).orElse(false);
        for (Bean<?> bean : beans) {
            HealthCheckType hcType = type;
            if (bean.getBeanClass().isAnnotationPresent(BuiltInHealthCheck.class)) {
                if (disableDefaultHealthChecks) {
                    continue;
                }
                KumuluzHealthCheck kumuluzHealthCheckBean = getBuildInHealthCheckBeanInstance(bean);
                if (kumuluzHealthCheckBean == null) {
                    continue;
                }
                hcType = kumuluzHealthCheckBean.getHealthCheckType();
            }
            HealthCheck healthCheckBean = (HealthCheck) beanManager.getReference(bean, HealthCheck.class,
                    beanManager.createCreationalContext(bean));
            HealthRegistry.getInstance().register(healthCheckBean.getClass().getSimpleName(), healthCheckBean, hcType);
        }
    }

    private KumuluzHealthCheck getBuildInHealthCheckBeanInstance(Bean<?> bean) {

        if (KumuluzHealthCheck.class.isAssignableFrom(bean.getBeanClass())) {
            try {
                KumuluzHealthCheck instance = (KumuluzHealthCheck) bean.getBeanClass().getDeclaredConstructor().newInstance();
                String kumuluzHealthCheckName = instance.name();

                Optional<List<String>> healthCheckConfigRoot = ConfigurationUtil.getInstance().getMapKeys(kumuluzHealthCheckName);
                // DataSourceHealthCheck list is also valid
                Optional<Integer> healthCheckConfigRootList = ConfigurationUtil.getInstance().getListSize(kumuluzHealthCheckName);

                if ((healthCheckConfigRoot.isPresent() && !healthCheckConfigRoot.get().isEmpty()) ||
                        (healthCheckConfigRootList.isPresent() && healthCheckConfigRootList.get() > 0)) {

                    if (instance.initSuccess()) {
                        return instance;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
