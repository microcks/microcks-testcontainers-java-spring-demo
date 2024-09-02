package org.acme.order;

import com.github.dockerjava.api.command.InspectContainerResponse;
import io.github.microcks.testcontainers.MicrocksContainersEnsemble;
import io.github.microcks.testcontainers.connection.KafkaConnection;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@TestConfiguration(proxyBeanMethods = false)
public class ContainersConfiguration {

   private static Network network = Network.newNetwork();

   private KafkaContainer kafkaContainer;


   @Bean
   @ServiceConnection
   KafkaContainer kafkaContainer() {
      kafkaContainer = new KafkaWithListenerContainer("apache/kafka-native:3.8.0")
              .withListener(() -> "kafka:19092")
              .withNetwork(network)
              .withNetworkAliases("kafka");
      return kafkaContainer;
   }

   @Bean
   MicrocksContainersEnsemble microcksEnsemble(DynamicPropertyRegistry registry) {
      // Uncomment these lines (36-38) if you want to use the native image of Microcks
      // and comment the next MicrocksContainersEnsemble declaration line (40).
//      DockerImageName nativeImage = DockerImageName.parse("quay.io/microcks/microcks-uber:1.10.0-native")
//            .asCompatibleSubstituteFor("quay.io/microcks/microcks-uber:1.9.0");
//      MicrocksContainersEnsemble ensemble = new MicrocksContainersEnsemble(network, nativeImage)

      MicrocksContainersEnsemble ensemble = new MicrocksContainersEnsemble(network, "quay.io/microcks/microcks-uber:1.10.0-native")
            .withPostman()             // We need this to do contract-testing with Postman collection
            .withAsyncFeature()        // We need this for async mocking and contract-testing
            .withAccessToHost(true)   // We need this to access our webapp while it runs
            .withKafkaConnection(new KafkaConnection("kafka:19092"))   // We need this to connect to Kafka
            .withMainArtifacts("order-service-openapi.yaml", "order-events-asyncapi.yaml", "third-parties/apipastries-openapi.yaml")
            .withSecondaryArtifacts("order-service-postman-collection.json", "third-parties/apipastries-postman-collection.json")
            .withAsyncDependsOn(kafkaContainer);   // We need this to be sure Kafka will be up before Microcks async minion

      // We need to replace the default endpoints with those provided by Microcks.
      registry.add("application.pastries-base-url",
              () -> ensemble.getMicrocksContainer().getRestMockEndpoint("API Pastries", "0.0.1"));
      registry.add("application.order-events-reviewed-topic",
              () -> ensemble.getAsyncMinionContainer().getKafkaMockTopic("Order Events API", "0.1.0", "PUBLISH orders-reviewed"));

      return ensemble;
   }

   static class KafkaWithListenerContainer extends KafkaContainer {

      private List<Supplier<String>> listeners = new ArrayList<>();

      public KafkaWithListenerContainer(String image) {
         super(DockerImageName.parse(image));
      }

      @Override
      protected void configure() {
         super.configure();
         withEnv("KAFKA_LISTENERS",
                 String.format("%s,%s", "INTERNAL://0.0.0.0:19092", getEnvMap().get("KAFKA_LISTENERS")));
         withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP",
                 String.format("%s,%s", "INTERNAL:PLAINTEXT", getEnvMap().get("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP")));
      }

      @Override
      protected void containerIsStarting(InspectContainerResponse containerInfo) {
         String command = "#!/bin/bash\n";
         // exporting KAFKA_ADVERTISED_LISTENERS with the container hostname
         command += String.format("export KAFKA_ADVERTISED_LISTENERS=%s,%s,%s\n",
                 String.format("INTERNAL://%s", listeners.get(0).get()), String.format("PLAINTEXT://%s", getBootstrapServers()),
                 String.format("BROKER://%s:%s", containerInfo.getConfig().getHostName(), "9093"));

         command += "/etc/kafka/docker/run \n";
         copyFileToContainer(Transferable.of(command, 0777), "/tmp/testcontainers_start.sh");
      }

      public KafkaWithListenerContainer withListener(Supplier<String> listener) {
         this.listeners.add(listener);
         return this;
      }
   }
}
