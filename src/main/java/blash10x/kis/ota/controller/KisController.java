package blash10x.kis.ota.controller;

import blash10x.kis.ota.external.KisAuthService;
import blash10x.kis.ota.model.AccessToken;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/oauth2")
@RequiredArgsConstructor
public class KisController {
  private final KisAuthService service;

  @Operation(summary = "접근토큰발급")
  @PostMapping(value = "/authorize")
  public AccessToken token() {
    return service.authorize();
  }
}
