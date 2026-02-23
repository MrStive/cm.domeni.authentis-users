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
  private static final String KEYCLOAK_WEBHOOK_SECRET = "e2e-webhook-secret";
  private static final String KEYCLOAK_WEBHOOK_SECRET_HEADER = "X-Keycloak-Webhook-Secret";

  @LocalServerPort private int serverPort;
  @Autowired private JdbcClient jdbcClient;
  @Autowired private ObjectMapper objectMapper;

  private Map<String, Object> demoPayload;
  private Map<String, Object> userPayload;
  private Map<String, Object> rolePayload;
  private Map<String, Object> refreshTokenPayload;
  private Map<String, Object> resetPasswordPayload;
  private Response latestResponse;
  private UUID lastCreatedDemoId;
  private UUID lastRegisteredUserId;
  private String authenticatedAccessToken;

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

  @Given("^a reset password payload with token \"([^\"]*)\" and new password \"([^\"]*)\"$")
  public void aResetPasswordPayloadWithTokenAndNewPassword(String resetToken, String newPassword) {
    resetPasswordPayload = new LinkedHashMap<>();
    resetPasswordPayload.put("resetToken", resetToken);
    resetPasswordPayload.put("newPassword", newPassword);
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

  @When("^I call POST /auth/logout$")
  public void iCallPostAuthLogout() {
    latestResponse =
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(refreshTokenPayload)
            .when()
            .post("/auth/logout");
  }

  @When("^I call POST /auth/reset-password$")
  public void iCallPostAuthResetPassword() {
    latestResponse =
        RestAssured.given()
            .contentType(ContentType.JSON)
            .body(resetPasswordPayload)
            .when()
            .post("/auth/reset-password");
  }

  @Given("^an authenticated access token$")
  public void anAuthenticatedAccessToken() {
    authenticatedAccessToken = CucumberSpringConfiguration.issueToken();
  }

  @When("^I call POST /auth/logout as the authenticated user$")
  public void iCallPostAuthLogoutAsTheAuthenticatedUser() {
    assertThat(authenticatedAccessToken).isNotBlank();
    latestResponse =
        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth()
            .oauth2(authenticatedAccessToken)
            .body(refreshTokenPayload)
            .when()
            .post("/auth/logout");
  }

  @When("^I call GET /demo with the same access token$")
  public void iCallGetDemoWithTheSameAccessToken() {
    assertThat(authenticatedAccessToken).isNotBlank();
    latestResponse =
        RestAssured.given()
            .contentType(ContentType.JSON)
            .auth()
            .oauth2(authenticatedAccessToken)
            .when()
            .get("/demo");
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

  @Given("Keycloak profile for this user is updated to username {string} and email {string}")
  public void keycloakProfileForThisUserIsUpdatedToUsernameAndEmail(
      String updatedUserName, String updatedEmail) {
    assertNotNull(lastRegisteredUserId, "Missing registered user id");
    CucumberSpringConfiguration.stubKeycloakUserById(
        lastRegisteredUserId.toString(), updatedUserName, updatedEmail, "Synced", "User", true);
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

  @When("^I call POST /internal/keycloak/events with this user id$")
  public void iCallPostInternalKeycloakEventsWithThisUserId() {
    assertNotNull(lastRegisteredUserId, "Missing registered user id");
    Map<String, Object> keycloakEventPayload = new LinkedHashMap<>();
    keycloakEventPayload.put("resourceType", "USER");
    keycloakEventPayload.put("resourcePath", "users/%s".formatted(lastRegisteredUserId));

    latestResponse =
        RestAssured.given()
            .contentType(ContentType.JSON)
            .header(KEYCLOAK_WEBHOOK_SECRET_HEADER, KEYCLOAK_WEBHOOK_SECRET)
            .body(keycloakEventPayload)
            .when()
            .post("/internal/keycloak/events");
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

  @Then("^I should have a user with email \"([^\"]*)\" in database$")
  public void iShouldHaveAUserWithEmailInDatabase(String email) {
    assumeThat(lastRegisteredUserId).as("Registered user id must be present").isNotNull();
    Long rows =
        jdbcClient
            .sql("SELECT COUNT(*) FROM t_user WHERE c_id = :id AND c_email = :email")
            .param("id", lastRegisteredUserId.toString())
            .param("email", email)
            .query(Long.class)
            .single();
    assumeThat(rows).as("User query result must exist").isNotNull();
    assumeThat(rows).as("User row should be present in database").isGreaterThan(0L);
    assertThat(rows)
        .as("Expected user email '%s' persisted in database".formatted(email))
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

  @Then("^the problem detail title should be \"([^\"]*)\"$")
  public void theProblemDetailTitleShouldBe(String expectedTitle) {
    assertThat(latestResponse.jsonPath().getString("title")).isEqualTo(expectedTitle);
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
