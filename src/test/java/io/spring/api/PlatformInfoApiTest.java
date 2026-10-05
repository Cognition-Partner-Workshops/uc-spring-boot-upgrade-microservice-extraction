package io.spring.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.path.json.JsonPath;
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
  public void should_get_platform_info_without_authorization() {
    JsonPath json =
        given()
            .when()
            .get("/platform/info")
            .prettyPeek()
            .then()
            .statusCode(200)
            .body("platform.runtime.springBootVersion", startsWith("3."))
            .body("platform.runtime.javaVersion", startsWith("17"))
            .body("platform.baseline.javaVersion", equalTo("11"))
            .body("platform.baseline.springBootVersion", equalTo("2.6.3"))
            .body(
                "platform.endpoints",
                hasItem(
                    allOf(
                        hasEntry("method", "GET"),
                        hasEntry("path", "/tags"),
                        hasEntry("controller", "TagsApi"))))
            .body(
                "platform.endpoints",
                hasItem(
                    allOf(
                        hasEntry("method", "GET"),
                        hasEntry("path", "/platform/info"),
                        hasEntry("controller", "PlatformInfoApi"))))
            .extract()
            .jsonPath();

    List<Map<String, Object>> endpoints = json.getList("platform.endpoints");
    List<Map<String, Object>> dependencies = json.getList("platform.dependencies");
    Assertions.assertEquals(endpoints.size(), json.getInt("platform.summary.endpointCount"));
    Assertions.assertEquals(
        dependencies.size(), json.getInt("platform.summary.upgradedDependencyCount"));
    Assertions.assertEquals(
        List.of("platform"), List.copyOf(((Map<String, Object>) json.get("$")).keySet()));
  }
}
