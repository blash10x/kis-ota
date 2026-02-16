package blash10x.kis.ota.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author myungsik.sung@gmail.com
 */
public record OrderResult(
    @JsonProperty("KRX_FWDG_ORD_ORGNO") // 거래소코드
    String exchangeOrganizationCode,
    @JsonProperty("ODNO") // 주문번호
    String orderNo,
    @JsonProperty("ORD_TMD") // 주문시간
    String orderTime
) {}
