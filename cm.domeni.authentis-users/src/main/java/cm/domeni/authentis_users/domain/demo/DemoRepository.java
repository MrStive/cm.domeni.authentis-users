package cm.domeni.authentis_users.domain.demo;

import java.util.List;

public interface DemoRepository {

  Demo save(Demo value);

  List<Demo> findAll();
}
