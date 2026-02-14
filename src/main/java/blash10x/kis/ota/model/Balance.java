package blash10x.kis.ota.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Myungsik Sung (myungsik.sung@nol-universe.com)
 */
public record Balance(
    @JsonProperty("pdno") // 상품번호
    String productNo,
    @JsonProperty("prdt_name") // 상품명
    String productName,
    @JsonProperty("hldg_qty") // 보유수량
    String holdingQuantity,
    @JsonProperty("ord_psbl_qty") // 보유수량
    String orderPossibleQuantity,
    @JsonProperty("pchs_avg_pric") // 매입평균가격
    String purchaseAvgPrice,
    @JsonProperty("prpr") // 현재가
    String presentPrice,
    @JsonProperty("evlu_pfls_rt") // 평가손익율
    String evaluationProfitLossRatio
) {}
