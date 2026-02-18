package blash10x.kis.ota.controller;

import blash10x.kis.ota.model.InterestStock;
import blash10x.kis.ota.service.InterestStocksService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping(value = "/quotations", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class InterestStocksController {
  private final InterestStocksService service;

  @Operation(summary = "관심종목 그룹별 종목조회")
  @GetMapping(value = "/intstock-stocklist-by-group")
  public List<InterestStock> inquireInterestStocks(
      @RequestParam(defaultValue = "001") String interestGroupCode) {
    return service.inquireInterestStocks(interestGroupCode);
  }
}
