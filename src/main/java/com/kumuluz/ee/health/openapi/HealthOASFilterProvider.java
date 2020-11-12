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
package com.kumuluz.ee.health.openapi;

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.health.utils.HealthServletMappingUtil;
import com.kumuluz.ee.openapi.mp.spi.OASFilterProvider;
import org.eclipse.microprofile.openapi.OASFilter;

/**
 * OpenAPI OAS Filter Provider SPI implementation.
 *
 * @author Urban Malc
 * @since 2.3.0
 */
public class HealthOASFilterProvider implements OASFilterProvider {

    @Override
    public OASFilter registerOasFilter() {
        return new HealthOASFilter(isEnabled(), getServletMapping());
    }

    private boolean isEnabled() {
        return ConfigurationUtil.getInstance().getBoolean("kumuluzee.health.openapi-mp.enabled")
                .orElse(false);
    }

    private String getServletMapping() {
        String mapping = HealthServletMappingUtil.getMapping();

        // remove trailing "/*"
        mapping = mapping.substring(0, mapping.length() - 2);

        return mapping;
    }
}
