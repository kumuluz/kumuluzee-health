# KumuluzEE Health
[![Build Status](https://img.shields.io/travis/kumuluz/kumuluzee-health/master.svg?style=flat)](https://travis-ci.org/kumuluz/kumuluzee-health)

> Health extension in compliance with microprofile-health specification provides you with a consistent, unified way of performing microservice health checks for the lightweight KumuluzEE framework.

KumuluzEE Health is a health check extension for the KumuluzEE microservice framework. Is was specifically designed for checking the health of microservices.
KumuluzEE Health is compliant with MicroProfile Healthcheck specification 1.0 (preliminary).
KumuluzEE Health exposes a `/health` endpoint, which returns the health check status of the microservice.

## Usage
You can enable the health module by adding the following dependencies:

```xml
<dependency>
    <groupId>com.kumuluz.ee.health</groupId>
    <artifactId>kumuluzee-cdi-weld</artifactId>
    <version>${kumuluzee.version}</version>
</dependency>
<dependency>
    <groupId>com.kumuluz.ee.health</groupId>
    <artifactId>kumuluzee-health</artifactId>
    <version>${kumuluzee-health.version}</version>
</dependency>
```

## Health checks

To check health of a microservice, you can either use the provided health checks, or you can define your own health checks.

The following health checks are available out-of-the-box: 

- **DataSourceHealthCheck** for checking the availability of the data source
- **DiskSpaceHealthCheck** for checking available disk space against a threshold
- **MongoHealthCheck** for checking the availability of mongo database
- **RedisHealthCheck** for checking the availability of redis store

### Implementing custom health checks

Bellow is an example of a custom health check implementation in which we check if the KumuluzEE github page is accessible: 

```java
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

public class GithubHealthCheck implements HealthCheck {

    private static final String url = "https://github.com/kumuluz/kumuluzee";

    private static final Logger LOG = Logger.getLogger(GithubHealthCheck.class.getSimpleName());

    @Override
    public HealthCheckResponse call() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("HEAD");

            if (connection.getResponseCode() == 200) {
                return HealthCheckResponse.named(GithubHealthCheck.class.getSimpleName()).up().build();
            }
        } catch (Exception exception) {
            LOG.severe(exception.getMessage());
        }
        return HealthCheckResponse.named(GithubHealthCheck.class.getSimpleName()).down().build();
    }
}
```

### Registering custom health checks

Registering custom health checks can be done via HealthRegistry instance by providing health check unique name and an instance of the health check class.

```java
HealthRegistry.getInstance().register(GithubHealthCheck.class.getSimpleName(), new GithubHealthCheck());
```

### Unregistering custom health checks

Unregistering custom health checks can be done via HealthRegistry instance by providing a health check unique name.

```java
HealthRegistry.getInstance().unregister(GithubHealthCheck.class.getSimpleName());
```

### Retrieving health check results

Retrieving health check results can be done via HealthRegistry instance. The results will be returned in a list of health check responses.

```java
List<HealthCheckResponse> results = HealthRegistry.getInstance().getResults();
```

### Integration with CDI

CDI beans which implement `HealthCheck` and are annotated with `@Health` are discover and registered to the HealthRegistry automatically.

Bellow is an example of such a bean:

```java
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Logger;

public class GithubHealthCheck implements HealthCheck {

    private static final String url = "https://github.com/kumuluz/kumuluzee";

    private static final Logger LOG = Logger.getLogger(GithubHealthCheck.class.getSimpleName());

    @Override
    public HealthCheckResponse call() {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("HEAD");

            if (connection.getResponseCode() == 200) {
                return HealthCheckResponse.named(GithubHealthCheck.class.getSimpleName()).up().build();
            }
        } catch (Exception exception) {
            LOG.severe(exception.getMessage());
        }
        return HealthCheckResponse.named(GithubHealthCheck.class.getSimpleName()).down().build();
    }
}
```

## Configuration of health checks

The health servlet is registered automatically on path `/health`. However, custom servlet mapping can be specified by providing servlet mapping location in the configuration file.

Example of the configuration:

```yaml
kumuluzee:
  health:
    servlet:
      mapping: /health
```

### DataSourceHealthCheck

To enable data source availability health check the check needs to be provided in the health checks sections in the configuration file. Jndi-name or connection-url, username and password need to be provided as part of the health check configuration.

Example of the configuration:

```yaml
kumuluzee:
  datasources:
    - jndi-name: jdbc/CustomersDS
      connection-url: jdbc:postgresql://localhost:5432/customers
      username: postgres
      password: postgres
      max-pool-size: 20
  health:
    checks:
      data-source-health-check:
        jndi-name: jdbc/CustomersDS
```

Another example of the configuration:

```yaml
kumuluzee:
  health:
    checks:
      data-source-health-check:
        connection-url: jdbc:db2://localhost:5021/customers
        username: db2
        password: db2
```

Another example of the configuration:

```yaml
kumuluzee:
  health:
    checks:
      data-source-health-check:
        connection-url: jdbc:mysql://localhost:3306/customers?user=mysql&password=mysql
```

### DiskSpaceHealthCheck

To enable disk space health check the check needs to be provided in the health checks sections in the configuration file. The default disk space threshold is `100MB`, but can be overwritten by providing threshold.

Example of the configuration:

```yaml
kumuluzee:
  health:
    checks:
      disk-space-health-check:
        threshold: 100000000
```

### MongoHealthCheck

To enable mongo database health check the check needs to be provided in the health checks sections in the configuration file. Connection-url with user, password, database name and other options need to be provided as part of the health check configuration as described in the [mongo-java-driver-documentation](https://mongodb.github.io/mongo-java-driver/3.5/javadoc/com/mongodb/MongoClientURI.html). The default connection-url is `mongodb://localhost:27017/local?serverSelectionTimeoutMS=2000`.

Example of the configuration:

```yaml
kumuluzee:
  health:
    checks:
      mongo-health-check:
        connection-url: mongodb://user:password@localhost:27017/customers?serverSelectionTimeoutMS=2000
```

### RedisHealthCheck

To enable redis store health check the check needs to be provided in the health checks sections in the configuration file. Connection-url with port, secret and database number need to be provided as part of the health check configuration. The default connection-url is `redis://localhost:6379/0`.

Example of the configuration:

```yaml
kumuluzee:
  health:
    checks:
      redis-health-check:
        connection-url: redis://:secret@localhost:6379/0
```

## /health endpoint output

The `/health` endpoint output returns:
- 200 with payload, when health checks are defined with positive outcome or are not defined
- 503 with payload, when health checks are defined, but at least one outcome is negative
- 500 without payload, when an exception occurred in the procedure of health checking

The health check is available on http://IP:PORT/health by default, payload example is provided below:

```json
{
  "outcome" : "UP",
  "checks" : [ {
    "id" : "DataSourceHealthCheck",
    "result" : "UP"
  }, {
    "id" : "DiskSpaceHealthCheck",
    "result" : "UP"
  }, {
    "id" : "MongoHealthCheck",
    "result" : "UP"
  }, {
    "id" : "RedisHealthCheck",
    "result" : "UP"
  } ]
}
```

The URL also accepts a query parameter `pretty=false` (http://IP:PORT/health?pretty=false) which results in a single line response, payload example is provided below:

```json
{"outcome":"UP","checks":[{"id":"DataSourceHealthCheck","result":"UP"},{"id":"DiskSpaceHealthCheck","result":"UP"},{"id":"MongoHealthCheck","result":"UP"},{"id":"RedisHealthCheck","result":"UP"}]}
```

**Build the microservice**

Ensure you have JDK 8 (or newer), Maven 3.2.1 (or newer) and Git installed.
    
Build the health library with command:

```bash
mvn install
```
    
Build archives are located in the modules respected folder `target` and local repository `.m2`.

**Run the microservice**

Use the following command to run the sample from Windows CMD:
```
java -cp target/classes;target/dependency/* com.kumuluz.ee.EeApplication 
```

## Changelog

Recent changes can be viewed on Github on the [Releases Page](https://github.com/TFaga/KumuluzEE/releases)

## Contribute

See the [contributing docs](https://github.com/kumuluz/kumuluzee-health/blob/master/CONTRIBUTING.md)

When submitting an issue, please follow the [guidelines](https://github.com/kumuluz/kumuluzee-health/blob/master/CONTRIBUTING.md#bugs).

When submitting a bugfix, write a test that exposes the bug and fails before applying your fix. Submit the test alongside the fix.

When submitting a new feature, add tests that cover the feature.

## License

MIT
