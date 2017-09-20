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
package com.kumuluz.ee.health.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.eclipse.microprofile.health.HealthCheckResponse;

import java.util.List;

/**
 * @author Marko Škrjanec
 * @since 1.0.0
 */
@JsonInclude(Include.NON_NULL)
public class HealthServletResponse {

    private HealthCheckResponse.State outcome;

    private List<HealthCheckResponse> checks;

    public HealthServletResponse() {
    }

    public HealthCheckResponse.State getOutcome() {
        return outcome;
    }

    public void setOutcome(HealthCheckResponse.State outcome) {
        this.outcome = outcome;
    }

    public List<HealthCheckResponse> getChecks() {
        return checks;
    }

    public void setChecks(List<HealthCheckResponse> checks) {
        this.checks = checks;
    }
}
