package blash10x.kis.ota.config;

import blash10x.kis.ota.model.MarketName;
import blash10x.kis.ota.model.OrderCode;
import blash10x.kis.ota.model.Product;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author myungsik.sung@gmail.com
 */
@Configuration
@ConfigurationProperties(prefix = "ota")
@Data
public class OtaProperties {
  private Map<OrderCode, Integer> maxRepetitions;
  private Map<MarketName, Double> baseRates;
  private Map<MarketName, Double> stepRates;
  private Map<OrderCode, Double> multipleRates;
  private Map<String, Product> products;
}
