package cm.domeni.authentis_users.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import static cm.domeni.authentis_users.config.Scopes.ROLE_ASSIGN;
import static cm.domeni.authentis_users.config.Scopes.ROLE_CREATE;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            authorize ->
                authorize
                    .requestMatchers(HttpMethod.POST, "/register")
                    .permitAll()
                    .requestMatchers(HttpMethod.POST, "/role")
                    .hasAuthority(ROLE_CREATE)
                    .requestMatchers(HttpMethod.POST, "/users/{userId}/roles/{roleName}")
                    .hasAuthority(ROLE_ASSIGN)
                    .anyRequest()
                    .denyAll())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));
    return http.build();
  }
}
