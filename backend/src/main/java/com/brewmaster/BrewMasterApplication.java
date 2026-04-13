package com.brewmaster;

import com.brewmaster.config.JwtConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtConfig.class)
public class BrewMasterApplication {

    public static void main(String[] args) {
        SpringApplication.run(BrewMasterApplication.class, args);
    }
}
