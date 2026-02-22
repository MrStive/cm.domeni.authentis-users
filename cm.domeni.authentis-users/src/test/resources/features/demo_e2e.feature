@e2e
Feature: Users and roles end-to-end

  Scenario: Create and fetch demos using the real database
    Given a demo payload with name "Cucumber Demo"
    When I call POST /demo as an authenticated user
    Then the HTTP status should be 201
    And the response should contain a valid UUID
    And I should have a demo named "Cucumber Demo" in database
    When I call GET /demo as an authenticated user
    Then the HTTP status should be 200
    And the demo list should contain name "Cucumber Demo"

  Scenario: Register a user and fetch all users
    Given a user payload with username "cucumber-user" and email "cucumber-user@example.test"
    When I call POST /register
    Then the HTTP status should be 201
    And the response should contain a valid UUID
    And I should have a user with username "cucumber-user" in database
    When I call GET /user as an authenticated user
    Then the HTTP status should be 200
    And the users response should contain username "cucumber-user"

  Scenario: Refresh token to obtain a new JWT
    Given a refresh token payload with token "mock-refresh-token"
    When I call POST /auth/refresh
    Then the HTTP status should be 200
    And the refresh response should contain token type "Bearer"

  Scenario: Reset password with a valid token
    Given a reset password payload with token "valid-reset-token" and new password "new-password-123"
    When I call POST /auth/reset-password
    Then the HTTP status should be 204

  Scenario: Reset password with an invalid token
    Given a reset password payload with token "invalid-reset-token" and new password "new-password-123"
    When I call POST /auth/reset-password
    Then the HTTP status should be 400
    And the problem detail title should be "Invalid Reset Token"

  Scenario: Reset password with active token but invalid scope
    Given a reset password payload with token "active-invalid-scope-token" and new password "new-password-123"
    When I call POST /auth/reset-password
    Then the HTTP status should be 400
    And the problem detail title should be "Invalid Reset Token"

  Scenario: Logout with a valid refresh token
    Given a refresh token payload with token "mock-refresh-token"
    When I call POST /auth/logout
    Then the HTTP status should be 204

  Scenario: Logout revokes the current access token immediately
    Given a refresh token payload with token "mock-refresh-token"
    And an authenticated access token
    When I call POST /auth/logout as the authenticated user
    Then the HTTP status should be 204
    When I call GET /demo with the same access token
    Then the HTTP status should be 401

  Scenario: Logout with an invalid refresh token
    Given a refresh token payload with token "invalid-refresh-token"
    When I call POST /auth/logout
    Then the HTTP status should be 400
    And the problem detail title should be "Invalid Refresh Token"

  Scenario: Create role and assign/remove it to a registered user
    Given a registered user with username "role-user"
    And an existing role named "manager"
    When I assign role "manager" to the registered user
    Then the HTTP status should be 204
    When I remove role "manager" from the registered user
    Then the HTTP status should be 204
