package blash10x.kis.ota.service;

import blash10x.kis.ota.config.OtaProperties;
import blash10x.kis.ota.external.TradingService;
import blash10x.kis.ota.domain.LadderWeight;
import blash10x.kis.ota.model.OrderCode;
import blash10x.kis.ota.model.OrderResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

@Service
public class CashOrderService extends LadderOrderService<OrderResult> {
  private static final Logger LOGGER = LoggerFactory.getLogger(CashOrderService.class);
  private static final String PATH = "/uapi/domestic-stock/v1/trading/order-cash";
  private static final String TR_ID_SELL = "TTTC0011U";
  private static final String TR_ID_BUY = "TTTC0012U";
  private static final String ORD_DVSN = "00"; // 주문구분: 00:지정가
  private static final String ORD_QTY = "1"; // 주문수량
  private static final String EXCG_SOR = "SOR"; // 거래소ID구분코드: Smart Order Routing (KRX/NXT 최선집행)
  private static final String EXCG_KRX = "KRX"; // 거래소ID구분코드: 한국거래소 직행
  /** SOR 대상(NXT 거래가능) 마스터에 없는 종목의 거절 코드: "해당 종목정보가 없습니다" */
  private static final String MSG_CD_NOT_SOR_ELIGIBLE = "APBK3026";

  /**
   * SOR 대상이 아니라고 확인된 종목. NXT 거래가능 목록은 분기별로 바뀌지만, KRX 직행은 어느 종목에나
   * 유효하므로 낡은 항목이 남아 있어도 주문이 실패하지는 않는다(최선집행 이점만 놓친다).
   */
  private final Set<String> sorIneligible = ConcurrentHashMap.newKeySet();

  public CashOrderService(
      TradingService tradingService,
      BalanceService balanceService,
      RealtimePriceService realtimePriceService,
      InterestStocksService interestStocksService,
      ExtractionService extractionService,
      LadderWeight ladderWeight,
      OtaProperties otaProperties,
      PurchasableCashService purchasableCashService) {
    super(tradingService, balanceService, realtimePriceService,
        interestStocksService, extractionService, ladderWeight, otaProperties,
        purchasableCashService);
  }

  @Override
  protected OrderResult submit(
      String productNo, int orderUnitPrice, OrderCode orderCode, boolean real) {
    if (!real) {
      tradingService.sleep(MOCK_ORDER_INTERVAL_MILLIS);
      return new OrderResult(null, "Mock", null);
    }

    String trId = OrderCode.SELL == orderCode ? TR_ID_SELL : TR_ID_BUY;
    MultiValueMap<String, String> headers = tradingService.buildRequestHeaders(trId);

    boolean sor = !sorIneligible.contains(productNo);
    Response response = send(productNo, orderUnitPrice, headers, sor ? EXCG_SOR : EXCG_KRX);
    // SOR 는 NXT 거래가능 종목에만 유효해, 대상이 아닌 종목(ETF 등)은 APBK3026 으로 거절된다.
    // 업무 거절은 미접수가 보장되므로 KRX 직행으로 다시 보내도 중복이 생기지 않는다.
    if (sor && response != null && MSG_CD_NOT_SOR_ELIGIBLE.equals(response.msg_cd)) {
      sorIneligible.add(productNo);
      LOGGER.info("{} not SOR-eligible, falling back to KRX", productNo);
      response = send(productNo, orderUnitPrice, headers, EXCG_KRX);
    }

    if (response == null) {
      // 접수 여부를 알 수 없는 경우다. 미접수 확정(null 반환)과 달리 예산 반환 대상이 아니다.
      return new OrderResult(null, "Unknown", null);
    }
    // 성공 판정의 계약은 rt_cd("0") 이다. output 유무로만 보면 실패 응답에 output 골격이 실려 올 때 성공으로 오인한다.
    if (!"0".equals(response.rt_cd) || response.output == null) {
      LOGGER.warn("{} order rejected: rt_cd={}, msg_cd={}, msg={}",
          productNo, response.rt_cd, response.msg_cd, response.msg);
      return null;
    }

    return response.output;
  }

  private Response send(
      String productNo,
      int orderUnitPrice,
      MultiValueMap<String, String> headers,
      String exchangeIdDivisionCode) {
    Request request = buildRequest(productNo, "" + orderUnitPrice, exchangeIdDivisionCode);
    Response response = tradingService.post(PATH, headers, null, request, Response.class).block();
    tradingService.sleep(ORDER_INTERVAL_MILLIS);
    return response;
  }

  private Request buildRequest(
      String productNo, String orderUnitPrice, String exchangeIdDivisionCode) {
    return Request.builder()
        .accountNo(tradingService.getAccountNo())
        .accountProductCode(tradingService.getAccountProductCode())
        .productNo(productNo)
        .orderDivision(ORD_DVSN)
        .orderQuantity(ORD_QTY)
        .orderUnitPrice(orderUnitPrice)
        .exchangeIdDivisionCode(exchangeIdDivisionCode)
        // 공식 샘플은 선택 필드도 키를 항상 전송한다. 빈 값이면 기본 동작(일반매도, 조건가 없음)이다.
        .sellType("")
        .conditionPrice("")
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
      String exchangeIdDivisionCode,
      @JsonProperty("SLL_TYPE") // 매도유형: 01:일반매도, 02:임의매매, 05:대차매도. 빈 값이면 일반매도
      String sellType,
      @JsonProperty("CNDT_PRIC") // 조건가격: 스탑지정가 주문 전용. 미사용
      String conditionPrice) {}

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
