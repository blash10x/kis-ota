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
  @DisplayName("베타 1.0 이면 로그 변환으로 1 근처가 된다")
  void weightNearOneAtBetaOne() {
    // log(1.75)+1 = 1.5596...
    assertThat(betaWeight.of(withBeta(1.0))).isCloseTo(1.56, within(0.01));
  }

  @ParameterizedTest(name = "yearBeta={0}")
  @ValueSource(doubles = {0.0, 0.3, 1.0, 2.0, 3.0})
  @DisplayName("가중치는 사다리 간격 정규화가 기대는 1 스케일(0.5~2.5)을 벗어나지 않는다")
  void weightStaysNearOneScale(double yearBeta) {
    // LadderWeight 계약: 값이 1 근처여야 단계 간격(weight*stepRate)이 의도한 스케일을 유지한다.
    assertThat(betaWeight.of(withBeta(yearBeta))).isBetween(0.5, 2.5);
  }

  @Test
  @DisplayName("베타가 높을수록 사다리를 넓히는 가중치가 커진다")
  void weightIncreasesWithBeta() {
    assertThat(betaWeight.of(withBeta(2.0))).isGreaterThan(betaWeight.of(withBeta(0.5)));
  }
}
