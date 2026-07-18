package blash10x.kis.ota.service;

import blash10x.kis.ota.config.OtaProperties;
import blash10x.kis.ota.external.TradingService;
import blash10x.kis.ota.domain.LadderWeight;
import blash10x.kis.ota.model.OrderCode;
import blash10x.kis.ota.model.OrderResult;
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
public class CashOrderService extends LadderOrderService<OrderResult> {
  private static final Logger LOGGER = LoggerFactory.getLogger(CashOrderService.class);
  private static final String PATH = "/uapi/domestic-stock/v1/trading/order-cash";
  private static final String TR_ID_SELL = "TTTC0011U";
  private static final String TR_ID_BUY = "TTTC0012U";
  private static final String ORD_DVSN = "00"; // 주문구분: 00:지정가
  private static final String ORD_QTY = "1"; // 주문수량
  private static final String EXCG_ID_DVSN_CD = "SOR"; // 거래소ID구분코드: SOR: Smart Order Routing

  public CashOrderService(
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

  // TODO: TBD
  @Override
  protected OrderResult submit(
      String productNo, int orderUnitPrice, OrderCode orderCode, boolean real) {
    if (!real) {
      tradingService.sleep(MOCK_ORDER_INTERVAL_MILLIS);
      return new OrderResult(null, "Mock", null);
    }

    String trId = OrderCode.SELL == orderCode ? TR_ID_SELL : TR_ID_BUY;
    MultiValueMap<String, String> headers = tradingService.buildRequestHeaders(trId);
    Request request = buildRequest(productNo, "" + orderUnitPrice);
    Response response = tradingService.post(PATH, headers, null, request, Response.class).block();
    tradingService.sleep(ORDER_INTERVAL_MILLIS);
    if (response == null) {
      return new OrderResult(null, "Unknown", null);
    }
    // 성공 판정의 계약은 rt_cd("0") 이다. output 유무로만 보면 실패 응답에 output 골격이 실려 올 때 성공으로 오인한다.
    if (!"0".equals(response.rt_cd) || response.output == null) {
      LOGGER.warn("{} order rejected: rt_cd={}, msg_cd={}, msg={}",
          productNo, response.rt_cd, response.msg_cd, response.msg);
      return new OrderResult(null, "", null);
    }

    return response.output;
  }

  private Request buildRequest(String productNo, String orderUnitPrice) {
    return Request.builder()
        .accountNo(tradingService.getAccountNo())
        .accountProductCode(tradingService.getAccountProductCode())
        .productNo(productNo)
        .orderDivision(ORD_DVSN)
        .orderQuantity(ORD_QTY)
        .orderUnitPrice(orderUnitPrice)
        .exchangeIdDivisionCode(EXCG_ID_DVSN_CD)
        .build();
  }

  @Builder
  private record Request(
      @JsonProperty("CANO")
      String accountNo,
      @JsonProperty("ACNT_PRDT_CD")
      String accountProductCode,
      @JsonProperty("PDNO") // 상품번호
      String productNo,
      @JsonProperty("ORD_DVSN") // 주문구분
      String orderDivision,
      @JsonProperty("ORD_QTY") // 주문수량
      String orderQuantity,
      @JsonProperty("ORD_UNPR") // 주문단가
      String orderUnitPrice,
      @JsonProperty("EXCG_ID_DVSN_CD") // 거래소ID구분코드: KRX:한국거래소, NXT:대체거래소, SOR:SOR
      String exchangeIdDivisionCode) {}

  private record Response(
      @JsonProperty("rt_cd") // 성공 실패 여부
      String rt_cd,
      @JsonProperty("msg_cd") // 응답코드
      String msg_cd,
      @JsonProperty("msg1") // 응답메세지
      String msg,
      @JsonProperty("output")
      OrderResult output) {}
}
