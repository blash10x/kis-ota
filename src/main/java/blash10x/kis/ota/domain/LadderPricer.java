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
   * 실효 등락률(실제 주문가의 현재가 대비)의 상한. 상/하한가 가드와 별개로 반드시 필요하다.
   *
   * <p>상/하한가(stck_mxpr/stck_llam)는 오늘 기준가(전일 종가) 기준인데, 예약주문은 익영업일에 실행되고 그날의
   * 가격제한폭은 오늘 종가 기준이다. 종목이 당일 급락하면 stck_mxpr 이 익영업일 실제 상한보다 높아져, 그 사이의
   * 주문이 로컬 가드를 통과하고도 KIS 에서 거부된다. 장 마감 후 현재가 = 오늘 종가이므로 이 상한이 익영업일
   * 제한폭을 대신한다. 명목 rate 가 아니라 실효 등락률에 걸어야 한다 — 손익분기가 앵커에서는 명목 rate 가 작아도
   * 실효 등락률이 30% 를 넘을 수 있다.
   *
   * <p>KIS 문서상 정리매매종목·ELW·신주인수권은 가격제한폭이 적용되지 않는다(주식예약주문 유의사항). 그런 종목에서
   * stck_mxpr/stck_llam 이 어떤 값으로 오는지는 확인하지 못했으므로, 상/하한가 가드의 백스톱 역할도 겸한다.
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

    // 사다리를 벌리는 기준점. 손실 종목(현재가 < 평단) 매도는 손익분기가(평단*1.01)를 기준점으로 삼는다.
    // 그러면 모든 단이 손익분기가 위에서 등간격으로 벌어져 손실 회피가 구조적으로 보장되고, 현재가 기준으로
    // 낮은 단들이 손익분기가로 눌려 한 가격으로 뭉쳤다 중복 제거되며 붕괴하던 문제가 사라진다.
    // 수익 종목은 수익률이 1% 미만이라 손익분기가가 현재가보다 높아도 현재가가 기준점이다 — 손익분기가로
    // 재앵커하면 첫 단이 현재가에 붙어(추가 수익 ≈ 0) 사실상 현재가 매도가 된다. 수익 종목은 현재가 대비
    // 추가 수익이 원칙이고, 첫 단 = 현재가*(1 + weight*(base+step)) ≥ 평단*1.01 이라(base+step > 1% 는
    // LadderInput 이 강제한다) +1% 보장도 그대로 성립한다. 매수는 손익분기 개념이 없다.
    double anchor = input.realtimePrice();
    boolean fromBreakEven = false;
    if (sell && input.realtimePrice() < input.purchaseAvgPrice()) {
      anchor = input.purchaseAvgPrice() * 1.01;
      fromBreakEven = true;
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
        // base 에도 weight 를 곱한다: 시장별 사다리 템플릿(base + i*step)을 종목 베타로 통째로 스케일해,
        // 고베타일수록 시작 깊이와 간격이 함께 벌어진다. weight 하한이 1.0 이라 설정 base 가 곧 최소 시작 깊이다.
        rate = weight * (input.baseRate() + i * input.stepRate());
        if (sell && input.dayOverDayRate() < 0.0) {
          rate += Math.abs(input.dayOverDayRate()) * 0.20;
        }
      }

      double unitPrice = anchor * (100 + direction * rate) / 100;
      int tickPrice = TickSize.round(unitPrice, input.marketName(), orderCode);

      // 실효 등락률: 실제 주문가의 현재가 대비 등락률이다(LadderOrder 계약). 손실 종목은 기준점이 손익분기가라
      // 주문가가 현재가에서 크게 벌어지는데, 명목 rate 가 아니라 이 실제 값으로 상한을 걸고 기록해야 주문가와
      // 어긋나지 않는다. 매도는 양수, 매수는 음수가 된다.
      double actualRate = (tickPrice - input.realtimePrice()) * 100.0 / input.realtimePrice();

      // 사다리는 현재가에서 멀어지는 방향으로 단조라, 한 단이 상한을 넘으면 이후도 전부 넘는다.
      if (Math.abs(actualRate) > RATE_CAP) {
        break;
      }

      // 매도는 i 가 커질수록 주문가가 오르고 매수는 내리므로, 한쪽을 벗어나면 이후도 전부 벗어난다.
      if (tickPrice > input.upperPriceLimit() || tickPrice < input.lowerPriceLimit()) {
        break;
      }

      // 호가단위 반올림 때문에 앞 단과 같은 가격이 나올 수 있다. 같은 가격은 한 번만 낸다.
      if (!orderedPrices.add(tickPrice)) {
        continue;
      }

      orders.add(new LadderOrder(tickPrice, actualRate));
    }
    return orders;
  }
}
