package blash10x.kis.ota.controller;

import blash10x.kis.ota.service.TradingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author myungsik.sung@gmail.com
 */
@RestController
@RequiredArgsConstructor
public class KisController {
  private final TradingService tradingService;

  @GetMapping("/")
  public Object index() {
    return tradingService.index();
  }
}
