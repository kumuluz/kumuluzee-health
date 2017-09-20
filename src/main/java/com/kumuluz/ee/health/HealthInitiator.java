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

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.WebListener;
import java.util.logging.Logger;

/**
 * Initializes Health module.
 *
 * @author Marko Škrjanec
 * @since 1.0.0
 */
@ApplicationScoped
@WebListener
public class HealthInitiator implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(HealthInitiator.class.getName());

    private boolean beanInitialised;

    @Override
    public void contextInitialized(ServletContextEvent servletContextEvent) {
        if (!beanInitialised) {
            beanInitialised = initialiseBean(servletContextEvent.getServletContext());
        }
    }

    public void cdiInitialised(@Observes @Initialized(ApplicationScoped.class) Object init) {
        if (!beanInitialised && init instanceof ServletContext) {
            beanInitialised = initialiseBean((ServletContext) init);
        }
    }

    private boolean initialiseBean(ServletContext servletContext) {
        LOG.info("Initializing KumuluzEE Health extension");

        // servlet mapping
        String servletMapping = ConfigurationUtil.getInstance().get("kumuluzee.health.servlet.mapping")
                .orElse("/health");

        LOG.info("Registering health servlet on " + servletMapping);

        // register servlet
        ServletRegistration.Dynamic dynamicRegistration = servletContext.addServlet("health", new
                HealthServlet());

        dynamicRegistration.addMapping(servletMapping);

        LOG.info("KumuluzEE Health extension initialized");
        return true;
    }

    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
    }
}