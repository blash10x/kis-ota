package blash10x.kis.ota.service;

import blash10x.kis.ota.external.TradingService;
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
public class ReservationOrderCancelService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ReservationOrderCancelService.class);
  private static final String PATH = "/uapi/domestic-stock/v1/trading/order-resv-rvsecncl";
  private static final String TR_ID = "CTSC0009U";

  private final TradingService tradingService;

  public ReservationOrderCancelService(TradingService tradingService) {
    this.tradingService = tradingService;
  }

  public NormalProcessingResult cancelReservationOrder(String reservationOrderSeq) {
    MultiValueMap<String, String> headers = tradingService.buildRequestHeaders(TR_ID);
    Request request = buildRequest(reservationOrderSeq);
    Response response = tradingService.post(PATH, headers, null, request, Response.class).block();
    return response != null ? response.output : new NormalProcessingResult("Unknown");
  }

  private Request buildRequest(String reservationOrderSeq) {
    return Request.builder()
        .accountNo(tradingService.getAccountNo())
        .accountProductCode(tradingService.getAccountProductCode())
        .reservationOrderSeq(reservationOrderSeq)
        .build();
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
