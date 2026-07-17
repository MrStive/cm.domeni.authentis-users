package cm.domeni.authentis_users.api;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import cm.domeni.authentis_users.dto.UserDTO;
import cm.domeni.authentis_users.service.UserService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserResourceTest {

  @Mock private UserService userService;

  @Test
  void shouldFetchUserById() {
    UUID userId = UUID.randomUUID();
    var expectedUser = new UserDTO().id(userId).userName("john_doe");

    when(userService.fetchUserById(userId)).thenReturn(expectedUser);

    var actualUser =
        given()
            .standaloneSetup(new UserResource(userService))
            .when()
            .get("/users/{userId}", userId.toString())
            .then()
            .statusCode(200)
            .extract()
            .body()
            .as(UserDTO.class);

    assertThat(actualUser).isEqualTo(expectedUser);
  }
}
