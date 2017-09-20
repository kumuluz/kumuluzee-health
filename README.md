# KumuluzEE Health
[![Build Status](https://img.shields.io/travis/kumuluz/kumuluzee-health/master.svg?style=flat)](https://travis-ci.org/kumuluz/kumuluzee-health)

> KumuluzEE Health extension provides consistent, unified way of performing microservice health checks and exposing health information.

KumuluzEE Health is a health check extension for the KumuluzEE microservice framework. Is provides easy, consistent and unified way of performing health checking on microservices and exposing health information to be used by monitoring and container orchestration environments such as Kubernetes. KumuluzEE Health is fully compliant with Kubernetes and has been extensively tested to work in Kubernetes.
KumuluzEE Health is compliant with the [MicroProfile Service Health Checks specification 1.0]( https://github.com/eclipse/microprofile-health).
KumuluzEE Health exposes a `/health` endpoint (customizable), which returns the health check status of the microservice.

## Usage
You can enable the KumuluzEE Health module by adding the following dependencies:

```xml
<dependency>
    <groupId>com.kumuluz.ee.health</groupId>
    <artifactId>kumuluzee-health</artifactId>
    <version>${kumuluzee-health.version}</version>
</dependency>
```

CDI dependency is a prerequisite. Please refer to KumuluzEE [readme]( https://github.com/kumuluz/kumuluzee/) for more information. 

## Health checks

To check health of a microservice, you can use the provided health checks or you can define your own health checks.

## Built-in health checks

The following health checks are available out-of-the-box: 

- **DataSourceHealthCheck** for checking the availability of the data source
- **DiskSpaceHealthCheck** for checking available disk space against a threshold
- **MongoHealthCheck** for checking the availability of Mongo database
- **RedisHealthCheck** for checking the availability of Redis store

Additional built-in health check will be provided (contributions are welcome).

## Implementing custom health checks

There are two ways how we can implement a custom health check.
* We can use the `@Health` annotation to define health check classes.
* We can implement health check classes and register them manually.

### @Health annotation

To implement health checks using `@Health` annotation, we have to implement a CDI bean class, which implements the `HealthCheck` interface. Such health checks are auto antically discoverd and registered to the `HealthRegistry`.

Shown below is an example of a CDI bean health check using `@Health` annotation:

```java
import org.eclipse.microprofile.health.Health;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;

import javax.enterprise.context.ApplicationScoped;

@Health
@ApplicationScoped
public class SuccessfulHealthCheckBean implements HealthCheck {

    public HealthCheckResponse call() {
        return HealthCheckResponse.named(SuccessfulHealthCheckBean.class.getSimpleName()).up().build();
    }

}
```

### Health check implemented as classes

To implement a health check with a custom class, the class has to implement the `HealthCheck` interface. Such class has to be manually registered with the `HealthRegistry`.

Shown below is a custom health check implementation. It checks if the KumuluzEE GitHub page is accessible. 

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

To register a custom health check class we have to use the `HealthRegistry` instance. We provide the health check unique name and an instance of the health check class.

```java
HealthRegistry.getInstance().register(GithubHealthCheck.class.getSimpleName(), new GithubHealthCheck());
```

### Unregistering custom health checks

To unregister custom health checks we can use the `HealthRegistry` instance and provide the health check unique name.

```java
HealthRegistry.getInstance().unregister(GithubHealthCheck.class.getSimpleName());
```

## Retrieving health check results

To invoke the health check and retrieve the result we can use the `HealthRegistry` instance. The results will be returned in a list of health check responses.

```java
List<HealthCheckResponse> results = HealthRegistry.getInstance().getResults();
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
    "name" : "DataSourceHealthCheck",
    "state" : "UP"
  }, {
    "name" : "DiskSpaceHealthCheck",
    "state" : "UP"
  }, {
    "name" : "MongoHealthCheck",
    "state" : "UP"
  }, {
    "name" : "RedisHealthCheck",
    "state" : "UP"
  } ]
}
```

The URL also accepts a query parameter `pretty=false` (http://IP:PORT/health?pretty=false) which results in a single line response, payload example is provided below:

```json
{"outcome":"UP","checks":[{"name":"DataSourceHealthCheck","state":"UP"},{"name":"DiskSpaceHealthCheck","state":"UP"},{"name":"MongoHealthCheck","state":"UP"},{"name":"RedisHealthCheck","state":"UP"}]}
```

## Configuring health checks

The provide access to health check via URL, the health servlet is registered automatically on path `/health`. However, custom servlet mapping can be specified by providing servlet mapping location in the configuration.

Example of the configuration:

```yaml
kumuluzee:
  health:
    servlet:
      mapping: /health
```

## Configuration of built-in health checks

To configure built-in health checks, we can use the configuration parameters listed below for each built-in health check.

### DataSourceHealthCheck

To enable data source availability health check, we need to provide in the health check sections. `Jndi-name`, `connection-url`, `username` and `password` need to be provided as part of the health check configuration.

Example configuration:

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

To enable disk space health check, we need to provide the health check config parameters, listed below. The default disk space threshold is `100MB`, but can be overwritten by providing your own threshold.

Example of the configuration:

```yaml
kumuluzee:
  health:
    checks:
      disk-space-health-check:
        threshold: 100000000
```

### MongoHealthCheck

To enable the Mongo database health check, we need to provide the `connection-url` config parameter with user, password, database name and other options need to be provided as part of the health check configuration as described in the [mongo-java-driver-documentation](https://mongodb.github.io/mongo-java-driver/3.5/javadoc/com/mongodb/MongoClientURI.html). The default connection-url is `mongodb://localhost:27017/local?serverSelectionTimeoutMS=2000`.

Example of the configuration:

```yaml
kumuluzee:
  health:
    checks:
      mongo-health-check:
        connection-url: mongodb://user:password@localhost:27017/customers?serverSelectionTimeoutMS=2000
```

### RedisHealthCheck

To enable Redis store health check, we need to specify the `connection-url` with port, secret and database number as a part of the health check configuration. The default connection-url is `redis://localhost:6379/0`.

Example of the configuration:

```yaml
kumuluzee:
  health:
    checks:
      redis-health-check:
        connection-url: redis://:secret@localhost:6379/0
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
