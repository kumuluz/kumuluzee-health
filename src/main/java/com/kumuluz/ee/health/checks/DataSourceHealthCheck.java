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
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.HealthCheckResponseBuilder;

import javax.enterprise.context.ApplicationScoped;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data source health check.
 *
 * @author Marko Škrjanec
 * @since 1.0.0
 */
@ApplicationScoped
@BuiltInHealthCheck
public class DataSourceHealthCheck extends KumuluzHealthCheck implements HealthCheck {

    private static final Logger LOG = Logger.getLogger(DataSourceHealthCheck.class.getName());

    @Override
    public HealthCheckResponse call() {
        HealthCheckResponseBuilder healthCheckResponseBuilder = HealthCheckResponse
                .named(DataSourceHealthCheck.class.getSimpleName())
                .up();

        List<DataSourceHealthCheckConfiguration> configurationList = getConfigurationList();

        for (DataSourceHealthCheckConfiguration configuration : configurationList) {
            checkConnection(configuration, healthCheckResponseBuilder);
        }

        return healthCheckResponseBuilder.build();
    }

    @Override
    public String name() {
        return kumuluzBaseHealthConfigPath + "data-source-health-check";
    }

    @Override
    public boolean initSuccess() {
        if (!DriverManager.getDrivers().hasMoreElements()) {
            LOG.severe("No database driver library appears to be provided.");
            return false;
        }

        return true;
    }

    private List<DataSourceHealthCheckConfiguration> getConfigurationList() {
        ConfigurationUtil configurationUtil = ConfigurationUtil.getInstance();

        List<DataSourceHealthCheckConfiguration> configurationList = new ArrayList<>();

        Optional<Integer> datasourcesListSizeOptional = ConfigurationUtil.getInstance().getListSize(name());

        boolean hasJndiNameReference = false;
        if (datasourcesListSizeOptional.isPresent()) {
            for (int i = 0; i < datasourcesListSizeOptional.get(); i++) {
                DataSourceHealthCheckConfiguration configuration = new DataSourceHealthCheckConfiguration();

                Optional<String> jndiNameOptional = configurationUtil.get(name() + "[" + i + "].jndi-name");
                if (jndiNameOptional.isPresent()) {
                    configuration.setJndiName(jndiNameOptional.get());

                    hasJndiNameReference = true;
                } else {
                    configurationUtil.get(name() + "[" + i + "].connection-url").ifPresent(configuration::setConnectionUrl);
                    configurationUtil.get(name() + "[" + i + "].username").ifPresent(configuration::setUsername);
                    configurationUtil.get(name() + "[" + i + "].password").ifPresent(configuration::setPassword);
                }

                configurationList.add(configuration);
            }
        } else {
            DataSourceHealthCheckConfiguration configuration = new DataSourceHealthCheckConfiguration();

            Optional<String> jndiNameOptional = configurationUtil.get(name() + ".jndi-name");
            if (jndiNameOptional.isPresent()) {
                configuration.setJndiName(jndiNameOptional.get());

                hasJndiNameReference = true;
            } else {
                configurationUtil.get(name() + ".connection-url").ifPresent(configuration::setConnectionUrl);
                configurationUtil.get(name() + ".username").ifPresent(configuration::setUsername);
                configurationUtil.get(name() + ".password").ifPresent(configuration::setPassword);
            }

            configurationList.add(configuration);
        }

        Optional<Integer> dsSizeOpt = configurationUtil.getListSize("kumuluzee.datasources");
        if (hasJndiNameReference && dsSizeOpt.isPresent()) {
            for (DataSourceHealthCheckConfiguration configuration : configurationList) {
                if (configuration.getJndiName() == null) {
                    continue;
                }

                for (int i = 0; i < dsSizeOpt.get(); i++) {
                    String prefix = "kumuluzee.datasources[" + i + "]";
                    Optional<String> dsJndiName = configurationUtil.get(prefix + ".jndi-name");

                    if (dsJndiName.isPresent() && dsJndiName.get().equals(configuration.getJndiName())) {
                        configurationUtil.get(prefix + ".connection-url").ifPresent(configuration::setConnectionUrl);
                        configurationUtil.get(prefix + ".username").ifPresent(configuration::setUsername);
                        configurationUtil.get(prefix + ".password").ifPresent(configuration::setPassword);
                        break;
                    }
                }
            }
        }

        return configurationList;
    }

    /**
     * Helper method for checking connection.
     */
    private void checkConnection(DataSourceHealthCheckConfiguration configuration, HealthCheckResponseBuilder healthCheckResponseBuilder) {
        Connection connection = null;

        try {
            if (configuration.getUsername() == null && configuration.getPassword() == null) {
                connection = DriverManager.getConnection(configuration.getConnectionUrl());
            } else {
                connection = DriverManager.getConnection(configuration.getConnectionUrl(), configuration.getUsername(), configuration.getPassword());
            }

            healthCheckResponseBuilder.withData(configuration.getConnectionUrl(), HealthCheckResponse.State.UP.toString());
        } catch (Exception exception) {
            LOG.log(Level.SEVERE, String.format("An exception occurred when trying to establish connection to data source (%s).", configuration.getConnectionUrl()), exception);
            healthCheckResponseBuilder.withData(configuration.getConnectionUrl(), HealthCheckResponse.State.DOWN.toString());
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException exception) {
                    LOG.log(Level.SEVERE, String.format("An exception occurred when trying to close connection to data source (%s).", configuration.getConnectionUrl()), exception);
                }
            }
        }
    }
}

class DataSourceHealthCheckConfiguration {

    private String jndiName;
    private String connectionUrl;
    private String username;
    private String password;

    public String getJndiName() {
        return jndiName;
    }

    public void setJndiName(String jndiName) {
        this.jndiName = jndiName;
    }

    public String getConnectionUrl() {
        return connectionUrl;
    }

    public void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
