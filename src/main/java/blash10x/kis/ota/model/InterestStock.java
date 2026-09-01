package blash10x.kis.ota.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

public record InterestStock(
    @Schema(description = "FID 시장 구분 코드") @JsonProperty("fid_mrkt_cls_code") String marketClassCode,
    @Schema(description = "데이터 순위") @JsonProperty("data_rank") String dataRank,
    @Schema(description = "거래소코드") @JsonProperty("exch_code") String exchangeCode,
    @Schema(description = "종목코드") @JsonProperty("jong_code") String stockCode,
    @Schema(description = "생상 코드") @JsonProperty("color_code") String colorCode, // -
    @Schema(description = "메모") @JsonProperty("memo") String memo,
    @Schema(description = "HTS 한글 종목명") @JsonProperty("hts_kor_isnm") String htsKoreanName,
    @Schema(description = "기준일 순매수 수량") @JsonProperty("fxdt_ntby_qty") String fixedNetBuyQty,
    @Schema(description = "체결단가") @JsonProperty("cntg_unpr") String contractUnitPrice,
    @Schema(description = "체결 구분 코드") @JsonProperty("cntg_cls_code") String contractClassCode) {}
