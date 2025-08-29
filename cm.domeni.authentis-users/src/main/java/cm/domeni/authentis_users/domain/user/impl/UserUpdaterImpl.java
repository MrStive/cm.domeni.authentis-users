package cm.domeni.authentis_users.domain.user.impl;

import cm.domeni.authentis_users.domain.user.UserUpdater;
import cm.domeni.authentis_users.external.keycloak.KeycloakGateway;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserUpdaterImpl implements UserUpdater {

  private final KeycloakGateway keycloakGateway;

  @Override
  public void assignRole(UUID userId, String roleName) {
    keycloakGateway.assignRoleToUser(userId, roleName);
  }
}
