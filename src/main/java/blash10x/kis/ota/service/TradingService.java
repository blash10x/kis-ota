package blash10x.kis.ota.service;

import blash10x.kis.ota.config.KisProperties;
import blash10x.kis.ota.core.external.ExternalService;
import blash10x.kis.ota.core.util.JsonNodes;
import blash10x.kis.ota.model.AccessToken;
import blash10x.kis.ota.model.Balance;
import blash10x.kis.ota.model.MarketName;
import blash10x.kis.ota.model.OrderCode;
import blash10x.kis.ota.model.ProductPrice;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.Map;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * @author myungsik.sung@gmail.com
 */
@Service
@Data
abstract class TradingService {
  private static final Logger LOGGER = LoggerFactory.getLogger(TradingService.class);
  private final KisProperties kisProperties;
  private final KisAuthService kisAuthService;
  private final String appKey;
  private final String appSecret;
  private final String accountNo;
  private final String accountProductCode;
  private final ExternalService externalService;
  private AccessToken accessToken;

  public TradingService(KisProperties kisProperties, KisAuthService kisAuthService) {
    this.kisProperties = kisProperties;
    this.kisAuthService = kisAuthService;

    appKey = kisProperties.getAppKey();
    appSecret = kisProperties.getAppSecret();
    accountNo = kisProperties.getAccountNo();
    accountProductCode = kisProperties.getAccountProductCode();

    String host = kisProperties.getHost();
    externalService = new ExternalService(host);
  }

  public <T> Mono<T> get(
      String path,
      MultiValueMap<String, String> headers,
      MultiValueMap<String, ?> queryParams,
      Class<T> responseType) {
    return externalService
        .get(path, headers, queryParams, JsonNode.class)
        .retryWhen(Retry.backoff(3, Duration.ofMillis(100)))
        .mapNotNull(
            responseEntity -> {
              JsonNode jsonNode = responseEntity.getBody();
              LOGGER.debug("{}", jsonNode);
              return JsonNodes.toValue(jsonNode, responseType);
            });
  }

  public <T> Mono<T> post(
      String path,
      MultiValueMap<String, String> headers,
      MultiValueMap<String, ?> queryParams,
      Object requestBody,
      Class<T> responseType) {
    return externalService
        .post(path, headers, queryParams, requestBody, JsonNode.class)
        .retryWhen(Retry.backoff(3, Duration.ofMillis(100)))
        .mapNotNull(
            responseEntity -> {
              JsonNode jsonNode = responseEntity.getBody();
              LOGGER.debug("{}", jsonNode);
              return JsonNodes.toValue(jsonNode, responseType);
            });
  }

  MultiValueMap<String, String> buildRequestHeaders(String trId) {
    if (accessToken == null) {
      accessToken = getKisAuthService().authorize();
    }

    String authorization = accessToken.accessToken();
    String tokenType = accessToken.tokenType();

    MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    headers.set(HttpHeaders.AUTHORIZATION, tokenType + " " + authorization);
    headers.set("appkey", appKey);
    headers.set("appsecret", appSecret);
    headers.set("tr_id", trId);
    return headers;
  }

  int getOrderSize(Balance balance, OrderCode orderCode, Map<OrderCode, Integer> maxRepetitions) {
    if (OrderCode.BUY == orderCode) {
      return maxRepetitions.get(orderCode);
    }
    if (balance == null) {
      return 0;
    }
    int orderPossibleQuantity = Integer.parseInt(balance.orderPossibleQuantity());
    return Math.min(orderPossibleQuantity, maxRepetitions.get(orderCode));
  }

  double calculateRate(
      int i,
      double beta,
      ProductPrice productPrice,
      OrderCode orderCode,
      Map<MarketName, Double> baseRates,
      Map<MarketName, Double> stepRates,
      Map<OrderCode, Double> multipleRates) {
    MarketName marketName = MarketName.valueOf(productPrice.marketName());
    double baseRate = baseRates.get(marketName);
    double stepRate = stepRates.get(marketName);
    double multipleRate = multipleRates.get(orderCode);

    return baseRate + i * beta * stepRate * multipleRate;
  }

  int calculateTickPrice(double price, MarketName marketName, OrderCode code) {
    int tick;
    if (price < 2000) {
      tick = 1;
    } else if (price < 5000 || marketName == MarketName.ETF) {
      tick = 5;
    } else if (price < 20000) {
      tick = 10;
    } else if (price < 50000) {
      tick = 50;
    } else if (price < 200000) {
      tick = 100;
    } else if (price < 500000) {
      tick = 500;
    } else {
      tick = 1000;
    }
    return (int) (OrderCode.SELL == code ? Math.ceil(price / tick) : Math.floor(price / tick)) * tick;
  }

  void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      LOGGER.warn("Interrupted while waiting for sleep", e);
    }
  }
}
