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
package com.kumuluz.ee.health.checks;

import com.kumuluz.ee.configuration.utils.ConfigurationUtil;
import com.kumuluz.ee.health.annotations.BuiltInHealthCheck;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

import javax.enterprise.context.ApplicationScoped;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mongo DB health check.
 *
 * @author Marko Škrjanec
 * @since 1.0.0
 */
@ApplicationScoped
@BuiltInHealthCheck
public class MongoHealthCheck extends KumuluzHealthCheck implements HealthCheck {

    private static final Logger LOG = Logger.getLogger(MongoHealthCheck.class.getName());

    // Default mongo connection url
    private static final String DEFAULT_MONGO_URL = "mongodb://localhost:27017/local?serverSelectionTimeoutMS=2000";

    @Override
    public HealthCheckResponse call() {
        String connectionUrl = ConfigurationUtil.getInstance()
                .get(name() + ".connection-url")
                .orElse(DEFAULT_MONGO_URL);

        MongoClient mongoClient = null;

        try {
            MongoClientURI mongoClientURI = new MongoClientURI(connectionUrl);
            mongoClient = new MongoClient(mongoClientURI);

            if (databaseExist(mongoClient, mongoClientURI.getDatabase())) {
                return HealthCheckResponse.up(MongoHealthCheck.class.getSimpleName());
            } else {
                LOG.severe("Mongo database not found.");
                return HealthCheckResponse.down(MongoHealthCheck.class.getSimpleName());
            }
        } catch (Exception exception) {
            LOG.log(Level.SEVERE, "An exception occurred when trying to establish connection to Mongo.", exception);
            return HealthCheckResponse.down(MongoHealthCheck.class.getSimpleName());
        } finally {
            if (mongoClient != null) {
                mongoClient.close();
            }

        }
    }

    /**
     * Helper method for checking if database name exists.
     *
     * @param mongoClient
     * @param databaseName
     * @return
     */
    private Boolean databaseExist(MongoClient mongoClient, String databaseName) {

        if (mongoClient != null && databaseName != null) {

            for (String s : mongoClient.listDatabaseNames()) {
                if (s.equals(databaseName))
                    return true;
            }
        }

        return false;
    }

    @Override
    public String name() {
        return kumuluzBaseHealthConfigPath + "mongo-health-check";
    }

    @Override
    public boolean initSuccess() {
        try {
            Class.forName("com.mongodb.MongoClient");
            return true;
        } catch (ClassNotFoundException e) {
            LOG.severe("The required mongo-java-driver library appears to be missing.");
            return false;
        }
    }
}
