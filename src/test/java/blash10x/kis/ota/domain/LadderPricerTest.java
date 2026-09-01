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

class LadderPricerTest {

  private static final Map<MarketName, Double> BASE_RATES =
      Map.of(MarketName.KOSPI200, 3.20, MarketName.ETF, 2.05);
  private static final Map<MarketName, Double> STEP_RATES =
      Map.of(MarketName.KOSPI200, 1.20, MarketName.ETF, 0.55);

  private final LadderPricer ladderPricer = new LadderPricer(new BetaWeight());

  /**
   * 449450 PLUS K방산. 현재가(54,240)가 평단(63,547.59)보다 14.65% 아래인 손실 종목이라, 사다리 기준점이
   * 손익분기가(평단*1.0175 = 64,659.67)로 잡힌다.
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
   * rate = weight*(baseRate + i*stepRate) 만큼 벌어진다. 5단 주문가는 75,300 / 76,700 / 78,100 / 79,500 / 81,000.
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

    // 첫 단은 손익분기가(64,659.67) 그 자체다(호가단위 올림 64,660). baseRate 를 얹지 않아, 깊은 손실
    // 종목에서도 손익분기가에 걸어 두는 한 건이 살아남는다.
    assertThat(orders.getFirst().unitPrice()).isEqualTo(64_660);
    // 모든 단이 손익분기가 위, 상한가 이하이고 서로 다르며 단조 증가한다.
    assertThat(orders).extracting(LadderOrder::unitPrice)
        .doesNotHaveDuplicates()
        .isSorted()
        .allSatisfy(price -> assertThat(price).isGreaterThan(64_659).isLessThanOrEqualTo(70_510));

    // 단 간격이 일정하다(이론 간격 ≈ 451.7원, 호가단위 5원 반올림으로 450~455). 첫 간격만 튀던 문제가 사라졌다.
    List<Integer> prices = orders.stream().map(LadderOrder::unitPrice).toList();
    for (int i = 1; i < prices.size(); i++) {
      assertThat(prices.get(i) - prices.get(i - 1)).isBetween(450, 455);
    }
    // 현재가에서 손익분기가까지 이미 +19.2% 를 쓰고 시작하므로, 남은 실효 등락률 상한(RATE_CAP 29.985%)
    // 안에 13단이 들어간다.
    assertThat(orders).hasSize(13);
  }

  /**
   * 133690 TIGER 미국나스닥100 실제 사례. 현재가(179,850)가 평단(178,149.57) 위인 수익 종목(+0.95%)이지만
   * 수익률이 1.75% 미만이라 손익분기가(181,267.19)는 현재가보다 높다.
   */
  private static LadderInput.LadderInputBuilder marginallyProfitableEtf() {
    return LadderInput.builder()
        .orderCode(OrderCode.SELL)
        .marketName(MarketName.ETF)
        .realtimePrice(179_850)
        .upperPriceLimit(233_800)
        .lowerPriceLimit(125_900)
        .dayOverDayRate(0.0)
        .yearBeta(1.0)
        .purchaseAvgPrice(178_149.57)
        .size(3)
        .baseRates(BASE_RATES)
        .stepRates(STEP_RATES);
  }

  @Test
  @DisplayName("수익률 1.75% 미만이어도 수익 종목이면 현재가 기준으로 사다리를 벌린다")
  void marginallyProfitableSellAnchorsAtCurrentPrice() {
    // 예전에는 이 구간(평단 ≤ 현재가 < 손익분기가)도 손익분기가로 재앵커해 첫 단이 현재가 +0.05% 에
    // 붙었다 — 수익 종목은 현재가 대비 추가 수익이 원칙이다.
    List<LadderOrder> orders = new LadderPricer(in -> 1.0).price(marginallyProfitableEtf().build());

    // 첫 단 = 현재가 * (1 + 1.0*(2.05 + 1*0.55)/100) = 184,526.1 → 호가단위 올림 184,530. 손익분기가가 아니다.
    assertThat(orders.getFirst().unitPrice()).isEqualTo(184_530);
    // 현재가 앵커여도 모든 단이 손익분기가(호가단위 올림 181,270) 이상이라 손익분기 보장은 유지된다.
    assertThat(orders).extracting(LadderOrder::unitPrice)
        .allSatisfy(price -> assertThat(price).isGreaterThanOrEqualTo(181_270));
  }

