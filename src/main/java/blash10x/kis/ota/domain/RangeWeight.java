package blash10x.kis.ota.domain;

/**
 * 52주 변동폭으로 사다리 간격을 조절한다. 변동폭이 큰 종목일수록 사다리를 넓게 편다.
 *
 * <p>변동폭 비율 {@code r = (52주최고 - 52주최저) / 기준가} 를 {@code log(r + C) + 1} 로 눌러 1 근처로 만든다. 베타 가중치와 같은
 * 로그 변환이며, 중간 변동성(r ≈ {@value #MEDIAN_RATIO})에서 가중치가 1.0 이 되도록 상수를 잡았다.
 *
 * @author myungsik.sung@gmail.com
 */
public final class RangeWeight implements LadderWeight {

  /**
   * 가중치가 1.0 이 되는 변동폭 비율. 2026-07-18 보유 11종목의 변동폭 비율 중앙값(≈0.89)으로 보정했다. 포트폴리오가 크게 바뀌면
   * 다시 실측해 조정할 튜닝 지점이다.
   */
  private static final double MEDIAN_RATIO = 0.89;

  /** log(r + C) 가 r=MEDIAN_RATIO 에서 0 이 되도록: C = 1 - MEDIAN_RATIO. */
  private static final double C = 1.0 - MEDIAN_RATIO;

  /** 손실 회피 가드가 기대는 1-스케일을 벗어나지 않도록, 극단 종목의 가중치를 이 범위로 가둔다. */
  private static final double MIN_WEIGHT = 0.5;
  private static final double MAX_WEIGHT = 2.5;

  @Override
  public double of(LadderInput input) {
    double base = input.standardPrice();
    double range = input.yearHigh() - input.yearLow();
    // 스크래핑 실패(0)나 비정상 데이터(최고 < 최저)면 중립값으로 둔다.
    if (base <= 0 || range <= 0) {
      return 1.0;
    }
    double weight = Math.log(range / base + C) + 1;
    return Math.max(MIN_WEIGHT, Math.min(MAX_WEIGHT, weight));
  }
}
