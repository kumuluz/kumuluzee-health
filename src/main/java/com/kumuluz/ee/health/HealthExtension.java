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

import com.kumuluz.ee.common.Extension;
import com.kumuluz.ee.common.KumuluzServer;
import com.kumuluz.ee.common.ServletServer;
import com.kumuluz.ee.common.config.EeConfig;
import com.kumuluz.ee.common.dependencies.*;
import com.kumuluz.ee.common.wrapper.KumuluzServerWrapper;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.health.logs.HealthCheckLogger;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * KumuluzEE Health extension.
 *
 * @author Marko Škrjanec
 * @since 1.0.0
 */
@EeExtensionDef(name = "Health", group = EeExtensionGroup.HEALTH)
@EeComponentDependencies({
        @EeComponentDependency(EeComponentType.CDI),
        @EeComponentDependency(EeComponentType.JAX_RS)
})
public class HealthExtension implements Extension {

    private static final Logger LOG = Logger.getLogger(HealthExtension.class.getName());

    private ScheduledExecutorService scheduler;

    @Override
    public void load() {
    }

    @Override
    public void init(KumuluzServerWrapper kumuluzServerWrapper, EeConfig eeConfig) {

        LOG.info("Initializing Health extension");

        KumuluzServer server = kumuluzServerWrapper.getServer();

        if (!(server instanceof ServletServer)) {
            LOG.warning("Server is not instance of ServletServer. Health servlet will not be initialized.");
            return;
        }

        ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();

        // initialize servlet mapping
        String servletMapping = configurationUtil.get("kumuluzee.health.servlet.mapping").orElse("/health/*");

        if (!servletMapping.endsWith("/*")) {
            if (servletMapping.endsWith("/")) {
                servletMapping += "*";
            } else {
                servletMapping += "/*";
            }
        }

        LOG.info("Registering health servlet on " + servletMapping);

        // register servlet
        ((ServletServer) server).registerServlet(HealthServlet.class, servletMapping,
                Collections.singletonMap("com.kumuluz.ee.health.servletMapping",
                        servletMapping.substring(0, servletMapping.length() - 2)));

        // initialize health logger
        if (configurationUtil.getBoolean("kumuluzee.health.logs.enabled").orElse(true)) {
            int period = configurationUtil.getInteger("kumuluzee.health.logs.period-s").orElse(60);
            String level = configurationUtil.get("kumuluzee.health.logs.level").orElse("FINE");

            scheduler = Executors.newScheduledThreadPool(1);
            LOG.log(Level.INFO, "Starting health logger to log health check results every {0} s", period);

            HealthCheckLogger logger = new HealthCheckLogger(level);
            scheduler.scheduleWithFixedDelay(logger, period, period, TimeUnit.SECONDS);
        }
    }

    @Override
    public boolean isEnabled() {
        Optional<Boolean> enabled = ConfigurationUtil.getInstance().getBoolean("kumuluzee.health.enabled");

        return enabled.orElse(true);
    }
}
