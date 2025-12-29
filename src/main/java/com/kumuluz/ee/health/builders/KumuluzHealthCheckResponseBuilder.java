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
package com.kumuluz.ee.health.builders;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * KumuluzEE HealthCheckResponse builder.
 *
 * @author Marko Škrjanec
 * @since 1.0.0
 */
public class KumuluzHealthCheckResponseBuilder extends HealthCheckResponseBuilder {

    private String name;
    private Map<String, Object> data;
    private HealthCheckResponse.Status status;

    public KumuluzHealthCheckResponseBuilder() {
        this.data = new HashMap<>();
    }

    @Override
    public HealthCheckResponseBuilder name(String name) {
        this.name = name;
        return this;
    }

    @Override
    public HealthCheckResponseBuilder withData(String name, String value) {
        this.data.put(name, value);
        return this;
    }

    @Override
    public HealthCheckResponseBuilder withData(String name, long value) {
        this.data.put(name, value);
        return this;
    }

    @Override
    public HealthCheckResponseBuilder withData(String name, boolean value) {
        this.data.put(name, value);
        return this;
    }

    @Override
    public HealthCheckResponseBuilder up() {
        this.status = HealthCheckResponse.Status.UP;
        return this;
    }

    @Override
    public HealthCheckResponseBuilder down() {
        this.status = HealthCheckResponse.Status.DOWN;
        return this;
    }

    @Override
    public HealthCheckResponseBuilder status(boolean value) {
        this.status = value ? HealthCheckResponse.Status.UP : HealthCheckResponse.Status.DOWN;
        return this;
    }

    @Override
    public HealthCheckResponse build() {
        return new KumuluzHealthCheckResponse();
    }

    class KumuluzHealthCheckResponse extends HealthCheckResponse {

        @Override
        public String getName() {
            return name;
        }

        @JsonProperty("status")
        @Override
        public HealthCheckResponse.Status getStatus() {
            return status;
        }

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        @Override
        public Optional<Map<String, Object>> getData() {
            return Optional.ofNullable(data == null || data.isEmpty() ? null : data);
        }
    }
}
