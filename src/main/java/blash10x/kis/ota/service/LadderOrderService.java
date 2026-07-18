package blash10x.kis.ota.service;

import blash10x.kis.ota.config.KisProperties;
import blash10x.kis.ota.controller.dto.CreateOrderRequest;
import blash10x.kis.ota.domain.BetaWeight;
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
abstract class LadderOrderService<T> extends TradingService {
  private static final String INTEREST_STOCK_GROUP = "001";

  /** 주문 전송 간격. 실전계좌는 초당 20건(50ms)까지라 여유를 크게 둔다. */
  protected static final long ORDER_INTERVAL_MILLIS = 180;

  /** 모의 주문 간격. 실제 전송이 없으므로 더 짧다. */
  protected static final long MOCK_ORDER_INTERVAL_MILLIS = 100;

  private final Logger logger = LoggerFactory.getLogger(getClass());
  // TODO: 가중치 알고리즘을 전역 설정으로 선택하게 되면 주입으로 올린다. 지금은 베타로 고정.
  private final LadderWeight ladderWeight = new BetaWeight();
  private final LadderPricer ladderPricer = new LadderPricer(ladderWeight);

  private final BalanceService balanceService;
  private final RealtimePriceService realtimePriceService;
  private final InterestStocksService interestStocksService;
  private final ExtractionService extractionService;

  protected LadderOrderService(
      KisProperties kisProperties,
      KisAuthService kisAuthService,
      BalanceService balanceService,
      RealtimePriceService realtimePriceService,
      InterestStocksService interestStocksService,
      ExtractionService extractionService) {
    super(kisProperties, kisAuthService);
    this.balanceService = balanceService;
    this.realtimePriceService = realtimePriceService;
    this.interestStocksService = interestStocksService;
    this.extractionService = extractionService;
  }

  /** 주문 1건을 전송한다. real 이 false 면 전송하지 않는다. */
  protected abstract T submit(
      String productNo, int orderUnitPrice, OrderCode orderCode, boolean real);

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

    // 한 종목이 실패해도 나머지는 진행하고, 이미 전송한 주문은 결과에 남긴다.
    List<T> results = new ArrayList<>();
    for (String productNo : orderProductNos) {
      try {
        orderProduct(productNo, request, balances, interestStocks, results);
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
      List<T> results) {
    OrderCode orderCode = request.orderCode();
    Map<OrderCode, Integer> maxRepetitions = request.maxRepetitions();
    Map<MarketName, Double> baseRates = request.baseRates().get(orderCode);
    Map<MarketName, Double> stepRates = request.stepRates().get(orderCode);
    boolean real = request.real();

    ProductPrice productPrice = realtimePriceService.inquirePrice(MarketCode.J, productNo);
    int realtimePrice = Integer.parseInt(productPrice.presentPrice());
    // 상/하한가는 전일 종가(기준가) 기준이라 현재가로 환산할 수 없다. KIS 가 계산해 준 값을 그대로 쓴다.
    int upperPriceLimit = Integer.parseInt(productPrice.upperPriceLimit());
    int lowerPriceLimit = Integer.parseInt(productPrice.lowerPriceLimit());
    double dayOverDayRate = Double.parseDouble(productPrice.dayOverDayRate());
    ExtractionService.StockMetrics metrics = extractionService.extractMetrics(productNo);
    double _beta = metrics.yearBeta();

    MarketName marketName = MarketName.valueOf(productPrice.marketName());
    InterestStock interestStock = interestStocks.get(productNo);
    Balance balance = balances.get(productNo);

    LadderInput input = LadderInput.builder()
        .orderCode(orderCode)
        .marketName(marketName)
        .realtimePrice(realtimePrice)
        .upperPriceLimit(upperPriceLimit)
        .lowerPriceLimit(lowerPriceLimit)
        .dayOverDayRate(dayOverDayRate)
        .yearBeta(_beta)
        .yearHigh(metrics.yearHigh())
        .yearLow(metrics.yearLow())
        .purchaseAvgPrice(parseOrZero(balance, Balance::purchaseAvgPrice))
        // 매도에서만 읽는 값이다. 매수에서도 파싱하면 KIS 가 빈 값을 줬을 때 낼 이유가 없는 예외를 낸다.
        .evaluationProfitLossRatio(OrderCode.SELL == orderCode
            ? parseOrZero(balance, Balance::evaluationProfitLossRatio)
            : 0.0)
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
      T result = submit(productNo, order.unitPrice(), orderCode, real);
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
