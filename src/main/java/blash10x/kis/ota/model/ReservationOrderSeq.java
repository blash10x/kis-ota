package blash10x.kis.ota.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author myungsik.sung@gmail.com
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReservationOrderSeq(
    @JsonProperty("RSVN_ORD_SEQ") // 예약주문순번
    String value
) {}
