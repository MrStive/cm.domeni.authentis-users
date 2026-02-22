package cm.domeni.authentis_users.e2e;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.cucumber.spring.CucumberContextConfiguration;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2e")
@TestPropertySource(properties = "spring.cloud.config.enabled=false")
public class CucumberSpringConfiguration {
  private static final String REALM = "authentis-user";
  private static final String CLIENT_ID = "authentis-users";
  private static final String CLIENT_SECRET = "test-client-secret";
  private static final String FIXED_USER_ID = "11111111-1111-1111-1111-111111111111";
  private static final String FIXED_ROLE_ID = "22222222-2222-2222-2222-222222222222";
  private static final String KEY_ID = "e2e-wiremock-key";

  private static final WireMockServer WIREMOCK =
      new WireMockServer(WireMockConfiguration.options().dynamicPort());
  private static final RSAKey RSA_KEY = generateRsaKey();

  @DynamicPropertySource
  static void registerDynamicProperties(DynamicPropertyRegistry registry) {
    startWireMockIfNeeded();
    registry.add("keycloak.admin-client.server-url", WIREMOCK::baseUrl);
    registry.add("keycloak.admin-client.realm", () -> REALM);
    registry.add("keycloak.admin-client.client-id", () -> CLIENT_ID);
    registry.add("keycloak.admin-client.client-secret", () -> CLIENT_SECRET);
    registry.add("keycloak.token-client.server-url", WIREMOCK::baseUrl);
    registry.add("keycloak.token-client.realm", () -> REALM);
    registry.add("keycloak.token-client.client-id", () -> CLIENT_ID);
    registry.add("keycloak.token-client.client-secret", () -> CLIENT_SECRET);
    registry.add(
        "spring.security.oauth2.resourceserver.jwt.issuer-uri",
        CucumberSpringConfiguration::issuerUri);
    registry.add(
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
        CucumberSpringConfiguration::jwkSetUri);
  }

  public static String issueToken(String... scopes) {
    try {
      String scopeClaim =
          Arrays.stream(scopes)
              .filter(Objects::nonNull)
              .filter(scope -> !scope.isBlank())
              .collect(Collectors.joining(" "));
      Instant now = Instant.now();

      JWTClaimsSet claims =
          new JWTClaimsSet.Builder()
              .issuer(issuerUri())
              .subject(UUID.randomUUID().toString())
              .issueTime(Date.from(now))
              .expirationTime(Date.from(now.plusSeconds(600)))
              .claim("scope", scopeClaim)
              .build();

      SignedJWT signedJWT =
          new SignedJWT(
              new JWSHeader.Builder(JWSAlgorithm.RS256)
                  .type(JOSEObjectType.JWT)
                  .keyID(KEY_ID)
                  .build(),
              claims);
      signedJWT.sign(new RSASSASigner(RSA_KEY.toPrivateKey()));
      return signedJWT.serialize();
    } catch (JOSEException e) {
      throw new IllegalStateException("Cannot issue JWT for e2e tests", e);
    }
  }

  private static synchronized void startWireMockIfNeeded() {
    if (WIREMOCK.isRunning()) {
      return;
    }
    WIREMOCK.start();
    stubOidcAndKeycloakAdmin();
  }

  private static void stubOidcAndKeycloakAdmin() {
    WIREMOCK.stubFor(
        post(urlEqualTo("/realms/%s/protocol/openid-connect/token".formatted(REALM)))
            .willReturn(
                okJson(
                    """
                    {
                      "access_token": "mock-access-token",
                      "refresh_token": "mock-refresh-token",
                      "expires_in": 300,
                      "refresh_expires_in": 1800,
                      "token_type": "Bearer",
                      "not-before-policy": 0,
                      "scope": "profile email"
                    }
                    """)));

    WIREMOCK.stubFor(
        post(urlEqualTo("/realms/%s/protocol/openid-connect/logout".formatted(REALM)))
            .atPriority(1)
            .withRequestBody(containing("refresh_token=invalid-refresh-token"))
            .willReturn(
                aResponse()
                    .withStatus(400)
                    .withHeader("Content-Type", "application/json")
                    .withBody(
                        """
                        {
                          "error": "invalid_grant",
                          "error_description": "Invalid refresh token"
                        }
                        """)));

    WIREMOCK.stubFor(
        post(urlEqualTo("/realms/%s/protocol/openid-connect/logout".formatted(REALM)))
            .atPriority(10)
            .willReturn(aResponse().withStatus(204)));

    WIREMOCK.stubFor(
        get(urlEqualTo("/realms/%s/protocol/openid-connect/certs".formatted(REALM)))
            .willReturn(okJson(new JWKSet(RSA_KEY.toPublicJWK()).toString())));

    WIREMOCK.stubFor(
        get(urlEqualTo("/realms/%s/.well-known/openid-configuration".formatted(REALM)))
            .willReturn(
                okJson(
                    """
                    {
                      "issuer": "%s",
                      "jwks_uri": "%s"
                    }
                    """
                        .formatted(issuerUri(), jwkSetUri()))));

    WIREMOCK.stubFor(
        post(urlEqualTo("/admin/realms/%s/users".formatted(REALM)))
            .willReturn(
                aResponse()
                    .withStatus(201)
                    .withHeader(
                        "Location",
                        "%s/admin/realms/%s/users/%s"
                            .formatted(WIREMOCK.baseUrl(), REALM, FIXED_USER_ID))));

    WIREMOCK.stubFor(
        delete(urlPathMatching("/admin/realms/%s/users/[^/]+".formatted(REALM)))
            .willReturn(aResponse().withStatus(204)));

    WIREMOCK.stubFor(
        post(urlEqualTo("/admin/realms/%s/roles".formatted(REALM)))
            .willReturn(aResponse().withStatus(201)));

    WIREMOCK.stubFor(
        get(urlPathMatching("/admin/realms/%s/roles/[^/]+".formatted(REALM)))
            .willReturn(
                okJson(
                    """
                    {
                      "id": "%s",
                      "name": "stub-role"
                    }
                    """
                        .formatted(FIXED_ROLE_ID))));

    WIREMOCK.stubFor(
        post(urlPathMatching("/admin/realms/%s/users/[^/]+/role-mappings/realm".formatted(REALM)))
            .willReturn(aResponse().withStatus(204)));

    WIREMOCK.stubFor(
        delete(urlPathMatching("/admin/realms/%s/users/[^/]+/role-mappings/realm".formatted(REALM)))
            .willReturn(aResponse().withStatus(204)));
  }

  private static String issuerUri() {
    return "%s/realms/%s".formatted(WIREMOCK.baseUrl(), REALM);
  }

  private static String jwkSetUri() {
    return "%s/realms/%s/protocol/openid-connect/certs".formatted(WIREMOCK.baseUrl(), REALM);
  }

  private static RSAKey generateRsaKey() {
    try {
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
      keyPairGenerator.initialize(2048);
      KeyPair keyPair = keyPairGenerator.generateKeyPair();
      return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
          .privateKey((RSAPrivateKey) keyPair.getPrivate())
          .keyID(KEY_ID)
          .build();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("Cannot generate RSA key for e2e tests", e);
    }
  }
}
