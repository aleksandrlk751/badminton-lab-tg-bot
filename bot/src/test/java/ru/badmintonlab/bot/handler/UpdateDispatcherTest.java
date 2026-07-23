package ru.badmintonlab.bot.handler;

import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.badmintonlab.bot.model.PlayerCard;
import ru.badmintonlab.bot.model.PlayerSearchResult;
import ru.badmintonlab.bot.model.RivalRow;
import ru.badmintonlab.bot.model.RivalsPage;
import ru.badmintonlab.bot.service.PlayerCardLoader;
import ru.badmintonlab.bot.service.PlayerSearchOperations;
import ru.badmintonlab.bot.service.RivalLookup;
import ru.badmintonlab.bot.session.ChatSessionStore;
import ru.badmintonlab.bot.view.CallbackData;
import ru.badmintonlab.bot.view.Keyboards;
import ru.badmintonlab.bot.view.Texts;
import ru.badmintonlab.core.domain.Discipline;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdateDispatcherTest {

    private final FakeSearch search = new FakeSearch();
    private final FakeCard card = new FakeCard();
    private final FakeRival rival = new FakeRival();
    private final ChatSessionStore sessions = new ChatSessionStore();
    private final H2hFlowHandler h2hFlow =
            new H2hFlowHandler(search, card, null, sessions, new Texts(), new Keyboards());
    private final UpdateDispatcher dispatcher =
            new UpdateDispatcher(search, card, rival, h2hFlow, sessions, new Texts(), new Keyboards());

    @Test
    void startShowsMenu() {
        List<BotApiMethod<?>> res = dispatcher.dispatch(textUpdate(100L, "/start"));
        assertEquals(1, res.size());
        SendMessage sm = assertInstanceOf(SendMessage.class, res.get(0));
        assertTrue(sm.getText().contains("Badminton LAB"));
        assertTrue(sm.getText().contains("обновление ежедневно"));
        assertNotNull(sm.getReplyMarkup());
    }

    @Test
    void shortQueryPrompts() {
        search.tooShort = true;
        SendMessage sm = assertInstanceOf(SendMessage.class,
                dispatcher.dispatch(textUpdate(100L, "ab")).get(0));
        assertTrue(sm.getText().contains("фамилию или ник"));
    }

    @Test
    void notFoundQuery() {
        search.tooShort = false;
        search.results = List.of();
        SendMessage sm = assertInstanceOf(SendMessage.class,
                dispatcher.dispatch(textUpdate(100L, "Иванов")).get(0));
        assertTrue(sm.getText().contains("ничего не найдено"));
    }

    @Test
    void searchReturnsResultsWithKeyboard() {
        search.tooShort = false;
        search.results = List.of(new PlayerSearchResult(
                1L, "Nick", "Иванов", "Иван", null, "Москва", null, new BigDecimal("400")));
        SendMessage sm = assertInstanceOf(SendMessage.class,
                dispatcher.dispatch(textUpdate(100L, "Иванов")).get(0));
        assertTrue(sm.getText().contains("под критерии поиска"));
        assertNotNull(sm.getReplyMarkup());
    }

    @Test
    void cardCallbackSendsNewMessage() {
        card.card = Optional.of(sampleCard());
        rival.hasRivals = true;

        List<BotApiMethod<?>> res = dispatcher.dispatch(callbackUpdate("card:5", 100L, 7));

        assertEquals(2, res.size());
        SendMessage sm = assertInstanceOf(SendMessage.class, res.get(0));
        assertTrue(sm.getText().contains("Иванов Иван"));
        assertInstanceOf(AnswerCallbackQuery.class, res.get(1));
    }

    @Test
    void cardCallbackNotFound() {
        card.card = Optional.empty();
        List<BotApiMethod<?>> res = dispatcher.dispatch(callbackUpdate("card:5", 100L, 7));
        SendMessage sm = assertInstanceOf(SendMessage.class, res.get(0));
        assertTrue(sm.getText().contains("не найден"));
    }

    @Test
    void h2hCallbackStartsStep2() {
        card.card = Optional.of(sampleCard());
        List<BotApiMethod<?>> res = dispatcher.dispatch(callbackUpdate("h2h:5", 100L, 7));
        assertEquals(2, res.size());
        EditMessageText edit = assertInstanceOf(EditMessageText.class, res.get(0));
        assertTrue(edit.getText().contains("шаг 2/2"));
    }

    @Test
    void h2hCommandStartsWizard() {
        List<BotApiMethod<?>> res = dispatcher.dispatch(textUpdate(100L, "/h2h"));
        SendMessage sm = assertInstanceOf(SendMessage.class, res.get(0));
        assertTrue(sm.getText().contains("шаг 1/2"));
    }

    @Test
    void rivalsDefaultWithoutRivalsAnswersAlert() {
        rival.hasRivals = false;
        List<BotApiMethod<?>> res = dispatcher.dispatch(callbackUpdate("rv:5", 100L, 7));
        assertEquals(1, res.size());
        AnswerCallbackQuery answer = assertInstanceOf(AnswerCallbackQuery.class, res.get(0));
        assertTrue(answer.getText().contains("Соперников пока нет"));
    }

    @Test
    void rivalsDefaultSendsNewMessage() {
        rival.hasRivals = true;
        rival.page = new RivalsPage(5L, "Иванов",
                null,
                List.of(new RivalRow(2L, "Foe", "Петров", "Москва", 2, 0)),
                0, 8, 1, List.of(Discipline.MD));

        List<BotApiMethod<?>> res = dispatcher.dispatch(callbackUpdate("rv:5", 100L, 7));

        assertEquals(2, res.size());
        SendMessage sm = assertInstanceOf(SendMessage.class, res.get(0));
        assertTrue(sm.getText().contains("Соперники"));
        assertInstanceOf(AnswerCallbackQuery.class, res.get(1));
    }

    @Test
    void h2hChangeOpponentSendsNewMessage() {
        card.card = Optional.of(sampleCard());
        sessions.put(100L, new ru.badmintonlab.bot.session.ChatSession(
                ru.badmintonlab.bot.session.ChatSession.Mode.H2H_RESULT, 5L, 7, false));

        List<BotApiMethod<?>> res = dispatcher.dispatch(callbackUpdate("h2c:5", 100L, 7));

        assertEquals(2, res.size());
        SendMessage sm = assertInstanceOf(SendMessage.class, res.get(0));
        assertTrue(sm.getText().contains("шаг 2/2"));
        assertInstanceOf(AnswerCallbackQuery.class, res.get(1));
    }

    @Test
    void rivalsPageEditsMessage() {
        rival.page = new RivalsPage(5L, "Иванов",
                Discipline.MD,
                List.of(new RivalRow(2L, "Foe", "Петров", "Москва", 2, 0)),
                0, 8, 1, List.of(Discipline.MD));

        List<BotApiMethod<?>> res = dispatcher.dispatch(callbackUpdate("rvp:5:MD:0", 100L, 7));

        assertEquals(2, res.size());
        assertInstanceOf(EditMessageText.class, res.get(0));
        assertInstanceOf(AnswerCallbackQuery.class, res.get(1));
    }

    private PlayerCard sampleCard() {
        return new PlayerCard(5L, "Rocket", "Иванов Иван", "Москва", List.of(), null, null, null, null, null);
    }

    private Update textUpdate(long chatId, String text) {
        Message message = new Message();
        message.setChat(Chat.builder().id(chatId).type("private").build());
        message.setText(text);
        Update update = new Update();
        update.setMessage(message);
        return update;
    }

    private Update callbackUpdate(String data, long chatId, int messageId) {
        Message message = new Message();
        message.setChat(Chat.builder().id(chatId).type("private").build());
        message.setMessageId(messageId);
        CallbackQuery query = new CallbackQuery();
        query.setId("cb-id");
        query.setData(data);
        query.setMessage(message);
        Update update = new Update();
        update.setCallbackQuery(query);
        return update;
    }

    private static final class FakeSearch implements PlayerSearchOperations {
        boolean tooShort;
        List<PlayerSearchResult> results = List.of();

        @Override
        public boolean isQueryTooShort(String rawQuery) {
            return tooShort;
        }

        @Override
        public List<PlayerSearchResult> search(String rawQuery) {
            return results;
        }
    }

    private static final class FakeCard implements PlayerCardLoader {
        Optional<PlayerCard> card = Optional.empty();

        @Override
        public Optional<PlayerCard> card(long playerId) {
            return card;
        }
    }

    private static final class FakeRival implements RivalLookup {
        boolean hasRivals;
        RivalsPage page;

        @Override
        public boolean hasRivals(long playerId) {
            return hasRivals;
        }

        @Override
        public RivalsPage rivals(long playerId, Discipline discipline, int page) {
            return this.page;
        }
    }
}
