package blash10x.kis.ota.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "kis")
@Data
public class KisProperties {
  private String configRoot;
  private String host;
  private String appKey;
  private String appSecret;
  private String accountNo;
  private String accountProductCode;
  private String userId;
}
