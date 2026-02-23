package cm.domeni.authentis_users.config;

import static cm.domeni.authentis_users.config.Scopes.*;

import cm.domeni.authentis_users.security.AccessTokenRevocationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(
      HttpSecurity http, AccessTokenRevocationService accessTokenRevocationService)
      throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(HttpMethod.POST, "/register")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/refresh")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/logout")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/auth/reset-password")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/internal/keycloak/events")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/demo")
                    .authenticated()
                    .requestMatchers(HttpMethod.POST, "/demo")
                    .authenticated()
                    .requestMatchers(HttpMethod.GET, "/user")
                    .authenticated()
                    .requestMatchers(HttpMethod.POST, "/role")
                    .hasAuthority(ROLE_CREATE)
                    .requestMatchers(HttpMethod.PUT, "/users/*/roles/*")
                    .hasAuthority(ROLE_ASSIGN)
                    .requestMatchers(HttpMethod.DELETE, "/users/*/roles/*")
                    .hasAuthority(ROLE_DELETE)
                    .anyRequest()
                    .denyAll())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2.jwt(
                    jwt ->
                        jwt.jwtAuthenticationConverter(
                            revocationAwareJwtAuthenticationConverter(
                                accessTokenRevocationService))));
    return http.build();
  }

  private Converter<Jwt, ? extends AbstractAuthenticationToken>
      revocationAwareJwtAuthenticationConverter(
          AccessTokenRevocationService accessTokenRevocationService) {
    JwtAuthenticationConverter delegate = new JwtAuthenticationConverter();
    return jwt -> {
      accessTokenRevocationService.ensureNotRevoked(jwt);
      return delegate.convert(jwt);
    };
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }
}
