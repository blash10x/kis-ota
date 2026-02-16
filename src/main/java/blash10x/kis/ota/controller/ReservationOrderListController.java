package blash10x.kis.ota.controller;

import blash10x.kis.ota.service.ReservationOrderListService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author myungsik.sung@gmail.com
 */
@Tag(name = "Trading API", description = "API 명세")
@RestController
@RequestMapping(value = "/trading", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ReservationOrderListController {
  private final ReservationOrderListService service;

  @Operation(summary = "주식예약주문조회")
  @GetMapping(value = "/inquire-resv-order")
  public JsonNode inquireReservationOrder(@RequestParam(required = false) String date) {
    return service.inquireReservationOrder(date);
  }
}
