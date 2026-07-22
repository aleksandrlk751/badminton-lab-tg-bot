package ru.badmintonlab.bot.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.badmintonlab.bot.model.PlayerCard;
import ru.badmintonlab.bot.model.PlayerSearchResult;
import ru.badmintonlab.bot.model.RivalsPage;
import ru.badmintonlab.bot.service.PlayerCardService;
import ru.badmintonlab.bot.service.PlayerSearchService;
import ru.badmintonlab.bot.service.RivalService;
import ru.badmintonlab.bot.view.CallbackData;
import ru.badmintonlab.bot.view.Keyboards;
import ru.badmintonlab.bot.view.Texts;
import ru.badmintonlab.core.domain.Discipline;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Маршрутизация Telegram-обновлений в ответы бота (этап 4: меню, поиск, карточка, соперники).
 */
@Component
public class UpdateDispatcher {

    private final PlayerSearchService searchService;
    private final PlayerCardService cardService;
    private final RivalService rivalService;
    private final Texts texts;
    private final Keyboards keyboards;

    public UpdateDispatcher(PlayerSearchService searchService,
                            PlayerCardService cardService,
                            RivalService rivalService,
                            Texts texts,
                            Keyboards keyboards) {
        this.searchService = searchService;
        this.cardService = cardService;
        this.rivalService = rivalService;
        this.texts = texts;
        this.keyboards = keyboards;
    }

    public List<BotApiMethod<?>> dispatch(Update update) {
        if (update.hasCallbackQuery()) {
            return onCallback(update.getCallbackQuery());
        }
        if (update.hasMessage() && update.getMessage().hasText()) {
            return onText(update.getMessage());
        }
        return List.of();
    }

    private List<BotApiMethod<?>> onText(Message message) {
        long chatId = message.getChatId();
        String text = message.getText().trim();
        if (text.startsWith("/")) {
            return onCommand(chatId, text);
        }
        return onFreeText(chatId, text);
    }

    private List<BotApiMethod<?>> onCommand(long chatId, String text) {
        String command = text.split("\\s+", 2)[0].toLowerCase();
        int at = command.indexOf('@');
        if (at > 0) {
            command = command.substring(0, at);
        }
        return switch (command) {
            case "/start" -> List.of(send(chatId, texts.menu(), keyboards.mainMenu()));
            case "/help" -> List.of(send(chatId, texts.help(), null));
            case "/h2h" -> List.of(send(chatId, texts.h2hStub(), null));
            default -> List.of(send(chatId,
                    "Неизвестная команда. Введите фамилию или ник игрока, либо /start.", null));
        };
    }

    private List<BotApiMethod<?>> onFreeText(long chatId, String text) {
        if (searchService.isQueryTooShort(text)) {
            return List.of(send(chatId, texts.queryTooShort(), null));
        }
        List<PlayerSearchResult> results = searchService.search(text);
        if (results.isEmpty()) {
            return List.of(send(chatId, texts.notFound(text), null));
        }
        return List.of(send(chatId, texts.searchResultsHeader(results.size()),
                keyboards.searchResults(results)));
    }

    private List<BotApiMethod<?>> onCallback(CallbackQuery query) {
        List<BotApiMethod<?>> out = new ArrayList<>();
        Long chatId = query.getMessage() == null ? null : query.getMessage().getChatId();
        Integer messageId = query.getMessage() == null ? null : query.getMessage().getMessageId();
        String[] parts = CallbackData.parse(query.getData());
        String action = parts.length > 0 ? parts[0] : "";

        String answerText = null;
        boolean alert = false;

        try {
            switch (action) {
                case "menu" -> handleMenu(parts, chatId, out);
                case CallbackData.CARD -> handleCard(parseId(parts, 1), chatId, messageId, out);
                case CallbackData.RIVALS -> {
                    answerText = handleRivalsDefault(parseId(parts, 1), chatId, messageId, out);
                    alert = answerText != null;
                }
                case CallbackData.RIVALS_PAGE -> handleRivalsPage(
                        parseId(parts, 1),
                        CallbackData.parseRivalsDiscipline(parts[2]),
                        Integer.parseInt(parts[3]),
                        chatId, messageId, out);
                case CallbackData.H2H -> {
                    answerText = texts.h2hStub();
                    alert = true;
                }
                case CallbackData.HISTORY -> {
                    answerText = texts.historyStub();
                    alert = true;
                }
                default -> { /* NOOP */ }
            }
        } catch (RuntimeException e) {
            answerText = "Не удалось выполнить действие.";
            alert = true;
        }

        out.add(AnswerCallbackQuery.builder()
                .callbackQueryId(query.getId())
                .text(answerText)
                .showAlert(alert)
                .build());
        return out;
    }

    private void handleMenu(String[] parts, Long chatId, List<BotApiMethod<?>> out) {
        if (chatId == null || parts.length < 2) {
            return;
        }
        switch (parts[1]) {
            case "search" -> out.add(send(chatId,
                    "Отправьте ник или фамилию игрока (от 3 символов).", null));
            case "help" -> out.add(send(chatId, texts.help(), null));
            case "h2h" -> out.add(send(chatId, texts.h2hStub(), null));
            default -> { /* ничего */ }
        }
    }

    private void handleCard(long playerId, Long chatId, Integer messageId, List<BotApiMethod<?>> out) {
        if (chatId == null || messageId == null) {
            return;
        }
        Optional<PlayerCard> card = cardService.card(playerId);
        if (card.isEmpty()) {
            out.add(edit(chatId, messageId, "Игрок не найден.", null));
            return;
        }
        boolean hasRivals = rivalService.hasRivals(playerId);
        out.add(edit(chatId, messageId, texts.card(card.get()), keyboards.card(card.get(), hasRivals)));
    }

    private String handleRivalsDefault(long playerId, Long chatId, Integer messageId,
                                       List<BotApiMethod<?>> out) {
        if (chatId == null || messageId == null) {
            return null;
        }
        if (!rivalService.hasRivals(playerId)) {
            return "Соперников пока нет.";
        }
        RivalsPage page = rivalService.rivals(playerId, null, 0);
        out.add(edit(chatId, messageId, texts.rivals(page), keyboards.rivals(page)));
        return null;
    }

    private void handleRivalsPage(long playerId, Discipline discipline, int page,
                                  Long chatId, Integer messageId, List<BotApiMethod<?>> out) {
        if (chatId == null || messageId == null) {
            return;
        }
        RivalsPage rivalsPage = rivalService.rivals(playerId, discipline, page);
        out.add(edit(chatId, messageId, texts.rivals(rivalsPage), keyboards.rivals(rivalsPage)));
    }

    private long parseId(String[] parts, int index) {
        return Long.parseLong(parts[index]);
    }

    private SendMessage send(long chatId, String text, InlineKeyboardMarkup markup) {
        return SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("HTML")
                .replyMarkup(markup)
                .build();
    }

    private EditMessageText edit(long chatId, int messageId, String text, InlineKeyboardMarkup markup) {
        return EditMessageText.builder()
                .chatId(String.valueOf(chatId))
                .messageId(messageId)
                .text(text)
                .parseMode("HTML")
                .replyMarkup(markup)
                .build();
    }
}
