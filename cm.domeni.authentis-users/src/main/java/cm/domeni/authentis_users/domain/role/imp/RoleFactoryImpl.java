package cm.domeni.authentis_users.domain.role.imp;

import cm.domeni.authentis_users.domain.role.RoleData;
import cm.domeni.authentis_users.domain.role.RoleFactory;
import cm.domeni.authentis_users.exception.RoleAlreadyExistException;
import cm.domeni.authentis_users.external.keycloak.KeycloakGateway;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RoleFactoryImpl implements RoleFactory {
  private final KeycloakGateway keycloakGateway;

  @Override
  public String create(RoleData roleData) throws RoleAlreadyExistException {
    return keycloakGateway.createRole(roleData);
  }
}
