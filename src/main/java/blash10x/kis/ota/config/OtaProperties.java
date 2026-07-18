package blash10x.kis.ota.config;

import blash10x.kis.ota.model.MarketName;
import blash10x.kis.ota.model.OrderCode;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * @author myungsik.sung@gmail.com
 */
@Configuration
@ConfigurationProperties(prefix = "ota")
@Validated
@Data
public class OtaProperties {
  private Map<MarketName, String> extractionUrls;
  /** 사다리 가중치 알고리즘. 설정이 없으면 기존과 동일하게 베타를 쓴다. */
  private LadderWeightType ladderWeight = LadderWeightType.BETA;

  /**
   * 주문 구분별 사다리 최대 단수. 요청마다 받던 값을 전역 설정으로 옮겼다. 설정이 없으면 종전 기본값을 쓴다.
   *
   * <p>값 하나당 상한 50: 저(低)베타 ETF 가 stepRates 0.50 으로 가격제한폭 전체를 덮는 데 약 49단이 필요해 그 위로 잡았다.
   */
  private Map<OrderCode, @NotNull @Max(50) Integer> maxRepetitions =
      Map.of(OrderCode.SELL, 20, OrderCode.BUY, 16);

  /** 주문 구분·시장별 기본 요율(사다리 첫 단의 벌어짐). 요청마다 받던 값을 전역 설정으로 옮겼다. */
  private Map<OrderCode, Map<MarketName, @NotNull Double>> baseRates = Map.of(
      OrderCode.SELL, Map.of(MarketName.KOSPI200, 3.20, MarketName.ETF, 2.05),
      OrderCode.BUY, Map.of(MarketName.KOSPI200, 3.05, MarketName.ETF, 2.05));

  /**
   * 주문 구분·시장별 단계 요율(단 간격). 사다리 단가가 i 에 대해 단조라는 전제(LadderPricer 의 break)가 양수에 기댄다.
   */
  private Map<OrderCode, Map<MarketName, @NotNull @Positive Double>> stepRates = Map.of(
      OrderCode.SELL, Map.of(MarketName.KOSPI200, 1.20, MarketName.ETF, 0.55),
      OrderCode.BUY, Map.of(MarketName.KOSPI200, 1.15, MarketName.ETF, 0.50));

  /** 요청 시점이 아니라 기동 시점에 설정 누락을 막는다. 누락된 채 주문이 실행되면 NPE 로 터진다. */
  @AssertTrue(message = "max-repetitions, base-rates, step-rates must have an entry"
      + " for every OrderCode, and rates for every MarketName rate key")
  public boolean isOrderConfigComplete() {
    for (OrderCode orderCode : OrderCode.values()) {
      if (!maxRepetitions.containsKey(orderCode)
          || !hasAllRateKeys(baseRates.get(orderCode))
          || !hasAllRateKeys(stepRates.get(orderCode))) {
        return false;
      }
    }
    return true;
  }

  private static boolean hasAllRateKeys(Map<MarketName, Double> rates) {
    return rates != null && rates.keySet().containsAll(MarketName.rateKeys());
  }
}
