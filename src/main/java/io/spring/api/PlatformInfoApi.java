package io.spring.api;

import io.spring.application.PlatformInfoQueryService;
import io.spring.application.data.PlatformInfoData;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/platform")
@AllArgsConstructor
public class PlatformInfoApi {
  private PlatformInfoQueryService platformInfoQueryService;

  @GetMapping("info")
  public ResponseEntity<PlatformInfoData> platformInfo() {
    return ResponseEntity.ok(platformInfoQueryService.platformInfo());
  }
}
