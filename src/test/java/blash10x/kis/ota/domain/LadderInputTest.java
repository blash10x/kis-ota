package blash10x.kis.ota.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import blash10x.kis.ota.model.MarketName;
import blash10x.kis.ota.model.OrderCode;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
  @DisplayName("매도 요율은 base+step 이 손익분기 여유 이상이어야 한다")
  void rejectsSellRatesNotReachingBreakEvenMargin() {
    // 수익 종목 매도의 손익분기 보장은 첫 단 깊이(base+step, weight 하한 1.0)가 여유 이상이라는 전제 위에 있다.
    // 설정이 전제를 깨면 계산 결과가 조용히 손익분기 아래로 내려가므로 입력 시점에 막는다.
    // 경계는 여유에서 파생한다 — 리터럴로 박아 두면 여유를 조정했을 때 경계를 벗어난 채로 통과한다.
    // (half + half 는 double 에서도 정확히 MARGIN_RATE 다.)
    double half = BreakEven.MARGIN_RATE / 2;

    // 하한 직전(합 = 여유 - 0.05)은 거부한다.
    assertThatThrownBy(() -> valid()
        .baseRates(Map.of(MarketName.ETF, half - 0.05))
        .stepRates(Map.of(MarketName.ETF, half))
        .build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("break-even margin");

    // 하한 정확히는 허용한다(운영 설정의 ETF 매도 1.25+0.50 이 이 경계에 붙어 있다). 첫 단이 손익분기가와
    // 같고, 매도는 호가단위 올림이라 그 아래로 내려가지 않는다.
    assertThat(valid()
        .baseRates(Map.of(MarketName.ETF, half))
        .stepRates(Map.of(MarketName.ETF, half))
        .build())
        .isNotNull();

    // 매수는 손익분기 개념이 없어 하한 미달 요율도 허용된다.
    assertThat(valid()
        .orderCode(OrderCode.BUY)
        .baseRates(Map.of(MarketName.ETF, half - 0.05))
        .stepRates(Map.of(MarketName.ETF, half))
        .build())
        .isNotNull();
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
