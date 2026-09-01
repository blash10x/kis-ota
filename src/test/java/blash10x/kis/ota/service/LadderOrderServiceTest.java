package blash10x.kis.ota.service;

import static org.assertj.core.api.Assertions.assertThat;

import blash10x.kis.ota.model.Balance;
import blash10x.kis.ota.model.OrderCode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link LadderOrderService#sortByProfitRate} 회귀 테스트.
 */
class LadderOrderServiceTest {

  private static Balance balanceWithProfitRate(String productNo, String profitLossRatio) {
    return new Balance(productNo, productNo + "-name", "10", "10", "1000", "1100", profitLossRatio);
  }

  @Test
  @DisplayName("매도는 평가손익율이 높은 종목부터 정렬한다")
  void sellSortsByProfitRateDescending() {
    Map<String, Balance> balances = Map.of(
        "A", balanceWithProfitRate("A", "-3.5"),
        "B", balanceWithProfitRate("B", "12.0"),
        "C", balanceWithProfitRate("C", "0.0"));

    List<String> sorted =
        LadderOrderService.sortByProfitRate(List.of("A", "B", "C"), OrderCode.SELL, balances);

    assertThat(sorted).containsExactly("B", "C", "A");
  }

  @Test
  @DisplayName("매수는 평가손익율이 낮은 종목부터 정렬한다")
  void buySortsByProfitRateAscending() {
    Map<String, Balance> balances = Map.of(
        "A", balanceWithProfitRate("A", "-3.5"),
        "B", balanceWithProfitRate("B", "12.0"),
        "C", balanceWithProfitRate("C", "0.0"));

    List<String> sorted =
        LadderOrderService.sortByProfitRate(List.of("A", "B", "C"), OrderCode.BUY, balances);

    assertThat(sorted).containsExactly("A", "C", "B");
  }

  @Test
  @DisplayName("보유하지 않은 매수 후보(balance 없음)는 손익율 0.0 으로 본다")
  void buyTreatsUnheldCandidateAsZeroProfit() {
    // A(-5): 하락 보유, HELD0(0): 손익 없는 보유, B(+8): 수익 보유, UNHELD: 미보유(0.0 취급)
    Map<String, Balance> balances = Map.of(
        "A", balanceWithProfitRate("A", "-5.0"),
        "HELD0", balanceWithProfitRate("HELD0", "0.0"),
        "B", balanceWithProfitRate("B", "8.0"));

    List<String> sorted = LadderOrderService.sortByProfitRate(
        List.of("A", "HELD0", "B", "UNHELD"), OrderCode.BUY, balances);

    // 미보유(0.0)는 하락 보유(A) 뒤, 수익 보유(B) 앞. 0.0 동률은 입력 순서(HELD0 → UNHELD) 유지.
    assertThat(sorted).containsExactly("A", "HELD0", "UNHELD", "B");
  }

  @Test
  @DisplayName("손익율이 같은 종목은 입력 순서를 유지한다(안정 정렬)")
  void keepsInputOrderOnTies() {
    Map<String, Balance> balances = Map.of(
        "X", balanceWithProfitRate("X", "1.0"),
        "Y", balanceWithProfitRate("Y", "1.0"),
        "Z", balanceWithProfitRate("Z", "1.0"));

    assertThat(LadderOrderService.sortByProfitRate(List.of("X", "Y", "Z"), OrderCode.SELL, balances))
        .containsExactly("X", "Y", "Z");
    assertThat(LadderOrderService.sortByProfitRate(List.of("X", "Y", "Z"), OrderCode.BUY, balances))
        .containsExactly("X", "Y", "Z");
  }

  @Test
  @DisplayName("손익율이 빈 문자열이면 0.0 으로 보고 정렬이 중단되지 않는다")
  void treatsBlankProfitRateAsZero() {
    Map<String, Balance> balances = Map.of(
        "A", balanceWithProfitRate("A", "-4.0"),
        "BLANK", balanceWithProfitRate("BLANK", ""),
        "B", balanceWithProfitRate("B", "6.0"));

    List<String> sorted = LadderOrderService.sortByProfitRate(
        List.of("A", "BLANK", "B"), OrderCode.BUY, balances);

    assertThat(sorted).containsExactly("A", "BLANK", "B");
  }
}
