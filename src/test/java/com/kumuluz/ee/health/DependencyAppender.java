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

import com.kumuluz.ee.testing.arquillian.spi.MavenDependencyAppender;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * Adds required dependencies to the deployments.
 *
 * @author Urban Malc
 * @since 1.1.0
 */
public class DependencyAppender implements MavenDependencyAppender {

    private static final ResourceBundle versionsBundle = ResourceBundle.getBundle("META-INF/kumuluzee/versions");

    @Override
    public List<String> addLibraries() {

        List<String> libs = new ArrayList<>();

        libs.add("com.kumuluz.ee:kumuluzee-jax-rs-jersey:");
        libs.add("org.eclipse.microprofile.health:microprofile-health-api:" +
                versionsBundle.getString("microprofile-health-version"));
        libs.add("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:" +
                versionsBundle.getString("jackson-version"));

        return libs;
    }
}
