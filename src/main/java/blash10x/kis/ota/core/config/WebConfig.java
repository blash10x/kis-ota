package blash10x.kis.ota.core.config;

import blash10x.kis.ota.core.annotation.RequestParamObjectMethodArgumentResolver;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author myungsik.sung@gmail.com
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

  @Autowired
  private ConfigurableBeanFactory beanFactory;

  @Override
  public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
    resolvers.add(new RequestParamObjectMethodArgumentResolver(beanFactory));
  }
}
