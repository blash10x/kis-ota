package blash10x.kis.ota.core.service;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public class ServiceException extends RuntimeException {
  private final HttpStatus httpStatus;

  public ServiceException(String message) {
    this(HttpStatus.INTERNAL_SERVER_ERROR, message);
  }

  public ServiceException(HttpStatusCode httpStatusCode, String message) {
    super(message);
    this.httpStatus = HttpStatus.valueOf(httpStatusCode.value());
  }
}