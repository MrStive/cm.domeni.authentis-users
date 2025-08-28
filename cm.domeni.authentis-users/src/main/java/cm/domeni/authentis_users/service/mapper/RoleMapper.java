package cm.domeni.authentis_users.service.mapper;

import cm.domeni.authentis_users.domain.role.RoleData;
import cm.domeni.authentis_users.dto.CreateRoleDTO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RoleMapper {
  RoleData map(CreateRoleDTO dto);
}
