package blash10x.kis.ota.domain;

/**
 * 52주 변동폭으로 사다리 간격을 조절한다. 변동폭이 큰 종목일수록 사다리를 넓게 편다.
 *
 * <p>변동폭 비율 {@code r = (52주최고 - 52주최저) / 기준가} 를 {@code log(r + C) + 1} 로 눌러 1 근처로 만든다. 베타 가중치와 같은
 * 로그 변환이며, 최저 변동폭 종목(r ≈ {@value #NEUTRAL_RATIO})에서 가중치가 하한 1.0 에 닿도록 상수를 잡았다.
 */
public final class RangeWeight implements LadderWeight {

  /**
   * 가중치가 1.0 이 되는 변동폭 비율. 하한이 1.0 이므로 이 값은 "분포의 어디를 바닥에 앉힐 것인가"다 — 중앙값으로
   * 잡으면 분포 하반이 전부 바닥에 접혀 변별력이 죽는다(2026-07-19 실측: 10/16종목 수렴). 보유·관심 16종목의
   * 최저 변동폭(S&P500, r=0.276) 기준으로 보정해 최저 종목만 바닥에 닿는다. 포트폴리오가 크게 바뀌면 재측정할
   * 튜닝 지점이다.
   */
  private static final double NEUTRAL_RATIO = 0.30;

  /** log(r + C) 가 r=NEUTRAL_RATIO 에서 0 이 되도록: C = 1 - NEUTRAL_RATIO. */
  private static final double C = 1.0 - NEUTRAL_RATIO;

  /**
   * 가중치 하한. {@link BetaWeight} 와 같은 1.0 으로, 저변동폭 종목도 설정 요율(stepRate) 자체가 최소 단 간격으로
   * 보장된다.
   */
  private static final double MIN_WEIGHT = 1.0;

  /** 사다리 간격 정규화가 기대는 1-스케일 상한. */
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
