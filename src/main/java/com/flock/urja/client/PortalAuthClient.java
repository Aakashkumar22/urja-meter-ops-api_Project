package com.flock.urja.client;

import com.flock.urja.config.PortalProperties;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class PortalAuthClient {

    private final PortalProperties props;
    private WebClient loginClient;

    private volatile String sessionCookie;
    private volatile Instant expiresAt = Instant.EPOCH;
    private final Object lock = new Object();

    public PortalAuthClient(PortalProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() {
        HttpClient http = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.getConnectTimeoutMs())
            .responseTimeout(Duration.ofMillis(props.getReadTimeoutMs()))
            .doOnConnected(c -> c.addHandlerLast(
                new ReadTimeoutHandler(props.getReadTimeoutMs(), TimeUnit.MILLISECONDS)))
            .followRedirect(false);

        this.loginClient = WebClient.builder()
            .baseUrl(props.getBaseUrl())
            .clientConnector(new ReactorClientHttpConnector(http))
            .defaultHeader(HttpHeaders.USER_AGENT, "urja-api/1.0")
            .build();
    }

    public String getSessionCookie() {
        if (isValid()) return sessionCookie;
        synchronized (lock) {
            if (isValid()) return sessionCookie;
            login();
            return sessionCookie;
        }
    }

    public void invalidate() {
        synchronized (lock) {
            sessionCookie = null;
            expiresAt = Instant.EPOCH;
        }
    }

    private boolean isValid() {
        return sessionCookie != null && Instant.now().isBefore(expiresAt);
    }

    private void login() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("email", props.getUsername());
        form.add("password", props.getPassword());

        log.info("Logging into portal at {}", props.getBaseUrl());

        String setCookie = loginClient.post()
            .uri("/login")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .header(HttpHeaders.ORIGIN, props.getBaseUrl())
            .header(HttpHeaders.REFERER, props.getBaseUrl() + "/login")
            .body(BodyInserters.fromFormData(form))
            .exchangeToMono(resp -> {
                if (!resp.statusCode().is2xxSuccessful() && !resp.statusCode().is3xxRedirection()) {
                    return resp.createException().flatMap(Mono::error);
                }
                return Mono.justOrEmpty(resp.headers().asHttpHeaders().getFirst(HttpHeaders.SET_COOKIE));
            })
            .block();

        if (setCookie == null || !setCookie.startsWith("__Secure-better-auth.session_token=")) {
            throw new IllegalStateException(
                "Login response did not set the expected better-auth session cookie. Got: " + setCookie);
        }

        String cookieValue = setCookie.split(";", 2)[0];
        this.sessionCookie = cookieValue;
        long ttl = props.getSessionTtlSeconds() - props.getSessionRefreshSafetySeconds();
        this.expiresAt = Instant.now().plusSeconds(ttl);
        log.info("Portal login OK. Cookie valid for {} more seconds.", ttl);
    }
}
