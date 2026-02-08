package blash10x.kis.ota.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author myungsik.sung@gmail.com
 */
@Service
@RequiredArgsConstructor
public class TradingService {
  private final BalanceService balanceService;

  public Object inquireBalance() {
    return balanceService.inquireBalance();
  }
}
