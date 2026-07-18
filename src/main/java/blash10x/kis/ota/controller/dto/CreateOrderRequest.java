package blash10x.kis.ota.controller.dto;

import blash10x.kis.ota.model.OrderCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 주문 대상만 받는다. 사다리 파라미터(maxRepetitions, baseRates, stepRates)는 전역 설정(ota.yaml)으로 옮겼다.
 *
 * @author myungsik.sung@gmail.com
 */
public record CreateOrderRequest(
    @NotNull OrderCode orderCode,
    @NotNull List<String> productNos,
    @Schema(defaultValue = "false") @NotNull Boolean real) {}
