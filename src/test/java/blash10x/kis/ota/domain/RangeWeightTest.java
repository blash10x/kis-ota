package blash10x.kis.ota.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import blash10x.kis.ota.model.MarketName;
import blash10x.kis.ota.model.OrderCode;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author myungsik.sung@gmail.com
 */
class RangeWeightTest {

  private final RangeWeight rangeWeight = new RangeWeight();

  private static LadderInput.LadderInputBuilder base() {
    return LadderInput.builder()
        .orderCode(OrderCode.SELL)
        .marketName(MarketName.ETF)
        .realtimePrice(54_240)
        .upperPriceLimit(70_510)
        .lowerPriceLimit(37_970)
        .dayOverDayRate(0.0)
        .yearBeta(1.0)
        .purchaseAvgPrice(0.0)
        .size(1)
        .baseRates(Map.of(MarketName.ETF, 2.05))
        .stepRates(Map.of(MarketName.ETF, 0.55));
  }

  @Test
  @DisplayName("중앙 변동성(r=0.89)에서 가중치가 1.0 이다")
  void weightIsOneAtMedianVolatility() {
    // MEDIAN_RATIO 로 보정한 중앙값. (189-100)/100 = 0.89
    LadderInput input = base().yearHigh(189).yearLow(100).standardPrice(100).build();
    assertThat(rangeWeight.of(input)).isCloseTo(1.0, within(0.001));
  }

  @Test
  @DisplayName("449450 실제 값(변동폭 77%)은 중앙보다 낮아 0.87 근처가 된다")
  void weightForRealEtf() {
    // (87,946 - 46,061) / 54,240 = 0.7722 → log(0.7722+0.11)+1 = 0.8747
    LadderInput input = base().yearHigh(87_946).yearLow(46_061).standardPrice(54_240).build();
    assertThat(rangeWeight.of(input)).isCloseTo(0.8747, within(0.001));
  }

  @Test
  @DisplayName("변동성이 커질수록 가중치가 커진다")
  void weightIncreasesWithVolatility() {
    // clamp 에 걸리지 않는 중간 구간(r=0.60, 1.20)에서 단조 증가를 본다.
    double low = rangeWeight.of(base().yearHigh(160).yearLow(100).standardPrice(100).build());
    double high = rangeWeight.of(base().yearHigh(220).yearLow(100).standardPrice(100).build());
    assertThat(low).isCloseTo(0.6575, within(0.001));
    assertThat(high).isCloseTo(1.2700, within(0.001));
    assertThat(high).isGreaterThan(low);
  }

  @Test
  @DisplayName("극단값은 사다리 간격 정규화가 기대는 1-스케일(0.5~2.5)로 가둔다")
  void clampsToOneScale() {
    // r=0.1 → raw 0.489 → 하한 0.5 로 올린다
    assertThat(rangeWeight.of(base().yearHigh(110).yearLow(100).standardPrice(100).build()))
        .isEqualTo(0.5);
    // r 이 아무리 커도 상한 2.5 를 넘지 않는다
    assertThat(rangeWeight.of(base().yearHigh(1_000_000).yearLow(1).standardPrice(100).build()))
        .isEqualTo(2.5);
  }

  @Test
  @DisplayName("스크래핑 실패나 비정상 데이터는 중립값 1.0 으로 떨어진다")
  void neutralWhenDataMissing() {
    // 최고/최저 미조회(0)
    assertThat(rangeWeight.of(base().yearHigh(0).yearLow(0).standardPrice(54_240).build()))
        .isEqualTo(1.0);
    // 기준가 미조회(0) — 0 으로 나눌 수 없다
    assertThat(rangeWeight.of(base().yearHigh(87_946).yearLow(46_061).standardPrice(0).build()))
        .isEqualTo(1.0);
    // 최고 < 최저 (비정상)
    assertThat(rangeWeight.of(base().yearHigh(100).yearLow(200).standardPrice(100).build()))
        .isEqualTo(1.0);
  }
}
