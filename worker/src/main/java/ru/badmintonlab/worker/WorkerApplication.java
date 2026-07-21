package ru.badmintonlab.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import ru.badmintonlab.core.config.CoreJpaConfig;

@SpringBootApplication(scanBasePackages = {"ru.badmintonlab.worker", "ru.badmintonlab.parser"})
@Import(CoreJpaConfig.class)
public class WorkerApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorkerApplication.class, args);
    }
}
