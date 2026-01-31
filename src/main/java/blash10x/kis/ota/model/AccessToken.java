package blash10x.kis.ota.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Author: myungsik.sung@gmail.com
 */
public record AccessToken(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("access_token_token_expired") String accessTokenExpired,
    @JsonProperty("token_type") String tokenType,
    @JsonProperty("expires_in") Long expiresIn) {
}
