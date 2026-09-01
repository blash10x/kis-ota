package blash10x.kis.ota.domain;

/**
 * 손익분기 — 손실 종목 매도가 최소한 확보해야 하는 평단 대비 이익.
 *
 * <p>여유(margin)는 매매 수수료·세금과 최소 실현 이익을 함께 덮는 운영 판단값이라 조정된다. 이 값 하나에
 * 사다리의 두 갈래가 동시에 걸려 있어 여기 모아 둔다 — {@link LadderPricer} 는 손실 종목 매도의 기준점을
 * 여기서 잡고, {@link LadderInput} 은 수익 종목 매도의 첫 단이 손익분기가 위에 놓이도록 설정 요율의 하한을
 * 여기에 맞춰 검증한다(기동 시점 검증은 {@code OtaProperties}). 한쪽만 바꾸면 컴파일도 테스트도 통과하면서
 * 손익분기 보장만 조용히 깨지므로, 두 곳이 같은 상수를 보게 한다.
 */
public final class BreakEven {

  /** 평단 대비 최소 확보 이익률(%). */
  public static final double MARGIN_RATE = 1.75;

  private BreakEven() {}

  /** 손익분기가. 평단에 여유를 얹은 값으로, 손실 종목 매도 사다리의 기준점이다. */
  public static double priceOf(double purchaseAvgPrice) {
    return purchaseAvgPrice * (1 + MARGIN_RATE / 100);
  }
}
