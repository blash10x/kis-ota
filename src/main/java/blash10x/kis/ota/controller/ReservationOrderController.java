package blash10x.kis.ota.controller;

import blash10x.kis.ota.controller.dto.CreateReservationOrderRequest;
import blash10x.kis.ota.model.ReservationOrderSeq;
import blash10x.kis.ota.service.ReservationOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author myungsik.sung@gmail.com
 */
@Tag(name = "Trading API", description = "API 명세")
@RestController
@RequestMapping(value = "/trading", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ReservationOrderController {
  private final ReservationOrderService service;

  @Operation(summary = "주식예약주문")
  @PostMapping(value = "/order-resv")
  public List<ReservationOrderSeq> orderReservation(
      @RequestBody CreateReservationOrderRequest request) {
    return service.orderReservation(request.productNos(), request.orderCode(), request.real());
  }
}
