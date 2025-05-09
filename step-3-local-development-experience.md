# Step 3: Local development experience with Microcks

Our application uses Kafka and external dependencies.

Currently, if you run the application from your terminal, you will see the following error:

```shell
./mvnw spring-boot:run  

[INFO] Attaching agents: []

  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/

 :: Spring Boot ::                (v3.4.5)

2025-05-09T17:00:57.949+02:00  INFO 73264 --- [  restartedMain] org.acme.order.OrderServiceApplication   : Starting OrderServiceApplication using Java 17.0.6 with PID 73264 (/Users/laurent/Development/github/microcks-testcontainers-java-spring-demo/target/classes started by laurent in /Users/laurent/Development/github/microcks-testcontainers-java-spring-demo)
2025-05-09T17:00:57.952+02:00  INFO 73264 --- [  restartedMain] org.acme.order.OrderServiceApplication   : No active profile set, falling back to 1 default profile: "default"
[...]
2025-05-09T17:00:58.788+02:00  INFO 73264 --- [  restartedMain] o.a.kafka.common.utils.AppInfoParser     : Kafka version: 3.8.1
2025-05-09T17:00:58.788+02:00  INFO 73264 --- [  restartedMain] o.a.kafka.common.utils.AppInfoParser     : Kafka commitId: 70d6ff42debf7e17
2025-05-09T17:00:58.788+02:00  INFO 73264 --- [  restartedMain] o.a.kafka.common.utils.AppInfoParser     : Kafka startTimeMs: 1746802858787
2025-05-09T17:00:58.789+02:00  INFO 73264 --- [  restartedMain] o.a.k.c.c.internals.LegacyKafkaConsumer  : [Consumer clientId=consumer-order-service-1, groupId=order-service] Subscribed to topic(s): orders-reviewed
2025-05-09T17:00:58.797+02:00  INFO 73264 --- [  restartedMain] org.acme.order.OrderServiceApplication   : Started OrderServiceApplication in 0.995 seconds (process running for 1.165)
2025-05-09T17:00:58.870+02:00  INFO 73264 --- [ntainer#0-0-C-1] org.apache.kafka.clients.NetworkClient   : [Consumer clientId=consumer-order-service-1, groupId=order-service] Node -1 disconnected.
2025-05-09T17:00:58.871+02:00  WARN 73264 --- [ntainer#0-0-C-1] org.apache.kafka.clients.NetworkClient   : [Consumer clientId=consumer-order-service-1, groupId=order-service] Connection to node -1 (localhost/127.0.0.1:9092) could not be established. Node may not be available.
[...]
```

To run the application locally, we need to have a Kafka broker up and running + the other dependencies corresponding to our Pastry API provider and reviewing system.

