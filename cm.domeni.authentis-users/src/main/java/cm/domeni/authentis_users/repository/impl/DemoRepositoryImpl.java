package cm.domeni.authentis_users.repository.impl;

import cm.domeni.authentis_users.domain.demo.Demo;
import cm.domeni.authentis_users.domain.demo.DemoRepository;
import cm.domeni.authentis_users.repository.DemoSpringRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DemoRepositoryImpl implements DemoRepository {
  private final DemoSpringRepository demoSpringRepository;

  @Override
  public Demo save(Demo value) {
    return demoSpringRepository.save(value);
  }

  @Override
  public List<Demo> findAll() {
    return new ArrayList<>(demoSpringRepository.findAll());
  }
}
