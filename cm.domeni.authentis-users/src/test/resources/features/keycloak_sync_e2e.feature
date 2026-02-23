@e2e
Feature: Keycloak synchronization end-to-end

  Scenario: Synchronize local user data after a Keycloak admin update through webhook
    Given a registered user with username "sync-user"
    And Keycloak profile for this user is updated to username "sync-user-updated" and email "sync-user-updated@example.test"
    When I call POST /internal/keycloak/events with this user id
    Then the HTTP status should be 202
    And I should have a user with username "sync-user-updated" in database
    And I should have a user with email "sync-user-updated@example.test" in database
