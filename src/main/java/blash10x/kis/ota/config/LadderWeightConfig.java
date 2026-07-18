package blash10x.kis.ota.config;

import blash10x.kis.ota.domain.BetaWeight;
import blash10x.kis.ota.domain.LadderWeight;
import blash10x.kis.ota.domain.RangeWeight;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 전역 설정({@code ota.ladder-weight})에 따라 활성 {@link LadderWeight} 를 고른다. 도메인 구현체를 Spring 과 분리해 두기 위해
 * 애너테이션 대신 이 팩터리에서 조립한다.
 *
 * @author myungsik.sung@gmail.com
 */
@Configuration
public class LadderWeightConfig {

  @Bean
  public LadderWeight ladderWeight(OtaProperties otaProperties) {
    return switch (otaProperties.getLadderWeight()) {
      case BETA -> new BetaWeight();
      case RANGE -> new RangeWeight();
    };
  }
}
