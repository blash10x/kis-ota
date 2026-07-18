package blash10x.kis.ota.domain;

/**
 * 연간 베타로 사다리 간격을 조절한다. 베타가 낮아도 1 근처를 유지하도록 로그를 씌워, 저(低)베타 종목에서 사다리가 지나치게 좁아지지 않게 한다.
 *
 * @author myungsik.sung@gmail.com
 */
public final class BetaWeight implements LadderWeight {

  @Override
  public double of(LadderInput input) {
    return Math.log(input.yearBeta() + 0.75) + 1;
  }
}
