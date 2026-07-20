package blash10x.kis.ota.service;

import blash10x.kis.ota.config.OtaProperties;
import blash10x.kis.ota.external.TradingService;
import blash10x.kis.ota.controller.dto.CreateOrderRequest;
import blash10x.kis.ota.domain.LadderInput;
import blash10x.kis.ota.domain.LadderOrder;
import blash10x.kis.ota.domain.LadderPricer;
import blash10x.kis.ota.domain.LadderWeight;
import blash10x.kis.ota.model.Balance;
import blash10x.kis.ota.model.InterestStock;
import blash10x.kis.ota.model.MarketCode;
import blash10x.kis.ota.model.MarketName;
import blash10x.kis.ota.model.OrderCode;
import blash10x.kis.ota.model.ProductPrice;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 현재가에서 단계별로 벌어지는 지정가 주문(사다리)을 생성한다. 주문 전송 방식만 {@link #submit} 으로 갈라진다.
 *
 * @author myungsik.sung@gmail.com
 */
abstract class LadderOrderService<T> {
  private static final String INTEREST_STOCK_GROUP = "001";

  /** 주문 전송 간격. 실전계좌는 초당 20건(50ms)까지라 여유를 크게 둔다. */
  protected static final long ORDER_INTERVAL_MILLIS = 180;

  /** 모의 주문 간격. 실제 전송이 없으므로 더 짧다. */
  protected static final long MOCK_ORDER_INTERVAL_MILLIS = 100;

  private final Logger logger = LoggerFactory.getLogger(getClass());
  /** 주문 전송에 쓰는 KIS 클라이언트. 하위 서비스의 {@link #submit} 이 사용한다. */
  protected final TradingService tradingService;
  // 활성 가중치 알고리즘은 전역 설정으로 고른다(LadderWeightConfig). 사다리 로그에 가중치를 찍어야 해 파이서와 함께 들고 있다.
  private final LadderWeight ladderWeight;
  private final LadderPricer ladderPricer;

  private final BalanceService balanceService;
  private final RealtimePriceService realtimePriceService;
  private final InterestStocksService interestStocksService;
  private final ExtractionService extractionService;
  private final OtaProperties otaProperties;
  private final PurchasableCashService purchasableCashService;

  protected LadderOrderService(
      TradingService tradingService,
      BalanceService balanceService,
      RealtimePriceService realtimePriceService,
      InterestStocksService interestStocksService,
      ExtractionService extractionService,
      LadderWeight ladderWeight,
      OtaProperties otaProperties,
      PurchasableCashService purchasableCashService) {
    this.tradingService = tradingService;
    this.balanceService = balanceService;
    this.realtimePriceService = realtimePriceService;
    this.interestStocksService = interestStocksService;
    this.extractionService = extractionService;
    this.ladderWeight = ladderWeight;
    this.ladderPricer = new LadderPricer(ladderWeight);
    this.otaProperties = otaProperties;
    this.purchasableCashService = purchasableCashService;
  }

  /**
   * 주문 1건을 전송한다. real 이 false 면 전송하지 않는다.
   *
   * @return 접수 결과. 업무 거절(rt_cd≠0, 미접수 확정)이면 null — 호출자가 예산을 되돌린다.
   *     접수 여부를 알 수 없는 실패는 null 이 아닌 마커를 반환해 예산을 소진된 것으로 남긴다(fail-closed).
   */
  protected abstract T submit(
      String productNo, int orderUnitPrice, OrderCode orderCode, boolean real);

  /**
   * 사다리를 가둘 실행일의 상/하한가.
   *
   * <p>기본은 KIS 가 계산해 준 오늘 한도(stck_mxpr/stck_llam, 전일 종가 기준)로, 오늘 실행되는 주문에 정확하다.
   * 실행일이 오늘이 아닌 주문(예약주문)은 그날의 한도로 재정의한다 — 낡은 오늘 한도를 그대로 쓰면 당일 등락만큼
   * 유효 구간이 잘린다(상한가 마감 시 매도 예약 0건, 하한가 마감 시 매수 예약 0건).
   */
  protected PriceLimits priceLimits(ProductPrice productPrice) {
    return new PriceLimits(
        Integer.parseInt(productPrice.upperPriceLimit()),
        Integer.parseInt(productPrice.lowerPriceLimit()));
  }

  protected record PriceLimits(int upper, int lower) {}

  public List<T> order(CreateOrderRequest request) {
    OrderCode orderCode = request.orderCode();
    List<String> productNos = request.productNos();

    Map<String, Balance> balances = balanceService.getBalances();
    Map<String, InterestStock> interestStocks =
        interestStocksService.getInterestStocks(INTEREST_STOCK_GROUP);

    // 매도는 보유 종목, 매수는 관심 종목이 대상이다. productNos 를 비워 보내면 그 전체가 대상이 된다.
    Set<String> candidates =
        OrderCode.SELL == orderCode ? balances.keySet() : interestStocks.keySet();
    List<String> orderProductNos = productNos.isEmpty()
        ? candidates.stream().toList()
        : productNos.stream().filter(candidates::contains).toList();

    // 지정한 종목이 대상에 없으면 그 종목만 빠진다. 전체로 넓히지 않는다.
    List<String> unknownProductNos =
        productNos.stream().filter(productNo -> !candidates.contains(productNo)).toList();
    if (!unknownProductNos.isEmpty()) {
      logger.warn("not in {}: {}",
          OrderCode.SELL == orderCode ? "balances" : "interestStocks", unknownProductNos);
    }
    logger.info("orderProductNos={}", orderProductNos);

    // 매수 예산: 미수(외상) 매수를 막는 가드. 예약주문도 익영업일에 현금주문으로 전환되므로 같은 예산을 적용한다.
    // 조회에 실패하면 여기서 예외로 멈춘다(fail-closed) — 예산을 모른 채 매수를 내는 것이 곧 미수 위험이다.
    // dry-run(Mock)에도 같은 차감을 적용해, 예산상 몇 단까지 나가는지 실주문 없이 미리 볼 수 있게 한다.
    CashBudget budget = OrderCode.BUY == orderCode && !orderProductNos.isEmpty()
        ? CashBudget.of(purchasableCashService.inquireNoCreditBuyAmount(orderProductNos.getFirst()))
        : CashBudget.unlimited();
    if (OrderCode.BUY == orderCode) {
      logger.info("noCreditBuyAmount={}", String.format("%,d", budget.remaining()));
    }

    // 한 종목이 실패해도 나머지는 진행하고, 이미 전송한 주문은 결과에 남긴다.
    List<T> results = new ArrayList<>();
    for (String productNo : orderProductNos) {
      try {
        orderProduct(productNo, request, balances, interestStocks, budget, results);
      } catch (RuntimeException e) {
        logger.warn("{} skipped", productNo, e);
      }
    }
    return results;
  }

  /**
   * 종목 하나의 사다리를 만들어 전송한다.
   *
   * <p>결과를 반환하지 않고 {@code results} 에 직접 담는다. 지역 리스트에 모아 반환하도록 바꾸면, 중간에 예외가 났을 때
   * 이미 전송된 주문의 기록이 통째로 사라진다.
   */
  private void orderProduct(
      String productNo,
      CreateOrderRequest request,
      Map<String, Balance> balances,
      Map<String, InterestStock> interestStocks,
      CashBudget budget,
      List<T> results) {
    OrderCode orderCode = request.orderCode();
    Map<OrderCode, Integer> maxRepetitions = otaProperties.getMaxRepetitions();
    Map<MarketName, Double> baseRates = otaProperties.getBaseRates().get(orderCode);
    Map<MarketName, Double> stepRates = otaProperties.getStepRates().get(orderCode);
    boolean real = request.real();

    ProductPrice productPrice = realtimePriceService.inquirePrice(MarketCode.J, productNo);
    int realtimePrice = Integer.parseInt(productPrice.presentPrice());
    PriceLimits priceLimits = priceLimits(productPrice);
    double dayOverDayRate = Double.parseDouble(productPrice.dayOverDayRate());
    ExtractionService.StockMetrics metrics = extractionService.extractMetrics(productPrice);
    double _beta = metrics.yearBeta();

    MarketName marketName = MarketName.valueOf(productPrice.marketName());
    InterestStock interestStock = interestStocks.get(productNo);
    Balance balance = balances.get(productNo);

    LadderInput input = LadderInput.builder()
        .orderCode(orderCode)
        .marketName(marketName)
        .realtimePrice(realtimePrice)
        .upperPriceLimit(priceLimits.upper())
        .lowerPriceLimit(priceLimits.lower())
        .dayOverDayRate(dayOverDayRate)
        .yearBeta(_beta)
        .yearHigh(metrics.yearHigh())
        .yearLow(metrics.yearLow())
        .standardPrice(Double.parseDouble(productPrice.standardPrice()))
        .purchaseAvgPrice(parseOrZero(balance, Balance::purchaseAvgPrice))
        .size(getOrderSize(balance, orderCode, maxRepetitions))
        .baseRates(baseRates)
        .stepRates(stepRates)
        .build();

    // 건너뛴 단은 LadderPricer 가 이미 걸러냈으므로, 여기 남은 것은 전부 전송할 주문이다.
    List<LadderOrder> orders = ladderPricer.price(input);
    double weight = ladderWeight.of(input);
    int orderCount = 0;
    for (LadderOrder order : orders) {
      orderCount++;
      // 예산 부족 단은 건너뛴다(skip). 매수 사다리는 아래로 갈수록 싸져, 다음 단은 남은 예산에 들어갈 수 있다.
      if (!budget.tryReserve(order.unitPrice())) {
        logger.info("{} | {} | skipped by budget: price={}, remaining={}",
            String.format("%2d", orderCount), productNo,
            String.format("%,d", order.unitPrice()), String.format("%,d", budget.remaining()));
        continue;
      }
      logger.info(
          "{} | {} | {} ({}) | {} ({}:{}) | {} | {} | {} | {}",
          String.format("%2d", orderCount),
          productNo,
          interestStock != null ? interestStock.htsKoreanName() : balance.productName(),
          marketName,
          orderCode,
          _beta,
          String.format("%4.2f", weight),
          String.format("%,6.2f", input.purchaseAvgPrice()),
          String.format("%,6d", realtimePrice),
          String.format("%6.2f", order.rate()),
          String.format("%,6d", order.unitPrice()));
      T result;
      try {
        result = submit(productNo, order.unitPrice(), orderCode, real);
      } catch (RuntimeException e) {
        // 단 단위 격리: 한 단의 전송 실패가 같은 종목의 남은 단까지 버리게 두지 않는다.
        // 미전송 단이 차지했던 예산은 되돌려, 뒤의 단이 그 몫을 쓸 수 있게 한다.
        budget.release(order.unitPrice());
        logger.warn("{} | {} | rung skipped", String.format("%2d", orderCount), productNo, e);
        continue;
      }
      if (result == null) {
        // 업무 거절은 미접수가 확정이므로 예산만 되돌린다. 거절 사유는 submit 이 이미 로그로 남겼다.
        budget.release(order.unitPrice());
        continue;
      }
      results.add(result);
    }
  }

  /** 보유하지 않은 종목은 balance 가 없다. 매수 후보라도 보유 중이면 balance 가 있다. */
  private static double parseOrZero(Balance balance, Function<Balance, String> field) {
    return balance != null ? Double.parseDouble(field.apply(balance)) : 0.0;
  }

  private static int getOrderSize(
      Balance balance, OrderCode orderCode, Map<OrderCode, Integer> maxRepetitions) {
    if (OrderCode.BUY == orderCode) {
      return maxRepetitions.get(orderCode);
    }
    if (balance == null) {
      return 0;
    }
    int orderPossibleQuantity = Integer.parseInt(balance.orderPossibleQuantity());
    return Math.min(orderPossibleQuantity, maxRepetitions.get(orderCode));
  }
}
