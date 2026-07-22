package ru.badmintonlab.bot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.badmintonlab.bot.handler.UpdateDispatcher;

import java.io.Serializable;
import java.util.List;

@Component
@ConditionalOnProperty(prefix = "telegram.bot", name = "enabled", havingValue = "true")
public class BadmintonLabBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(BadmintonLabBot.class);

    private final TelegramClient telegramClient;
    private final String botToken;
    private final UpdateDispatcher dispatcher;

    public BadmintonLabBot(@Value("${telegram.bot.token}") String botToken,
                           UpdateDispatcher dispatcher) {
        this.botToken = botToken;
        this.dispatcher = dispatcher;
        this.telegramClient = new OkHttpTelegramClient(botToken);
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        List<BotApiMethod<?>> responses;
        try {
            responses = dispatcher.dispatch(update);
        } catch (RuntimeException e) {
            log.error("Ошибка обработки обновления {}", update.getUpdateId(), e);
            return;
        }
        for (BotApiMethod<?> method : responses) {
            execute(method);
        }
    }

    private <T extends Serializable> void execute(BotApiMethod<T> method) {
        try {
            telegramClient.execute(method);
        } catch (TelegramApiException e) {
            // «message is not modified» — не ошибка (повторное нажатие той же кнопки), остальное логируем.
            if (e.getMessage() != null && e.getMessage().contains("message is not modified")) {
                return;
            }
            log.warn("Не удалось выполнить {}: {}", method.getMethod(), e.getMessage());
        }
    }
}
