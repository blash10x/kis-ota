package blash10x.kis.ota.config;

import blash10x.kis.ota.model.MarketName;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author myungsik.sung@gmail.com
 */
@Configuration
@ConfigurationProperties(prefix = "ota")
@Data
public class OtaProperties {
  private Map<MarketName, String> extractionUrls;
  /** 사다리 가중치 알고리즘. 설정이 없으면 기존과 동일하게 베타를 쓴다. */
  private LadderWeightType ladderWeight = LadderWeightType.BETA;
}
