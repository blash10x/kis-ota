package blash10x.kis.ota.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import blash10x.kis.ota.model.MarketName;
import blash10x.kis.ota.model.OrderCode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * @author myungsik.sung@gmail.com
 */
class LadderPricerTest {

  private static final Map<MarketName, Double> BASE_RATES =
      Map.of(MarketName.KOSPI200, 3.20, MarketName.ETF, 2.05);
  private static final Map<MarketName, Double> STEP_RATES =
      Map.of(MarketName.KOSPI200, 1.20, MarketName.ETF, 0.55);

  private final LadderPricer ladderPricer = new LadderPricer(new BetaWeight());

  /**
   * 449450 PLUS K방산. 현재가(54,240)가 평단(63,547.59)보다 14.65% 아래인 손실 종목이라, 사다리 기준점이
   * 손익분기가(평단*1.01 = 64,183.07)로 잡힌다.
   */
  private static LadderInput.LadderInputBuilder deepUnderwaterEtf() {
    return LadderInput.builder()
        .orderCode(OrderCode.SELL)
        .marketName(MarketName.ETF)
        .realtimePrice(54_240)
        .upperPriceLimit(70_510)
        .lowerPriceLimit(37_970)
        .dayOverDayRate(0.0)
        .yearBeta(0.31)
        .purchaseAvgPrice(63_547.59)
        .size(20)
        .baseRates(BASE_RATES)
        .stepRates(STEP_RATES);
  }

  /**
   * 평단(10,000)보다 현재가(70,000)가 한참 위인 수익 종목. 기준점이 현재가라 주문가는 현재가에서
   * rate = baseRate + i*weight*stepRate 만큼 벌어진다. 5단 주문가는 73,600 / 74,900 / 76,200 / 77,500 / 78,800.
   */
  private static LadderInput.LadderInputBuilder climbingKospi() {
    return LadderInput.builder()
        .orderCode(OrderCode.SELL)
        .marketName(MarketName.KOSPI200)
        .realtimePrice(70_000)
        .upperPriceLimit(91_000)
        .lowerPriceLimit(49_000)
        .dayOverDayRate(0.0)
        .yearBeta(1.0)
        .purchaseAvgPrice(10_000)
        .size(5)
        .baseRates(BASE_RATES)
        .stepRates(STEP_RATES);
  }

  @Test
  @DisplayName("손실 종목 매도는 손익분기가 그 자체에서 시작해 등간격으로 오른다")
  void underwaterSellLaddersUpFromBreakEven() {
    List<LadderOrder> orders = ladderPricer.price(deepUnderwaterEtf().build());

    // 첫 단은 손익분기가(64,183.07) 그 자체다(호가단위 올림 64,185). baseRate 를 얹지 않아, 깊은 손실
    // 종목에서도 손익분기가에 걸어 두는 한 건이 살아남는다.
    assertThat(orders.getFirst().unitPrice()).isEqualTo(64_185);
    // 모든 단이 손익분기가 위, 상한가 이하이고 서로 다르며 단조 증가한다.
    assertThat(orders).extracting(LadderOrder::unitPrice)
        .doesNotHaveDuplicates()
        .isSorted()
        .allSatisfy(price -> assertThat(price).isGreaterThan(64_183).isLessThanOrEqualTo(70_510));

    // 단 간격이 일정하다(이론 간격 ≈ 373.6원, 호가단위 5원 반올림으로 370~375). 첫 간격만 튀던 문제가 사라졌다.
    List<Integer> prices = orders.stream().map(LadderOrder::unitPrice).toList();
    for (int i = 1; i < prices.size(); i++) {
      assertThat(prices.get(i) - prices.get(i - 1)).isBetween(370, 375);
    }
    // baseRate 를 뺀 만큼 사다리 바닥이 낮아져, 같은 상한가(70,510) 안에 17단이 들어간다.
    assertThat(orders).hasSize(17);
  }

