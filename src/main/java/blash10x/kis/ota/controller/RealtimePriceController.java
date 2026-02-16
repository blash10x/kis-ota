package blash10x.kis.ota.controller;

import blash10x.kis.ota.model.MarketCode;
import blash10x.kis.ota.model.ProductPrice;
import blash10x.kis.ota.service.RealtimePriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
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
public class RealtimePriceController {
  private final RealtimePriceService service;

  @Operation(summary = "주식현재가 시세")
  @GetMapping(value = "/inquire-price")
  public List<ProductPrice> inquireBalance(
      MarketCode marketCode,
      @Schema(description = "종목코드 (',' 구분자 복수개 허용: 예, ...&id=123,467,789)",
              example = "005930",
              implementation = String.class)
      @RequestParam(name = "productNo") List<String> productNos) {
    return service.inquirePrices(marketCode, productNos);
  }
}
