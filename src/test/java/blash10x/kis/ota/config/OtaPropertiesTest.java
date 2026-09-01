package blash10x.kis.ota.config;

import static org.assertj.core.api.Assertions.assertThat;

import blash10x.kis.ota.domain.BreakEven;
import blash10x.kis.ota.model.MarketName;
import blash10x.kis.ota.model.OrderCode;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OtaPropertiesTest {

  /**
   * ETF 매도 요율만 바꾼 설정. 하한 검증이 보는 것은 SELL 뿐이라 BUY 는 세우지 않고, KOSPI200 은 여유에서
   * 파생한 값(합 = 여유의 두 배)으로 세워 여유를 조정해도 판정에서 빠져 있게 한다.
   */
  private static OtaProperties withSellEtfRates(double base, double step) {
    double kospiRate = BreakEven.MARGIN_RATE;
    OtaProperties properties = new OtaProperties();
    properties.setBaseRates(
        Map.of(OrderCode.SELL, Map.of(MarketName.KOSPI200, kospiRate, MarketName.ETF, base)));
    properties.setStepRates(
        Map.of(OrderCode.SELL, Map.of(MarketName.KOSPI200, kospiRate, MarketName.ETF, step)));
    return properties;
  }

  @Test
  @DisplayName("코드 기본 요율은 손익분기 하한을 지킨다")
  void defaultRatesReachBreakEvenMargin() {
    assertThat(new OtaProperties().isSellRateAtLeastBreakEvenMargin()).isTrue();
  }

  @Test
  @DisplayName("매도 base+step 이 손익분기 여유에 못 미치면 기동 시점에 막는다")
  void rejectsSellRatesBelowBreakEvenMargin() {
    // 이 검증이 없으면 기동은 성공하고, 첫 매도에서야 LadderInput 예외가 종목별 skip 으로 흡수되어
    // "주문 0건 + WARN 로그"로 조용히 끝난다.
    // 경계는 여유에서 파생한다 — 리터럴이면 여유를 조정했을 때 두 케이스가 모두 하한 위로 올라가 버린다.
    // (half + half 는 double 에서도 정확히 MARGIN_RATE 다.)
    double half = BreakEven.MARGIN_RATE / 2;

    assertThat(withSellEtfRates(half - 0.05, half).isSellRateAtLeastBreakEvenMargin()).isFalse();

    // 하한 정확히는 허용한다. 첫 단이 손익분기가와 같고, 매도는 호가단위 올림이라 그 아래로 내려가지
    // 않는다(운영 설정의 ETF 매도 1.25+0.50 이 이 경계에 붙어 있다).
    assertThat(withSellEtfRates(half, half).isSellRateAtLeastBreakEvenMargin()).isTrue();
  }

  @Test
  @DisplayName("요율이 누락되거나 값이 비면 완전성 검증이 잡고, 하한 검증은 NPE 없이 통과한다")
  void leavesIncompleteRatesToCompletenessCheck() {
    // 같은 설정 오류를 두 제약이 겹쳐 보고하지 않도록, 누락은 isOrderConfigComplete 만 잡는다.
    OtaProperties missingKey = new OtaProperties();
    missingKey.setBaseRates(Map.of(OrderCode.SELL, Map.of(MarketName.ETF, 1.25)));

    assertThat(missingKey.isOrderConfigComplete()).isFalse();
    assertThat(missingKey.isSellRateAtLeastBreakEvenMargin()).isTrue();

    // ETF 키만 두고 값을 비우면 null 로 바인딩된다. 키 존재만 보면 하한 검증이 값을 꺼내 언박싱 NPE 로 터진다.
    Map<MarketName, Double> nullValued = new HashMap<>();
    nullValued.put(MarketName.KOSPI200, 3.20);
    nullValued.put(MarketName.ETF, null);
    OtaProperties nullValue = new OtaProperties();
    nullValue.setBaseRates(Map.of(OrderCode.SELL, nullValued));

    assertThat(nullValue.isOrderConfigComplete()).isFalse();
    assertThat(nullValue.isSellRateAtLeastBreakEvenMargin()).isTrue();
  }
}
