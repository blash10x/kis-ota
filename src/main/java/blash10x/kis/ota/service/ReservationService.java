package blash10x.kis.ota.service;

import blash10x.kis.ota.config.KisProperties;
import blash10x.kis.ota.config.ProductProperties;
import blash10x.kis.ota.core.util.JsonNodes;
import blash10x.kis.ota.model.AccessToken;
import blash10x.kis.ota.model.Balance;
import blash10x.kis.ota.model.OrderCode;
import blash10x.kis.ota.model.Product;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
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

  public JsonNode orderReservation(OrderCode orderCode) {
    if (accessToken == null) {
      accessToken = getKisAuthService().authorize();
      headers = buildRequestHeaders(accessToken, TR_ID);
    }

    List<Balance> balances = balanceService.inquireBalance();
    balances.stream().filter(balance -> products.containsKey(balance.productNo())).forEach(balance -> {
      Product product = products.get(balance.productNo());
      if (product.productName().equals(balance.productName())) {
        for (int i = 1; i <= repetitions; i++) {
          double rate = calculateRate(i, baseRate, product.beta() * applyRate, orderCode);
          double orderUnitPrice = Double.parseDouble(balance.presentPrice()) * rate;
          System.out.println(product + ": " + balance.presentPrice() + " -> " + orderUnitPrice + " -> " + calculateTickPrice(orderUnitPrice, orderCode) + ", rate=" + rate);
          //orderReservation(balance.productNo(), orderUnitPrice, orderCode);
        }
      }
    });

    return JsonNodes.createEmptyObjectNode();
  }

  private JsonNode orderReservation(String productNo, double orderUnitPrice, OrderCode orderCode) {
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

    Mono<ResponseEntity<JsonNode>> mono = externalService.post(PATH, headers, null, request, JsonNode.class);
    JsonNode jsonNode = mono.mapNotNull(HttpEntity::getBody).block();
    return jsonNode;
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
}
