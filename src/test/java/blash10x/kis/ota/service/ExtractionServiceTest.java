package blash10x.kis.ota.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * @author myungsik.sung@gmail.com
 */
class ExtractionServiceTest {

  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource({
    "'374,500원', 374500.0", // KOSPI200 52주최고
    "'64,400원', 64400.0", // KOSPI200 52주최저
    "'87,946', 87946.0", // ETF YR_HIGH
    "'46,061', 46061.0", // ETF YR_LOW
    "'1.19', 1.19", // 베타
    "'0.31', 0.31",
  })
  @DisplayName("콤마·단위가 섞인 실제 스크래핑 문자열에서 숫자만 뽑는다")
  void parsesRealScrapedStrings(String raw, double expected) {
    assertThat(ExtractionService.parseNumber(raw, -1.0)).isEqualTo(expected);
  }

  @Test
  @DisplayName("null 이나 숫자 없는 문자열은 기본값으로 떨어진다")
  void fallsBackWhenNoDigits() {
    assertThat(ExtractionService.parseNumber(null, 1.0)).isEqualTo(1.0);
    assertThat(ExtractionService.parseNumber("-", 0.0)).isEqualTo(0.0);
    assertThat(ExtractionService.parseNumber("", 7.0)).isEqualTo(7.0);
  }

  @Test
  @DisplayName("숫자·점만 남았지만 파싱 불가능한 문자열도 예외 없이 기본값으로 떨어진다")
  void fallsBackWhenUnparseable() {
    // 파싱 예외가 나면 그 종목이 통째로 누락되므로, 여기서 삼켜 중립값으로 떨어뜨린다.
    assertThat(ExtractionService.parseNumber(".", 1.0)).isEqualTo(1.0);
    assertThat(ExtractionService.parseNumber("1.2.3", 1.0)).isEqualTo(1.0);
  }
}
