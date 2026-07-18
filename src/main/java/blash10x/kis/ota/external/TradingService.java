package blash10x.kis.ota.external;

import blash10x.kis.ota.config.KisProperties;
import blash10x.kis.ota.core.external.ExternalService;
import blash10x.kis.ota.core.util.JsonNodes;
import blash10x.kis.ota.model.AccessToken;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * KIS REST API 호출을 담당하는 공용 클라이언트. 인증 헤더 구성·재시도·계정 정보 접근을 모아 둔다.
 *
 * <p>이전에는 이 기능을 추상 베이스에서 상속으로 나눠 썼으나, 잔고 조회처럼 무관한 서비스까지 HTTP 를 물려받고 서비스마다
 * {@link ExternalService}(WebClient)와 토큰 캐시가 따로 생기는 문제가 있어 합성으로 바꿨다. 빈 하나라 WebClient·토큰이 공유된다.
 *
 * @author myungsik.sung@gmail.com
 */
@Service
public class TradingService {
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
    return getEntity(path, headers, queryParams, responseType).mapNotNull(HttpEntity::getBody);
  }

  /** 응답 헤더가 필요한 경우 사용한다. (연속조회의 tr_cont 등) */
  public <T> Mono<ResponseEntity<T>> getEntity(
      String path,
      MultiValueMap<String, String> headers,
      MultiValueMap<String, ?> queryParams,
      Class<T> responseType) {
    return externalService
        .get(path, headers, queryParams, JsonNode.class)
        .retryWhen(Retry.backoff(3, Duration.ofMillis(100)))
        .map(
            responseEntity -> {
              JsonNode jsonNode = responseEntity.getBody();
              LOGGER.debug("{}", jsonNode);
              return ResponseEntity.status(responseEntity.getStatusCode())
                  .headers(responseEntity.getHeaders())
                  .body(JsonNodes.toValue(jsonNode, responseType));
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

  public MultiValueMap<String, String> buildRequestHeaders(String trId) {
    if (accessToken == null) {
      accessToken = kisAuthService.authorize();
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

  public void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      LOGGER.warn("Interrupted while waiting for sleep", e);
    }
  }

  public KisProperties getKisProperties() {
    return kisProperties;
  }

  public String getAccountNo() {
    return accountNo;
  }

  public String getAccountProductCode() {
    return accountProductCode;
  }
}
