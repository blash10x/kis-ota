package blash10x.kis.ota.controller.dto;

import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author myungsik.sung@gmail.com
 */
public record DatePeriodParamRequest(
    @RequestParam(required = false) String startDate,
    @RequestParam(required = false) String endDate) {}
