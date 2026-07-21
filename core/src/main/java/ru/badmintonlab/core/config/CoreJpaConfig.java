package ru.badmintonlab.core.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EntityScan(basePackages = "ru.badmintonlab.core.entity")
@EnableJpaRepositories(basePackages = "ru.badmintonlab.core.repository")
@EnableConfigurationProperties(MetricsProperties.class)
public class CoreJpaConfig {
}
