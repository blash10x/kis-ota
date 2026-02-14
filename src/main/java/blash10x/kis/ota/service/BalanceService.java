package blash10x.kis.ota.service;

import blash10x.kis.ota.config.KisProperties;
import blash10x.kis.ota.core.util.JsonNodes;
import blash10x.kis.ota.model.AccessToken;
import blash10x.kis.ota.model.Balance;
import blash10x.kis.ota.model.OrderCode;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
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
public class BalanceService extends TradingService {
  private static final Logger LOGGER = LoggerFactory.getLogger(BalanceService.class);
  private static final String PATH = "/uapi/domestic-stock/v1/trading/inquire-balance";
  private static final String TR_ID = "TTTC8434R";

  private final ExternalService externalService;
  private final String accountNo;
  private final String accountProductCode;
  private AccessToken accessToken;
  private MultiValueMap<String, String> headers;

  public BalanceService(KisProperties kisProperties, KisAuthService kisAuthService) {
    super(kisProperties, kisAuthService);

    String host = kisProperties.getHost();
    externalService = new ExternalService(host);
    accountNo = kisProperties.getAccountNo();
    accountProductCode = kisProperties.getAccountProductCode();
  }

  public List<Balance> inquireBalance() {
    if (accessToken == null) {
      accessToken = getKisAuthService().authorize();
      headers = buildRequestHeaders(accessToken, TR_ID);
    }

    MultiValueMap<String, String> queryParams = buildRequestParams();
    Mono<ResponseEntity<JsonNode>> mono =
        externalService.get(PATH, headers, queryParams, JsonNode.class);

    JsonNode jsonNode = mono.mapNotNull(HttpEntity::getBody).block();
    Response response = JsonNodes.toValue(jsonNode, Response.class);
    response.output1.forEach( balance -> {
      double price = Double.parseDouble(balance.presentPrice()) * (1.0 - 0.024);
      System.out.println(balance.toString() + ", " + calculateOrderPrice(price, OrderCode.BUY));

    });

    return response.output1;
  }

  private MultiValueMap<String, String> buildRequestParams() {
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
    return queryParams;
  }

  private record Response(List<Balance> output1) {}
}
