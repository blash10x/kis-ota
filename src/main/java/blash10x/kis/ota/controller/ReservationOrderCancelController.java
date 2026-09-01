package blash10x.kis.ota.controller;

import blash10x.kis.ota.model.NormalProcessingResult;
import blash10x.kis.ota.service.ReservationOrderCancelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Trading API", description = "API 명세")
@RestController
@RequestMapping(value = "/trading", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ReservationOrderCancelController {
  private final ReservationOrderCancelService service;

  @Operation(summary = "주식예약주문취소")
  @GetMapping(value = "/cancel-resv-order")
  public NormalProcessingResult cancelReservationOrder(String reservationOrderSeq) {
    return service.cancelReservationOrder(reservationOrderSeq);
  }
}
