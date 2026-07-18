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
 * @param purchaseAvgPrice 매입 평균가. 보유하지 않은 종목(매수)은 0 이다
 * @param evaluationProfitLossRatio 평가손익률. 매도에서만 읽으며, 그 외에는 0 이다
 * @param size 사다리 최대 단수. 0 이면 주문하지 않는다
 * @param baseRates 시장별 기본 요율
 * @param stepRates 시장별 단계 요율
 * @author myungsik.sung@gmail.com
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
    double purchaseAvgPrice,
    double evaluationProfitLossRatio,
    int size,
    Map<MarketName, Double> baseRates,
    Map<MarketName, Double> stepRates) {

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
  }

  public double baseRate() {
    return baseRates.get(marketName.rateKey());
  }

  public double stepRate() {
    return stepRates.get(marketName.rateKey());
  }
}
