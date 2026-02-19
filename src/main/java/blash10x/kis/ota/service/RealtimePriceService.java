package blash10x.kis.ota.service;

import blash10x.kis.ota.config.KisProperties;
import blash10x.kis.ota.model.MarketCode;
import blash10x.kis.ota.model.ProductPrice;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * @author myungsik.sung@gmail.com
 */
@Service
public class RealtimePriceService extends TradingService {
  private static final Logger LOGGER = LoggerFactory.getLogger(BalanceService.class);
  private static final String PATH = "/uapi/domestic-stock/v1/quotations/inquire-price";
  private static final String TR_ID = "FHKST01010100";

  public RealtimePriceService(KisProperties kisProperties, KisAuthService kisAuthService) {
    super(kisProperties, kisAuthService);
  }

  public List<ProductPrice> inquirePrices(MarketCode marketDivisionCode, List<String> productNos) {
    return productNos.stream()
        .map(productNo -> inquirePrice(marketDivisionCode, productNo))
        .toList();
  }

  public ProductPrice inquirePrice(MarketCode marketDivisionCode, String productNo) {
    MultiValueMap<String, String> headers = buildRequestHeaders(TR_ID);
    MultiValueMap<String, String> queryParams = buildRequestParams(marketDivisionCode, productNo);
    Response response = get(PATH, headers, queryParams, Response.class).block();
    return response != null ? response.output : null;
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
