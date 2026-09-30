package blash10x.kis.ota.domain;

/** 도메인 테스트가 함께 쓰는 입력값. */
final class LadderFixtures {

  /**
   * 손익분기 여유(%). 매도 {@link LadderInput} 의 필수값이라 여유와 무관한 테스트도 채워야 한다.
   *
   * <p>{@code LadderPricerTest} 의 주문가 기대값(손익분기가 = 평단*1.0165)이 이 값에 맞춰져 있다.
   */
  static final double BREAK_EVEN_MARGIN_RATE = 1.65;

  private LadderFixtures() {}
}
