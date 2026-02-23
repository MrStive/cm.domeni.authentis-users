package cm.domeni.authentis_users.service;

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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakUserSyncService {
  private final UserRepository userRepository;
  private final KeycloakGateway keycloakGateway;
  private final KeycloakSyncProperties syncProperties;

  @Transactional
  public void syncAllUsersFromKeycloak() {
    List<User> localUsers = userRepository.findAll();
    Map<String, User> localUsersById = new HashMap<>();
    for (User localUser : localUsers) {
      if (localUser.getId() != null && hasText(localUser.getId().getValue())) {
        localUsersById.put(localUser.getId().getValue(), localUser);
      }
    }

    List<KeycloakUserSnapshot> keycloakUsers =
        keycloakGateway.fetchAllUsers(syncProperties.getPageSize());
    int created = 0;
    int updated = 0;
    int deleted = 0;

    for (KeycloakUserSnapshot keycloakUser : keycloakUsers) {
      User localUser = localUsersById.remove(keycloakUser.id());
      if (localUser == null) {
        userRepository.save(newLocalUser(keycloakUser));
        created++;
        continue;
      }
      if (applyProfile(localUser, keycloakUser)) {
        userRepository.save(localUser);
        updated++;
      }
    }

    for (User missingInKeycloak : localUsersById.values()) {
      missingInKeycloak.markAsDeleted();
      userRepository.save(missingInKeycloak);
      deleted++;
    }

    log.info(
        "Keycloak reconciliation completed: created={}, updated={}, deleted={}",
        created,
        updated,
        deleted);
  }

  @Transactional
  public void syncUserFromKeycloak(String keycloakUserId) {
    String normalizedUserId = requireNonBlank(keycloakUserId, "keycloak user id");
    Optional<KeycloakUserSnapshot> keycloakUser = keycloakGateway.findUserById(normalizedUserId);
    Optional<User> localUser = userRepository.findById(new UserId(normalizedUserId));

    if (keycloakUser.isPresent()) {
      KeycloakUserSnapshot snapshot = keycloakUser.get();
      if (localUser.isPresent()) {
        User existing = localUser.get();
        if (applyProfile(existing, snapshot)) {
          userRepository.save(existing);
        }
      } else {
        userRepository.save(newLocalUser(snapshot));
      }
      return;
    }

    localUser.ifPresent(
        user -> {
          user.markAsDeleted();
          userRepository.save(user);
        });
  }

  private User newLocalUser(KeycloakUserSnapshot snapshot) {
    User user = User.builder().id(new UserId(snapshot.id())).build();
    applyProfile(user, snapshot);
    return user;
  }

  private boolean applyProfile(User localUser, KeycloakUserSnapshot snapshot) {
    boolean changed = false;

    String normalizedUserName = normalizeNullable(snapshot.username());
    if (!Objects.equals(value(localUser.getUserName()), normalizedUserName)) {
      localUser.setUserName(wrapUserName(normalizedUserName));
      changed = true;
    }

    String normalizedEmail = normalizeNullable(snapshot.email());
    if (!Objects.equals(value(localUser.getEmail()), normalizedEmail)) {
      localUser.setEmail(wrapEmail(normalizedEmail));
      changed = true;
    }

    String normalizedFirstName = normalizeNullable(snapshot.firstName());
    if (!Objects.equals(value(localUser.getFirstName()), normalizedFirstName)) {
      localUser.setFirstName(wrapFirstName(normalizedFirstName));
      changed = true;
    }

    String normalizedLastName = normalizeNullable(snapshot.lastName());
    if (!Objects.equals(value(localUser.getLastName()), normalizedLastName)) {
      localUser.setLastName(wrapLastName(normalizedLastName));
      changed = true;
    }

    return changed;
  }

  private String requireNonBlank(String value, String field) {
    if (!hasText(value)) {
      throw new IllegalArgumentException("%s is required".formatted(field));
    }
    return value.trim();
  }

  private boolean hasText(String value) {
    return value != null && !value.trim().isEmpty();
  }

  private String normalizeNullable(String value) {
    if (!hasText(value)) {
      return null;
    }
    return value.trim();
  }

  private String value(UserName userName) {
    return userName != null ? userName.getValue() : null;
  }

  private String value(Email email) {
    return email != null ? email.getValue() : null;
  }

  private String value(FirstName firstName) {
    return firstName != null ? firstName.getValue() : null;
  }

  private String value(LastName lastName) {
    return lastName != null ? lastName.getValue() : null;
  }

  private UserName wrapUserName(String value) {
    return value != null ? new UserName(value) : null;
  }

  private Email wrapEmail(String value) {
    return value != null ? new Email(value) : null;
  }

  private FirstName wrapFirstName(String value) {
    return value != null ? new FirstName(value) : null;
  }

  private LastName wrapLastName(String value) {
    return value != null ? new LastName(value) : null;
  }
}
