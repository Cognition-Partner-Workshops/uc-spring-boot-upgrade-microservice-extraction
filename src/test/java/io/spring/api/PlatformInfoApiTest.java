package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.module.mockmvc.response.MockMvcResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    MockMvcResponse response = given().accept("application/json").when().get("/platform/info");
    response.then().statusCode(200).contentType("application/json");

    Map<String, Object> body = response.jsonPath().getMap("$");
    Assertions.assertEquals(Set.of("runtime", "dependencies", "endpoints"), body.keySet());
  }

  @Test
  public void should_report_upgraded_runtime() {
    given()
        .when()
        .get("/platform/info")
        .then()
        .statusCode(200)
        .body("runtime.javaVersion", startsWith("17"))
        .body("runtime.springBoot", startsWith("3.3"))
        .body("runtime.springFramework", startsWith("6."))
        .body("runtime.javaVendor", not(nullValue()))
        .body("runtime.javaRuntimeName", not(nullValue()))
        .body("runtime.osName", not(nullValue()))
        .body("runtime.osArch", not(nullValue()))
        .body("runtime.startedAt", endsWithZulu())
        .body("runtime.uptimeSeconds", greaterThanOrEqualTo(0))
        .body("runtime.activeProfiles", not(nullValue()));
  }

  @Test
  public void should_list_before_and_after_dependency_versions() {
    given()
        .when()
        .get("/platform/info")
        .then()
        .statusCode(200)
        .body("dependencies.name", hasItem("Java"))
        .body("dependencies.find { it.name == 'Java' }.before", equalTo("11"))
        .body("dependencies.find { it.name == 'Java' }.after", equalTo("17"))
        .body("dependencies.name", hasItem("Spring Boot"))
        .body("dependencies.find { it.name == 'Spring Boot' }.before", equalTo("2.6.3"))
        .body("dependencies.find { it.name == 'Spring Boot' }.after", startsWith("3.3"))
        .body("dependencies[0].name", equalTo("Java"))
        .body("dependencies.before", everyItem(not(nullValue())))
        .body("dependencies.after", everyItem(not(nullValue())));
  }

  @Test
  public void should_list_rest_endpoints_from_handler_mapping() {
    MockMvcResponse response = given().when().get("/platform/info");
    response.then().statusCode(200);

    List<Map<String, String>> endpoints = response.jsonPath().getList("endpoints");
    Assertions.assertTrue(
        endpoints.stream()
            .anyMatch(
                endpoint ->
                    endpoint.get("method").equals("GET")
                        && endpoint.get("path").equals("/tags")
                        && endpoint.get("handler").equals("TagsApi#getTags")));
    Assertions.assertTrue(
        endpoints.stream()
            .anyMatch(
                endpoint ->
                    endpoint.get("method").equals("GET")
                        && endpoint.get("path").equals("/platform/info")
                        && endpoint.get("handler").equals("PlatformInfoController#platformInfo")));
    Assertions.assertTrue(
        endpoints.stream().noneMatch(endpoint -> endpoint.get("path").equals("/error")));
    Assertions.assertTrue(
        endpoints.stream().noneMatch(endpoint -> endpoint.get("path").startsWith("/graphql")));
    Assertions.assertTrue(
        endpoints.stream().allMatch(endpoint -> endpoint.get("path").startsWith("/")));

    List<Map<String, String>> sorted =
        endpoints.stream()
            .sorted(
                (a, b) -> {
                  int byPath = a.get("path").compareTo(b.get("path"));
                  return byPath != 0 ? byPath : a.get("method").compareTo(b.get("method"));
                })
            .toList();
    Assertions.assertEquals(sorted, endpoints);

    response
        .then()
        .body(
            "endpoints*.keySet().flatten().unique()",
            containsInAnyOrder("method", "path", "handler"));
  }

  private static org.hamcrest.Matcher<String> endsWithZulu() {
    return org.hamcrest.Matchers.endsWith("Z");
  }
}
