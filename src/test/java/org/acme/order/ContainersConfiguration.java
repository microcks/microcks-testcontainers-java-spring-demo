package org.acme.order;

import io.github.microcks.testcontainers.MicrocksContainersEnsemble;
import io.github.microcks.testcontainers.connection.KafkaConnection;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class ContainersConfiguration {

   private static Network network = Network.newNetwork();

   @Bean
   @ServiceConnection
   KafkaContainer kafkaContainer() {
      KafkaContainer kafkaContainer = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))
            .withNetwork(network)
            .withNetworkAliases("kafka")
            .withListener(() -> "kafka:19092");
      return kafkaContainer;
   }

   @Bean
   MicrocksContainersEnsemble microcksEnsemble(KafkaContainer kafkaContainer) {
      // Uncomment these lines (36-38) if you want to use the native image of Microcks
      // and comment the next MicrocksContainersEnsemble declaration line (40).
//      DockerImageName nativeImage = DockerImageName.parse("quay.io/microcks/microcks-uber:1.11.2-native")
//            .asCompatibleSubstituteFor("quay.io/microcks/microcks-uber:1.9.0");
//      MicrocksContainersEnsemble ensemble = new MicrocksContainersEnsemble(network, nativeImage)

      MicrocksContainersEnsemble ensemble = new MicrocksContainersEnsemble(network, "quay.io/microcks/microcks-uber:1.11.2")
            .withPostman()             // We need this to do contract-testing with Postman collection
            .withAsyncFeature()        // We need this for async mocking and contract-testing
            .withAccessToHost(true)   // We need this to access our webapp while it runs
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
