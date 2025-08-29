package cm.domeni.authentis_users.service;

import cm.domeni.authentis_users.domain.role.RoleFactory;
import cm.domeni.authentis_users.dto.CreateRoleDTO;
import cm.domeni.authentis_users.exception.RoleAlreadyExistException;
import cm.domeni.authentis_users.service.mapper.RoleMapper;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class RoleService {

  private final RoleFactory roleFactory;
  private final RoleMapper roleMapper;

  @Transactional
  public UUID createRole(CreateRoleDTO roleData) throws RoleAlreadyExistException {
    return UUID.fromString(roleFactory.create(roleMapper.map(roleData)));
  }
}
