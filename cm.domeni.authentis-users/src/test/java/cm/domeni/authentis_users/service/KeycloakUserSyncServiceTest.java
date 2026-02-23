package cm.domeni.authentis_users.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cm.domeni.authentis_users.config.KeycloakSyncProperties;
import cm.domeni.authentis_users.domain.user.Email;
import cm.domeni.authentis_users.domain.user.FirstName;
import cm.domeni.authentis_users.domain.user.LastName;
import cm.domeni.authentis_users.domain.user.User;
import cm.domeni.authentis_users.domain.user.UserId;
import cm.domeni.authentis_users.domain.user.UserName;
import cm.domeni.authentis_users.domain.user.UserRepository;
import cm.domeni.authentis_users.external.keycloak.KeycloakGateway;
import cm.domeni.authentis_users.external.keycloak.KeycloakUserSnapshot;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KeycloakUserSyncServiceTest {
  @Mock private UserRepository userRepository;
  @Mock private KeycloakGateway keycloakGateway;

  @Test
  void shouldUpdateExistingLocalUserDuringFullSync() {
    KeycloakSyncProperties properties = defaultProperties();
    KeycloakUserSyncService service =
        new KeycloakUserSyncService(userRepository, keycloakGateway, properties);

    User existing = user("user-id-1", "local-name", "local@mail.com", "Local", "User");
    when(userRepository.findAll()).thenReturn(List.of(existing));
    when(keycloakGateway.fetchAllUsers(50))
        .thenReturn(
            List.of(
                new KeycloakUserSnapshot(
                    "user-id-1", "kc-name", "kc@mail.com", "Keycloak", "Updated", true)));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.syncAllUsersFromKeycloak();

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User saved = captor.getValue();
    assertThat(saved.getUserName().getValue()).isEqualTo("kc-name");
    assertThat(saved.getEmail().getValue()).isEqualTo("kc@mail.com");
    assertThat(saved.getFirstName().getValue()).isEqualTo("Keycloak");
    assertThat(saved.getLastName().getValue()).isEqualTo("Updated");
  }

  @Test
  void shouldCreateMissingLocalUserDuringFullSync() {
    KeycloakSyncProperties properties = defaultProperties();
    KeycloakUserSyncService service =
        new KeycloakUserSyncService(userRepository, keycloakGateway, properties);

    when(userRepository.findAll()).thenReturn(List.of());
    when(keycloakGateway.fetchAllUsers(50))
        .thenReturn(
            List.of(
                new KeycloakUserSnapshot(
                    "new-user-id", "new-user", "new@mail.com", "New", "User", true)));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.syncAllUsersFromKeycloak();

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User saved = captor.getValue();
    assertThat(saved.getId().getValue()).isEqualTo("new-user-id");
    assertThat(saved.getUserName().getValue()).isEqualTo("new-user");
    assertThat(saved.getEmail().getValue()).isEqualTo("new@mail.com");
    assertThat(saved.getPassword()).isNull();
  }

  @Test
  void shouldMarkMissingUserAsDeletedWhenNotInKeycloakAnymore() {
    KeycloakSyncProperties properties = defaultProperties();
    KeycloakUserSyncService service =
        new KeycloakUserSyncService(userRepository, keycloakGateway, properties);

    User existing = user("user-id-1", "local-name", "local@mail.com", "Local", "User");
    when(userRepository.findAll()).thenReturn(List.of(existing));
    when(keycloakGateway.fetchAllUsers(50)).thenReturn(List.of());
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.syncAllUsersFromKeycloak();

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    assertThat(captor.getValue().isDeleted()).isTrue();
  }

  @Test
  void shouldSyncSingleUserFromKeycloakOnDemand() {
    KeycloakSyncProperties properties = defaultProperties();
    KeycloakUserSyncService service =
        new KeycloakUserSyncService(userRepository, keycloakGateway, properties);

    User existing = user("user-id-1", "local-name", "local@mail.com", "Local", "User");
    when(keycloakGateway.findUserById("user-id-1"))
        .thenReturn(
            Optional.of(
                new KeycloakUserSnapshot(
                    "user-id-1", "on-demand-name", "on-demand@mail.com", "On", "Demand", true)));
    when(userRepository.findById(any(UserId.class))).thenReturn(Optional.of(existing));
    when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

    service.syncUserFromKeycloak("user-id-1");

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    User saved = captor.getValue();
    assertThat(saved.getUserName().getValue()).isEqualTo("on-demand-name");
    assertThat(saved.getEmail().getValue()).isEqualTo("on-demand@mail.com");
  }

  private KeycloakSyncProperties defaultProperties() {
    KeycloakSyncProperties properties = new KeycloakSyncProperties();
    properties.setPageSize(50);
    return properties;
  }

  private User user(String id, String userName, String email, String firstName, String lastName) {
    User user = User.builder().id(new UserId(id)).build();
    user.setUserName(new UserName(userName));
    user.setEmail(new Email(email));
    user.setFirstName(new FirstName(firstName));
    user.setLastName(new LastName(lastName));
    return user;
  }
}
