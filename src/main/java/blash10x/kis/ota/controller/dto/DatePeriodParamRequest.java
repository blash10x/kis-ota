package blash10x.kis.ota.controller.dto;

import org.springframework.web.bind.annotation.RequestParam;

public record DatePeriodParamRequest(
    @RequestParam(required = false) String startDate,
    @RequestParam(required = false) String endDate) {}
