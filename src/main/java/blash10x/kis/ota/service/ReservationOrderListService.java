package blash10x.kis.ota.service;

import blash10x.kis.ota.config.KisProperties;
import blash10x.kis.ota.core.util.JsonNodes;
import blash10x.kis.ota.model.Balance;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
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
public class ReservationOrderListService extends TradingService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ReservationOrderListService.class);
  private static final String PATH = "/uapi/domestic-stock/v1/trading/order-resv-ccnl";
  private static final String TR_ID = "CTSC0004R";

  public ReservationOrderListService(KisProperties kisProperties, KisAuthService kisAuthService) {
    super(kisProperties, kisAuthService);
  }

  public JsonNode inquireReservationOrder(String date) {
    if (date == null) {
      LocalDate now = LocalDate.now();
      date = now.toString();
    }
    date = date.replace("-", "");

    MultiValueMap<String, String> headers = buildRequestHeaders(TR_ID);
    MultiValueMap<String, String> queryParams = buildRequestParams(date, date);
    JsonNode response = get(PATH, headers, queryParams, JsonNode.class).block();
    return response != null ? response : JsonNodes.createEmptyObjectNode();
  }

  private MultiValueMap<String, String> buildRequestParams(String startDate, String endDate) {
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("CANO", getAccountNo());
    queryParams.add("ACNT_PRDT_CD", getAccountProductCode());

    queryParams.add("RSVN_ORD_ORD_DT", startDate);
    queryParams.add("RSVN_ORD_END_DT", endDate);
    queryParams.add("RSVN_ORD_SEQ", "");
    queryParams.add("TMNL_MDIA_KIND_CD", "00");
    queryParams.add("PRCS_DVSN_CD", "0");
    queryParams.add("CNCL_YN", "N");
    queryParams.add("PDNO", "");
    queryParams.add("SLL_BUY_DVSN_CD", "");
    queryParams.add("CTX_AREA_FK200", "");
    queryParams.add("CTX_AREA_NK200", "");
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
