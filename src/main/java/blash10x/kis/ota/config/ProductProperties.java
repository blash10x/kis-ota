package blash10x.kis.ota.config;

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
public class ProductProperties {
  private int repetitions;
  private double baseRate;
  private double applyRate;
  private Map<String, Product> products;
}
