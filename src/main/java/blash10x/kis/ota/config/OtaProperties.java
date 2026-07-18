package blash10x.kis.ota.config;

import blash10x.kis.ota.model.MarketName;
import blash10x.kis.ota.model.OrderCode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
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
}
