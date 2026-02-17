package blash10x.kis.ota.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * @author myungsik.sung@gmail.com
 */
public record InterestStockGroup(
    @Schema(description = "일자") @JsonProperty("date") String date,
    @Schema(description = "전송 시간") @JsonProperty("trnm_hour") String transactionTime,
    @Schema(description = "데이터 순위") @JsonProperty("data_rank") String dataRank,
    @Schema(description = "관심 그룹 코드") @JsonProperty("inter_grp_code") String interestGroupCode,
    @Schema(description = "관심 그룹 명") @JsonProperty("inter_grp_name") String interestGroupName,
    @Schema(description = "요청 개수") @JsonProperty("ask_cnt") String askCount) {}
