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

  private final LadderWeight ladderWeight;

  public LadderPricer(LadderWeight ladderWeight) {
    this.ladderWeight = ladderWeight;
  }

  /** 사다리를 계산한다. 건너뛴 단이 있으면 결과에서 빠지므로, 반환 크기는 {@code input.size()} 이하다. */
  public List<LadderOrder> price(LadderInput input) {
    OrderCode orderCode = input.orderCode();
    boolean sell = OrderCode.SELL == orderCode;
    double weight = ladderWeight.of(input);
    int direction = sell ? 1 : -1;

    // 사다리를 벌리는 기준점. 손실 종목(현재가 < 손익분기가) 매도는 손익분기가(평단*1.01)를 기준점으로 삼는다.
    // 그러면 모든 단이 손익분기가 위에서 등간격으로 벌어져 손실 회피가 구조적으로 보장되고, 현재가 기준으로
    // 낮은 단들이 손익분기가로 눌려 한 가격으로 뭉쳤다 중복 제거되며 붕괴하던 문제가 사라진다. 현재가가 이미
    // 손익분기가 위면(수익 종목) 현재가가 그대로 기준점이라 기존 동작과 같다. 매수는 손익분기 개념이 없다.
    double anchor = input.realtimePrice();
    boolean fromBreakEven = false;
    if (sell) {
      double breakEven = input.purchaseAvgPrice() * 1.01;
      if (breakEven > anchor) {
        anchor = breakEven;
        fromBreakEven = true;
      }
    }

    List<LadderOrder> orders = new ArrayList<>();
    Set<Integer> orderedPrices = new HashSet<>();
    for (int i = 1; i <= input.size(); i++) {
      double rate;
      if (fromBreakEven) {
        // 손익분기가에서 시작하는 사다리는 첫 단(i=1)이 손익분기가 그 자체다. baseRate 를 얹으면 깊은 손실
        // 종목에서 첫 단부터 상한가를 넘어, 손익분기가에 걸어 둘 수 있는 한 건마저 사라진다. 하락일 보정도
        // 빼는데, 그 보정은 눌린 현재가 기준의 사다리를 밀어 올리는 장치라 현재가와 무관한 기준점에서는 의미가 없다.
        rate = (i - 1) * weight * input.stepRate();
      } else {
        rate = input.baseRate() + i * weight * input.stepRate();
        if (sell && input.dayOverDayRate() < 0.0) {
          rate += Math.abs(input.dayOverDayRate()) * 0.20;
        }
      }

      if (rate > RATE_CAP) {
        break;
      }

      double unitPrice = anchor * (100 + direction * rate) / 100;
      int tickPrice = TickSize.round(unitPrice, input.marketName(), orderCode);

      // 매도는 i 가 커질수록 주문가가 오르고 매수는 내리므로, 한쪽을 벗어나면 이후도 전부 벗어난다.
      if (tickPrice > input.upperPriceLimit() || tickPrice < input.lowerPriceLimit()) {
        break;
      }

      // 호가단위 반올림 때문에 앞 단과 같은 가격이 나올 수 있다. 같은 가격은 한 번만 낸다.
      if (!orderedPrices.add(tickPrice)) {
        continue;
      }

      // rate 는 실제 주문가의 현재가 대비 등락률이다(LadderOrder 계약). 손실 종목은 기준점이 손익분기가라
      // 주문가가 현재가에서 크게 벌어지는데, 명목 rate 가 아니라 이 실제 값을 담아야 로그·후속 처리가
      // 주문가와 어긋나지 않는다. 매도는 양수, 매수는 음수가 된다.
      double actualRate = (tickPrice - input.realtimePrice()) * 100.0 / input.realtimePrice();
      orders.add(new LadderOrder(tickPrice, actualRate));
    }
    return orders;
  }
}
