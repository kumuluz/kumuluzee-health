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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.health.models.HealthServletResponse;
import org.eclipse.microprofile.health.HealthCheckResponse;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Health Servlet.
 *
 * @author Marko Škrjanec
 * @since 1.0.0
 */
public class HealthServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(HealthServlet.class.getName());

    private ConfigurationUtil configurationUtil;
    private HealthRegistry healthCheckRegistry;

    private ObjectMapper mapper;

    public void init() throws ServletException {
        this.configurationUtil = ConfigurationUtil.getInstance();
        this.healthCheckRegistry = HealthRegistry.getInstance();
        this.mapper = new ObjectMapper().registerModule(new Jdk8Module());
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setHeader("Cache-Control", "must-revalidate,no-cache,no-store");

        ServletOutputStream output = null;
        try {
            output = response.getOutputStream();
            response.setStatus(HttpServletResponse.SC_OK);

            // get results
            List<HealthCheckResponse> results = this.healthCheckRegistry.getResults();

            // prepare response
            HealthServletResponse healthServletResponse = new HealthServletResponse();
            healthServletResponse.setChecks(results);
            healthServletResponse.setOutcome(HealthCheckResponse.State.UP);

            // check if any check is down
            for (HealthCheckResponse result : results) {
                if (HealthCheckResponse.State.DOWN.equals(result.getState())) {
                    response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                    healthServletResponse.setOutcome(HealthCheckResponse.State.DOWN);
                    break;
                }
            }

            // write results to response if servlet.response or debug is enabled
            if (this.configurationUtil.getBoolean("kumuluzee.health.servlet.enabled").orElse(true) ||
                    this.configurationUtil.getBoolean("kumuluzee.debug").orElse(false)) {
                response.setContentType(MediaType.APPLICATION_JSON);
                getWriter(request).writeValue(output, healthServletResponse);
            }
        } catch (Exception exception) {
            LOG.log(Level.SEVERE, "An error occurred when trying to evaluate health checks.", exception);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    private ObjectWriter getWriter(HttpServletRequest request) {
        boolean prettyPrintOff = "false".equals(request.getParameter("pretty"));
        return prettyPrintOff ? this.mapper.writer() : this.mapper.writerWithDefaultPrettyPrinter();
    }
}