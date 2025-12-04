package com.infinitosoft.infinitosecurity;

import com.infinitosoft.infinitosecurity.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(JwtProperties.class)
public class InfinitoSecurityApplication {

    public static void main(String[] args) {
        SpringApplication.run(InfinitoSecurityApplication.class, args);
    }

}