  @Test
  @DisplayName("rate 는 실제 주문가의 현재가 대비 등락률과 일치한다")
  void rateReflectsActualOrderPrice() {
    // 손익분기 앵커로 주문가가 현재가에서 크게 벌어져도, rate 는 명목값이 아니라 실제 주문가 기준으로 기록된다.
    List<LadderOrder> orders = ladderPricer.price(deepUnderwaterEtf().build());

    assertThat(orders).allSatisfy(order ->
        assertThat(order.rate())
            .isCloseTo((order.unitPrice() - 54_240) * 100.0 / 54_240, within(1e-9)));
    // 첫 단(64,185)의 실효 등락률은 +18.34% 다. 현재가가 손익분기가보다 그만큼 아래라는 뜻이다.
    assertThat(orders.getFirst().rate()).isCloseTo(18.335, within(0.001));
  }

  @Test
  @DisplayName("첫 단부터 상한가를 넘으면 한 건도 내지 않는다")
  void emptyWhenEvenTheFirstRungExceedsUpperLimit() {
    // 상한가를 손익분기가(64,185) 바로 아래로 낮추면 첫 단부터 걸린다.
    List<LadderOrder> orders =
        ladderPricer.price(deepUnderwaterEtf().upperPriceLimit(64_180).build());

    assertThat(orders).isEmpty();
  }

  @Test
  @DisplayName("실효 등락률이 가격제한폭(약 30%)을 넘는 단은 상한가 가드와 무관하게 잘린다")
  void capsActualRateAtDailyPriceLimit() {
    // 000660 SK하이닉스가 당일 급락한 실제 사례. 상한가(stck_mxpr)는 어제 종가 기준 2,754,000 으로 낡아 있지만,
    // 예약주문의 익영업일 제한폭은 오늘 종가(1,842,000) 기준 +30% = 2,394,600 이다. 손익분기가(2,198,114.7) 앵커라
    // 명목 rate 는 작아도 실효 등락률이 30% 를 넘는 단이 생기는데, 그 단부터 KIS 가 거부하므로 내지 않아야 한다.
    List<LadderOrder> orders = ladderPricer.price(LadderInput.builder()
        .orderCode(OrderCode.SELL)
        .marketName(MarketName.KOSPI200)
        .realtimePrice(1_842_000)
        .upperPriceLimit(2_754_000)
        .lowerPriceLimit(1_486_000)
        .dayOverDayRate(-11.4)
        .yearBeta(1.75)
        .purchaseAvgPrice(2_176_351.19)
        .size(20)
        .baseRates(BASE_RATES)
        .stepRates(STEP_RATES)
        .build());

    // 실계좌 로그에서 5단(2,401,000, +30.35%)부터 제한폭 초과였다. 4단까지만 살아남는다.
    assertThat(orders).extracting(LadderOrder::unitPrice)
        .containsExactly(2_199_000, 2_249_000, 2_300_000, 2_350_000);
    assertThat(orders).extracting(LadderOrder::rate).allSatisfy(
        rate -> assertThat(rate).isLessThanOrEqualTo(29.985));
  }

  @Test
  @DisplayName("하락일 보정은 손익분기가 사다리에는 붙지 않는다")
  void downDayBumpDoesNotMoveBreakEvenLadder() {
    // 하락일 보정은 눌린 현재가 기준의 사다리를 밀어 올리는 장치다. 손익분기가 기준 사다리에 붙이면
    // 손익분기가에 걸어 두는 첫 단이 밀려 올라가 취지가 깨진다.
    List<LadderOrder> flat = ladderPricer.price(deepUnderwaterEtf().dayOverDayRate(0.0).build());
    List<LadderOrder> down = ladderPricer.price(deepUnderwaterEtf().dayOverDayRate(-3.0).build());
    assertThat(down).isEqualTo(flat);

    // 수익 종목(현재가 기준)에는 기존대로 붙는다. 첫 단이 |전일대비| * 0.20 만큼 위로 밀린다.
    int flatFirst = ladderPricer.price(climbingKospi().build()).getFirst().unitPrice();
    int downFirst =
        ladderPricer.price(climbingKospi().dayOverDayRate(-3.0).build()).getFirst().unitPrice();
    assertThat(downFirst).isGreaterThan(flatFirst);
  }

