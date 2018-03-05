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
package com.kumuluz.ee.health;

import com.kumuluz.ee.health.providers.KumuluzHealthCheckResponseProvider;
import com.kumuluz.ee.health.utils.HealthCheckInitializationExtension;
import org.jboss.arquillian.container.test.spi.client.deployment.CachedAuxilliaryArchiveAppender;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;

/**
 * Packages KumuluzEE Health library as a ShrinkWrap archive and adds it to deployments.
 *
 * @author Urban Malc
 * @since 1.1.0
 */
public class HealthLibraryAppender extends CachedAuxilliaryArchiveAppender {

    @Override
    protected Archive<?> buildArchive() {

        return ShrinkWrap.create(JavaArchive.class, "kumuluzee-health.jar")
                .addPackages(true, HealthExtension.class.getPackage())
                .addAsServiceProvider(com.kumuluz.ee.common.Extension.class, HealthExtension.class)
                .addAsServiceProvider(javax.enterprise.inject.spi.Extension.class,
                        HealthCheckInitializationExtension.class)
                .addAsServiceProvider(org.eclipse.microprofile.health.spi.HealthCheckResponseProvider.class,
                        KumuluzHealthCheckResponseProvider.class)
                .addAsResource("META-INF/beans.xml");
    }
}
