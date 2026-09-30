package blash10x.kis.ota.domain;

/**
 * 손익분기 — 손실 종목 매도가 최소한 확보해야 하는 평단 대비 이익.
 *
 * <p>여유(margin)는 매매 수수료·세금과 최소 실현 이익을 함께 덮는 운영 판단값이라 코드가 아니라 설정
 * ({@code ota.break-even-margin-rate})에서 오고, {@link LadderInput#breakEvenMarginRate()} 에 실려 들어온다.
 * 이 값 하나에 사다리의 두 갈래가 동시에 걸려 있다 — 손실 종목 매도의 기준점
 * ({@link LadderInput#breakEvenPrice()})과, 수익 종목 매도의 첫 단이 손익분기가 위에 놓이도록 하는 설정 요율의
 * 하한({@link #isMarginReached})이다. 하한은 계산 직전({@link LadderInput})과 기동 시점({@code OtaProperties})
 * 두 곳에서 검증하는데, 같은 설정을 두고 판정이 갈리지 않도록 둘 다 여기 한 메서드를 쓴다.
 */
public final class BreakEven {

  /**
   * 요율 합의 부동소수 오차 허용치(%p). 요율은 소수 둘째 자리로 쓰는데 그 합은 double 에서 어긋날 수 있다 —
   * 2.05 + 0.55 는 2.5999999999999996 이라, 허용치가 없으면 여유 2.60 에 정확히 붙인 설정이 미달로 거부된다.
   * 설정 단위(0.01)보다 한참 작아 실제 미달은 그대로 걸린다.
   */
  private static final double RATE_TOLERANCE = 1e-9;

  private BreakEven() {}

  /**
   * 매도 첫 단 요율(base + step)이 손익분기 여유에 닿는지. 등호는 허용한다 — 첫 단이 손익분기가와 같고,
   * 매도는 호가단위 올림이라 그 아래로 내려가지 않는다.
   *
   * @param firstRungRate 매도 첫 단 깊이의 기본 요율 합(%)
   * @param marginRate 손익분기 여유(%)
   */
  public static boolean isMarginReached(double firstRungRate, double marginRate) {
    return firstRungRate >= marginRate - RATE_TOLERANCE;
  }
}
