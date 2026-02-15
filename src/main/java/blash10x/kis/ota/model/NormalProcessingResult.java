package blash10x.kis.ota.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author myungsik.sung@gmail.com
 */
public record NormalProcessingResult(
    @JsonProperty("nrml_prcs_yn") // 상품번호
    String value
) {}
