package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.path.json.JsonPath;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
  public void should_get_platform_info_without_auth() {
    given()
        .when()
        .get("/platform/info")
        .then()
        .statusCode(200)
        .body("$", not(hasKey("platformInfo")))
        .body("runtime.java", startsWith("17"))
        .body("runtime.springBoot", startsWith("3."))
        .body("dependencies.size()", not(equalTo(0)))
        .body("dependencies.name", everyItem(not(equalTo(""))))
        .body("dependencies.before", everyItem(not(equalTo(""))))
        .body("dependencies.after", everyItem(not(equalTo(""))))
        .body("dependencies.name", hasItem("Spring Boot"))
        .body("endpoints.path", everyItem(startsWith("/")))
        .body("endpoints.method", everyItem(not(equalTo(""))))
        .body(
            "endpoints.find { it.method == 'GET' && it.path == '/platform/info' }.path",
            equalTo("/platform/info"))
        .body(
            "endpoints.find { it.method == 'POST' && it.path == '/users/login' }.path",
            equalTo("/users/login"));
  }

  @Test
  public void should_list_endpoints_sorted_by_path_then_method() {
    JsonPath json =
        given().when().get("/platform/info").then().statusCode(200).extract().jsonPath();
    List<Map<String, String>> endpoints = json.getList("endpoints");
    assertFalse(endpoints.isEmpty());
    List<Map<String, String>> sorted = new ArrayList<>(endpoints);
    sorted.sort(
        Comparator.comparing((Map<String, String> e) -> e.get("path"))
            .thenComparing(e -> e.get("method")));
    assertEquals(sorted, endpoints);
    for (Map<String, String> endpoint : endpoints) {
      assertTrue(endpoint.containsKey("method"));
      assertTrue(endpoint.get("path").startsWith("/"));
    }
  }
}
