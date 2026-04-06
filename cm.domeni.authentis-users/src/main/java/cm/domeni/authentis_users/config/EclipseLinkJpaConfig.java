package cm.domeni.authentis_users.config;

import cm.domeni.authentis_users.repository.DemoSpringRepository;
import com.domeni.kapita.jpa.autoconfigure.EnableKapitaJpaRepositories;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableKapitaJpaRepositories(basePackageClasses = DemoSpringRepository.class)
public class EclipseLinkJpaConfig {}
