package io.spring.api;

import io.spring.application.PlatformInfoQueryService;
import java.util.HashMap;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "platform")
@AllArgsConstructor
public class PlatformInfoApi {
  private PlatformInfoQueryService platformInfoQueryService;

  @GetMapping("info")
  public ResponseEntity getInfo() {
    return ResponseEntity.ok(
        new HashMap<String, Object>() {
          {
            put("platform", platformInfoQueryService.platformInfo());
          }
        });
  }
}
