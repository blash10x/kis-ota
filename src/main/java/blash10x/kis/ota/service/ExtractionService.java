package blash10x.kis.ota.service;

import blash10x.kis.ota.config.OtaProperties;
import blash10x.kis.ota.core.util.JsonNodes;
import blash10x.kis.ota.model.MarketName;
import blash10x.kis.ota.model.ProductPrice;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Collections;
import java.util.HashMap;
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

@Service
@Data
public class ExtractionService {
  private static final Logger LOGGER = LoggerFactory.getLogger(ExtractionService.class);
  private static final Pattern PATTERN =
      Pattern.compile("var\\s+status_data\\s*=\\s*(\\{.*?});", Pattern.DOTALL);
  private final OtaProperties otaProperties;

  /**
   * 사다리 가중치 계산에 쓰는 종목 지표를 한 번의 스크래핑으로 모아 온다.
   *
   * @param yearBeta 52주베타. 없으면 1.0 (중립)
   * @param yearHigh 52주 최고가. 없으면 0.0
   * @param yearLow 52주 최저가. 없으면 0.0
   */
  public record StockMetrics(double yearBeta, double yearHigh, double yearLow) {}

  public StockMetrics extractMetrics(ProductPrice productPrice) {
    Map<String, String> data = extract(productPrice);
    return new StockMetrics(
        parseNumber(data.get("YR_BETA"), 1.0),
        parseNumber(data.get("YR_HIGH"), 0.0),
        parseNumber(data.get("YR_LOW"), 0.0));
  }

  /** "374,500원", "87,946", "1.19" 처럼 콤마·단위가 섞인 문자열에서 숫자만 뽑는다. 못 뽑으면 fallback. */
  static double parseNumber(String raw, double fallback) {
    if (raw == null) {
      return fallback;
    }
    String digits = raw.replaceAll("[^0-9.]", "");
    try {
      return digits.isEmpty() ? fallback : Double.parseDouble(digits);
    } catch (NumberFormatException e) {
      // "." 나 "1.2.3" 처럼 숫자·점만 남았지만 파싱 불가능한 경우. 그 종목만 건너뛰지 않도록 중립값으로 떨어진다.
      return fallback;
    }
  }

  public Map<String, String> extract(ProductPrice productPrice) {
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
    Map<String, String> data = new HashMap<>();
    putSiblingText(doc, "52주베타", data, "YR_BETA");

    // "52Weeks 최고/최저" 는 "374,500원 / 64,400원" 처럼 한 칸에 최고·최저가 슬래시로 붙어 온다.
    String highLow = siblingText(doc, "52Weeks 최고/최저");
    if (highLow != null) {
      String[] parts = highLow.split("/");
      if (parts.length == 2) {
        data.put("YR_HIGH", parts[0]);
        data.put("YR_LOW", parts[1]);
      }
    }
    return data;
  }

  private static void putSiblingText(Document doc, String label, Map<String, String> data, String key) {
    String value = siblingText(doc, label);
    if (value != null) {
      data.put(key, value);
    }
  }

  /** 라벨 요소 바로 다음 형제(값 칸)의 텍스트. 라벨이나 값이 없으면 null. */
  private static String siblingText(Document doc, String label) {
    Elements labels = doc.getElementsContainingOwnText(label);
    if (labels.isEmpty()) {
      return null;
    }
    Element sibling = labels.first().nextElementSibling();
    return sibling != null ? sibling.text().trim() : null;
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