Instead of installing these services on our local machine, or using Docker to run these services manually,
we will use [Spring Boot support for Testcontainers at Development Time](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#features.testing.testcontainers.at-development-time) to provision these services automatically.

> **NOTE**
>
> Before Spring Boot 3.1.0, Testcontainers libraries are mainly used for testing.
Spring Boot 3.1.0 introduced out-of-the-box support for Testcontainers which not only simplified testing,
but we can use Testcontainers for local development as well.
>
> To learn more, please read [Spring Boot Application Testing and Development with Testcontainers](https://www.atomicjar.com/2023/05/spring-boot-3-1-0-testcontainers-for-testing-and-local-development/)

In order to see what's needed to run this, you may check the `pom.xml` file.


## Review ContainersConfiguration class under src/test/java/org/acme/order

In order to specify the dependant services we need, we use a specific Spring `Configuration` class located into `/src/test/java``

Let's review `ContainersConfiguration` class under `src/test/java/org/acme/order` to configure the required containers.

```java
package org.acme.order;

import io.github.microcks.testcontainers.MicrocksContainersEnsemble;
import io.github.microcks.testcontainers.connection.KafkaConnection;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class ContainersConfiguration {

   private static Network network = Network.newNetwork();
   
   @Bean
   @ServiceConnection
   KafkaContainer kafkaContainer() {
      kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))
            .withNetwork(network)
            .withNetworkAliases("kafka")
            .withListener(() -> "kafka:19092");
      return kafkaContainer;
   }

   @Bean
   MicrocksContainersEnsemble microcksEnsemble(KafkaContainer kafkaContainer) {
      MicrocksContainersEnsemble ensemble = new MicrocksContainersEnsemble(network, "quay.io/microcks/microcks-uber:1.11.2")
            .withPostman()             // We need this to do contract-testing with Postman collection
            .withAsyncFeature()        // We need this for async mocking and contract-testing
            .withAccessToHost(true)    // We need this to access our webapp while it runs
            .withKafkaConnection(new KafkaConnection("kafka:19092"))   // We need this to connect to Kafka
            .withMainArtifacts("order-service-openapi.yaml", "order-events-asyncapi.yaml", "third-parties/apipastries-openapi.yaml")
            .withSecondaryArtifacts("order-service-postman-collection.json", "third-parties/apipastries-postman-collection.json")
            .withAsyncDependsOn(kafkaContainer);   // We need this to be sure Kafka will be up before Microcks async minion

      return ensemble;
   }

   @Bean
   public DynamicPropertyRegistrar endpointsProperties(MicrocksContainersEnsemble ensemble) {
      // We need to replace the default endpoints with those provided by Microcks.
      return (properties) -> {
         properties.add("application.pastries-base-url", () -> ensemble.getMicrocksContainer()
               .getRestMockEndpoint("API Pastries", "0.0.1"));
         properties.add("application.order-events-reviewed-topic", () -> ensemble.getAsyncMinionContainer()
               .getKafkaMockTopic("Order Events API", "0.1.0", "PUBLISH orders-reviewed"));
      };
   }
}
```

Let's understand what this configuration class does:

* `@TestConfiguration` annotation indicates that this configuration class defines the beans that can be used for Spring Boot tests.
* Spring Boot provides `ServiceConnection` support `KafkaConnectionDetails` out-of-the-box.
  So, we configured `KafkaContainer` as beans with `@ServiceConnection` annotation.
  This configuration will automatically start these containers and register the **Kafka** connection properties automatically.
* We also configure a `MicrocksContainersEnsemble` that will be responsible for providing mocks for our 3rd party systems.
  As REST Client URL properties are not standard ones, Microcks does not contribute any `ServiceConnection`. 
* Instead, we have  the ability to use the `DynamicPropertyRegistrar` to wire our application properties corresponding to REST Client URL 
  and Kafka Topic name. This way our application is using the endpoints that are provided by Microcks.

And that's it! 🎉 You don't need to download and install extra-things, or clone other repositories and figure out how to start your dependant services. 

## Review TestOrderServiceApplication class under src/test/java/org/acme/order

Next, let's create a `TestOrderServiceApplication` class under `src/test/java` to start the application with the containers configuration.

```java
package org.acme.order;

import org.springframework.boot.SpringApplication;

/**
 * A Test instance of the OrderServiceApplication.
 * @author laurent
 */
class TestOrderServiceApplication {

