package blash10x.kis.ota.controller;

import blash10x.kis.ota.controller.dto.CreateOrderRequest;
import blash10x.kis.ota.model.OrderResult;
import blash10x.kis.ota.service.CashOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
public class CashOrderController {
  private final CashOrderService service;

  @Operation(summary = "주식주문(현금)")
  @PostMapping(value = "/order-cash")
  public List<OrderResult> orderCash(
      @RequestBody @Valid CreateOrderRequest request) {
    return service.order(request);
  }
}
