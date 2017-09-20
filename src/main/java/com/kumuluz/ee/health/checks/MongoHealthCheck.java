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
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCursor;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

import java.util.logging.Logger;

/**
 * @author Marko Škrjanec
 * @since 1.0.0
 */
public class MongoHealthCheck implements HealthCheck {

    private Logger LOG = Logger.getLogger(MongoHealthCheck.class.getName());

    // Default redis connection url
    private static final String DEFAULT_MONGO_URL = "mongodb://localhost:27017/local?serverSelectionTimeoutMS=2000";

    @Override
    public HealthCheckResponse call() {
        ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();

        String connectionUrl = configurationUtil.get("kumuluzee.health.checks.mongo-health-check.connection-url")
                .orElse(DEFAULT_MONGO_URL);

        MongoClient mongoClient = null;

        try {
            MongoClientURI mongoClientURI = new MongoClientURI(connectionUrl);
            mongoClient = new MongoClient(mongoClientURI);

            if (databaseExist(mongoClient, mongoClientURI.getDatabase())) {
                return HealthCheckResponse.named(MongoHealthCheck.class.getSimpleName()).up().build();
            } else {
                LOG.severe("Mongo database not found.");
                return HealthCheckResponse.named(MongoHealthCheck.class.getSimpleName()).down().build();
            }
        } catch (Exception exception) {
            LOG.severe("Connection to Mongo couldn't be established. " + exception.getMessage());
            return HealthCheckResponse.named(MongoHealthCheck.class.getSimpleName()).down().build();
        } finally {
            if (mongoClient != null) {
                mongoClient.close();
            }

        }
    }

    private Boolean databaseExist(MongoClient mongoClient, String databaseName) {
        if (mongoClient != null && databaseName != null) {
            MongoCursor<String> mongoCursor = mongoClient.listDatabaseNames().iterator();

            while (mongoCursor.hasNext()) {
                if (mongoCursor.next().equals(databaseName))
                    return true;
            }
        }

        return false;
    }
}
