package com.flock.urja.client;

import com.flock.urja.config.PortalProperties;
import com.flock.urja.exception.PortalException;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.HttpClient;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Component
public class PortalDataClient {

    private final PortalProperties props;
    private final PortalAuthClient auth;
    private WebClient client;

    public PortalDataClient(PortalProperties props, PortalAuthClient auth) {
        this.props = props;
        this.auth = auth;
    }

    @PostConstruct
    void init() {
        HttpClient http = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.getConnectTimeoutMs())
            .responseTimeout(Duration.ofMillis(props.getReadTimeoutMs()))
            .doOnConnected(c -> c.addHandlerLast(
                new ReadTimeoutHandler(props.getReadTimeoutMs(), TimeUnit.MILLISECONDS)));

        this.client = WebClient.builder()
            .baseUrl(props.getBaseUrl())
            .clientConnector(new ReactorClientHttpConnector(http))
            .defaultHeader(HttpHeaders.USER_AGENT, "urja-api/1.0")
            .build();
    }

    /** GET with a simple Class<T> return type. */
    public <T> T get(String path, Class<T> responseType) {
        return withRetry(() -> client.get()
            .uri(path)
            .header(HttpHeaders.COOKIE, auth.getSessionCookie())
            .retrieve()
            .bodyToMono(responseType)
            .block());
    }

    /** GET with a generic return type (e.g. PortalPage<PortalMeter>). */
    public <T> T get(String path, ParameterizedTypeReference<T> type) {
        return withRetry(() -> client.get()
            .uri(path)
            .header(HttpHeaders.COOKIE, auth.getSessionCookie())
            .retrieve()
            .bodyToMono(type)
            .block());
    }

    private <T> T withRetry(Supplier<T> call) {
        try {
            return call.get();
        } catch (WebClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.UNAUTHORIZED
                    || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                log.warn("Portal returned {}. Re-authenticating and retrying once.", e.getStatusCode());
                auth.invalidate();
                auth.getSessionCookie();
                try {
                    return call.get();
                } catch (WebClientResponseException retry) {
                    throw new PortalException(
                        "Portal call failed after re-auth: " + retry.getStatusCode(), retry);
                }
            }
            throw new PortalException("Portal call failed: " + e.getStatusCode(), e);
        }
    }
}
