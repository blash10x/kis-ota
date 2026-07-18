package blash10x.kis.ota.service;

/**
 * 매수 예산. 전송 루프가 단마다 {@link #tryReserve} 로 차감하며, 부족한 단은 전송하지 않는다.
 *
 * <p>매수 사다리는 아래로 갈수록 싸지므로, 부족한 단을 건너뛰어도(skip) 더 깊은 싼 단은 남은 예산에 들어갈 수
 * 있다. 한 요청의 여러 종목이 같은 예산을 공유한다.
 *
 * @author myungsik.sung@gmail.com
 */
public final class CashBudget {

  private long remaining;

  private CashBudget(long remaining) {
    this.remaining = remaining;
  }

  /** 매수가능조회로 받은 금액(원)의 예산. 음수는 0 으로 본다. */
  public static CashBudget of(long amount) {
    return new CashBudget(Math.max(0, amount));
  }

  /** 예산 제한이 없는 경로(매도)용. */
  public static CashBudget unlimited() {
    return new CashBudget(Long.MAX_VALUE);
  }

  /** 예산이 남아 있으면 차감하고 true, 부족하면 차감 없이 false. */
  public boolean tryReserve(long price) {
    if (price > remaining) {
      return false;
    }
    remaining -= price;
    return true;
  }

  public long remaining() {
    return remaining;
  }
}
