package blash10x.kis.ota.core.annotation;

import blash10x.kis.ota.core.util.Reflections;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ValueConstants;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.RequestParamMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class RequestParamObjectMethodArgumentResolver implements HandlerMethodArgumentResolver {
  private final RequestParamFieldResolver requestParamResolver;

  public RequestParamObjectMethodArgumentResolver(@Nullable ConfigurableBeanFactory beanFactory) {
    requestParamResolver = new RequestParamFieldResolver(beanFactory);
  }

  @Override
  public boolean supportsParameter(MethodParameter parameter) {
    return parameter.getParameterAnnotation(RequestParamObject.class) != null;
  }

  @Override
  @SuppressWarnings("ConstantConditions")
  public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
      NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
    Class<?> clazz = parameter.getParameterType();
    Field[] fields = clazz.getDeclaredFields();
    Class<?>[] parameterTypes = Arrays.stream(fields).map(Field::getType).toArray(Class[]::new);
    Constructor<?> constructor = getDeclaredConstructor(clazz, parameterTypes);
    if (constructor.getParameters().length != fields.length) {
      throw new IllegalArgumentException("No all-args constructor found");
    }

    Object[] objects = new Object[fields.length];
    for (int i = 0; i < fields.length; i++) {
      MethodParameter methodParameter = new MethodParameter(constructor, i);
      Object object = requestParamResolver.resolveArgument(methodParameter, mavContainer, webRequest, binderFactory);
      objects[i] = object;
    }
    return constructor.newInstance(objects);
  }

  private Constructor<?> getDeclaredConstructor(Class<?> clazz, Class<?>... parameterTypes)
      throws NoSuchMethodException {
    try {
      return clazz.getDeclaredConstructor(parameterTypes);
    } catch (NoSuchMethodException e) {
      return clazz.getDeclaredConstructor();
    }
  }

  private static class RequestParamFieldResolver extends RequestParamMethodArgumentResolver {

    private RequestParamFieldResolver(@Nullable ConfigurableBeanFactory beanFactory) {
      super(beanFactory, true);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
      return parameter.hasMethodAnnotation(Parameter.class)
          || parameter.hasParameterAnnotation(RequestParam.class);
    }

    @Override
    protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
      RequestParam requestParamAnn = parameter.getParameterAnnotation(RequestParam.class);
      if (requestParamAnn != null) {
        String name = getName(requestParamAnn, parameter);
        return new RequestParamNamedValueInfo(name, requestParamAnn);
      }

      Parameter parameterAnn = parameter.getParameterAnnotation(Parameter.class);
      if (parameterAnn != null) {
        String name = getName(parameterAnn, parameter);
        return new RequestParamNamedValueInfo(name, parameterAnn);
      }

      Class<?> declaringClass = parameter.getDeclaringClass();
      Field declaredField = declaringClass.getDeclaredFields()[parameter.getParameterIndex()];
      parameterAnn = declaredField.getAnnotation(Parameter.class);
      Assert.state(parameterAnn != null && parameterAnn.in() == ParameterIn.QUERY,
          "No Parameter(in = ParameterIn.QUERY) or RequestParam annotation: " + parameter.getParameter());
      String name = getName(parameterAnn, parameter);
      return new RequestParamNamedValueInfo(name, parameterAnn);
    }

    private String getName(RequestParam requestParamAnn, MethodParameter parameter) {
      return requestParamAnn.name().isEmpty() && requestParamAnn.value().isEmpty()
          ? parameter.getParameter().getName() : requestParamAnn.name();
    }

    private String getName(Parameter parameterAnn, MethodParameter parameter) {
      return parameterAnn.name().isEmpty()
          ? parameter.getParameter().getName() : parameterAnn.name();
    }

    private static final class RequestParamNamedValueInfo extends NamedValueInfo {

      private RequestParamNamedValueInfo(String name, RequestParam annotation) {
        super(name, annotation.required(), annotation.defaultValue());
      }

      private RequestParamNamedValueInfo(String name, Parameter annotation) {
        super(name, annotation.required(), null);
        Reflections.setFieldValue("defaultValue", this, defaultValue(annotation));
      }

      private String defaultValue(Parameter annotation) {
        Schema schema = annotation.schema();
        if (schema == null) {
          return null;
        }
        return schema.defaultValue().isBlank()
            ? ValueConstants.DEFAULT_NONE : schema.defaultValue().trim();
      }
    }
  }
}
