package cm.domeni.authentis_users.api;

import cm.domeni.authentis_users.dto.CreateRoleDTO;
import cm.domeni.authentis_users.exception.RoleAlreadyExistException;
import cm.domeni.authentis_users.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class RoleResource implements RoleApi {

  private final RoleService roleService;

  @Override
  public ResponseEntity<UUID> createRole(CreateRoleDTO createRoleDTO) {
    try {
      UUID createdRoleId = roleService.createRole(createRoleDTO);
      return ResponseEntity.status(HttpStatus.CREATED).body(createdRoleId);
    } catch (RoleAlreadyExistException e) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    }
  }
}
