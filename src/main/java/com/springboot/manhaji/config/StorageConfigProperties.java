package com.springboot.manhaji.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.storage")
@Getter
@Setter
public class StorageConfigProperties {

    private String uploadDir = "./uploads";
    private String audioDir = "./uploads/audio";
    private String imageDir = "./uploads/";
}
