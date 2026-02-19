package blash10x.kis.ota.service;

import blash10x.kis.ota.config.OtaProperties;
import blash10x.kis.ota.core.util.JsonNodes;
import blash10x.kis.ota.model.MarketCode;
import blash10x.kis.ota.model.MarketName;
import blash10x.kis.ota.model.ProductPrice;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Collections;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Data;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author myungsik.sung@gmail.com
 */
@Service
@Data
public class ExtractionService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExtractionService.class);
  private static final Pattern PATTERN =
      Pattern.compile("var\\s+status_data\\s*=\\s*(\\{.*?});", Pattern.DOTALL);
  private final OtaProperties otaProperties;
  private final RealtimePriceService realtimePriceService;

  public double extractYearBeta(String stockCode) {
    String beta = extract(stockCode).getOrDefault("YR_BETA", "1.0");
    return Double.parseDouble(beta);
  }

  public Map<String, String> extract(String stockCode) {
    ProductPrice productPrice = realtimePriceService.inquirePrice(MarketCode.J, stockCode);
    MarketName marketName = MarketName.valueOf(productPrice.marketName());
    String url = otaProperties.getExtractionUrls().get(marketName) + productPrice.shortCode();
    try {
      Document doc = Jsoup.connect(url).get();
      return marketName == MarketName.KOSPI200 ? extractFromKOSPI200(doc) : extractFromETF(doc);
    } catch (Exception e) {
      LOGGER.warn(e.getMessage(), e);
    }
    return Collections.emptyMap();
  }

  private Map<String, String> extractFromKOSPI200(Document doc) {
    Elements labels = doc.getElementsContainingOwnText("52주베타");
    if (!labels.isEmpty()) {
      Element betaValueElement = labels.first().nextElementSibling();
      if (betaValueElement != null) {
        String betaValue = betaValueElement.text().trim();
        return Collections.singletonMap("YR_BETA", betaValue);
      }
    }
    return Collections.emptyMap();
  }

  private Map<String, String> extractFromETF(Document doc) {
    Elements scripts = doc.select("script");
    for (Element script : scripts) {
      String scriptContent = script.data();
      if (scriptContent.contains("status_data")) {
        Matcher matcher = PATTERN.matcher(scriptContent);
        if (matcher.find()) {
          String statusDataJson = matcher.group(1);
          return JsonNodes.toValue(statusDataJson, new TypeReference<>() {});
        }
      }
    }
    return Collections.emptyMap();
  }
}
