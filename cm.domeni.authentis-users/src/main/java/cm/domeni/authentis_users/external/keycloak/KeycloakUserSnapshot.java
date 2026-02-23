package cm.domeni.authentis_users.external.keycloak;

public record KeycloakUserSnapshot(
    String id, String username, String email, String firstName, String lastName, boolean enabled) {}
