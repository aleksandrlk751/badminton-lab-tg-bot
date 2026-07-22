package ru.badmintonlab.bot.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.badmintonlab.bot.model.H2hResult;
import ru.badmintonlab.bot.model.PlayerSearchResult;
import ru.badmintonlab.bot.service.H2hService;
import ru.badmintonlab.bot.service.PlayerCardLoader;
import ru.badmintonlab.bot.service.PlayerSearchOperations;
import ru.badmintonlab.bot.session.ChatSession;
import ru.badmintonlab.bot.session.ChatSessionStore;
import ru.badmintonlab.bot.view.CallbackData;
import ru.badmintonlab.bot.view.Keyboards;
import ru.badmintonlab.bot.view.Texts;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * H2H-wizard и экран результата ({@code docs/messages/05-h2h.md}).
 */
@Component
public class H2hFlowHandler {

    private final PlayerSearchOperations searchService;
    private final PlayerCardLoader cardService;
    private final H2hService h2hService;
    private final ChatSessionStore sessionStore;
    private final Texts texts;
    private final Keyboards keyboards;

    public H2hFlowHandler(PlayerSearchOperations searchService,
                          PlayerCardLoader cardService,
                          H2hService h2hService,
                          ChatSessionStore sessionStore,
                          Texts texts,
                          Keyboards keyboards) {
        this.searchService = searchService;
        this.cardService = cardService;
        this.h2hService = h2hService;
        this.sessionStore = sessionStore;
        this.texts = texts;
        this.keyboards = keyboards;
    }

    public List<BotApiMethod<?>> startFromMenu(long chatId) {
        sessionStore.put(chatId, ChatSession.step1(true, 0));
        return List.of(send(chatId, texts.h2hStep1(), null));
    }

    public List<BotApiMethod<?>> startFromCommand(long chatId) {
        sessionStore.put(chatId, ChatSession.step1(false, 0));
        return List.of(send(chatId, texts.h2hStep1(), null));
    }

    public List<BotApiMethod<?>> startFromCard(long chatId, int messageId, long playerAId) {
        sessionStore.put(chatId, new ChatSession(ChatSession.Mode.H2H_STEP2, playerAId, messageId, false));
        return editFlow(chatId, messageId, step2Text(playerAId), null);
    }

    private String step2Text(long playerAId) {
        return cardService.card(playerAId)
                .map(c -> texts.h2hStep2(c.fullName(), c.playerId(), c.nick()))
                .orElse(texts.h2hStep2("", playerAId, null));
    }

    public List<BotApiMethod<?>> onFreeText(long chatId, String text) {
        Optional<ChatSession> session = sessionStore.get(chatId);
        if (session.isEmpty()) {
            return List.of();
        }
        if (searchService.isQueryTooShort(text)) {
            return List.of(send(chatId, texts.queryTooShort(), null));
        }
        List<PlayerSearchResult> results = searchService.search(text);
        if (results.isEmpty()) {
            return List.of(send(chatId, texts.notFound(text), null));
        }
        ChatSession s = session.get();
        return switch (s.mode()) {
            case H2H_STEP1 -> handleStep1Search(chatId, s, results);
            case H2H_STEP2, H2H_CHANGE_OPPONENT -> handleStep2Search(chatId, s, results);
            default -> List.of();
        };
    }

    public List<BotApiMethod<?>> onCallback(String action, String[] parts, long chatId, Integer messageId) {
        return switch (action) {
            case CallbackData.H2H -> startFromCard(chatId, messageId, Long.parseLong(parts[1]));
            case CallbackData.H2H_SELECT_A -> onSelectPlayerA(chatId, messageId, Long.parseLong(parts[1]));
            case CallbackData.H2H_SELECT_B -> showResult(chatId, messageId,
                    Long.parseLong(parts[1]), Long.parseLong(parts[2]), sessionFromMenu(chatId));
            case CallbackData.H2H_CHANGE -> onChangeOpponent(chatId, messageId, Long.parseLong(parts[1]));
            default -> List.of();
        };
    }

    private List<BotApiMethod<?>> handleStep1Search(long chatId, ChatSession session,
                                                   List<PlayerSearchResult> results) {
        if (results.size() == 1) {
            return onSelectPlayerA(chatId, session.messageId(), results.get(0).playerId());
        }
        return List.of(send(chatId, texts.searchResultsHeader(results.size()),
                keyboards.h2hSelectA(results)));
    }

    private List<BotApiMethod<?>> handleStep2Search(long chatId, ChatSession session,
                                                    List<PlayerSearchResult> results) {
        long playerA = session.playerAId();
        if (results.size() == 1) {
            return showResult(chatId, session.messageId(), playerA, results.get(0).playerId(), session.fromMenu());
        }
        return List.of(send(chatId, texts.searchResultsHeader(results.size()),
                keyboards.h2hSelectB(playerA, results)));
    }

    private List<BotApiMethod<?>> onSelectPlayerA(long chatId, Integer messageId, long playerAId) {
        ChatSession prev = sessionStore.get(chatId).orElse(ChatSession.step1(false, messageId));
        ChatSession next = prev.withPlayerA(playerAId);
        if (messageId != null && messageId > 0) {
            sessionStore.put(chatId, next);
            return editFlow(chatId, messageId, step2Text(playerAId), null);
        }
        sessionStore.put(chatId, next);
        return List.of(send(chatId, step2Text(playerAId), null));
    }

    private List<BotApiMethod<?>> onChangeOpponent(long chatId, int messageId, long playerAId) {
        ChatSession prev = sessionStore.get(chatId).orElse(
                new ChatSession(ChatSession.Mode.H2H_STEP2, playerAId, messageId, false));
        ChatSession next = new ChatSession(ChatSession.Mode.H2H_CHANGE_OPPONENT, playerAId, 0, prev.fromMenu());
        sessionStore.put(chatId, next);
        return List.of(send(chatId, step2Text(playerAId), null));
    }

    private List<BotApiMethod<?>> showResult(long chatId, Integer messageId,
                                             long playerAId, long playerBId, boolean fromMenu) {
        Optional<H2hResult> result = h2hService.compare(playerAId, playerBId);
        if (result.isEmpty()) {
            sessionStore.clear(chatId);
            String text = playerAId == playerBId
                    ? texts.h2hSamePlayer()
                    : "Не удалось построить H2H — игрок не найден.";
            if (messageId != null && messageId > 0) {
                return editFlow(chatId, messageId, text, null);
            }
            return List.of(send(chatId, text, null));
        }
        sessionStore.put(chatId, new ChatSession(ChatSession.Mode.H2H_RESULT, playerAId, messageId, fromMenu));
        InlineKeyboardMarkup markup = keyboards.h2hResult(playerAId, fromMenu);
        String text = texts.h2hResult(result.get());
        if (messageId != null && messageId > 0) {
            return editFlow(chatId, messageId, text, markup);
        }
        return List.of(send(chatId, text, markup));
    }

    private boolean sessionFromMenu(long chatId) {
        return sessionStore.get(chatId).map(ChatSession::fromMenu).orElse(false);
    }

    private List<BotApiMethod<?>> editFlow(long chatId, int messageId, String text, InlineKeyboardMarkup markup) {
        List<BotApiMethod<?>> out = new ArrayList<>();
        out.add(edit(chatId, messageId, text, markup));
        return out;
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
