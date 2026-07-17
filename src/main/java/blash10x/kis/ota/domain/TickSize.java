package blash10x.kis.ota.domain;

import blash10x.kis.ota.model.MarketName;
import blash10x.kis.ota.model.OrderCode;

/**
 * KRX 호가단위 규칙.
 *
 * @author myungsik.sung@gmail.com
 */
public final class TickSize {

  private TickSize() {}

  /**
   * 주문 가능한 가격으로 맞춘다.
   *
   * <p>매도는 올리고 매수는 내린다. 반대로 깎으면 의도한 값보다 불리한 가격에 체결된다.
   */
  public static int round(double price, MarketName marketName, OrderCode orderCode) {
    int tick = of(price, marketName);
    double units = price / tick;
    return (int) (OrderCode.SELL == orderCode ? Math.ceil(units) : Math.floor(units)) * tick;
  }

  /** ETF 는 가격대와 무관하게 5원이다. */
  private static int of(double price, MarketName marketName) {
    if (price < 2000) {
      return 1;
    }
    if (price < 5000 || MarketName.ETF == marketName) {
      return 5;
    }
    if (price < 20000) {
      return 10;
    }
    if (price < 50000) {
      return 50;
    }
    if (price < 200000) {
      return 100;
    }
    if (price < 500000) {
      return 500;
    }
    return 1000;
  }
}
