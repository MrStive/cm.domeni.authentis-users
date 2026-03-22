package cm.domeni.authentis_users.config;

import com.domeni.kapita.jpa.autoconfigure.EnableKapitaJpaRepositories;
import cm.domeni.authentis_users.repository.DemoSpringRepository;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableKapitaJpaRepositories(basePackageClasses = DemoSpringRepository.class)
public class EclipseLinkJpaConfig {}
