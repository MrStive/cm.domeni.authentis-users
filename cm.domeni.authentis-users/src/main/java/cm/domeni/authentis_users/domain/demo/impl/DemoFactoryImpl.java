package cm.domeni.authentis_users.domain.demo.impl;

import cm.domeni.authentis_users.domain.demo.*;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DemoFactoryImpl implements DemoFactory {
  private final DemoRepository demoRepository;

  @Override
  public Demo create(DemoData demoData) {
    return demoRepository.save(Demo.builder().name(new DemoName(demoData.name())).build());
  }
}
