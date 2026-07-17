package blash10x.kis.ota.domain;

import blash10x.kis.ota.model.OrderCode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 현재가에서 단계별로 벌어지는 지정가 주문(사다리)을 계산한다. 전송은 하지 않는다.
 *
 * <p>아래 단계의 순서에는 의미가 있다. 중복 제거는 반드시 호가단위 반올림과 상/하한가 가드 뒤여야 한다. 반올림 전에 보면 같은 값으로 수렴할
 * 가격들을 놓치고, 가드 앞에 두면 내지도 않을 주문이 중복 판정을 오염시킨다.
 *
 * @author myungsik.sung@gmail.com
 */
public final class LadderPricer {

  /**
   * rate 자체의 상한. 가격제한폭 적용 종목에서는 상/하한가 가드가 항상 먼저 걸리므로 발동하지 않는 백스톱이다.
   *
   * <p>KIS 문서상 정리매매종목·ELW·신주인수권은 가격제한폭이 적용되지 않는다(주식예약주문 유의사항). 그런 종목에서
   * stck_mxpr/stck_llam 이 어떤 값으로 오는지는 확인하지 못했으므로, 상/하한가 가드를 신뢰할 수 없는 경우를 대비해 남겨 둔다.
   */
  private static final double RATE_CAP = 29.985;

  /** 사다리를 계산한다. 건너뛴 단이 있으면 결과에서 빠지므로, 반환 크기는 {@code input.size()} 이하다. */
  public List<LadderOrder> price(LadderInput input) {
    OrderCode orderCode = input.orderCode();
    boolean sell = OrderCode.SELL == orderCode;
    double beta = input.betaWeight();
    int direction = sell ? 1 : -1;

    List<LadderOrder> orders = new ArrayList<>();
    Set<Integer> orderedPrices = new HashSet<>();
    for (int i = 1; i <= input.size(); i++) {
      double rate = input.baseRate() + i * beta * input.stepRate();
      if (sell && input.dayOverDayRate() < 0.0) {
        rate += Math.abs(input.dayOverDayRate()) * 0.20;
      }

      if (rate > RATE_CAP) {
        break;
      }

      // 손실 구간에서 굳이 팔 이유가 없는 단은 건너뛴다.
      if (sell
          && rate < 5.0 * beta
          && input.evaluationProfitLossRatio() + rate < 0.5) {
        continue;
      }

      double unitPrice = input.realtimePrice() * (100 + direction * rate) / 100;
      double gain = unitPrice - input.purchaseAvgPrice() * 1.01;
      if (sell && gain < 0) {
        unitPrice -= gain;
      }

      int tickPrice = TickSize.round(unitPrice, input.marketName(), orderCode);

      // 매도는 i 가 커질수록 주문가가 오르고 매수는 내리므로, 한쪽을 벗어나면 이후도 전부 벗어난다.
      if (tickPrice > input.upperPriceLimit() || tickPrice < input.lowerPriceLimit()) {
        break;
      }

      // 손익분기 보정(gain)이나 호가단위 반올림 때문에 앞 단과 같은 가격이 나올 수 있다. 같은 가격은 한 번만 낸다.
      if (!orderedPrices.add(tickPrice)) {
        continue;
      }

      orders.add(new LadderOrder(tickPrice, direction * rate));
    }
    return orders;
  }
}
