package blash10x.kis.ota.service;

import blash10x.kis.ota.external.TradingService;
import blash10x.kis.ota.model.Balance;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Service
public class BalanceService {
  private static final Logger LOGGER = LoggerFactory.getLogger(BalanceService.class);
  private static final String PATH = "/uapi/domestic-stock/v1/trading/inquire-balance";
  private static final String TR_ID = "TTTC8434R";

  private final TradingService tradingService;

  public BalanceService(TradingService tradingService) {
    this.tradingService = tradingService;
  }

  public List<Balance> inquireBalances() {
    MultiValueMap<String, String> headers = tradingService.buildRequestHeaders(TR_ID);
    MultiValueMap<String, String> queryParams = buildRequestParams();
    Response response = tradingService.get(PATH, headers, queryParams, Response.class).block();
    return response != null ? response.output : List.of();
  }

  public Map<String, Balance> getBalances() {
    List<Balance> balances = inquireBalances();
    return balances.stream().collect(Collectors.toMap(Balance::productNo, v -> v));
  }

  private MultiValueMap<String, String> buildRequestParams() {
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("CANO", tradingService.getAccountNo());
    queryParams.add("ACNT_PRDT_CD", tradingService.getAccountProductCode());

    queryParams.add("AFHR_FLPR_YN", "N");
    queryParams.add("OFL_YN", "");
    queryParams.add("INQR_DVSN", "01");
    queryParams.add("UNPR_DVSN", "01");
    queryParams.add("FUND_STTL_ICLD_YN", "N");
    queryParams.add("FNCG_AMT_AUTO_RDPT_YN", "N");
    queryParams.add("PRCS_DVSN", "00");
    queryParams.add("CTX_AREA_FK100", "");
    queryParams.add("CTX_AREA_NK100", "");
    return queryParams;
  }

  private record Response(
      @JsonProperty("rt_cd") // 성공 실패 여부
      String rt_cd,
      @JsonProperty("msg_cd") // 응답코드
      String msg_cd,
      @JsonProperty("msg1") // 응답메세지
      String msg,
      @JsonProperty("output1") // 응답메세지
      List<Balance> output) {}
}
