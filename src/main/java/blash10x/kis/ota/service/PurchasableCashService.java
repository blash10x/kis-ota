package blash10x.kis.ota.service;

import blash10x.kis.ota.core.service.ServiceException;
import blash10x.kis.ota.external.TradingService;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * 매수가능조회(TTTC8908R). 매수 사다리의 예산으로 쓸 "미수 없는 매수금액"을 조회한다.
 *
 * <p>잔고조회의 예수금과 달리, 이 값은 이미 나가 있는 미체결 매수주문·증거금까지 반영된 공식 가용액이다.
 *
 * @author myungsik.sung@gmail.com
 */
@Service
public class PurchasableCashService {
  private static final String PATH = "/uapi/domestic-stock/v1/trading/inquire-psbl-order";
  private static final String TR_ID = "TTTC8908R";

  /** 시장가로 조회해야 종목증거금율이 반영된다(공식 문서). 금액만 쓰므로 단가는 0 이다. */
  private static final String ORD_DVSN = "01";
  private static final String ORD_UNPR = "0";

  private final TradingService tradingService;

  public PurchasableCashService(TradingService tradingService) {
    this.tradingService = tradingService;
  }

  /**
   * 미수 없이 매수에 쓸 수 있는 현금(원). 계좌 수준 값이라 어느 종목으로 조회해도 같지만, PDNO 가 필수라 대표
   * 종목 하나를 받는다.
   *
   * <p>조회에 실패하면 예외를 던진다(fail-closed) — 예산을 모른 채 매수를 내는 것이 곧 미수 위험이므로,
   * 가드가 뚫린 채 진행하는 것보다 매수 전체를 멈추는 편이 안전하다.
   */
  public long inquireNoCreditBuyAmount(String productNo) {
    MultiValueMap<String, String> headers = tradingService.buildRequestHeaders(TR_ID);
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("CANO", tradingService.getAccountNo());
    queryParams.add("ACNT_PRDT_CD", tradingService.getAccountProductCode());
    queryParams.add("PDNO", productNo);
    queryParams.add("ORD_UNPR", ORD_UNPR);
    queryParams.add("ORD_DVSN", ORD_DVSN);
    queryParams.add("CMA_EVLU_AMT_ICLD_YN", "N");
    queryParams.add("OVRS_ICLD_YN", "N");

    Response response = tradingService.get(PATH, headers, queryParams, Response.class).block();
    if (response == null || !"0".equals(response.rt_cd) || response.output == null) {
      throw new ServiceException("failed to inquire purchasable cash: "
          + (response == null ? "no response"
              : "rt_cd=" + response.rt_cd + ", msg_cd=" + response.msg_cd + ", msg=" + response.msg));
    }
    try {
      return Long.parseLong(response.output.noCreditBuyAmount);
    } catch (NumberFormatException e) {
      // 원인 값을 남기지 않으면 fail-closed 로 매수가 전부 멈춘 이유를 진단할 수 없다.
      throw new ServiceException(
          "unparsable nrcvb_buy_amt: '" + response.output.noCreditBuyAmount + "'");
    }
  }

  private record Output(
      @JsonProperty("nrcvb_buy_amt") // 미수없는매수금액
      String noCreditBuyAmount) {}

  private record Response(
      @JsonProperty("rt_cd") // 성공 실패 여부
      String rt_cd,
      @JsonProperty("msg_cd") // 응답코드
      String msg_cd,
      @JsonProperty("msg1") // 응답메세지
      String msg,
      @JsonProperty("output") Output output) {}
}