  @Test
  @DisplayName("상한가 이하인 단만 나가고 넘는 단은 잘린다")
  void keepsOnlyRungsWithinUpperLimit() {
    // 매도 사다리는 단조 증가라, 상한가를 중간에 걸면 앞 단만 남고 뒤는 전부 잘린다.

    // 상한가가 넉넉하면 5단 전부 나간다.
    assertThat(ladderPricer.price(climbingKospi().build()))
        .extracting(LadderOrder::unitPrice)
        .containsExactly(73_600, 74_900, 76_200, 77_500, 78_800);

    // 상한가를 3단(76,200)과 4단(77,500) 사이에 걸면 3건만 남는다.
    assertThat(ladderPricer.price(climbingKospi().upperPriceLimit(77_000).build()))
        .extracting(LadderOrder::unitPrice)
        .containsExactly(73_600, 74_900, 76_200);
  }

  @Test
  @DisplayName("매도 사다리는 현재가 위로 오르고 매수 사다리는 아래로 내린다")
  void sellClimbsAndBuyDescends() {
    List<LadderOrder> sells = ladderPricer.price(climbingKospi().orderCode(OrderCode.SELL).build());
    List<LadderOrder> buys = ladderPricer.price(climbingKospi().orderCode(OrderCode.BUY).build());

    assertThat(sells).extracting(LadderOrder::unitPrice).isSorted().allSatisfy(
        price -> assertThat(price).isGreaterThan(70_000));
    assertThat(buys).extracting(LadderOrder::unitPrice)
        .isSortedAccordingTo(Comparator.reverseOrder())
        .allSatisfy(price -> assertThat(price).isLessThan(70_000));
  }

  @Test
  @DisplayName("KOSPI 는 KOSPI200 요율을 쓴다")
  void kospiFallsBackToKospi200Rates() {
    assertThat(ladderPricer.price(climbingKospi().marketName(MarketName.KOSPI).build()))
        .extracting(LadderOrder::rate)
        .isEqualTo(
            ladderPricer.price(climbingKospi().marketName(MarketName.KOSPI200).build()).stream()
                .map(LadderOrder::rate)
                .toList());
  }

  @Test
  @DisplayName("주입된 가중치로 사다리 간격을 계산한다")
  void usesInjectedWeight() {
    // 한 단만 보면 주문가 = 현재가 * (1 + (baseRate + i*weight*stepRate)/100) 이 그대로 드러난다.
    LadderInput input = climbingKospi().size(1).build();

    // 가중치 1.0: rate = 3.20 + 1*1.0*1.20 = 4.40 → 70,000*1.044 = 73,080 → 호가단위 올림 73,100
    assertThat(new LadderPricer(in -> 1.0).price(input).getFirst().unitPrice()).isEqualTo(73_100);
    // 가중치 2.0: rate = 3.20 + 1*2.0*1.20 = 5.60 → 70,000*1.056 = 73,920 → 호가단위 올림 74,000
    assertThat(new LadderPricer(in -> 2.0).price(input).getFirst().unitPrice()).isEqualTo(74_000);
  }

  @Test
  @DisplayName("보유하지 않은 종목은 현재가 기준으로 매수 사다리를 낸다")
  void buyLaddersDownFromCurrentPrice() {
    List<LadderOrder> orders = ladderPricer.price(LadderInput.builder()
        .orderCode(OrderCode.BUY)
        .marketName(MarketName.ETF)
        .realtimePrice(10_000)
        .upperPriceLimit(13_000)
        .lowerPriceLimit(7_000)
        .dayOverDayRate(0.0)
        .yearBeta(1.0)
        .purchaseAvgPrice(0.0)
        .size(3)
        .baseRates(BASE_RATES)
        .stepRates(STEP_RATES)
        .build());

    assertThat(orders).extracting(LadderOrder::unitPrice)
        .isSortedAccordingTo(Comparator.reverseOrder())
        .allSatisfy(price -> assertThat(price).isLessThan(10_000));
    assertThat(orders).extracting(LadderOrder::rate).allSatisfy(
        rate -> assertThat(rate).isNegative());
  }
}
