package blash10x.kis.ota.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ReservationOrderSeq(
    @JsonProperty("RSVN_ORD_SEQ") // 예약주문순번
    String value
) {}
