package blash10x.kis.ota.service;

import blash10x.kis.ota.config.KisProperties;
import blash10x.kis.ota.core.external.ExternalService;
import blash10x.kis.ota.model.MarketCode;
import blash10x.kis.ota.model.ProductPrice;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

/**
 * @author myungsik.sung@gmail.com
 */
@Service
public class RealtimePriceService extends TradingService {
  private static final Logger LOGGER = LoggerFactory.getLogger(BalanceService.class);
  private static final String PATH = "/uapi/domestic-stock/v1/quotations/inquire-price";
  private static final String TR_ID = "FHKST01010100";

  private final ExternalService externalService;
  private MultiValueMap<String, String> headers;

  public RealtimePriceService(
      KisProperties kisProperties,
      KisAuthService kisAuthService) {
    super(kisProperties, kisAuthService);

    String host = kisProperties.getHost();
    externalService = new ExternalService(host);
  }

  public ProductPrice inquirePrice(MarketCode marketDivisionCode, String productNo) {
    if (headers == null) {
      headers = buildRequestHeaders(TR_ID);
    }

    MultiValueMap<String, String> queryParams = buildRequestParams(marketDivisionCode, productNo);
    Mono<ResponseEntity<Response>> mono =
        externalService.get(PATH, headers, queryParams, Response.class);

    Response response = mono.mapNotNull(HttpEntity::getBody).block();
    if (response != null) {
      LOGGER.info("Response: {}", response.msg);
      return response.output;
    }
    return null;
  }

  private MultiValueMap<String, String> buildRequestParams(MarketCode marketDivisionCode, String productNo) {
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("FID_COND_MRKT_DIV_CODE", marketDivisionCode.name());
    queryParams.add("FID_INPUT_ISCD", productNo);
    return queryParams;
  }

  private record Response(
      @JsonProperty("rt_cd") // 성공 실패 여부
      String rt_cd,
      @JsonProperty("msg_cd") // 응답코드
      String msg_cd,
      @JsonProperty("msg1") // 응답메세지
      String msg,
      @JsonProperty("output")
      ProductPrice output) {}
}
