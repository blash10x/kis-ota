package blash10x.kis.ota.service;

import blash10x.kis.ota.config.KisProperties;
import blash10x.kis.ota.config.OtaProperties;
import blash10x.kis.ota.core.external.ExternalService;
import blash10x.kis.ota.core.util.JsonNodes;
import blash10x.kis.ota.model.AccessToken;
import blash10x.kis.ota.model.MarketName;
import blash10x.kis.ota.model.OrderCode;
import blash10x.kis.ota.model.Product;
import blash10x.kis.ota.model.ProductPrice;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

/**
 * @author myungsik.sung@gmail.com
 */
@Service
@Data
abstract class TradingService {
  private static final Logger LOGGER = LoggerFactory.getLogger(TradingService.class);
  private final OtaProperties otaProperties;
  private final KisProperties kisProperties;
  private final KisAuthService kisAuthService;
  private final String appKey;
  private final String appSecret;
  private final String accountNo;
  private final String accountProductCode;
  private final ExternalService externalService;
  private AccessToken accessToken;

  public TradingService(
      OtaProperties otaProperties, KisProperties kisProperties, KisAuthService kisAuthService) {
    this.otaProperties = otaProperties;
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
        .retry(2)
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
        .retry(2)
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

  double calculateRate(int i, Product product, ProductPrice productPrice, OrderCode orderCode) {
    MarketName marketName = MarketName.valueOf(productPrice.marketName());
    double multipleRate = otaProperties.getMultipleRates().get(orderCode);
    double baseRate = otaProperties.getBaseRates().get(marketName);
    double stepRate = otaProperties.getStepRates().get(marketName);
    double beta = Math.max(product.beta(), 0.95);

    return baseRate + beta * stepRate * multipleRate * i;
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
}
