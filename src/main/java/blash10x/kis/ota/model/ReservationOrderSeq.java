package blash10x.kis.ota.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author myungsik.sung@gmail.com
 */
public record ReservationOrderSeq(
    @JsonProperty("RSVN_ORD_SEQ") // 상품번호
    String value
) {}
