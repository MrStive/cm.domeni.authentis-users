package cm.domeni.authentis_users.api;

import cm.domeni.authentis_users.dto.CreateUser;
import cm.domeni.authentis_users.dto.UserDTO;
import cm.domeni.authentis_users.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class UserResource implements UserApi {
  private final UserService userService;

  @Override
  public ResponseEntity<List<UserDTO>> fetchAllUsers() {
    return ResponseEntity.ok(userService.fetchAllUsers());
  }

  @Override
  public ResponseEntity<UUID> register(CreateUser createUser) {
    UUID createdUserId = userService.createUser(createUser);
    return ResponseEntity.status(HttpStatus.CREATED).body(createdUserId);
  }
}
