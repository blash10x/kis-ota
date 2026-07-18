package blash10x.kis.ota.service;

import blash10x.kis.ota.config.OtaProperties;
import blash10x.kis.ota.external.TradingService;
import blash10x.kis.ota.domain.LadderWeight;
import blash10x.kis.ota.model.OrderCode;
import blash10x.kis.ota.model.ReservationOrderSeq;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

/**
 * @author myungsik.sung@gmail.com
 */
@Service
public class ReservationOrderService extends LadderOrderService<ReservationOrderSeq> {
  private static final Logger LOGGER = LoggerFactory.getLogger(ReservationOrderService.class);
  private static final String PATH = "/uapi/domestic-stock/v1/trading/order-resv";
  private static final String TR_ID = "CTSC0008U";
  private static final String ORD_QTY = "1"; // 주문수량
  private static final String ORD_DVSN_CD = "00"; // 지정가
  private static final String ORD_OBJT_CBLC_DVSN_CD = "10"; // 현금

  public ReservationOrderService(
      TradingService tradingService,
      BalanceService balanceService,
      RealtimePriceService realtimePriceService,
      InterestStocksService interestStocksService,
      ExtractionService extractionService,
      LadderWeight ladderWeight,
      OtaProperties otaProperties) {
    super(tradingService, balanceService, realtimePriceService,
        interestStocksService, extractionService, ladderWeight, otaProperties);
  }

  @Override
  protected ReservationOrderSeq submit(
      String productNo, int orderUnitPrice, OrderCode orderCode, boolean real) {
    if (!real) {
      tradingService.sleep(MOCK_ORDER_INTERVAL_MILLIS);
      return new ReservationOrderSeq("Mock");
    }

    MultiValueMap<String, String> headers = tradingService.buildRequestHeaders(TR_ID);
    Request request = buildRequest(productNo, "" + orderUnitPrice, orderCode);
    Response response = tradingService.post(PATH, headers, null, request, Response.class).block();
    tradingService.sleep(ORDER_INTERVAL_MILLIS);
    if (response == null) {
      return new ReservationOrderSeq("Unknown");
    }
    if (response.output == null) {
      LOGGER.warn("{} order rejected: rt_cd={}, msg_cd={}, msg={}",
          productNo, response.rt_cd, response.msg_cd, response.msg);
      return new ReservationOrderSeq("");
    }

    return response.output;
  }

  private Request buildRequest(
      String productNo, String orderUnitPrice, OrderCode orderCode) {
    return Request.builder()
        .accountNo(tradingService.getAccountNo())
        .accountProductCode(tradingService.getAccountProductCode())
        .productNo(productNo)
        .orderUnitPrice(orderUnitPrice)
        .sellBuyDivisionCode(orderCode.getCode())
        .orderQuantity(ORD_QTY)
        .orderDivisionCode(ORD_DVSN_CD)
        .orderObjectBalanceDivisionCode(ORD_OBJT_CBLC_DVSN_CD)
        .build();
  }

  @Builder
  private record Request(
      @JsonProperty("CANO") String accountNo,
      @JsonProperty("ACNT_PRDT_CD") String accountProductCode,
      @JsonProperty("PDNO") // 상품번호
          String productNo,
      @JsonProperty("ORD_QTY") // 주문수량
          String orderQuantity,
      @JsonProperty("ORD_UNPR") // 주문단가
          String orderUnitPrice,
      @JsonProperty("SLL_BUY_DVSN_CD") // 매도매수구분코드: 01:매도, 02:매수
          String sellBuyDivisionCode,
      @JsonProperty("ORD_DVSN_CD") // 주문구분코드: 지정가:00
          String orderDivisionCode,
      @JsonProperty("ORD_OBJT_CBLC_DVSN_CD") // 주문대상잔고구분코드: 현금:10
          String orderObjectBalanceDivisionCode) {}

  private record Response(
      @JsonProperty("rt_cd") // 성공 실패 여부
          String rt_cd,
      @JsonProperty("msg_cd") // 응답코드
          String msg_cd,
      @JsonProperty("msg1") // 응답메세지
          String msg,
      @JsonProperty("output") ReservationOrderSeq output) {}
}
