package blash10x.kis.ota.domain;

import static org.assertj.core.api.Assertions.assertThat;

import blash10x.kis.ota.model.MarketName;
import blash10x.kis.ota.model.OrderCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TickSizeTest {

  @ParameterizedTest(name = "{0}원대 호가단위 {1}원")
  @CsvSource({
    "1500, 1",
    "3000, 5",
    "10000, 10",
    "30000, 50",
    "100000, 100",
    "300000, 500",
    "700000, 1000",
  })
  @DisplayName("가격대별 호가단위")
  void tickPerBand(int aligned, int tick) {
    // aligned 는 밴드 한가운데의 호가단위 배수라, ±tick 을 해도 같은 밴드에 머문다.
    // 밴드 안에서 올림과 내림이 정확히 tick 만큼 벌어지는지로 단위를 확인한다.
    double inside = aligned + 0.4 * tick;
    assertThat(TickSize.round(aligned, MarketName.KOSPI200, OrderCode.SELL)).isEqualTo(aligned);
    assertThat(TickSize.round(inside, MarketName.KOSPI200, OrderCode.SELL)).isEqualTo(aligned + tick);
    assertThat(TickSize.round(inside, MarketName.KOSPI200, OrderCode.BUY)).isEqualTo(aligned);
  }

  @Test
  @DisplayName("ETF 는 가격대와 무관하게 5원이다")
  void etfAlwaysUsesFiveWon() {
    assertThat(TickSize.round(64_183.07, MarketName.ETF, OrderCode.SELL)).isEqualTo(64_185);
    assertThat(TickSize.round(64_183.07, MarketName.ETF, OrderCode.BUY)).isEqualTo(64_180);
  }

  @Test
  @DisplayName("매도는 올리고 매수는 내린다")
  void sellRoundsUpAndBuyRoundsDown() {
    // 반대로 깎으면 의도한 값보다 불리한 가격에 체결된다.
    assertThat(TickSize.round(10_001, MarketName.KOSPI200, OrderCode.SELL)).isEqualTo(10_010);
    assertThat(TickSize.round(10_009, MarketName.KOSPI200, OrderCode.BUY)).isEqualTo(10_000);
  }
}