   public static void main(String[] args) {
      SpringApplication.from(OrderServiceApplication::main)
            .with(ContainersConfiguration.class)
            .run(args);
   }
}
```

Run the `TestOrderServiceApplication` from our IDE and verify that the application starts successfully. 🙌

You should see the container startups messages into the logs:

```shell
[...]
16:56:24.419 [main] INFO  org.testcontainers.DockerClientFactory - Checking the system...
16:56:24.419 [main] INFO  org.testcontainers.DockerClientFactory - ✔︎ Docker server version should be at least 1.6.0
16:56:24.423 [main] INFO  tc.confluentinc/cp-kafka:7.5.0 - Creating container for image: confluentinc/cp-kafka:7.5.0
16:56:24.500 [main] INFO  tc.confluentinc/cp-kafka:7.5.0 - Container confluentinc/cp-kafka:7.5.0 is starting: 44c1859c5f78528e2bca9868e52731ef5148f99f6142bbe0cb8ba9e23197199b
16:56:27.355 [main] INFO  tc.confluentinc/cp-kafka:7.5.0 - Container confluentinc/cp-kafka:7.5.0 started in PT2.931612S
16:56:27.355 [main] INFO  tc.quay.io/microcks/microcks-uber:1.11.2 - Creating container for image: quay.io/microcks/microcks-uber:1.11.2
16:56:27.409 [main] INFO  tc.testcontainers/sshd:1.2.0 - Creating container for image: testcontainers/sshd:1.2.0
16:56:27.433 [main] INFO  tc.testcontainers/sshd:1.2.0 - Container testcontainers/sshd:1.2.0 is starting: 03f99cba3223ce9d2b972462f9ca0167b8974d1f0376e8f99867a454aae2f006
16:56:27.644 [main] INFO  tc.testcontainers/sshd:1.2.0 - Container testcontainers/sshd:1.2.0 started in PT0.235162S
16:56:27.733 [main] INFO  tc.quay.io/microcks/microcks-uber:1.11.2 - Container quay.io/microcks/microcks-uber:1.11.2 is starting: e51a3a42b29c2dae3efbd0766a43ff04f031cec4774e9f455a549f18d44910ae
16:56:30.560 [main] INFO  tc.quay.io/microcks/microcks-uber:1.11.2 - Container quay.io/microcks/microcks-uber:1.11.2 started in PT3.205068S
16:56:30.893 [main] INFO  tc.quay.io/microcks/microcks-postman-runtime:latest - Creating container for image: quay.io/microcks/microcks-postman-runtime:latest
16:56:30.937 [main] INFO  tc.quay.io/microcks/microcks-postman-runtime:latest - Container quay.io/microcks/microcks-postman-runtime:latest is starting: 32c1bb9511ffda7ecd35c16eb90251301980a0c047ede0b4fed8b068040cfee7
16:56:31.342 [main] INFO  tc.quay.io/microcks/microcks-postman-runtime:latest - Container quay.io/microcks/microcks-postman-runtime:latest started in PT0.448918S
16:56:31.345 [main] INFO  tc.quay.io/microcks/microcks-uber-async-minion:1.11.2 - Creating container for image: quay.io/microcks/microcks-uber-async-minion:1.11.2
16:56:31.376 [main] INFO  tc.quay.io/microcks/microcks-uber-async-minion:1.11.2 - Container quay.io/microcks/microcks-uber-async-minion:1.11.2 is starting: 4fb71aac11a9b55fbade81ae504e2f934db68de9a644d242f81410a724506478
16:56:32.944 [main] INFO  tc.quay.io/microcks/microcks-uber-async-minion:1.11.2 - Container quay.io/microcks/microcks-uber-async-minion:1.11.2 started in PT1.598934S
[...]
```

Now, you can invoke the APIs using CURL or Postman or any of your favourite HTTP Client tools.

## Create an order

```shell
curl -XPOST localhost:8080/api/orders -H 'Content-type: application/json' \
    -d '{"customerId": "lbroudoux", "productQuantities": [{"productName": "Millefeuille", "quantity": 1}], "totalPrice": 5.1}' -v
```

You should get a response similar to the following:

```shell
< HTTP/1.1 201 
< Content-Type: application/json
< Transfer-Encoding: chunked
< Date: Mon, 29 Jan 2024 17:15:42 GMT
< 
* Connection #0 to host localhost left intact
{"id":"2da3a517-9b3b-4788-81b5-b1a1aac71746","status":"CREATED","customerId":"lbroudoux","productQuantities":[{"productName":"Millefeuille","quantity":1}],"totalPrice":5.1}%
```

Now test with something else, requesting for another Pastry:

```shell
curl -XPOST localhost:8080/api/orders -H 'Content-type: application/json' \
    -d '{"customerId": "lbroudoux", "productQuantities": [{"productName": "Eclair Chocolat", "quantity": 1}], "totalPrice": 4.1}' -v
```

This time you get another "exception" response:

```shell
< HTTP/1.1 422 
< Content-Type: application/json
< Transfer-Encoding: chunked
< Date: Mon, 29 Jan 2024 17:19:08 GMT
< 
* Connection #0 to host localhost left intact
{"productName":"Eclair Chocolat","details":"Pastry Eclair Chocolat is not available"}%
```

and this is because Microcks has created different simulations for the Pastry API 3rd party API based on API artifacts we loaded.
Check the `src/test/resources/third-parties/apipastries-openapi.yaml` and `src/test/resources/third-parties/apipastries-postman-collection.json` files to get details.

### 
[Next](step-4-write-rest-tests.md)