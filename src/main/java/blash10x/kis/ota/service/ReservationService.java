package blash10x.kis.ota.service;

import blash10x.kis.ota.config.KisProperties;
import blash10x.kis.ota.config.ProductProperties;
import blash10x.kis.ota.model.AccessToken;
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
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

/**
 * @author myungsik.sung@gmail.com
 */
@Service
public class ReservationService extends TradingService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ReservationService.class);
  private static final String PATH = "/uapi/domestic-stock/v1/trading/order-resv";
  private static final String TR_ID = "CTSC0008U";
  private static final String ORD_QTY = "1"; // 주문수량
  private static final String ORD_DVSN_CD = "00"; // 지정가
  private static final String ORD_OBJT_CBLC_DVSN_CD = "10"; // 현금

  private final int repetitions;
  private final double baseRate;
  private final double applyRate;
  private final Map<String, Product> products;
  private final ExternalService externalService;
  private final BalanceService balanceService;
  private final String accountNo;
  private final String accountProductCode;
  private AccessToken accessToken;
  private MultiValueMap<String, String> headers;

  public ReservationService(
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

    String host = kisProperties.getHost();
    externalService = new ExternalService(host);
    accountNo = kisProperties.getAccountNo();
    accountProductCode = kisProperties.getAccountProductCode();
  }

  public List<ReservationOrderSeq> orderReservation(OrderCode orderCode) {
    if (accessToken == null) {
      accessToken = getKisAuthService().authorize();
      headers = buildRequestHeaders(accessToken, TR_ID);
    }

    List<ReservationOrderSeq> results = new ArrayList<>();
    List<Balance> balances = balanceService.inquireBalance();
    balances.stream().filter(balance -> products.containsKey(balance.productNo())).forEach(balance -> {
      Product product = products.get(balance.productNo());
      if (product.productName().equals(balance.productName())) {
        for (int i = 1; i <= repetitions; i++) {
          double rate = calculateRate(i, baseRate, product.beta() * applyRate, orderCode);
          double orderUnitPrice = Double.parseDouble(balance.presentPrice()) * rate;
          LOGGER.debug("{}: {} * {} = {}", orderCode, rate, balance.presentPrice(), orderUnitPrice);
          ReservationOrderSeq result = orderReservation(balance.productNo(), orderUnitPrice, orderCode);
          results.add(result);
        }
      }
    });
    return results;
  }

  private ReservationOrderSeq orderReservation(String productNo, double orderUnitPrice, OrderCode orderCode) {
    Request request = Request.builder()
        .accountNo(accountNo)
        .accountProductCode(accountProductCode)
        .productNo(productNo)
        .orderUnitPrice("" + calculateTickPrice(orderUnitPrice, orderCode))
        .sellBuyDivisionCode(orderCode.getCode())
        .orderQuantity(ORD_QTY)
        .orderDivisionCode(ORD_DVSN_CD)
        .orderObjectBalanceDivisionCode(ORD_OBJT_CBLC_DVSN_CD)
        .build();

    LOGGER.info("ReservationOrder: [{}] {}: {}",
        productNo, orderCode, String.format("%,8d", Integer.parseInt(request.orderUnitPrice)));

    Mono<ResponseEntity<Response>> mono = externalService.post(PATH, headers, null, request, Response.class);
    Response response = mono.mapNotNull(HttpEntity::getBody).block();
    if (response != null) {
      LOGGER.info("Response: {}", response.msg);
      return response.output;
    }
    return new ReservationOrderSeq("Unknown");
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
