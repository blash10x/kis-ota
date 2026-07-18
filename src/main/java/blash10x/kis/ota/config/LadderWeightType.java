package blash10x.kis.ota.config;

/**
 * 사다리 간격에 적용할 가중치 알고리즘. {@code ota.ladder-weight} 로 고른다.
 *
 * @author myungsik.sung@gmail.com
 */
public enum LadderWeightType {
  /** 52주베타 기반. */
  BETA,
  /** 52주 변동폭 기반. */
  RANGE
}
