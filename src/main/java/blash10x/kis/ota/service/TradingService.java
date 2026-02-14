package blash10x.kis.ota.service;

import blash10x.kis.ota.config.KisProperties;
import blash10x.kis.ota.model.AccessToken;
import blash10x.kis.ota.model.OrderCode;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * @author myungsik.sung@gmail.com
 */
@Service
@RequiredArgsConstructor
@Data
abstract class TradingService {
  private final KisProperties kisProperties;
  private final KisAuthService kisAuthService;

  MultiValueMap<String, String> buildRequestHeaders(AccessToken accessToken, String trId) {
    String authorization = accessToken.accessToken();
    String tokenType = accessToken.tokenType();
    String appKey = kisProperties.getAppKey();
    String appSecret = kisProperties.getAppSecret();

    MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    headers.set(HttpHeaders.AUTHORIZATION, tokenType + " " + authorization);
    headers.set("appkey", appKey);
    headers.set("appsecret", appSecret);
    headers.set("tr_id", trId);
    return headers;
  }

  int calculateOrderPrice(double price, OrderCode code) {
    int diff = OrderCode.BUY == code ? 0 : 4; // 내림(flooring) : 올림(ceiling)
    return (int) (((price + diff) / 5) * 5);
  }
}
