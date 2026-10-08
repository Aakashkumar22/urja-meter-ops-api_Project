package com.flock.urja;

import com.flock.urja.config.PortalProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;


import com.flock.urja.config.PortalProperties;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(PortalProperties.class)
@OpenAPIDefinition(info = @Info(
        title = "Urja Meter Ops API",
        version = "1.0.0",
        description = "A clean, documented REST API over the Urja Meter Ops portal."
))
public class UrjaApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(UrjaApiApplication.class, args);
    }
}
