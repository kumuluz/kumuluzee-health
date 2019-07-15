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
package com.kumuluz.ee.health.logs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.kumuluz.ee.health.HealthRegistry;
import com.kumuluz.ee.health.enums.HealthCheckType;
import com.kumuluz.ee.health.models.HealthResponse;
import org.eclipse.microprofile.health.HealthCheckResponse;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runnable which logs health check results to log.
 *
 * @author Marko Škrjanec
 * @since 1.0.0
 */
public class HealthCheckLogger implements Runnable {

    private static final Logger LOG = Logger.getLogger(HealthCheckLogger.class.getName());
    private static Level LEVEL;

    private ObjectMapper mapper;
    private HealthCheckType type;

    public HealthCheckLogger(String level, HealthCheckType type) {
        this.mapper = new ObjectMapper().registerModule(new Jdk8Module());

        if (type == null) {
            type = HealthCheckType.BOTH;
        }

        this.type = type;
        LEVEL = Level.parse(level.toUpperCase());
    }

    @Override
    public void run() {
        try {
            List<HealthCheckResponse> results = HealthRegistry.getInstance().getResults(type);

            HealthResponse healthResponse = new HealthResponse();
            healthResponse.setChecks(results);
            healthResponse.setStatus(
                    results.stream().anyMatch(result -> result.getState().equals(HealthCheckResponse.State.DOWN)) ?
                            HealthCheckResponse.State.DOWN : HealthCheckResponse.State.UP);

            LOG.log(LEVEL, this.mapper.writer().writeValueAsString(healthResponse));
        } catch (Exception exception) {
            LOG.log(Level.SEVERE, "An exception occurred when trying to evaluate and log health response.", exception);
        }
    }
}
