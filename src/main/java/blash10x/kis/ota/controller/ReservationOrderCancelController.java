package blash10x.kis.ota.controller;

import blash10x.kis.ota.model.NormalProcessingResult;
import blash10x.kis.ota.service.ReservationOrderCancelService;
import io.swagger.v3.oas.annotations.tags.Tag;
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
public class ReservationOrderCancelController {
  private final ReservationOrderCancelService reservationOrderCancelService;

  @GetMapping(value = "/cancel-resv-order", produces = MediaType.APPLICATION_JSON_VALUE)
  public NormalProcessingResult cancelReservationOrder(String reservationOrderSeq) {
    return reservationOrderCancelService.cancelReservationOrder(reservationOrderSeq);
  }
}
