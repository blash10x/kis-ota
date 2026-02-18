package blash10x.kis.ota.service;

import blash10x.kis.ota.config.KisProperties;
import blash10x.kis.ota.config.OtaProperties;
import blash10x.kis.ota.controller.dto.CreateReservationOrderRequest;
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
public class ReservationOrderService extends TradingService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ReservationOrderService.class);
  private static final String PATH = "/uapi/domestic-stock/v1/trading/order-resv";
  private static final String TR_ID = "CTSC0008U";
  private static final String ORD_QTY = "1"; // 주문수량
  private static final String ORD_DVSN_CD = "00"; // 지정가
  private static final String ORD_OBJT_CBLC_DVSN_CD = "10"; // 현금

  private final OtaProperties otaProperties;
  private final BalanceService balanceService;
  private final RealtimePriceService realtimePriceService;

  public ReservationOrderService(
      OtaProperties otaProperties,
      KisProperties kisProperties,
      KisAuthService kisAuthService,
      BalanceService balanceService,
      RealtimePriceService realtimePriceService) {
    super(otaProperties, kisProperties, kisAuthService);
    this.otaProperties = otaProperties;
    this.balanceService = balanceService;
    this.realtimePriceService = realtimePriceService;
  }

  public List<ReservationOrderSeq> orderReservation(
      CreateReservationOrderRequest request) {
    OrderCode orderCode = request.orderCode();
    List<String> productNos = request.productNos();
    Map<OrderCode, Integer> maxRepetitions = request.maxRepetitions();
    Map<MarketName, Double> baseRates = request.baseRates();
    Map<MarketName, Double> stepRates = request.stepRates();
    Map<OrderCode, Double> multipleRates = request.multipleRates();
    boolean real = request.real();

    Map<String, Balance> balances = balanceService.getBalances();
    Map<String, Product> products = otaProperties.getProducts();

    List<String> orderProductNos = OrderCode.SELL == orderCode
        ? productNos.stream().filter(balances::containsKey).toList()
        : productNos.stream().filter(products::containsKey).toList();
    LOGGER.info("orderProductNos={}", orderProductNos);

    if (orderProductNos.isEmpty() && OrderCode.SELL == orderCode) {
      orderProductNos = balances.keySet().stream().toList();
    } else if (orderProductNos.isEmpty() && OrderCode.BUY == orderCode) {
      orderProductNos = products.keySet().stream().toList();
    }

    List<ReservationOrderSeq> results = new ArrayList<>();
    orderProductNos.forEach(
        productNo -> {
          ProductPrice productPrice = realtimePriceService.inquirePrice(MarketCode.J, productNo);
          MarketName marketName = MarketName.valueOf(productPrice.marketName());
          Product product = products.get(productNo);
          Balance balance = balances.get(productNo);
          double beta = Math.max(product.beta(), 0.90);
          int size = getOrderSize(balance, orderCode, maxRepetitions);
          for (int i = 1; i <= size; i++) {
            double rate =
                calculateRate(
                    i, product, productPrice, orderCode, baseRates, stepRates, multipleRates);
            if (rate > 29.8) {
              break;
            }
            if (OrderCode.SELL == orderCode && rate < 5.0 * beta
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
            ReservationOrderSeq result =
                orderReservation(product.productNo(), tickPrice, orderCode, real);
            results.add(result);
          }
        });
    return results;
  }

  private ReservationOrderSeq orderReservation(
      String productNo, int orderUnitPrice, OrderCode orderCode, boolean real) {
    if (!real) {
      return new ReservationOrderSeq("Mock");
    }

    MultiValueMap<String, String> headers = buildRequestHeaders(TR_ID);
    Request request = buildRequest(productNo, "" + orderUnitPrice, orderCode);
    Response response = post(PATH, headers, null, request, Response.class).block();
    sleep(100); // 20 transactions per second per account
    return response != null ? response.output : new ReservationOrderSeq("Unknown");
  }

  private Request buildRequest(
      String productNo, String orderUnitPrice, OrderCode orderCode) {
    return Request.builder()
        .accountNo(getAccountNo())
        .accountProductCode(getAccountProductCode())
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
