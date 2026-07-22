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
import ru.badmintonlab.bot.service.PlayerCardService;
import ru.badmintonlab.bot.service.PlayerSearchService;
import ru.badmintonlab.bot.service.RivalService;
import ru.badmintonlab.bot.view.Keyboards;
import ru.badmintonlab.bot.view.Texts;
import ru.badmintonlab.core.domain.Discipline;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Тесты диспетчера без Mockito (форк JVM на dev-машине недоступен, {@code forkCount=0}):
 * реальные объекты Telegram + простые фейки сервисов.
 */
class UpdateDispatcherTest {

    private final FakeSearch search = new FakeSearch();
    private final FakeCard card = new FakeCard();
    private final FakeRival rival = new FakeRival();
    private final UpdateDispatcher dispatcher =
            new UpdateDispatcher(search, card, rival, new Texts(), new Keyboards());

    @Test
    void startShowsMenu() {
        List<BotApiMethod<?>> res = dispatcher.dispatch(textUpdate(100L, "/start"));
        assertEquals(1, res.size());
        SendMessage sm = assertInstanceOf(SendMessage.class, res.get(0));
        assertTrue(sm.getText().contains("Badminton LAB"));
        assertNotNull(sm.getReplyMarkup());
    }

    @Test
    void shortQueryPrompts() {
        search.tooShort = true;
        SendMessage sm = assertInstanceOf(SendMessage.class,
                dispatcher.dispatch(textUpdate(100L, "ab")).get(0));
        assertTrue(sm.getText().contains("3 символа"));
    }

    @Test
    void notFoundQuery() {
        search.tooShort = false;
        search.results = List.of();
        SendMessage sm = assertInstanceOf(SendMessage.class,
                dispatcher.dispatch(textUpdate(100L, "Иванов")).get(0));
        assertTrue(sm.getText().contains("никого не нашёл"));
    }

    @Test
    void searchReturnsResultsWithKeyboard() {
        search.tooShort = false;
        search.results = List.of(new PlayerSearchResult(1L, "Nick", "Иванов Иван", "Москва"));
        SendMessage sm = assertInstanceOf(SendMessage.class,
                dispatcher.dispatch(textUpdate(100L, "Иванов")).get(0));
        assertNotNull(sm.getReplyMarkup());
    }

    @Test
    void cardCallbackEditsMessage() {
        card.card = Optional.of(sampleCard());
        rival.disciplines = List.of(Discipline.MD);

        List<BotApiMethod<?>> res = dispatcher.dispatch(callbackUpdate("card:5", 100L, 7));

        assertEquals(2, res.size());
        EditMessageText edit = assertInstanceOf(EditMessageText.class, res.get(0));
        assertTrue(edit.getText().contains("Rocket"));
        assertInstanceOf(AnswerCallbackQuery.class, res.get(1));
    }

    @Test
    void cardCallbackNotFound() {
        card.card = Optional.empty();
        List<BotApiMethod<?>> res = dispatcher.dispatch(callbackUpdate("card:5", 100L, 7));
        EditMessageText edit = assertInstanceOf(EditMessageText.class, res.get(0));
        assertTrue(edit.getText().contains("не найден"));
    }

    @Test
    void h2hCallbackAnswersWithAlert() {
        List<BotApiMethod<?>> res = dispatcher.dispatch(callbackUpdate("h2h:5", 100L, 7));
        assertEquals(1, res.size());
        AnswerCallbackQuery answer = assertInstanceOf(AnswerCallbackQuery.class, res.get(0));
        assertEquals(Boolean.TRUE, answer.getShowAlert());
        assertNotNull(answer.getText());
    }

    @Test
    void rivalsDefaultWithoutRivalsAnswersAlert() {
        rival.defaultDiscipline = Optional.empty();
        List<BotApiMethod<?>> res = dispatcher.dispatch(callbackUpdate("rv:5", 100L, 7));
        assertEquals(1, res.size());
        AnswerCallbackQuery answer = assertInstanceOf(AnswerCallbackQuery.class, res.get(0));
        assertTrue(answer.getText().contains("Соперников пока нет"));
    }

    @Test
    void rivalsPageEditsMessage() {
        rival.page = new RivalsPage(5L, Discipline.MD,
                List.of(new RivalRow(2L, "Foe", "Петров", "Москва", 2, 0)),
                0, 8, 1, List.of(Discipline.MD));

        List<BotApiMethod<?>> res = dispatcher.dispatch(callbackUpdate("rvp:5:MD:1", 100L, 7));

        assertEquals(2, res.size());
        assertInstanceOf(EditMessageText.class, res.get(0));
        assertInstanceOf(AnswerCallbackQuery.class, res.get(1));
    }

    // ----- Хелперы -----

    private PlayerCard sampleCard() {
        return new PlayerCard(5L, "Rocket", "Иванов Иван", "Москва", List.of(), null, null);
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

    // ----- Фейки сервисов -----

    private static final class FakeSearch extends PlayerSearchService {
        boolean tooShort;
        List<PlayerSearchResult> results = List.of();

        FakeSearch() {
            super(null);
        }

        @Override
        public boolean isQueryTooShort(String rawQuery) {
            return tooShort;
        }

        @Override
        public List<PlayerSearchResult> search(String rawQuery) {
            return results;
        }
    }

    private static final class FakeCard extends PlayerCardService {
        Optional<PlayerCard> card = Optional.empty();

        FakeCard() {
            super(null, null, null, null);
        }

        @Override
        public Optional<PlayerCard> card(long playerId) {
            return card;
        }
    }

    private static final class FakeRival extends RivalService {
        List<Discipline> disciplines = List.of();
        Optional<Discipline> defaultDiscipline = Optional.empty();
        RivalsPage page;

        FakeRival() {
            super(null);
        }

        @Override
        public List<Discipline> disciplinesWithRivals(long playerId) {
            return disciplines;
        }

        @Override
        public Optional<Discipline> defaultDiscipline(long playerId) {
            return defaultDiscipline;
        }

        @Override
        public RivalsPage rivals(long playerId, Discipline discipline, int page) {
            return this.page;
        }
    }
}
