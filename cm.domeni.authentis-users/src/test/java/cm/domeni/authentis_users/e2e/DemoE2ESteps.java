package cm.domeni.authentis_users.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.simple.JdbcClient;

public class DemoE2ESteps {
  @LocalServerPort private int serverPort;
  @Autowired private JdbcClient jdbcClient;
  @Autowired private ObjectMapper objectMapper;

  private Map<String, Object> demoPayload;
  private Map<String, Object> userPayload;
  private Map<String, Object> rolePayload;
  private Map<String, Object> refreshTokenPayload;
  private Response latestResponse;
  private UUID lastCreatedDemoId;
  private UUID lastRegisteredUserId;

  @Before
  public void resetDatabaseAndHttpClient() {
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = serverPort;
    jdbcClient.sql("DELETE FROM t_demo").update();
    jdbcClient.sql("DELETE FROM t_user").update();
  }

  @Given("a demo payload with name {string}")
  public void aDemoPayloadWithName(String name) {
    demoPayload = new LinkedHashMap<>();
    demoPayload.put("name", name);
  }

  @When("^I call POST /demo as an authenticated user$")
  public void iCallPostDemoAsAnAuthenticatedUser() {
    latestResponse =
        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth()
            .oauth2(CucumberSpringConfiguration.issueToken())
            .body(demoPayload)
            .when()
            .post("/demo");
    if (latestResponse.statusCode() == 201) {
      lastCreatedDemoId = readUuidBody();
    }
  }

  @When("^I call GET /demo as an authenticated user$")
  public void iCallGetDemoAsAnAuthenticatedUser() {
    latestResponse =
        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth()
            .oauth2(CucumberSpringConfiguration.issueToken())
            .when()
            .get("/demo");
  }

  @Given("a user payload with username {string} and email {string}")
  public void aUserPayloadWithUsernameAndEmail(String userName, String email) {
    userPayload = new LinkedHashMap<>();
    userPayload.put("userName", userName);
    userPayload.put("email", email);
    userPayload.put("password", "safe-password");
    userPayload.put("firstName", "Integration");
    userPayload.put("lastName", "Test");
  }

  @When("^I call POST /register$")
  public void iCallPostRegister() {
    latestResponse =
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(userPayload)
            .when()
            .post("/register");
    if (latestResponse.statusCode() == 201) {
      lastRegisteredUserId = readUuidBody();
    }
  }

  @When("^I call GET /user as an authenticated user$")
  public void iCallGetUserAsAnAuthenticatedUser() {
    latestResponse =
        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth()
            .oauth2(CucumberSpringConfiguration.issueToken())
            .when()
            .get("/user");
  }

  @Then("the users response should contain username {string}")
  public void theUsersResponseShouldContainUsername(String expectedUserName) {
    List<Map<String, Object>> users = latestResponse.jsonPath().getList("$");
    assertTrue(
        users.stream().anyMatch(user -> expectedUserName.equals(user.get("userName"))),
        "Expected username '%s' in users response".formatted(expectedUserName));
  }

  @Given("a role payload with name {string}")
  public void aRolePayloadWithName(String roleName) {
    rolePayload = new LinkedHashMap<>();
    rolePayload.put("name", roleName);
    rolePayload.put("description", "e2e role %s".formatted(roleName));
  }

  @Given("^a refresh token payload with token \"([^\"]*)\"$")
  public void aRefreshTokenPayloadWithToken(String refreshToken) {
    refreshTokenPayload = new LinkedHashMap<>();
    refreshTokenPayload.put("refreshToken", refreshToken);
  }

  @When("^I call POST /role with scope \"([^\"]*)\"$")
  public void iCallPostRoleWithScope(String scope) {
    latestResponse =
        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth()
            .oauth2(CucumberSpringConfiguration.issueToken(scope))
            .body(rolePayload)
            .when()
            .post("/role");
  }

  @When("^I call POST /auth/refresh$")
  public void iCallPostAuthRefresh() {
    latestResponse =
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(refreshTokenPayload)
            .when()
            .post("/auth/refresh");
  }

  @Given("a registered user with username {string}")
  public void aRegisteredUserWithUsername(String userName) {
    aUserPayloadWithUsernameAndEmail(userName, "%s@example.test".formatted(userName));
    iCallPostRegister();
    assertEquals(201, latestResponse.statusCode(), "User registration must succeed in setup");
  }

