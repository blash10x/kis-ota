package blash10x.kis.ota.service;

import blash10x.kis.ota.model.AccessToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author myungsik.sung@gmail.com
 */
@Service
@RequiredArgsConstructor
public class TradingService {
  private final KisAuthService kisAuthService;

  public String index() {
    AccessToken accessToken = kisAuthService.authorize();

    return accessToken.accessTokenExpired();
  }
}
