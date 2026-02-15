package blash10x.kis.ota.service;

import blash10x.kis.ota.config.KisProperties;
import blash10x.kis.ota.model.AccessToken;
import blash10x.kis.ota.model.OrderCode;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * @author myungsik.sung@gmail.com
 */
@Service
@Data
abstract class TradingService {
  private final KisProperties kisProperties;
  private final KisAuthService kisAuthService;

  MultiValueMap<String, String> buildRequestHeaders(String trId) {
    AccessToken accessToken = getKisAuthService().authorize();

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

  double calculateRate(int i, double base, double beta, OrderCode code) {
    int direction = OrderCode.SELL == code ? 1 : -1;
    return (100.0 + direction * (base + beta * i)) / 100;
  }

  int calculateTickPrice(double price, OrderCode code) {
    return (int) (OrderCode.SELL == code ? Math.ceil(price / 5.0) : Math.floor(price / 5.0)) * 5;
  }
}
