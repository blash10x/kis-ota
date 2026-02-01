package blash10x.kis.ota.service;

import blash10x.kis.ota.config.KisProperties;
import blash10x.kis.ota.model.AccessToken;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

/**
 * @author myungsik.sung@gmail.com
 */
@Service
@RequiredArgsConstructor
public class KisAuthService {
  private static final Logger LOGGER = LoggerFactory.getLogger(KisAuthService.class);
  private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
  private static final ObjectMapper objectMapper = new ObjectMapper();
  private final KisProperties kisProperties;

  public AccessToken authorize() {
    AccessToken accessToken = readAccessToken();
    if (accessToken == null) {
      accessToken = getAccessToken();
      saveAccessToken(accessToken);
    }
    return accessToken;
  }

  private Path findConfigPath() {
    LocalDate now = LocalDate.now();
    String configRoot = kisProperties.getConfigRoot();
    String prefix = "KIS-" + now;
    Path dir = Paths.get(configRoot);
    try (var stream = Files.list(dir)) {
      List<Path> files = stream
          .filter(p -> !Files.isDirectory(p))
          .filter(p -> p.getFileName().toString().startsWith(prefix))
          .toList().reversed();
      return files.isEmpty() ? null : files.getFirst();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private AccessToken readAccessToken() {
    Path configPath = findConfigPath();
    if (configPath != null) {
      AccessToken accessToken = objectMapper.readValue(configPath, AccessToken.class);
      String accessTokenExpired = accessToken.accessTokenExpired().replace(" ", "T");
      LocalDateTime expirationDatetime = LocalDateTime.parse(accessTokenExpired);
      if (expirationDatetime.isAfter(LocalDateTime.now())) {
        return accessToken;
      }
    }
    return null;
  }

  private void saveAccessToken(AccessToken accessToken) {
    String configRoot = kisProperties.getConfigRoot();
    Path configPath = Paths.get(configRoot, "KIS-" + DATETIME_FORMATTER.format(LocalDateTime.now()) + ".json");
    objectMapper.writeValue(configPath, accessToken);
    LOGGER.info("saved successfully: " + configPath);
  }

  private AccessToken getAccessToken() {
    String host = kisProperties.getHost();
    String appKey = kisProperties.getAppKey();
    String appSecret = kisProperties.getAppSecret();

    WebClient webClient = WebClient.builder()
        .baseUrl(host)
        .build();

    GetAccessTokenRequest getAccessTokenRequest = GetAccessTokenRequest.builder()
        .grantType("client_credentials")
        .appKey(appKey)
        .appSecret(appSecret)
        .build();

    Mono<ResponseEntity<AccessToken>> mono = webClient.post()
        .uri("/oauth2/tokenP")
        .contentType(MediaType.APPLICATION_JSON)
        .bodyValue(getAccessTokenRequest)
        .retrieve()
        .toEntity(AccessToken.class);
    return mono.block().getBody();
  }

  @Builder
  private record GetAccessTokenRequest(
      @JsonProperty("grant_type") String grantType,
      @JsonProperty("appkey") String appKey,
      @JsonProperty("appsecret") String appSecret) {
  }
}
