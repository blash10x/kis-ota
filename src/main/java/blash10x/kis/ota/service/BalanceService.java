package blash10x.kis.ota.service;

import blash10x.kis.ota.config.KisProperties;
import blash10x.kis.ota.model.AccessToken;
import blash10x.kis.ota.core.util.JsonNodes;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * @author myungsik.sung@gmail.com
 */
@Service
@RequiredArgsConstructor
public class BalanceService {
  private static final Logger LOGGER = LoggerFactory.getLogger(BalanceService.class);
  private static final String PATH = "/uapi/domestic-stock/v1/trading/inquire-balance";
  private static final String TR_ID = "TTTC8434R";
  private final KisProperties kisProperties;
  private final KisAuthService kisAuthService;

  public String inquireBalance() {
    AccessToken accessToken = kisAuthService.authorize();

    String host = kisProperties.getHost();
    String authorization = accessToken.accessToken();
    String tokenType = accessToken.tokenType();
    String appKey = kisProperties.getAppKey();
    String appSecret = kisProperties.getAppSecret();
    String accountNo = kisProperties.getAccountNo();
    String accountProductCode = kisProperties.getAccountProductCode();

    WebClient webClient = WebClient.builder()
        .baseUrl(host)
        .defaultHeader(HttpHeaders.AUTHORIZATION, tokenType + " " + authorization)
        .build();

    MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    headers.set("appkey", appKey);
    headers.set("appsecret", appSecret);
    headers.set("tr_id", TR_ID);

    MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
    queryParams.add("CANO", accountNo);
    queryParams.add("ACNT_PRDT_CD", accountProductCode);

    queryParams.add("AFHR_FLPR_YN", "N");
    queryParams.add("OFL_YN", "");
    queryParams.add("INQR_DVSN", "01");
    queryParams.add("UNPR_DVSN", "01");
    queryParams.add("FUND_STTL_ICLD_YN", "N");
    queryParams.add("FNCG_AMT_AUTO_RDPT_YN", "N");
    queryParams.add("PRCS_DVSN", "00");
    queryParams.add("CTX_AREA_FK100", "");
    queryParams.add("CTX_AREA_NK100", "");

    Mono<ResponseEntity<JsonNode>> mono = webClient.get()
        .uri(uriBuilder -> uriBuilder
            .path(PATH)
            .queryParams(queryParams).build())
        .headers(httpHeaders -> httpHeaders.putAll(headers))
        .retrieve()
        .toEntity(JsonNode.class);
    JsonNode jsonNode = mono.mapNotNull(HttpEntity::getBody).block();
    Response response = JsonNodes.toValue(jsonNode, Response.class);
    response.output1.forEach( balance -> {
      double price = Double.parseDouble(balance.presentPrice) * (1.0 - 0.024);
      System.out.println(balance.toString() + ", " + Math.floor(price));

    });

    return jsonNode.toPrettyString();
  }

  private record Response(List<Balance> output1) {}

  private record Balance(
      @JsonProperty("pdno") // 상품번호
      String productNo,
      @JsonProperty("prdt_name") // 상품명
      String productName,
      @JsonProperty("hldg_qty") // 보유수량
      String holdingQuantity,
      @JsonProperty("pchs_avg_pric") // 매입평균가격
      String purchaseAvgPrice,
      @JsonProperty("prpr") // 현재가
      String presentPrice,
      @JsonProperty("evlu_pfls_rt") // 평가손익율
      String evaluationProfitLossRatio
  ) {}
}
