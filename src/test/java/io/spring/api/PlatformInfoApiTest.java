package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.response.MockMvcResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class PlatformInfoApiTest {

  @Autowired private MockMvc mvc;

  @BeforeEach
  public void setUp() {
    RestAssuredMockMvc.mockMvc(mvc);
  }

  @Test
  public void should_get_platform_info_without_authentication() {
    MockMvcResponse response =
        given()
            .when()
            .get("/platform/info")
            .prettyPeek()
            .then()
            .statusCode(200)
            .body("$", hasKey("platform"))
            .body("platform.runtime.javaVersion", startsWith("17"))
            .body("platform.runtime.javaVendor", notNullValue())
            .body("platform.runtime.springBootVersion", startsWith("3."))
            .body("platform.runtime.springFrameworkVersion", startsWith("6."))
            .body("platform.baseline.javaVersion", equalTo("11"))
            .body("platform.baseline.springBootVersion", equalTo("2.6.3"))
            .body("platform.endpoints", everyItem(hasKey("method")))
            .body("platform.endpoints", everyItem(hasKey("path")))
            .body("platform.endpoints", everyItem(hasKey("controller")))
            .body("platform.dependencies", everyItem(hasKey("name")))
            .body("platform.dependencies", everyItem(hasKey("from")))
            .body("platform.dependencies", everyItem(hasKey("to")))
            .body("platform.dependencies.name", hasItem("Spring Boot"))
            .body("platform.dependencies.name", hasItem("Servlet / Validation API"))
            .extract()
            .response();

    List<Map<String, Object>> endpoints = response.jsonPath().getList("platform.endpoints");
    List<Map<String, Object>> dependencies = response.jsonPath().getList("platform.dependencies");
    Assertions.assertEquals(
        endpoints.size(), response.jsonPath().getInt("platform.summary.endpointCount"));
    Assertions.assertEquals(
        dependencies.size(),
        response.jsonPath().getInt("platform.summary.upgradedDependencyCount"));
    Assertions.assertTrue(
        endpoints.stream().anyMatch(e -> isEndpoint(e, "GET", "/platform/info", "PlatformInfoApi")),
        "GET /platform/info should be listed");
    Assertions.assertTrue(
        endpoints.stream().anyMatch(e -> isEndpoint(e, "POST", "/users/login", "UsersApi")),
        "POST /users/login should be listed");
  }

  private static boolean isEndpoint(
      Map<String, Object> endpoint, String method, String path, String controller) {
    return method.equals(endpoint.get("method"))
        && path.equals(endpoint.get("path"))
        && controller.equals(endpoint.get("controller"));
  }
}
