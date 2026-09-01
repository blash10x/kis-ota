package blash10x.kis.ota.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NormalProcessingResult(
    @JsonProperty("nrml_prcs_yn") // 상품번호
    String value
) {}
