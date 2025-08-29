package cm.domeni.authentis_users.service;

import cm.domeni.authentis_users.domain.demo.Demo;
import cm.domeni.authentis_users.domain.demo.DemoFactory;
import cm.domeni.authentis_users.domain.demo.DemoFetcher;
import cm.domeni.authentis_users.domain.demo.DemoId;
import cm.domeni.authentis_users.dto.DemoDTO;
import cm.domeni.authentis_users.service.mapper.DemoMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class DemoService {
  private final DemoFactory demoFactory;
  private final DemoFetcher demoFetcher;
  private final DemoMapper demoMapper;

  @Transactional
  public UUID createDemo(DemoDTO data) {
    return Optional.ofNullable(data)
        .map(demoMapper::map)
        .map(demoFactory::create)
        .map(Demo::getId)
        .map(DemoId::getValue)
        .map(UUID::fromString)
        .orElseThrow();
  }

  @Transactional(readOnly = true)
  public List<DemoDTO> fetchAllDemos() {
    return demoFetcher.loadAllDemos().stream().map(demoMapper::map).toList();
  }
}
