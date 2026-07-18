package blash10x.kis.ota.service;

import blash10x.kis.ota.core.util.JsonNodes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * @author myungsik.sung@gmail.com
 */
@Service
public class ReservationOrderListService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ReservationOrderListService.class);
  private static final String PATH = "/uapi/domestic-stock/v1/trading/order-resv-ccnl";
  private static final String TR_ID = "CTSC0004R";
  private static final String TR_CONT = "tr_cont";
  private static final String TR_CONT_INITIAL = ""; // 최초 조회
  private static final String TR_CONT_NEXT = "N"; // 다음 페이지 조회
  private static final Set<String> TR_CONT_HAS_NEXT = Set.of("F", "M"); // 다음 페이지 존재
  private static final int MAX_PAGES = 50; // 1회 20건 * 50 = 기간예약주문 계좌당 최대 1,000건

  private final KisClient kisClient;

  public ReservationOrderListService(KisClient kisClient) {
    this.kisClient = kisClient;
  }

  /** 1회 조회 시 최대 20건만 응답하므로, tr_cont 헤더와 CTX_AREA_FK200/NK200으로 연속조회하여 모두 수집한다. */
  public JsonNode inquireReservationOrder(String date) {
    if (date == null) {
      LocalDate now = LocalDate.now();
      date = now.toString();
    }
    date = date.replace("-", "");

    ObjectNode result = null;
    ArrayNode outputs = JsonNodes.createEmptyArrayNode();
    String trCont = TR_CONT_INITIAL;
    String fk200 = "";
    String nk200 = "";

    for (int page = 1; page <= MAX_PAGES; page++) {
      MultiValueMap<String, String> headers = kisClient.buildRequestHeaders(TR_ID);
      headers.set(TR_CONT, trCont);
      MultiValueMap<String, String> queryParams = buildRequestParams(date, date, fk200, nk200);
      ResponseEntity<JsonNode> responseEntity =
          kisClient.getEntity(PATH, headers, queryParams, JsonNode.class).block();

      JsonNode body = responseEntity != null ? responseEntity.getBody() : null;
      if (body == null || !body.isObject()) {
        LOGGER.warn("Truncated at {} orders: page {} returned no data", outputs.size(), page);
        break;
      }
      if (result == null) {
        result = body.deepCopy();
      }

      JsonNode output = body.get("output");
      if (output != null && output.isArray()) {
        outputs.addAll((ArrayNode) output);
      }

      trCont = responseEntity.getHeaders().getFirst(TR_CONT);
      if (trCont == null || !TR_CONT_HAS_NEXT.contains(trCont)) {
        break;
      }
      if (page == MAX_PAGES) {
        LOGGER.warn("Truncated at {} orders: more pages remain (tr_cont={})", outputs.size(), trCont);
        break;
      }

      trCont = TR_CONT_NEXT;
      fk200 = getText(body, "ctx_area_fk200");
      nk200 = getText(body, "ctx_area_nk200");
      kisClient.sleep(100); // 20 transactions per second per account
    }

    if (result == null) {
      return JsonNodes.createEmptyObjectNode();
    }
    result.set("output", outputs);
    result.remove(List.of("ctx_area_fk200", "ctx_area_nk200")); // 병합 결과에서는 의미 없다
    return result;
  }

  private String getText(JsonNode body, String fieldName) {
    JsonNode node = body.get(fieldName);
    return node != null ? node.asText().trim() : "";
  }

  private MultiValueMap<String, String> buildRequestParams(
      String startDate, String endDate, String fk200, String nk200) {
    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("CANO", kisClient.getAccountNo());
    queryParams.add("ACNT_PRDT_CD", kisClient.getAccountProductCode());

    queryParams.add("RSVN_ORD_ORD_DT", startDate);
    queryParams.add("RSVN_ORD_END_DT", endDate);
    queryParams.add("RSVN_ORD_SEQ", "");
    queryParams.add("TMNL_MDIA_KIND_CD", "00");
    queryParams.add("PRCS_DVSN_CD", "0");
    queryParams.add("CNCL_YN", "N");
    queryParams.add("PDNO", "");
    queryParams.add("SLL_BUY_DVSN_CD", "");
    queryParams.add("CTX_AREA_FK200", fk200);
    queryParams.add("CTX_AREA_NK200", nk200);
    return queryParams;
  }
}
