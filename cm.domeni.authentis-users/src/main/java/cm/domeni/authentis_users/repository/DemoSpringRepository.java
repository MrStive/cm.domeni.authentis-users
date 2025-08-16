package cm.domeni.authentis_users.repository;

import cm.domeni.authentis_users.domain.demo.Demo;
import cm.domeni.authentis_users.domain.demo.DemoId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DemoSpringRepository extends JpaRepository<Demo, DemoId> {}
