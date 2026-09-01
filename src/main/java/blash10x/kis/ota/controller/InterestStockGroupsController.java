package blash10x.kis.ota.controller;

import blash10x.kis.ota.model.InterestStockGroup;
import blash10x.kis.ota.service.InterestStockGroupsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Trading API", description = "API 명세")
@RestController
@RequestMapping(value = "/quotations", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class InterestStockGroupsController {
  private final InterestStockGroupsService service;

  @Operation(summary = "관심종목 그룹조회")
  @GetMapping(value = "/intstock-grouplist")
  public List<InterestStockGroup> inquireInterestStockGroups() {
    return service.inquireInterestStockGroups();
  }
}
