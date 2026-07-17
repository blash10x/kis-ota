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

  private final LadderPricer ladderPricer = new LadderPricer();

  /** 449450 PLUS K방산. 평단보다 14.65% 아래에서 매도 사다리를 낸 실제 사례. */
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
        .evaluationProfitLossRatio(-14.65)
        .size(20)
        .baseRates(BASE_RATES)
        .stepRates(STEP_RATES);
  }

  @Test
  @DisplayName("깊게 물린 매도 사다리는 손익분기점 한 건으로 수렴한다")
  void deeplyUnderwaterSellCollapsesToBreakEven() {
    // 20단 최대 rate 가 13.69% 라, 손익분기 보정을 벗어나는 데 필요한 18.33% 에 끝내 닿지 못한다.
    // 보정 없이 계산하면 전 구간이 평단*1.01 = 64,183.07 로 눌리고, ETF 호가(5원) 올림으로 전부 64,185 가 된다.
    List<LadderOrder> orders = ladderPricer.price(deepUnderwaterEtf().build());

    assertThat(orders).extracting(LadderOrder::unitPrice).containsExactly(64_185);
    // 실계좌 dry-run 로그에 5.54 로 찍힌 그 단이다. Math.log 구현차가 있으니 표시 정밀도까지만 본다.
    assertThat(orders.getFirst().rate()).isCloseTo(5.5423, within(0.0001));
  }

  @Test
  @DisplayName("같은 가격은 한 번만 주문한다")
  void doesNotOrderTheSamePriceTwice() {
    List<LadderOrder> orders = ladderPricer.price(deepUnderwaterEtf().build());

    // 중복 제거가 없으면 이 사례는 64,185 한 가격에 15건이 나간다.
    assertThat(orders).extracting(LadderOrder::unitPrice).doesNotHaveDuplicates();
  }

  @Test
  @DisplayName("첫 단부터 상한가를 넘으면 한 건도 내지 않는다")
  void emptyWhenEvenTheFirstRungExceedsUpperLimit() {
    // 상한가를 손익분기점(64,185) 바로 아래로 낮추면 첫 단부터 걸린다.
    List<LadderOrder> orders =
        ladderPricer.price(deepUnderwaterEtf().upperPriceLimit(64_180).build());

    assertThat(orders).isEmpty();
  }

  @Test
  @DisplayName("상한가 이하인 단만 나가고 넘는 단은 잘린다")
  void keepsOnlyRungsWithinUpperLimit() {
    // 매도 사다리는 단조 증가라, 상한가를 중간에 걸면 앞 단만 남고 뒤는 전부 잘린다.
    LadderInput.LadderInputBuilder climbing = LadderInput.builder()
        .orderCode(OrderCode.SELL)
        .marketName(MarketName.KOSPI200)
        .realtimePrice(70_000)
        .lowerPriceLimit(49_000)
        .dayOverDayRate(0.0)
        .yearBeta(1.0)
        .purchaseAvgPrice(10_000)
        .evaluationProfitLossRatio(600.0)
        .size(5)
        .baseRates(BASE_RATES)
        .stepRates(STEP_RATES);

    // 상한가가 넉넉하면 5단 전부 나간다.
    assertThat(ladderPricer.price(climbing.upperPriceLimit(91_000).build()))
        .extracting(LadderOrder::unitPrice)
        .containsExactly(73_600, 74_900, 76_200, 77_500, 78_800);

    // 상한가를 3단(76,200)과 4단(77,500) 사이에 걸면 3건만 남는다.
    assertThat(ladderPricer.price(climbing.upperPriceLimit(77_000).build()))
        .extracting(LadderOrder::unitPrice)
        .containsExactly(73_600, 74_900, 76_200);
  }

  @Test
  @DisplayName("매도 사다리는 현재가 위로 오르고 매수 사다리는 아래로 내린다")
  void sellClimbsAndBuyDescends() {
    // 평단을 현재가보다 한참 아래로 두어 손익분기 보정과 손실 회피 skip 이 끼어들지 않게 한다.
    LadderInput.LadderInputBuilder kospi = LadderInput.builder()
        .marketName(MarketName.KOSPI200)
        .realtimePrice(70_000)
        .upperPriceLimit(91_000)
        .lowerPriceLimit(49_000)
        .dayOverDayRate(0.0)
        .yearBeta(1.0)
        .purchaseAvgPrice(10_000)
        .evaluationProfitLossRatio(600.0)
        .size(5)
        .baseRates(BASE_RATES)
        .stepRates(STEP_RATES);

    List<LadderOrder> sells = ladderPricer.price(kospi.orderCode(OrderCode.SELL).build());
    List<LadderOrder> buys = ladderPricer.price(kospi.orderCode(OrderCode.BUY).build());

    assertThat(sells).extracting(LadderOrder::unitPrice).isSorted().allSatisfy(
        price -> assertThat(price).isGreaterThan(70_000));
    assertThat(buys).extracting(LadderOrder::unitPrice)
        .isSortedAccordingTo(Comparator.reverseOrder())
        .allSatisfy(price -> assertThat(price).isLessThan(70_000));
  }

  @Test
  @DisplayName("KOSPI 는 KOSPI200 요율을 쓴다")
  void kospiFallsBackToKospi200Rates() {
    LadderInput.LadderInputBuilder common = LadderInput.builder()
        .orderCode(OrderCode.SELL)
        .realtimePrice(70_000)
        .upperPriceLimit(91_000)
        .lowerPriceLimit(49_000)
        .dayOverDayRate(0.0)
        .yearBeta(1.0)
        .purchaseAvgPrice(10_000)
        .evaluationProfitLossRatio(600.0)
        .size(5)
        .baseRates(BASE_RATES)
        .stepRates(STEP_RATES);

    assertThat(ladderPricer.price(common.marketName(MarketName.KOSPI).build()))
        .extracting(LadderOrder::rate)
        .isEqualTo(
            ladderPricer.price(common.marketName(MarketName.KOSPI200).build()).stream()
                .map(LadderOrder::rate)
                .toList());
  }

  @Test
  @DisplayName("보유하지 않은 종목은 손익분기 보정 없이 매수 사다리를 낸다")
  void buyIgnoresBreakEvenClamp() {
    List<LadderOrder> orders = ladderPricer.price(LadderInput.builder()
        .orderCode(OrderCode.BUY)
        .marketName(MarketName.ETF)
        .realtimePrice(10_000)
        .upperPriceLimit(13_000)
        .lowerPriceLimit(7_000)
        .dayOverDayRate(0.0)
        .yearBeta(1.0)
        .purchaseAvgPrice(0.0)
        .evaluationProfitLossRatio(0.0)
        .size(3)
        .baseRates(BASE_RATES)
        .stepRates(STEP_RATES)
        .build());

    assertThat(orders).hasSize(3);
    assertThat(orders).extracting(LadderOrder::rate).allSatisfy(
        rate -> assertThat(rate).isNegative());
  }
}
