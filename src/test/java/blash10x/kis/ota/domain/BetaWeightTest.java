package blash10x.kis.ota.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import blash10x.kis.ota.model.MarketName;
import blash10x.kis.ota.model.OrderCode;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author myungsik.sung@gmail.com
 */
class BetaWeightTest {

  private final BetaWeight betaWeight = new BetaWeight();

  private static LadderInput withBeta(double yearBeta) {
    return LadderInput.builder()
        .orderCode(OrderCode.SELL)
        .marketName(MarketName.KOSPI200)
        .realtimePrice(10_000)
        .upperPriceLimit(13_000)
        .lowerPriceLimit(7_000)
        .dayOverDayRate(0.0)
        .yearBeta(yearBeta)
        .purchaseAvgPrice(1_000)
        .size(1)
        .baseRates(Map.of(MarketName.KOSPI200, 3.20))
        .stepRates(Map.of(MarketName.KOSPI200, 1.20))
        .build();
  }

  @Test
  @DisplayName("베타 1.0 이면 log(2)+1 = 1.69 가 된다")
  void weightAtBetaOne() {
    assertThat(betaWeight.of(withBeta(1.0))).isCloseTo(1.69, within(0.01));
  }

  @Test
  @DisplayName("가중치는 1.0 아래로 내려가지 않아 저베타 종목도 최소 간격(설정 요율)이 보장된다")
  void weightNeverDropsBelowOne() {
    // C=1.0 이라 β=0 에서 곡선 자체가 정확히 1.0 이다.
    assertThat(betaWeight.of(withBeta(0.0))).isEqualTo(1.0);
    // 음수 베타(인버스 ETF)나 스크래핑 이상값은 하한 clamp 가 받는다. log 정의역(β ≤ -1)도 여기서 방어된다.
    assertThat(betaWeight.of(withBeta(-0.5))).isEqualTo(1.0);
    assertThat(betaWeight.of(withBeta(-1.5))).isEqualTo(1.0);
  }

  @Test
  @DisplayName("극단 고베타는 상한 2.5 로 가둔다")
  void clampsCeilingAtHighBeta() {
    // log(16)+1 = 3.77 이지만 1-스케일 상한에 걸린다.
    assertThat(betaWeight.of(withBeta(15.0))).isEqualTo(2.5);
  }

  @ParameterizedTest(name = "yearBeta={0}")
  @ValueSource(doubles = {-1.5, 0.0, 0.3, 1.0, 2.0, 3.0, 15.0})
  @DisplayName("가중치는 사다리 간격 정규화가 기대는 [1.0, 2.5] 를 벗어나지 않는다")
  void weightStaysInScale(double yearBeta) {
    assertThat(betaWeight.of(withBeta(yearBeta))).isBetween(1.0, 2.5);
  }

  @Test
  @DisplayName("베타가 높을수록 사다리를 넓히는 가중치가 커진다")
  void weightIncreasesWithBeta() {
    assertThat(betaWeight.of(withBeta(2.0))).isGreaterThan(betaWeight.of(withBeta(0.5)));
  }
}
