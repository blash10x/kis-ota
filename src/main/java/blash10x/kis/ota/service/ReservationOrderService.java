package blash10x.kis.ota.service;

import blash10x.kis.ota.config.KisProperties;
import blash10x.kis.ota.config.ProductProperties;
import blash10x.kis.ota.model.Balance;
import blash10x.kis.ota.model.OrderCode;
import blash10x.kis.ota.model.Product;
import blash10x.kis.ota.model.ReservationOrderSeq;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

/**
 * @author myungsik.sung@gmail.com
 */
@Service
public class ReservationOrderService extends TradingService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ReservationOrderService.class);
  private static final String PATH = "/uapi/domestic-stock/v1/trading/order-resv";
  private static final String TR_ID = "CTSC0008U";
  private static final String ORD_QTY = "1"; // 주문수량
  private static final String ORD_DVSN_CD = "00"; // 지정가
  private static final String ORD_OBJT_CBLC_DVSN_CD = "10"; // 현금

  private final int repetitions;
  private final double baseRate;
  private final double applyRate;
  private final Map<String, Product> products;
  private final BalanceService balanceService;
  private MultiValueMap<String, String> headers;

  public ReservationOrderService(
      KisProperties kisProperties,
      ProductProperties productProperties,
      KisAuthService kisAuthService,
      BalanceService balanceService) {
    super(kisProperties, kisAuthService);
    repetitions = productProperties.getRepetitions();
    baseRate = productProperties.getBaseRate();
    applyRate = productProperties.getApplyRate();
    products = productProperties.getProducts();
    this.balanceService = balanceService;
  }

  public List<ReservationOrderSeq> orderReservation(
      List<String> productNos, OrderCode orderCode, boolean real) {
    if (headers == null) {
      headers = buildRequestHeaders(TR_ID);
    }

    Map<String, Balance> balances = balanceService.getBalances();
    List<String> orderProductNos  = orderCode == OrderCode.SELL
        ? productNos.stream().filter(balances::containsKey).toList()
        : productNos.stream().filter(products::containsKey).toList();

    List<ReservationOrderSeq> results = new ArrayList<>();
    orderProductNos.forEach(productNo -> {
      Balance balance = balances.get(productNo);
      Product product = products.get(productNo);
      int orderPossibleQuantity = Integer.parseInt(balance.orderPossibleQuantity());
      int size = Math.min(orderPossibleQuantity, repetitions);
      for (int i = 1; i <= size; i++) {
        double rate = calculateRate(i, baseRate, product.beta() * applyRate, orderCode);
        if (rate > 29.98) {
          break;
        }

        double orderUnitPrice = Double.parseDouble(balance.presentPrice()) * rate;
        LOGGER.info("{} | {} | {} | {} | {} | {}",
            productNo, balance.productName(), orderCode, balance.presentPrice(),
            String.format("%2.3f", (rate - 1.0) * 100),
            String.format("%,8.2f", orderUnitPrice));
        ReservationOrderSeq result = orderReservation(balance.productNo(), orderUnitPrice, orderCode, real);
        results.add(result);
      }
    });
    return results;
  }

  private ReservationOrderSeq orderReservation(
      String productNo, double orderUnitPrice, OrderCode orderCode, boolean real) {
    Request request = Request.builder()
        .accountNo(getAccountNo())
        .accountProductCode(getAccountProductCode())
        .productNo(productNo)
        .orderUnitPrice("" + calculateTickPrice(orderUnitPrice, orderCode))
        .sellBuyDivisionCode(orderCode.getCode())
        .orderQuantity(ORD_QTY)
        .orderDivisionCode(ORD_DVSN_CD)
        .orderObjectBalanceDivisionCode(ORD_OBJT_CBLC_DVSN_CD)
        .build();

    LOGGER.info("ReservationOrder: [{}] {}: {}",
        productNo, orderCode, String.format("%,8d", Integer.parseInt(request.orderUnitPrice)));

    Response response = post(PATH, headers, null, request, Response.class).block();
    return response != null ? response.output : new ReservationOrderSeq("Unknown");
  }

  @Builder
  private record Request(
      @JsonProperty("CANO")
      String accountNo,
      @JsonProperty("ACNT_PRDT_CD")
      String accountProductCode,
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
      @JsonProperty("output")
      ReservationOrderSeq output) {}
}
