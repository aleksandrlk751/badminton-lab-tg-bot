package ru.badmintonlab.bot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Component
@ConditionalOnProperty(prefix = "telegram.bot", name = "enabled", havingValue = "true")
public class BadmintonLabBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private static final String START_MESSAGE = """
            Привет! Я бот Badminton LAB.

            Пока доступна только команда /start. Скоро — поиск игроков, карточки и H2H.

            Меню:
            • Найти игрока
            • Сравнить (H2H)
            • Помощь
            """;

    private final TelegramClient telegramClient;
    private final String botToken;

    public BadmintonLabBot(@Value("${telegram.bot.token}") String botToken) {
        this.botToken = botToken;
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
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        String text = update.getMessage().getText().trim();
        if (!text.startsWith("/start")) {
            return;
        }

        SendMessage message = SendMessage.builder()
                .chatId(update.getMessage().getChatId())
                .text(START_MESSAGE)
                .build();

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            throw new IllegalStateException("Failed to send /start response", e);
        }
    }
}