  @Given("an existing role named {string}")
  public void anExistingRoleNamed(String roleName) {
    aRolePayloadWithName(roleName);
    iCallPostRoleWithScope("role:create");
    assertEquals(201, latestResponse.statusCode(), "Role creation must succeed in setup");
  }

  @When("^I assign role \"([^\"]*)\" to the registered user$")
  public void iAssignRoleToTheRegisteredUser(String roleName) {
    assertNotNull(lastRegisteredUserId, "Missing registered user id");
    latestResponse =
        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth()
            .oauth2(CucumberSpringConfiguration.issueToken("role:assign"))
            .when()
            .put("/users/{userId}/roles/{roleName}", lastRegisteredUserId, roleName);
  }

  @When("^I remove role \"([^\"]*)\" from the registered user$")
  public void iRemoveRoleFromTheRegisteredUser(String roleName) {
    assertNotNull(lastRegisteredUserId, "Missing registered user id");
    latestResponse =
        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth()
            .oauth2(CucumberSpringConfiguration.issueToken("role:delete"))
            .when()
            .delete("/users/{userId}/roles/{roleName}", lastRegisteredUserId, roleName);
  }

  @Then("the HTTP status should be {int}")
  public void theHttpStatusShouldBe(int expectedStatus) {
    assertEquals(expectedStatus, latestResponse.getStatusCode());
  }

  @Then("the response should contain a valid UUID")
  public void theResponseShouldContainAValidUuid() {
    assertNotNull(readUuidBody());
  }

  @Then("the demo list should contain name {string}")
  public void theDemoListShouldContainName(String expectedName) {
    List<Map<String, Object>> demos = latestResponse.jsonPath().getList("$");
    assertTrue(
        demos.stream().anyMatch(demo -> expectedName.equals(demo.get("name"))),
        "Expected demo name '%s' in response".formatted(expectedName));
  }

  @Then("^I should have a demo named \"([^\"]*)\" in database$")
  public void iShouldHaveADemoNamedInDatabase(String demoName) {
    assumeThat(lastCreatedDemoId).as("Created demo id must be present").isNotNull();
    Long rows =
        jdbcClient
            .sql("SELECT COUNT(*) FROM t_demo WHERE c_id = :id AND c_name = :name")
            .param("id", lastCreatedDemoId.toString())
            .param("name", demoName)
            .query(Long.class)
            .single();
    assumeThat(rows).as("Demo query result must exist").isNotNull();
    assumeThat(rows).as("Demo row should be present in database").isGreaterThan(0L);
    assertThat(rows)
        .as("Expected demo '%s' persisted in database".formatted(demoName))
        .isGreaterThan(0L);
  }

  @Then("^I should have a user with username \"([^\"]*)\" in database$")
  public void iShouldHaveAUserWithUsernameInDatabase(String userName) {
    assumeThat(lastRegisteredUserId).as("Registered user id must be present").isNotNull();
    Long rows =
        jdbcClient
            .sql("SELECT COUNT(*) FROM t_user WHERE c_id = :id AND c_user_name = :userName")
            .param("id", lastRegisteredUserId.toString())
            .param("userName", userName)
            .query(Long.class)
            .single();
    assumeThat(rows).as("User query result must exist").isNotNull();
    assumeThat(rows).as("User row should be present in database").isGreaterThan(0L);
    assertThat(rows)
        .as("Expected user '%s' persisted in database".formatted(userName))
        .isGreaterThan(0L);
  }

  @Then("^the refresh response should contain token type \"([^\"]*)\"$")
  public void theRefreshResponseShouldContainTokenType(String expectedTokenType) {
    String accessToken = latestResponse.jsonPath().getString("accessToken");
    String tokenType = latestResponse.jsonPath().getString("tokenType");
    String refreshToken = latestResponse.jsonPath().getString("refreshToken");
    assertThat(accessToken).isNotBlank();
    assertThat(tokenType).isEqualTo(expectedTokenType);
    assertThat(refreshToken).isNotBlank();
  }

  private UUID readUuidBody() {
    try {
      return objectMapper.readValue(latestResponse.getBody().asString(), UUID.class);
    } catch (Exception e) {
      throw new IllegalStateException(
          "Expected UUID body but got: " + latestResponse.getBody().asString(), e);
    }
  }
}
