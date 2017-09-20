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

import com.kumuluz.ee.health.HealthRegistry;
import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.util.AnnotationLiteral;
import java.util.Set;

/**
 * Health check bean registration class.
 *
 * @author Marko Škrjanec
 * @since 1.0.0
 */
public class HealthCheckInitializationExtension implements Extension {

    public <T> void processAnnotatedType(@Observes @Initialized(ApplicationScoped.class) Object init, BeanManager
            beanManager) {

        Set<Bean<?>> beans = beanManager.getBeans(HealthCheck.class, new AnnotationLiteral<Health>() {
        });

        for (Bean bean : beans) {
            HealthCheck healthCheckBean = (HealthCheck) beanManager.getReference(bean, HealthCheck.class,
                    beanManager.createCreationalContext(bean));
            HealthRegistry.getInstance().register(bean.getBeanClass().getSimpleName(), healthCheckBean);
        }
    }
}