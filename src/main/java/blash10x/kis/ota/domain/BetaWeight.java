package blash10x.kis.ota.domain;

/**
 * 연간 베타로 사다리 간격을 조절한다. 베타가 낮아도 1 근처를 유지하도록 로그를 씌워, 저(低)베타 종목에서 사다리가 지나치게 좁아지지 않게 한다.
 *
 * @author myungsik.sung@gmail.com
 */
public final class BetaWeight implements LadderWeight {

  /**
   * log(β + C) 의 이동 상수. 1.0 이면 곡선의 바닥이 β=0 에서 정확히 1.0 이 되어, 저베타 종목도 설정 요율(stepRate)
   * 자체가 최소 단 간격으로 보장된다. (이전 값 0.75 는 β=0 에서 0.71 까지 내려가 저베타 사다리가 요율보다 좁아졌다.)
   */
  private static final double C = 1.0;

  /**
   * 가중치 하한. C=1.0 이면 β≥0 에서는 곡선이 이미 1.0 이상이므로, 이 하한은 음수 베타(인버스 ETF)와 스크래핑
   * 이상값까지 같은 최소 간격 보장으로 덮고 log 정의역(β ≤ -1) 도 방어하는 안전망이다.
   */
  private static final double MIN_WEIGHT = 1.0;

  /** 사다리 간격 정규화가 기대는 1-스케일 상한. {@link RangeWeight} 와 같은 값이다. */
  private static final double MAX_WEIGHT = 2.5;

  @Override
  public double of(LadderInput input) {
    double shifted = input.yearBeta() + C;
    if (shifted <= 0) {
      return MIN_WEIGHT;
    }
    double weight = Math.log(shifted) + 1;
    return Math.max(MIN_WEIGHT, Math.min(MAX_WEIGHT, weight));
  }
}
