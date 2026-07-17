package blash10x.kis.ota.model;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author myungsik.sung@gmail.com
 */
public enum MarketName {
  KOSPI, KOSPI200, ETF;

  private static final Set<MarketName> RATE_KEYS =
      Arrays.stream(values()).map(MarketName::rateKey).collect(Collectors.toUnmodifiableSet());

  /** 요율(baseRates/stepRates) 조회에 쓰는 구분. KOSPI 는 KOSPI200 과 같은 요율을 쓴다. */
  public MarketName rateKey() {
    return this == KOSPI ? KOSPI200 : this;
  }

  /** 요청의 요율 맵이 반드시 담아야 하는 구분. 새 시장을 추가하면 자동으로 필수가 된다. */
  public static Set<MarketName> rateKeys() {
    return RATE_KEYS;
  }
}
