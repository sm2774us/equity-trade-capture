package com.balyasny.tradecapture.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AdapterProperties.class)
public class AdapterConfig {}
