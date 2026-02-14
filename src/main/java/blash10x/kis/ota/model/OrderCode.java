package blash10x.kis.ota.model;

import lombok.Getter;

/**
 * @author Myungsik Sung (myungsik.sung@nol-universe.com)
 */
@Getter
public enum OrderCode {
  SELL("01"), BUY("02");

  private final String code;

  OrderCode(String code) {
    this.code = code;
  }
}
