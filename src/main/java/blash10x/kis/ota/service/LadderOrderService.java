package blash10x.kis.ota.service;

import blash10x.kis.ota.config.KisProperties;
import blash10x.kis.ota.controller.dto.CreateOrderRequest;
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

  /**
   * rate 자체의 상한. 가격제한폭 적용 종목에서는 상/하한가 가드가 항상 먼저 걸리므로 발동하지 않는 백스톱이다.
   *
   * <p>KIS 문서상 정리매매종목·ELW·신주인수권은 가격제한폭이 적용되지 않는다(주식예약주문 유의사항). 그런 종목에서
   * stck_mxpr/stck_llam 이 어떤 값으로 오는지는 확인하지 못했으므로, 상/하한가 가드를 신뢰할 수 없는 경우를 대비해 남겨 둔다.
   */
  private static final double RATE_CAP = 29.985;

  private final Logger logger = LoggerFactory.getLogger(getClass());
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
    double _beta = extractionService.extractYearBeta(productNo);

    MarketName marketName = MarketName.valueOf(productPrice.marketName());
    InterestStock interestStock = interestStocks.get(productNo);
    Balance balance = balances.get(productNo);
    double purchaseAvgPrice = balance != null ? Double.parseDouble(balance.purchaseAvgPrice()) : 0.0;
    double beta = Math.log(_beta + 0.75) + 1;
    int size = getOrderSize(balance, orderCode, maxRepetitions);
    for (int i = 1; i <= size; i++) {
      double rate = calculateRate(i, beta, productPrice, baseRates, stepRates);
      if (OrderCode.SELL == orderCode && dayOverDayRate < 0.0) {
        rate += Math.abs(dayOverDayRate) * 0.20;
      }

      if (rate > RATE_CAP) {
        break;
      }

      if (OrderCode.SELL == orderCode
          && rate < 5.0 * beta
          && Double.parseDouble(balance.evaluationProfitLossRatio()) + rate < 0.5) {
        continue;
      }

      int direction = OrderCode.SELL == orderCode ? 1 : -1;
      double orderUnitPrice = realtimePrice * (100 + direction * rate) / 100;
      double gain = orderUnitPrice - purchaseAvgPrice * 1.01;
      if (OrderCode.SELL == orderCode && gain < 0) {
        orderUnitPrice -= gain;
      }

      int tickPrice = calculateTickPrice(orderUnitPrice, marketName, orderCode);

      // 매도는 i 가 커질수록 주문가가 오르고 매수는 내리므로, 한쪽을 벗어나면 이후도 전부 벗어난다.
      if (tickPrice > upperPriceLimit || tickPrice < lowerPriceLimit) {
        break;
      }

      logger.info(
          "{} | {} | {} ({}) | {} ({}:{}) | {} | {} | {} | {}",
          String.format("%2d", i),
          productNo,
          interestStock != null ? interestStock.htsKoreanName() : balance.productName(),
          marketName,
          orderCode,
          _beta,
          String.format("%4.2f", beta),
          String.format("%,6.2f", purchaseAvgPrice),
          String.format("%,6d", realtimePrice),
          String.format("%6.2f", direction * rate),
          String.format("%,6d", tickPrice));
      T result = submit(productNo, tickPrice, orderCode, real);
      results.add(result);
    }
  }
}
