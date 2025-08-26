package cm.domeni.authentis_users.config;

import cm.domeni.authentis_users.domain.demo.DemoFactory;
import cm.domeni.authentis_users.domain.demo.DemoFetcher;
import cm.domeni.authentis_users.domain.demo.DemoRepository;
import cm.domeni.authentis_users.domain.demo.impl.DemoFactoryImpl;
import cm.domeni.authentis_users.domain.demo.impl.DemoFetcherImpl;
import cm.domeni.authentis_users.domain.user.UserFactory;
import cm.domeni.authentis_users.domain.user.UserFetcher;
import cm.domeni.authentis_users.domain.user.UserRepository;
import cm.domeni.authentis_users.domain.user.impl.UserFactoryImpl;
import cm.domeni.authentis_users.domain.user.impl.UserFetcherImpl;
import cm.domeni.authentis_users.external.keycloak.KeycloakGateway;
import cm.domeni.authentis_users.repository.DemoSpringRepository;
import cm.domeni.authentis_users.repository.UserSpringRepository;
import cm.domeni.authentis_users.repository.impl.DemoRepositoryImpl;
import cm.domeni.authentis_users.repository.impl.UserRepositoryImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class AuthentisUsersBeans {

  @Bean
  public DemoFactory demoFactory(DemoRepository demoRepository) {
    return new DemoFactoryImpl(demoRepository);
  }

  @Bean
  public DemoRepository demoRepository(DemoSpringRepository demoSpringRepository) {
    return new DemoRepositoryImpl(demoSpringRepository);
  }

  @Bean
  public DemoFetcher demoFetcher(DemoRepository demoRepository) {
    return new DemoFetcherImpl(demoRepository);
  }

  @Bean
  public UserRepository userRepository(UserSpringRepository userSpringRepository) {
    return new UserRepositoryImpl(userSpringRepository);
  }

  @Bean
  public UserFactory userFactory(UserRepository userRepository, KeycloakGateway keycloakGateway) {
    return new UserFactoryImpl(userRepository, keycloakGateway);
  }

  @Bean
  public UserFetcher userFetcher(UserRepository userRepository) {
    return new UserFetcherImpl(userRepository);
  }
}
