package blash10x.kis.ota.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author myungsik.sung@gmail.com
 */
public record ProductPrice(
    @Schema(description = "주식 현재가") @JsonProperty("stck_prpr") String presentPrice,
    @Schema(description = "전일 대비") @JsonProperty("prdy_vrss") String dayOverDayPrice,
    @Schema(description = "전일 대비율") @JsonProperty("prdy_ctrt") String dayOverDayRate,
    @Schema(description = "주식 기준가") @JsonProperty("stck_sdpr") String standardPrice) {}
