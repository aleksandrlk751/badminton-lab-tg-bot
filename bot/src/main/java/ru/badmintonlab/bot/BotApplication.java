package ru.badmintonlab.bot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import ru.badmintonlab.core.config.CoreJpaConfig;

@SpringBootApplication(scanBasePackages = "ru.badmintonlab.bot")
@Import(CoreJpaConfig.class)
public class BotApplication {

    public static void main(String[] args) {
        SpringApplication.run(BotApplication.class, args);
    }
}
