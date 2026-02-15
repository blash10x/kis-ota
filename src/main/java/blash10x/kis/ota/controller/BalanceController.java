package blash10x.kis.ota.controller;

import blash10x.kis.ota.model.Balance;
import blash10x.kis.ota.service.BalanceService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author myungsik.sung@gmail.com
 */
@Tag(name = "Trading API", description = "API 명세")
@RestController
@RequestMapping("/trading")
@RequiredArgsConstructor
public class BalanceController {
  private final BalanceService balanceService;

  @GetMapping(value = "/inquire-balance", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<Balance> inquireBalance() {
    return balanceService.inquireBalances();
  }
}
