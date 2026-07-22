package ru.badmintonlab.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.badmintonlab.core.config.CoreJpaConfig;

@SpringBootApplication(scanBasePackages = {"ru.badmintonlab.worker", "ru.badmintonlab.parser"})
@ConfigurationPropertiesScan("ru.badmintonlab.worker.config")
@EnableScheduling
@Import(CoreJpaConfig.class)
public class WorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkerApplication.class, args);
    }
}
