package com.flock.urja.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "portal")
public class PortalProperties {
    private String baseUrl;
    private String username;
    private String password;
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 15000;
    private int sessionTtlSeconds = 3600;
    private int sessionRefreshSafetySeconds = 300;
}
