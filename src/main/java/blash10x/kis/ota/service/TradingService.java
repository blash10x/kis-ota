package blash10x.kis.ota.service;

import blash10x.kis.ota.config.KisProperties;
import blash10x.kis.ota.core.external.ExternalService;
import blash10x.kis.ota.core.util.JsonNodes;
import blash10x.kis.ota.model.AccessToken;
import blash10x.kis.ota.model.OrderCode;
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

  double calculateRate(int i, double base, double beta, OrderCode code) {
    int direction = OrderCode.SELL == code ? 1 : -1;
    return (100.0 + direction * (base + beta * i)) / 100;
  }

  int calculateTickPrice(double price, OrderCode code) {
    return (int) (OrderCode.SELL == code ? Math.ceil(price / 5.0) : Math.floor(price / 5.0)) * 5;
  }
}
