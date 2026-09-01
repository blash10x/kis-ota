package blash10x.kis.ota.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public record ProductPrice(
    @Schema(description = "대표 시장 한글 명") @JsonProperty("rprs_mrkt_kor_name") String marketName,
    @Schema(description = "업종 한글 종목명") @JsonProperty("bstp_kor_isnm") String businessTypeName,
    @Schema(description = "주식 현재가") @JsonProperty("stck_prpr") String presentPrice,
    @Schema(description = "전일 대비") @JsonProperty("prdy_vrss") String dayOverDayPrice,
    @Schema(description = "전일 대비율") @JsonProperty("prdy_ctrt") String dayOverDayRate,
    @Schema(description = "주식 상한가") @JsonProperty("stck_mxpr") String upperPriceLimit,
    @Schema(description = "주식 하한가") @JsonProperty("stck_llam") String lowerPriceLimit,
    @Schema(description = "주식 기준가") @JsonProperty("stck_sdpr") String standardPrice,
    @Schema(description = "가중 평균 주식 가격") @JsonProperty("wghn_avrg_stck_prc")
        String weightedAvgPrice,
    @Schema(description = "피벗 2차 디저항 가격") @JsonProperty("pvt_scnd_dmrs_prc")
        String pvt2ndDeResPrice,
    @Schema(description = "피벗 1차 디저항 가격") @JsonProperty("pvt_frst_dmrs_prc")
        String pvt1stDeResPrice,
    @Schema(description = "피벗 포인트 값") @JsonProperty("pvt_pont_val") String pvtPointValue,
    @Schema(description = "피벗 1차 디지지 가격") @JsonProperty("pvt_frst_dmsp_prc")
        String pvt1stDeSupPrice,
    @Schema(description = "피벗 2차 디지지 가격") @JsonProperty("pvt_scnd_dmsp_prc")
        String pvt2ndDeSupPrice,
    @Schema(description = "디저항 값") @JsonProperty("dmrs_val") String deResPrice,
    @Schema(description = "디지지 값") @JsonProperty("dmsp_val") String deSupPrice,
    @Schema(description = "주식 단축 종목코드") @JsonProperty("stck_shrn_iscd") String shortCode) {}
