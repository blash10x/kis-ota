package blash10x.kis.ota.controller.dto;

import blash10x.kis.ota.model.OrderCode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * @author myungsik.sung@gmail.com
 */
public record CreateReservationOrderRequest(
    @NotNull OrderCode orderCode,
    List<String> productNos,
    @Schema(defaultValue = "false") @NotNull Boolean real) {}
