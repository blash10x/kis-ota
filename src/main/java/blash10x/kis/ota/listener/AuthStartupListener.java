package blash10x.kis.ota.listener;

import blash10x.kis.ota.model.AccessToken;
import blash10x.kis.ota.service.KisAuthService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * @author myungsik.sung@gmail.com
 */
@Profile("!test")
@Component
@RequiredArgsConstructor
public class AuthStartupListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(AuthStartupListener.class);
  private final KisAuthService authService;

  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    AccessToken accessToken = authService.authorize();
    if (accessToken == null) {
      LOGGER.warn("Failed to authorize access token");
    } else {
      LOGGER.info("The access token will expire on: {}", accessToken.accessTokenExpired());
    }
  }
}
