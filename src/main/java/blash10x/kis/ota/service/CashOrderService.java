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
public class CashOrderService extends TradingService {
  private static final Logger LOGGER = LoggerFactory.getLogger(CashOrderService.class);
  private static final String PATH = "/uapi/domestic-stock/v1/trading/order-cash";
  private static final String TR_ID_SELL = "TTTC0011U";
  private static final String TR_ID_BUY = "TTTC0012U";
  private static final String ORD_DVSN = "00"; // 주문구분: 00:지정가
  private static final String ORD_QTY = "1"; // 주문수량
  private static final String EXCG_ID_DVSN_CD = "SOR"; // 거래소ID구분코드: SOR: Smart Order Routing

  private final int repetitions;
  private final double baseRate;
  private final double applyRate;
  private final Map<String, Product> products;
  private final BalanceService balanceService;
  private MultiValueMap<String, String> headers;

  public CashOrderService(
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

  public List<ReservationOrderSeq> orderCash(
      List<String> productNos, OrderCode orderCode, boolean real) {
    String trId = orderCode == OrderCode.SELL ? TR_ID_SELL : TR_ID_BUY;
    headers = buildRequestHeaders(trId);

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
        ReservationOrderSeq result = orderCash(balance.productNo(), orderUnitPrice, orderCode, real);
        results.add(result);
      }
    });
    return results;
  }

  private ReservationOrderSeq orderCash(
      String productNo, double orderUnitPrice, OrderCode orderCode, boolean real) {
    int tickPrice = calculateTickPrice(orderUnitPrice, orderCode);
    Request request = buildRequest(productNo, "" + tickPrice);

    LOGGER.info(
        "ReservationOrder: [{}] {}: {}", productNo, orderCode, String.format("%,8d", tickPrice));
    if (!real) {
      return new ReservationOrderSeq("Mock");
    }

    Response response = post(PATH, headers, null, request, Response.class).block();
    return response != null ? response.output : new ReservationOrderSeq("Unknown");
  }

  private Request buildRequest(String productNo, String orderUnitPrice) {
    return Request.builder()
        .accountNo(getAccountNo())
        .accountProductCode(getAccountProductCode())
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
      @JsonProperty("EXCG_ID_DVSN_CD") // 주문대상잔고구분코드: 현금:10
      String exchangeIdDivisionCode) {}

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
