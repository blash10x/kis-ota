package blash10x.kis.ota.controller.dto;

import blash10x.kis.ota.model.MarketName;
import blash10x.kis.ota.model.OrderCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * @author myungsik.sung@gmail.com
 */
public record CreateReservationOrderRequest(
    @NotNull OrderCode orderCode,
    List<String> productNos,
    @Schema(example = MAX_REPETITIONS) Map<OrderCode, Integer> maxRepetitions,
    @Schema(example = BASE_RATES) Map<OrderCode, Map<MarketName, Double>> baseRates,
    @Schema(example = STEP_RATES) Map<OrderCode, Map<MarketName, Double>> stepRates,
    @Schema(example = MULTIPLE_RATES) Map<OrderCode, Double> multipleRates,
    @Schema(defaultValue = "false") @NotNull Boolean real) {

  static final String MAX_REPETITIONS =
      """
      {
        "SELL": 35,
        "BUY": 20
      }
      """;

  static final String BASE_RATES =
      """
      {
        "SELL": {
          "KOSPI200": 2.10,
          "ETF": 1.55
        },
        "BUY": {
          "KOSPI200": 2.20,
          "ETF": 1.60
        }
      }
      """;

  static final String STEP_RATES =
      """
      {
        "SELL": {
          "KOSPI200": 1.15,
          "ETF": 0.45
        },
        "BUY": {
          "KOSPI200": 1.10,
          "ETF": 0.40
        }
      }
      """;

  static final String MULTIPLE_RATES =
      """
      {
        "SELL": 1.0,
        "BUY": 0.80
      }
      """;
}
