package com.flock.urja;

import com.flock.urja.config.PortalProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(PortalProperties.class)
public class UrjaApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(UrjaApiApplication.class, args);
    }
}
