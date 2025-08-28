package cm.domeni.authentis_users.domain.role;

import cm.domeni.authentis_users.exception.RoleAlreadyExistException;

public interface RoleFactory {
  String create(RoleData roleData) throws RoleAlreadyExistException;
}
