package blash10x.kis.ota.service;

import blash10x.kis.ota.config.KisProperties;
import blash10x.kis.ota.model.NormalProcessingResult;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

/**
 * @author myungsik.sung@gmail.com
 */
@Service
public class ReservationOrderCancelService extends TradingService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ReservationOrderCancelService.class);
  private static final String PATH = "/uapi/domestic-stock/v1/trading/order-resv-rvsecncl";
  private static final String TR_ID = "CTSC0009U";

  private MultiValueMap<String, String> headers;

  public ReservationOrderCancelService(KisProperties kisProperties, KisAuthService kisAuthService) {
    super(kisProperties, kisAuthService);
  }

  public NormalProcessingResult cancelReservationOrder(String reservationOrderSeq) {
    if (headers == null) {
      headers = buildRequestHeaders(TR_ID);
    }

    Request request = Request.builder()
        .accountNo(getAccountNo())
        .accountProductCode(getAccountProductCode())
        .reservationOrderSeq(reservationOrderSeq)
        .build();

    Response response = post(PATH, headers, null, request, Response.class).block();
    return response != null ? response.output : new NormalProcessingResult("Unknown");
  }

  @Builder
  private record Request(
      @JsonProperty("CANO")
      String accountNo,
      @JsonProperty("ACNT_PRDT_CD")
      String accountProductCode,
      @JsonProperty("RSVN_ORD_SEQ") // 예약주문순번
      String reservationOrderSeq) {}

  private record Response(
      @JsonProperty("rt_cd") // 성공 실패 여부
      String rt_cd,
      @JsonProperty("msg_cd") // 응답코드
      String msg_cd,
      @JsonProperty("msg1") // 응답메세지
      String msg,
      @JsonProperty("output")
      NormalProcessingResult output) {}
}