  @Test
  @DisplayName("rate 는 실제 주문가의 현재가 대비 등락률과 일치한다")
  void rateReflectsActualOrderPrice() {
    // 손익분기 앵커로 주문가가 현재가에서 크게 벌어져도, rate 는 명목값이 아니라 실제 주문가 기준으로 기록된다.
    List<LadderOrder> orders = ladderPricer.price(deepUnderwaterEtf().build());

    assertThat(orders).allSatisfy(order ->
        assertThat(order.rate())
            .isCloseTo((order.unitPrice() - 54_240) * 100.0 / 54_240, within(1e-9)));
    // 첫 단(64,660)의 실효 등락률은 +19.21% 다. 현재가가 손익분기가보다 그만큼 아래라는 뜻이다.
    assertThat(orders.getFirst().rate()).isCloseTo(19.211, within(0.001));
  }

  @Test
  @DisplayName("첫 단부터 상한가를 넘으면 한 건도 내지 않는다")
  void emptyWhenEvenTheFirstRungExceedsUpperLimit() {
    // 상한가를 손익분기가(64,660) 바로 아래로 낮추면 첫 단부터 걸린다.
    List<LadderOrder> orders =
        ladderPricer.price(deepUnderwaterEtf().upperPriceLimit(64_655).build());

    assertThat(orders).isEmpty();
  }

  @Test
  @DisplayName("실효 등락률이 가격제한폭(약 30%)을 넘는 단은 상한가 가드와 무관하게 잘린다")
  void capsActualRateAtDailyPriceLimit() {
    // 000660 SK하이닉스가 당일 급락한 실제 사례. 상한가(stck_mxpr)는 어제 종가 기준 2,754,000 으로 낡아 있지만,
    // 예약주문의 익영업일 제한폭은 오늘 종가(1,842,000) 기준 +30% = 2,394,600 이다. 손익분기가(2,214,437.3) 앵커라
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

    // 5단(2,429,000, +31.9%)부터 제한폭 초과라 4단까지만 살아남는다.
    assertThat(orders).extracting(LadderOrder::unitPrice)
        .containsExactly(2_215_000, 2_268_000, 2_322_000, 2_375_000);
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

    // 수익률 1.75% 미만 수익 종목도 수익 종목이라 보정이 붙는다. 손익분기 재앵커 경로였던 시절에는 안 붙던 구간이다.
    LadderPricer flatWeight = new LadderPricer(in -> 1.0);
    int marginalFlat = flatWeight.price(marginallyProfitableEtf().build()).getFirst().unitPrice();
    int marginalDown = flatWeight.price(marginallyProfitableEtf().dayOverDayRate(-3.0).build())
        .getFirst().unitPrice();
    assertThat(marginalDown).isGreaterThan(marginalFlat);
  }

  @Test
  @DisplayName("상한가 이하인 단만 나가고 넘는 단은 잘린다")
  void keepsOnlyRungsWithinUpperLimit() {
    // 매도 사다리는 단조 증가라, 상한가를 중간에 걸면 앞 단만 남고 뒤는 전부 잘린다.

    // 상한가가 넉넉하면 5단 전부 나간다.
    assertThat(ladderPricer.price(climbingKospi().build()))
        .extracting(LadderOrder::unitPrice)
        .containsExactly(75_300, 76_700, 78_100, 79_500, 81_000);

    // 상한가를 2단(76,700)과 3단(78,100) 사이에 걸면 2건만 남는다.
    assertThat(ladderPricer.price(climbingKospi().upperPriceLimit(77_000).build()))
        .extracting(LadderOrder::unitPrice)
        .containsExactly(75_300, 76_700);
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
  @DisplayName("주입된 가중치로 사다리 시작 깊이와 간격을 함께 스케일한다")
  void usesInjectedWeight() {
    // 한 단만 보면 주문가 = 현재가 * (1 + weight*(baseRate + i*stepRate)/100) 이 그대로 드러난다.
    LadderInput input = climbingKospi().size(1).build();

    // 가중치 1.0: rate = 1.0*(3.20 + 1*1.20) = 4.40 → 70,000*1.044 = 73,080 → 호가단위 올림 73,100
    assertThat(new LadderPricer(in -> 1.0).price(input).getFirst().unitPrice()).isEqualTo(73_100);
    // 가중치 2.0: rate = 2.0*(3.20 + 1*1.20) = 8.80 → 70,000*1.088 = 76,160 → 호가단위 올림 76,200
    assertThat(new LadderPricer(in -> 2.0).price(input).getFirst().unitPrice()).isEqualTo(76_200);
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
