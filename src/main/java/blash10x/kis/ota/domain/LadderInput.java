package blash10x.kis.ota.domain;

import blash10x.kis.ota.model.MarketName;
import blash10x.kis.ota.model.OrderCode;
import java.util.Map;
import java.util.Objects;
import lombok.Builder;

/**
 * 사다리 하나를 계산하는 데 필요한 값 전부. 조회가 모두 끝난 뒤의 스냅샷이라 KIS 도, Spring 도 모른다.
 *
 * @param orderCode 매도/매수
 * @param marketName 요율 조회에는 {@link MarketName#rateKey()} 를 쓰지만, 호가단위는 ETF 여부를 봐야 해서 원본을 그대로 들고 있다
 * @param realtimePrice 현재가. 사다리는 이 값을 기준으로 벌어진다
 * @param upperPriceLimit 상한가. 전일 종가(기준가) 기준이라 현재가로 환산할 수 없어 KIS 가 계산해 준 값을 그대로 받는다
 * @param lowerPriceLimit 하한가. 상한가와 같은 이유로 KIS 값을 그대로 받는다
 * @param dayOverDayRate 전일 대비 등락률
 * @param yearBeta 스크래핑한 연간 베타 원본. {@link BetaWeight} 가 사다리 가중치로 환산한다
 * @param yearHigh 스크래핑한 52주 최고가. 스크래핑 실패 시 0. 변동폭 기반 가중치가 쓴다
 * @param yearLow 스크래핑한 52주 최저가. 스크래핑 실패 시 0. 변동폭 기반 가중치가 쓴다
 * @param standardPrice 기준가(전일 종가). 변동폭을 가격 대비 비율로 환산하는 분모. {@link RangeWeight} 가 쓴다
 * @param purchaseAvgPrice 매입 평균가. 손실 종목 매도의 사다리 기준점(손익분기가)이 된다. 보유하지 않은 종목(매수)은 0 이다
 * @param size 사다리 최대 단수. 0 이면 주문하지 않는다
 * @param baseRates 시장별 기본 요율
 * @param stepRates 시장별 단계 요율
 * @param breakEvenMarginRate 손익분기 여유(%). 평단 대비 최소 확보 이익률로, 손실 종목 매도의 기준점
 *     ({@link #breakEvenPrice()})과 매도 요율 하한({@link BreakEven})이 함께 이 값을 본다. 매수는 쓰지 않는다
 */
@Builder
public record LadderInput(
    OrderCode orderCode,
    MarketName marketName,
    int realtimePrice,
    int upperPriceLimit,
    int lowerPriceLimit,
    double dayOverDayRate,
    double yearBeta,
    double yearHigh,
    double yearLow,
    double standardPrice,
    double purchaseAvgPrice,
    int size,
    Map<MarketName, Double> baseRates,
    Map<MarketName, Double> stepRates,
    double breakEvenMarginRate) {

  /**
   * 빌더는 빠뜨린 값을 null 로 조용히 채우고, 그 대가는 한참 뒤 계산 중의 NPE 로 돌아온다. 여기서 막는다.
   *
   * <p>size 는 0 을 허용한다. 매도 가능 수량이 없으면 정상적으로 0 이다.
   */
  public LadderInput {
    Objects.requireNonNull(orderCode, "orderCode");
    Objects.requireNonNull(marketName, "marketName");
    Objects.requireNonNull(baseRates, "baseRates");
    Objects.requireNonNull(stepRates, "stepRates");
    if (size < 0) {
      throw new IllegalArgumentException("size must not be negative: " + size);
    }
    // 수익 종목 매도의 손익분기 보장은 첫 단 깊이 weight*(base+step)(weight 하한 1.0)가 손익분기
    // 여유(breakEvenMarginRate) 이상이라는 데 기댄다(LadderPricer 의 기준점 선택 참고). 설정이 이 전제를
    // 깨면 사다리가 조용히 손익분기 아래에서 시작한다. 기동 시점 검증(OtaProperties)이 1차 방어선이고,
    // 여기는 계산 직전의 최종 방어선이다. 인스턴스 메서드(baseRate())는 필드 대입 전이라 못 쓴다.
    if (OrderCode.SELL == orderCode) {
      // 빌더가 여유를 빠뜨리면 0.0 으로 채워져, 손익분기가가 평단 그 자체가 되고 아래 하한 검증도 그냥 통과한다.
      if (breakEvenMarginRate <= 0) {
        throw new IllegalArgumentException(
            "breakEvenMarginRate must be positive: " + breakEvenMarginRate);
      }
      double firstRungRate =
          baseRates.get(marketName.rateKey()) + stepRates.get(marketName.rateKey());
      if (!BreakEven.isMarginReached(firstRungRate, breakEvenMarginRate)) {
        throw new IllegalArgumentException("SELL base+step must reach the break-even margin "
            + breakEvenMarginRate + "%: " + firstRungRate);
      }
    }
  }

  public double baseRate() {
    return baseRates.get(marketName.rateKey());
  }

  public double stepRate() {
    return stepRates.get(marketName.rateKey());
  }

  /**
   * 손익분기가. 평단에 여유를 얹은 값으로, 손실 종목 매도 사다리의 기준점이다. 여유를 인자로 받지 않고
   * 생성자가 요율 하한을 검증한 바로 그 값을 쓴다 — 기준점과 하한이 서로 다른 여유를 볼 길을 없앤다.
   */
  public double breakEvenPrice() {
    return purchaseAvgPrice * (1 + breakEvenMarginRate / 100);
  }
}
