package blash10x.kis.ota.service;

import blash10x.kis.ota.core.service.ClientException;
import blash10x.kis.ota.core.service.ServiceException;
import blash10x.kis.ota.core.util.JsonNodes;
import com.fasterxml.jackson.databind.JsonNode;
import io.netty.handler.logging.LogLevel;
import java.net.URI;
import java.nio.charset.Charset;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriBuilder;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

/**
 * @author myungsik.sung@gmail.com
 */
final class ExternalService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExternalService.class);
  private final Duration timeout = Duration.ofMillis(500);
  private final WebClient webClient;

  ExternalService(String baseUrl) {
    this.webClient = createWebClient(baseUrl);
  }

  private WebClient createWebClient(String baseUrl) {
    return WebClient.builder()
        .clientConnector(createClientHttpConnector())
        .exchangeStrategies(buildExchangeStrategies())
        .baseUrl(baseUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

  private ClientHttpConnector createClientHttpConnector() {
    ConnectionProvider connectionProvider =
        ConnectionProvider.builder("ota")
            .maxIdleTime(Duration.ofSeconds(30))
            .maxLifeTime(Duration.ofSeconds(55))
            .pendingAcquireTimeout(Duration.ofSeconds(45))
            .evictInBackground(Duration.ofSeconds(55))
            .lifo()
            .metrics(true)
            .build();

    HttpClient httpClient =
        HttpClient.create(connectionProvider)
            .followRedirect(true)
            .wiretap(HttpClient.class.getName(), LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL);
    httpClient.warmup().block(); // Eager Initialization
    return new ReactorClientHttpConnector(httpClient);
  }

  private ExchangeStrategies buildExchangeStrategies() {
    return ExchangeStrategies.builder()
        .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
        .build(); // to unlimited memory size: -1
  }

  private URI buildURI(String path, MultiValueMap<String, ?> queryParams, UriBuilder uriBuilder) {
    uriBuilder.path(path);
    if (queryParams != null && !queryParams.isEmpty()) {
      queryParams.forEach(uriBuilder::queryParam);
    }
    return uriBuilder.build();
  }

  public <T> Mono<ResponseEntity<T>> get(
      String path,
      MultiValueMap<String, String> headers,
      MultiValueMap<String, ?> queryParams,
      Class<T> responseType) {
    RequestHeadersSpec<?> requestHeadersSpec =
        webClient.get().uri(uriBuilder -> buildURI(path, queryParams, uriBuilder));
    return retrieve(headers, requestHeadersSpec, responseType);
  }

  public <T> Mono<ResponseEntity<T>> post(
      String path,
      MultiValueMap<String, String> headers,
      MultiValueMap<String, ?> queryParams,
      Object requestBody,
      Class<T> responseType) {
    RequestBodySpec requestBodySpec =
        webClient
            .post()
            .uri(uriBuilder -> buildURI(path, queryParams, uriBuilder))
            .contentType(MediaType.APPLICATION_JSON);
    if (requestBody != null) {
      requestBodySpec.bodyValue(requestBody);
    }
    return retrieve(headers, requestBodySpec, responseType);
  }

  private <T> Mono<ResponseEntity<T>> retrieve(
      MultiValueMap<String, String> headers,
      RequestHeadersSpec<?> requestHeadersSpec,
      Class<T> responseType) {
    if (headers != null && !headers.isEmpty()) {
      requestHeadersSpec.headers(httpHeaders -> httpHeaders.putAll(headers));
    }
    return requestHeadersSpec
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, this::on4xxClientError)
        .onStatus(HttpStatusCode::is5xxServerError, this::on5xxServerError)
        .toEntity(responseType)
        .timeout(timeout);
  }

  private Mono<WebClientResponseException> on4xxClientError(ClientResponse clientResponse) {
    return clientResponse
        .createException()
        .flatMap(
            ex -> {
              String errorMessage = buildErrorMessage(ex);
              LOGGER.warn("{}", errorMessage);
              return Mono.error(new ClientException(ex.getStatusCode(), errorMessage));
            });
  }

  private Mono<WebClientResponseException> on5xxServerError(ClientResponse clientResponse) {
    return clientResponse
        .createException()
        .flatMap(
            ex -> {
              String errorMessage = buildErrorMessage(ex);
              LOGGER.warn("{}", errorMessage);
              return Mono.error(new ServiceException(ex.getStatusCode(), errorMessage));
            });
  }

  private String buildErrorMessage(WebClientResponseException responseEx) {
    StringBuilder message = new StringBuilder();
    message.append(responseEx.getStatusCode());
    if (responseEx.getRequest() != null) {
      message.append(" ").append(responseEx.getRequest().getURI());
    }

    String body = responseEx.getResponseBodyAsString(Charset.defaultCharset());
    try {
      JsonNode responseNode = JsonNodes.toValue(body, JsonNode.class);
      if (responseNode != null) {
        JsonNode errorNode = responseNode.get("error");
        if (errorNode == null) {
          errorNode = responseNode.get("message");
        }
        if (errorNode != null) {
          message.append(" ").append(errorNode.asText());
        } else {
          message.append(" ").append("Unknown message: ").append(responseNode);
        }
      }
    } catch (Exception ex) {
      LOGGER.warn("{}", ex.getMessage());
    }
    return message.toString();
  }
}
