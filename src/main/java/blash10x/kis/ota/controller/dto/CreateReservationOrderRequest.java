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
    @Schema(example = BASE_RATES) Map<MarketName, Double> baseRates,
    @Schema(example = STEP_RATES) Map<MarketName, Double> stepRates,
    @Schema(example = MULTIPLE_RATES) Map<OrderCode, Double> multipleRates,
    @Schema(defaultValue = "false") @NotNull Boolean real) {

  static final String MAX_REPETITIONS =
      """
      {
        "SELL": 20,
        "BUY": 11
      }
      """;

  static final String BASE_RATES =
      """
      {
        "KOSPI200": 1.8,
        "ETF": 1.45
      }
      """;

  static final String STEP_RATES =
      """
      {
        "KOSPI200": 1.5,
        "ETF": 0.6
      }
      """;

  static final String MULTIPLE_RATES =
      """
      {
        "SELL": 1.0,
        "BUY": 0.8
      }
      """;
}
