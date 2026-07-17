package blash10x.kis.ota.controller.dto;

import blash10x.kis.ota.model.MarketName;
import blash10x.kis.ota.model.OrderCode;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;
import java.util.Map;

/**
 * @author myungsik.sung@gmail.com
 */
public record CreateOrderRequest(
    @NotNull OrderCode orderCode,
    @NotNull List<String> productNos,
    @NotNull @Schema(example = MAX_REPETITIONS)
        Map<OrderCode, @NotNull @Max(REPETITION_LIMIT) Integer> maxRepetitions,
    @NotNull @Schema(example = BASE_RATES) Map<OrderCode, @NotNull Map<MarketName, @NotNull Double>> baseRates,
    // 사다리 단가가 i 에 대해 단조라는 전제(LadderOrderService 의 break)가 stepRates > 0 에 기댄다.
    @NotNull @Schema(example = STEP_RATES)
        Map<OrderCode, @NotNull Map<MarketName, @NotNull @Positive Double>> stepRates,
    @Schema(defaultValue = "false") @NotNull Boolean real) {

  /**
   * maxRepetitions 값 하나당 상한. 저(低)베타 ETF 가 stepRates 0.50 으로 가격제한폭 전체를 덮는 데 약 49단이 필요해 그 위로 잡았다.
   */
  static final int REPETITION_LIMIT = 50;

  static final String MAX_REPETITIONS =
      """
      {
        "SELL": 20,
        "BUY": 16
      }
      """;

  static final String BASE_RATES =
      """
      {
        "SELL": {
          "KOSPI200": 3.20,
          "ETF": 2.05
        },
        "BUY": {
          "KOSPI200": 3.05,
          "ETF": 2.05
        }
      }
      """;

  static final String STEP_RATES =
      """
      {
        "SELL": {
          "KOSPI200": 1.20,
          "ETF": 0.55
        },
        "BUY": {
          "KOSPI200": 1.15,
          "ETF": 0.50
        }
      }
      """;

  @JsonIgnore
  @Schema(hidden = true)
  @AssertTrue(message = "maxRepetitions, baseRates, stepRates must have an entry for the requested orderCode")
  public boolean isOrderCodeConfigured() {
    if (orderCode == null || maxRepetitions == null || baseRates == null || stepRates == null) {
      return true; // @NotNull 이 따로 보고한다
    }
    return maxRepetitions.containsKey(orderCode)
        && baseRates.containsKey(orderCode)
        && stepRates.containsKey(orderCode);
  }

  @JsonIgnore
  @Schema(hidden = true)
  @AssertTrue(message = "baseRates, stepRates must have an entry for every MarketName rate key")
  public boolean isMarketNameConfigured() {
    return hasRequiredMarketNames(baseRates) && hasRequiredMarketNames(stepRates);
  }

  /** 주문에 실제로 쓰이는 것은 요청한 orderCode 항목뿐이므로 그것만 본다. */
  private boolean hasRequiredMarketNames(Map<OrderCode, Map<MarketName, Double>> ratesByOrderCode) {
    if (orderCode == null || ratesByOrderCode == null) {
      return true; // @NotNull 이 따로 보고한다
    }
    Map<MarketName, Double> rates = ratesByOrderCode.get(orderCode);
    if (rates == null) {
      return true; // isOrderCodeConfigured 와 @NotNull 이 따로 보고한다
    }
    return rates.keySet().containsAll(MarketName.rateKeys());
  }
}
