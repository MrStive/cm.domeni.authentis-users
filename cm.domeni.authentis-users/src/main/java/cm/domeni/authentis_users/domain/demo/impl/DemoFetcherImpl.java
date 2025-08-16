package cm.domeni.authentis_users.domain.demo.impl;

import cm.domeni.authentis_users.domain.demo.Demo;
import cm.domeni.authentis_users.domain.demo.DemoFetcher;
import cm.domeni.authentis_users.domain.demo.DemoRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DemoFetcherImpl implements DemoFetcher {

  private final DemoRepository demoRepository;

  @Override
  public List<Demo> loadAllDemos() {
    return demoRepository.findAll();
  }
}
