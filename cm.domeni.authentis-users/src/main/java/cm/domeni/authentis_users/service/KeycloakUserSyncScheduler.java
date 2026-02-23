package cm.domeni.authentis_users.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    prefix = "keycloak.sync",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
public class KeycloakUserSyncScheduler {
  private final KeycloakUserSyncService keycloakUserSyncService;

  @Scheduled(cron = "${keycloak.sync.cron:0 */5 * * * *}")
  public void synchronizeUsers() {
    log.debug("Starting scheduled Keycloak user synchronization");
    keycloakUserSyncService.syncAllUsersFromKeycloak();
  }
}
