package blash10x.kis.ota.controller;

import blash10x.kis.ota.model.Balance;
import blash10x.kis.ota.model.OrderCode;
import blash10x.kis.ota.service.BalanceService;
import blash10x.kis.ota.service.ReservationService;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author myungsik.sung@gmail.com
 */
@RestController("/trading")
@RequiredArgsConstructor
public class KisController {
  private final BalanceService balanceService;
  private final ReservationService reservationService;

  @GetMapping(value = "/inquire-balance", produces = MediaType.APPLICATION_JSON_VALUE)
  public List<Balance> inquireBalance() {
    return balanceService.inquireBalance();
  }

  @GetMapping(value = "/order-resv", produces = MediaType.APPLICATION_JSON_VALUE)
  public JsonNode orderReservation(OrderCode orderCode) {
    return reservationService.orderReservation(orderCode);
  }
}
