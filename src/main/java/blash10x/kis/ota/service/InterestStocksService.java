package blash10x.kis.ota.service;

import blash10x.kis.ota.external.TradingService;
import blash10x.kis.ota.model.InterestStock;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
public class InterestStocksService {
  private static final Logger LOGGER = LoggerFactory.getLogger(InterestStocksService.class);
  private static final String PATH = "/uapi/domestic-stock/v1/quotations/intstock-stocklist-by-group";
  private static final String TR_ID = "HHKCM113004C6";

  private final TradingService tradingService;

  public InterestStocksService(TradingService tradingService) {
    this.tradingService = tradingService;
  }

  public List<InterestStock> inquireInterestStocks(String interestGroupCode) {
    MultiValueMap<String, String> headers = tradingService.buildRequestHeaders(TR_ID);
    MultiValueMap<String, String> queryParams = buildRequestParams(interestGroupCode);
    Response response = tradingService.get(PATH, headers, queryParams, Response.class).block();
    return response != null ? response.output2 : null;
  }

  public Map<String, InterestStock> getInterestStocks(String interestGroupCode) {
    List<InterestStock> balances = inquireInterestStocks(interestGroupCode);
    return balances.stream().collect(Collectors.toMap(InterestStock::stockCode, v -> v));
  }

  private MultiValueMap<String, String> buildRequestParams(String interestGroupCode) {
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("TYPE", "1");
    queryParams.add("USER_ID", tradingService.getKisProperties().getUserId());
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
      List<InterestStock> output2) {}
}
