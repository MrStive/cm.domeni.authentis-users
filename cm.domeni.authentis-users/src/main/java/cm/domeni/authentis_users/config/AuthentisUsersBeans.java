package cm.domeni.authentis_users.config;

import cm.domeni.authentis_users.domain.demo.DemoFactory;
import cm.domeni.authentis_users.domain.demo.DemoFetcher;
import cm.domeni.authentis_users.domain.demo.DemoRepository;
import cm.domeni.authentis_users.domain.demo.impl.DemoFactoryImpl;
import cm.domeni.authentis_users.domain.demo.impl.DemoFetcherImpl;
import cm.domeni.authentis_users.repository.DemoSpringRepository;
import cm.domeni.authentis_users.repository.impl.DemoRepositoryImpl;
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
}
