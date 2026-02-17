package blash10x.kis.ota.service;

import blash10x.kis.ota.config.KisProperties;
import blash10x.kis.ota.config.OtaProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * @author myungsik.sung@gmail.com
 */
@Service
public class InterestStocksService extends TradingService {
  private static final Logger LOGGER = LoggerFactory.getLogger(InterestStocksService.class);
  private static final String PATH = "/uapi/domestic-stock/v1/quotations/intstock-stocklist-by-group";
  private static final String TR_ID = "HHKCM113004C6";

  public InterestStocksService(
      OtaProperties otaProperties, KisProperties kisProperties, KisAuthService kisAuthService) {
    super(otaProperties, kisProperties, kisAuthService);
  }

  public JsonNode inquireInterestStocks(String interestGroupCode) {
    MultiValueMap<String, String> headers = buildRequestHeaders(TR_ID);
    MultiValueMap<String, String> queryParams = buildRequestParams(interestGroupCode);
    Response response = get(PATH, headers, queryParams, Response.class).block();
    return response != null ? response.output2 : null;
  }

  private MultiValueMap<String, String> buildRequestParams(String interestGroupCode) {
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("TYPE", "1");
    queryParams.add("USER_ID", getKisProperties().getUserId());
    queryParams.add("DATA_RANK", "");
    queryParams.add("INTER_GRP_CODE", interestGroupCode);
    queryParams.add("INTER_GRP_NAME", "");
    queryParams.add("HTS_KOR_ISNM", "");
    queryParams.add("CNTG_CLS_CODE", "");
    queryParams.add("FID_ETC_CLS_CODE", "4");
    return queryParams;
  }

  private record Response(
      @JsonProperty("rt_cd") // 성공 실패 여부
      String rt_cd,
      @JsonProperty("msg_cd") // 응답코드
      String msg_cd,
      @JsonProperty("msg1") // 응답메세지
      String msg,
      @JsonProperty("output1")
      JsonNode output,
      @JsonProperty("output2")
      JsonNode output2) {}
}
