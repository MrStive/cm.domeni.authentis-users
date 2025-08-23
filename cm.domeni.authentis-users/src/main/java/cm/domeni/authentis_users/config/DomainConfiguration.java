package cm.domeni.authentis_users.config;

import cm.domeni.authentis_users.domain.user.UserFactory;
import cm.domeni.authentis_users.domain.user.UserFetcher;
import cm.domeni.authentis_users.domain.user.UserRepository;
import cm.domeni.authentis_users.domain.user.impl.UserFactoryImpl;
import cm.domeni.authentis_users.domain.user.impl.UserFetcherImpl;
import cm.domeni.authentis_users.repository.UserSpringRepository;
import cm.domeni.authentis_users.repository.impl.UserRepositoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfiguration {

  @Bean
  public UserRepository userRepository(UserSpringRepository userSpringRepository) {
    return new UserRepositoryImpl(userSpringRepository);
  }

  @Bean
  public UserFactory userFactory(UserRepository userRepository) {
    return new UserFactoryImpl(userRepository);
  }

  @Bean
  public UserFetcher userFetcher(UserRepository userRepository) {
    return new UserFetcherImpl(userRepository);
  }
}
