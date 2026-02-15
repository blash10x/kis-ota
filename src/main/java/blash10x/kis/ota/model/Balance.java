package blash10x.kis.ota.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author myungsik.sung@gmail.com
 */
public record Balance(
    @Schema(description = "상품번호")
    @JsonProperty("pdno")
    String productNo,
    @Schema(description = "상품명")
    @JsonProperty("prdt_name")
    String productName,
    @Schema(description = "보유수량")
    @JsonProperty("hldg_qty")
    String holdingQuantity,
    @Schema(description = "주문가능수량")
    @JsonProperty("ord_psbl_qty")
    String orderPossibleQuantity,
    @Schema(description = "매입평균가격")
    @JsonProperty("pchs_avg_pric")
    String purchaseAvgPrice,
    @Schema(description = "현재가")
    @JsonProperty("prpr")
    String presentPrice,
    @Schema(description = "평가손익율")
    @JsonProperty("evlu_pfls_rt")
    String evaluationProfitLossRatio
) {}
