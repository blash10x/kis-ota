package blash10x.kis.ota.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import blash10x.kis.ota.model.MarketName;
import blash10x.kis.ota.model.OrderCode;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author myungsik.sung@gmail.com
 */
class LadderInputTest {

  private static LadderInput.LadderInputBuilder valid() {
    return LadderInput.builder()
        .orderCode(OrderCode.SELL)
        .marketName(MarketName.ETF)
        .realtimePrice(10_000)
        .upperPriceLimit(13_000)
        .lowerPriceLimit(7_000)
        .dayOverDayRate(0.0)
        .yearBeta(1.0)
        .purchaseAvgPrice(1_000)
        .evaluationProfitLossRatio(900.0)
        .size(3)
        .baseRates(Map.of(MarketName.ETF, 2.05))
        .stepRates(Map.of(MarketName.ETF, 0.55));
  }

  @Test
  @DisplayName("빌더가 필수 값을 빠뜨리면 만들 때 막는다")
  void rejectsMissingRequiredValues() {
    // 막지 않으면 한참 뒤 계산 중에 NPE 로 터진다.
    assertThatNullPointerException().isThrownBy(() -> valid().marketName(null).build());
    assertThatNullPointerException().isThrownBy(() -> valid().orderCode(null).build());
    assertThatNullPointerException().isThrownBy(() -> valid().baseRates(null).build());
    assertThatNullPointerException().isThrownBy(() -> valid().stepRates(null).build());
  }

  @Test
  @DisplayName("음수 단수는 막고, 0 은 허용한다")
  void rejectsNegativeSizeButAllowsZero() {
    assertThatThrownBy(() -> valid().size(-1).build())
        .isInstanceOf(IllegalArgumentException.class);

    // 매도 가능 수량이 없으면 정상적으로 0 이다.
    assertThat(valid().size(0).build().size()).isZero();
  }

  @Test
  @DisplayName("KOSPI 는 KOSPI200 요율로 조회한다")
  void kospiReadsKospi200Rates() {
    LadderInput input = valid()
        .marketName(MarketName.KOSPI)
        .baseRates(Map.of(MarketName.KOSPI200, 3.20))
        .stepRates(Map.of(MarketName.KOSPI200, 1.20))
        .build();

    assertThat(input.baseRate()).isEqualTo(3.20);
    assertThat(input.stepRate()).isEqualTo(1.20);
  }
}
