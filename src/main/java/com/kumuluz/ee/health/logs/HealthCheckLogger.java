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

import com.kumuluz.ee.health.HealthRegistry;
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

    public HealthCheckLogger(String level) {
        this.LEVEL = Level.parse(level.toUpperCase());
    }

    @Override
    public void run() {
        try {
            List<HealthCheckResponse> results = HealthRegistry.getInstance().getResults();

            LOG.log(this.LEVEL, "Overall health check outcome is {0}.",
                    results.stream().anyMatch(result -> result.getState().equals(HealthCheckResponse.State.DOWN)) ?
                            HealthCheckResponse.State.DOWN : HealthCheckResponse.State.UP);

            for (HealthCheckResponse healthCheckResponse : results) {
                String[] objects = new String[2];
                objects[0] = healthCheckResponse.getName();
                objects[1] = healthCheckResponse.getState().toString();
                LOG.log(this.LEVEL, "Health check {0} outcome is {1}.", objects);
            }
        } catch(Exception exception){
            LOG.log(Level.SEVERE, "An error occurred when trying to evaluate health checks.", exception);
        }
    }
}
