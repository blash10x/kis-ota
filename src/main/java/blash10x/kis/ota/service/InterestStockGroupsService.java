package blash10x.kis.ota.service;

import blash10x.kis.ota.config.KisProperties;
import blash10x.kis.ota.config.OtaProperties;
import blash10x.kis.ota.model.InterestStockGroup;
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
public class InterestStockGroupsService extends TradingService {
  private static final Logger LOGGER = LoggerFactory.getLogger(InterestStockGroupsService.class);
  private static final String PATH = "/uapi/domestic-stock/v1/quotations/intstock-grouplist";
  private static final String TR_ID = "HHKCM113004C7";

  public InterestStockGroupsService(
      OtaProperties otaProperties, KisProperties kisProperties, KisAuthService kisAuthService) {
    super(otaProperties, kisProperties, kisAuthService);
  }

  public List<InterestStockGroup> inquireInterestStockGroups() {
    MultiValueMap<String, String> headers = buildRequestHeaders(TR_ID);
    MultiValueMap<String, String> queryParams = buildRequestParams();
    Response response = get(PATH, headers, queryParams, Response.class).block();
    return response != null ? response.output : List.of();
  }

  private MultiValueMap<String, String> buildRequestParams() {
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("TYPE", "1");
    queryParams.add("FID_ETC_CLS_CODE", "00");
    queryParams.add("USER_ID", getKisProperties().getUserId());
    return queryParams;
  }

  private record Response(
      @JsonProperty("rt_cd") // 성공 실패 여부
      String rt_cd,
      @JsonProperty("msg_cd") // 응답코드
      String msg_cd,
      @JsonProperty("msg1") // 응답메세지
      String msg,
      @JsonProperty("output2")
      List<InterestStockGroup> output) {}
}
