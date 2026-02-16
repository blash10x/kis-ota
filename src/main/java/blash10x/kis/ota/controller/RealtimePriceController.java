package blash10x.kis.ota.controller;

import blash10x.kis.ota.model.MarketCode;
import blash10x.kis.ota.model.ProductPrice;
import blash10x.kis.ota.service.RealtimePriceService;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping(value = "/trading", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class RealtimePriceController {
  private final RealtimePriceService service;

  @Operation(summary = "주식현재가 시세")
  @GetMapping(value = "/inquire-price")
  public ProductPrice inquireBalance(MarketCode marketCode, String productNo) {
    return service.inquirePrice(marketCode, productNo);
  }
}
