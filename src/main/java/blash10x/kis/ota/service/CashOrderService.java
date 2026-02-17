package blash10x.kis.ota.service;

import blash10x.kis.ota.config.KisProperties;
import blash10x.kis.ota.config.OtaProperties;
import blash10x.kis.ota.model.Balance;
import blash10x.kis.ota.model.MarketCode;
import blash10x.kis.ota.model.MarketName;
import blash10x.kis.ota.model.OrderCode;
import blash10x.kis.ota.model.Product;
import blash10x.kis.ota.model.ProductPrice;
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

  private final OtaProperties otaProperties;
  private final BalanceService balanceService;
  private final RealtimePriceService realtimePriceService;

  public CashOrderService(
      OtaProperties otaProperties,
      KisProperties kisProperties,
      KisAuthService kisAuthService,
      BalanceService balanceService,
      RealtimePriceService  realtimePriceService) {
    super(otaProperties, kisProperties, kisAuthService);
    this.otaProperties = otaProperties;
    this.balanceService = balanceService;
    this.realtimePriceService = realtimePriceService;
  }

  public List<ReservationOrderSeq> orderCash(
      List<String> productNos, OrderCode orderCode, boolean real) {
    Map<String, Balance> balances = balanceService.getBalances();
    Map<String, Product> products = otaProperties.getProducts();

    List<String> orderProductNos = orderCode == OrderCode.SELL
        ? productNos.stream().filter(balances::containsKey).toList()
        : productNos.stream().filter(products::containsKey).toList();
    LOGGER.info("orderProductNos={}", orderProductNos);

    List<ReservationOrderSeq> results = new ArrayList<>();
    orderProductNos.forEach(
        productNo -> {
          ProductPrice productPrice = realtimePriceService.inquirePrice(MarketCode.J, productNo);
          MarketName marketName = MarketName.valueOf(productPrice.marketName());
          Product product = products.get(productNo);
          Balance balance = balances.get(productNo);
          double beta = Math.max(product.beta(), 0.90);
          int size = getOrderSize(balance, orderCode);
          for (int i = 1; i <= size; i++) {
            double rate = calculateRate(i, product, productPrice, orderCode);
            if (rate > 29.8) {
              break;
            }
            if (rate < 5.0 * beta
                && Double.parseDouble(balance.evaluationProfitLossRatio()) + rate < 0.5) {
              continue;
            }

            int realtimePrice = Integer.parseInt(productPrice.presentPrice());
            int direction = OrderCode.SELL == orderCode ? 1 : -1;
            double orderUnitPrice = realtimePrice * (100 + direction * rate) / 100;
            int tickPrice = calculateTickPrice(orderUnitPrice, marketName, orderCode);

            LOGGER.info(
                "{} | {} | {} ({}) | {} ({}) | {} | {} | {}",
                String.format("%2d", i),
                productNo,
                product.productName(),
                marketName,
                orderCode,
                product.beta(),
                String.format("%,6d", realtimePrice),
                String.format("%2.2f", direction * rate),
                String.format("%,6d", tickPrice));
            ReservationOrderSeq result = orderCash(balance.productNo(), tickPrice, orderCode, real);
            results.add(result);
          }
        });
    return results;
  }

  private ReservationOrderSeq orderCash(
      String productNo, int orderUnitPrice, OrderCode orderCode, boolean real) {
    if (!real) {
      return new ReservationOrderSeq("Mock");
    }

    String trId = orderCode == OrderCode.SELL ? TR_ID_SELL : TR_ID_BUY;
    MultiValueMap<String, String> headers = buildRequestHeaders(trId);
    Request request = buildRequest(productNo, "" + orderUnitPrice);
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
