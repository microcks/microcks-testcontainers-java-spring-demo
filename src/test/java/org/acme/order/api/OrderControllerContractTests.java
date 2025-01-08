package org.acme.order.api;

import io.github.microcks.testcontainers.model.RequestResponsePair;
import io.github.microcks.testcontainers.model.TestRequest;
import io.github.microcks.testcontainers.model.TestResult;
import io.github.microcks.testcontainers.model.TestRunnerType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.acme.order.BaseIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderControllerContractTests extends BaseIntegrationTest {

   @Test
   void testOpenAPIContract() throws Exception {
      // Ask for an Open API conformance to be launched.
      TestRequest testRequest = new TestRequest.Builder()
            .serviceId("Order Service API:0.1.0")
            .runnerType(TestRunnerType.OPEN_API_SCHEMA.name())
            .testEndpoint("http://host.testcontainers.internal:" + port + "/api")
            .build();

      TestResult testResult = microcksEnsemble.getMicrocksContainer().testEndpoint(testRequest);

      // You may inspect complete response object with following:
      ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
      System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(testResult));

      assertTrue(testResult.isSuccess());
      assertEquals(1, testResult.getTestCaseResults().size());
   }

   @Test
   void testOpenAPIContractAndBusinessConformance() throws Exception {
      // Ask for an Open API conformance to be launched.
      TestRequest testRequest = new TestRequest.Builder()
            .serviceId("Order Service API:0.1.0")
            .runnerType(TestRunnerType.OPEN_API_SCHEMA.name())
            .testEndpoint("http://host.testcontainers.internal:" + port + "/api")
            .build();

      TestResult testResult = microcksEnsemble.getMicrocksContainer().testEndpoint(testRequest);

      // You may inspect complete response object with following:
      ObjectMapper mapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
      System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(testResult));

      assertTrue(testResult.isSuccess());
      assertEquals(1, testResult.getTestCaseResults().size());

      // You may also check business conformance.
      List<RequestResponsePair> pairs = microcksEnsemble.getMicrocksContainer().getMessagesForTestCase(testResult, "POST /orders");
      for (RequestResponsePair pair : pairs) {
         if ("201".equals(pair.getResponse().getStatus())) {
            Map<String, Object> requestMap = mapper.readValue(pair.getRequest().getContent(), new TypeReference<>() {});
            Map<String, Object> responseMap = mapper.readValue(pair.getResponse().getContent(), new TypeReference<>() {});

            List<Map<String, Object>> requestPQ = (List<Map<String, Object>>) requestMap.get("productQuantities");
            List<Map<String, Object>> responsePQ = (List<Map<String, Object>>) responseMap.get("productQuantities");

            assertEquals(requestPQ.size(), responsePQ.size());
            for (int i = 0; i < requestPQ.size(); i++) {
               assertEquals(requestPQ.get(i).get("productName"), responsePQ.get(i).get("productName"));
            }
         }
      }
   }
}
