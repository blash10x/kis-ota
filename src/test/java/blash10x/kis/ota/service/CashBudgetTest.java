package blash10x.kis.ota.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CashBudgetTest {

  @Test
  @DisplayName("예산 안에서는 차감하며 승인하고, 넘어서면 차감 없이 거절한다")
  void reservesWithinBudgetAndRejectsBeyond() {
    CashBudget budget = CashBudget.of(100_000);

    assertThat(budget.tryReserve(60_000)).isTrue();
    assertThat(budget.remaining()).isEqualTo(40_000);
    // 부족한 요청은 거절되고 잔액이 변하지 않는다.
    assertThat(budget.tryReserve(50_000)).isFalse();
    assertThat(budget.remaining()).isEqualTo(40_000);
    // 매수 사다리의 skip 시나리오: 더 싼 다음 단은 남은 예산에 들어간다.
    assertThat(budget.tryReserve(40_000)).isTrue();
    assertThat(budget.remaining()).isZero();
    assertThat(budget.tryReserve(1)).isFalse();
  }

  @Test
  @DisplayName("정확히 잔액만큼의 주문은 승인된다")
  void allowsExactRemaining() {
    CashBudget budget = CashBudget.of(54_240);
    assertThat(budget.tryReserve(54_240)).isTrue();
    assertThat(budget.remaining()).isZero();
  }

  @Test
  @DisplayName("무제한 예산(매도)은 항상 승인한다")
  void unlimitedAlwaysReserves() {
    CashBudget budget = CashBudget.unlimited();
    assertThat(budget.tryReserve(Integer.MAX_VALUE)).isTrue();
    assertThat(budget.tryReserve(Integer.MAX_VALUE)).isTrue();
  }

  @Test
  @DisplayName("전송 실패한 단의 예약을 되돌리면 다음 단이 그 예산을 쓸 수 있다")
  void releaseReturnsReservedAmount() {
    CashBudget budget = CashBudget.of(100_000);

    assertThat(budget.tryReserve(60_000)).isTrue();
    budget.release(60_000);
    assertThat(budget.remaining()).isEqualTo(100_000);
    assertThat(budget.tryReserve(100_000)).isTrue();
  }

  @Test
  @DisplayName("무제한 예산은 release 해도 오버플로 없이 무제한을 유지한다")
  void unlimitedReleaseKeepsUnlimited() {
    CashBudget budget = CashBudget.unlimited();
    budget.release(Integer.MAX_VALUE);
    assertThat(budget.tryReserve(Integer.MAX_VALUE)).isTrue();
  }

  @Test
  @DisplayName("음수 조회값은 0 예산으로 본다")
  void negativeAmountBecomesZero() {
    CashBudget budget = CashBudget.of(-1);
    assertThat(budget.remaining()).isZero();
    assertThat(budget.tryReserve(1)).isFalse();
  }
}
