package blash10x.kis.ota.core.service;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * @author Myungsik Sung (myungsik.sung@nol-universe.com)
 */
@Getter
public class ClientException extends ServiceException {
  private final HttpStatus httpStatus;

  public ClientException(String message) {
    this(HttpStatus.BAD_REQUEST, message);
  }

  public ClientException(HttpStatusCode httpStatusCode, String message) {
    super(message);
    this.httpStatus = HttpStatus.valueOf(httpStatusCode.value());
  }
}