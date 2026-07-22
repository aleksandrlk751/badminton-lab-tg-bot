package ru.badmintonlab.bot.view;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import ru.badmintonlab.bot.model.PlayerCard;
import ru.badmintonlab.bot.model.PlayerSearchResult;
import ru.badmintonlab.bot.model.RivalsPage;
import ru.badmintonlab.core.domain.Discipline;

import java.util.ArrayList;
import java.util.List;

/**
 * Сборка inline-клавиатур для экранов бота (telegrambots 9.x: {@link InlineKeyboardRow}).
 */
@Component
public class Keyboards {

    public InlineKeyboardMarkup mainMenu() {
        return InlineKeyboardMarkup.builder()
                .keyboardRow(new InlineKeyboardRow(button("🔍 Найти игрока", CallbackData.MENU_SEARCH)))
                .keyboardRow(new InlineKeyboardRow(button("🆚 Сравнить (H2H)", CallbackData.MENU_H2H)))
                .keyboardRow(new InlineKeyboardRow(button("ℹ️ Помощь", CallbackData.MENU_HELP)))
                .build();
    }

    public InlineKeyboardMarkup searchResults(List<PlayerSearchResult> results) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (PlayerSearchResult r : results) {
            rows.add(new InlineKeyboardRow(button(SearchButtonLabel.format(r), CallbackData.card(r.playerId()))));
        }
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public InlineKeyboardMarkup card(PlayerCard card, boolean hasRivals) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        if (hasRivals) {
            rows.add(new InlineKeyboardRow(
                    button("🤺 Соперники", CallbackData.rivalsDefault(card.playerId()))));
        }
        rows.add(new InlineKeyboardRow(
                button("🆚 H2H", CallbackData.h2h(card.playerId())),
                button("📈 История рейтинга", CallbackData.history(card.playerId()))));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public InlineKeyboardMarkup rivals(RivalsPage page) {
        List<InlineKeyboardRow> rows = new ArrayList<>();

        InlineKeyboardRow filterRow = new InlineKeyboardRow();
        filterRow.add(button(
                page.allDisciplines() ? "• Все" : "Все",
                CallbackData.rivalsPage(page.playerId(), null, 0)));
        for (Discipline d : page.availableDisciplines()) {
            String label = d == page.discipline() ? "• " + d.name() : d.name();
            filterRow.add(button(label, CallbackData.rivalsPage(page.playerId(), d, 0)));
        }
        rows.add(filterRow);

        if (page.totalPages() > 1) {
            InlineKeyboardRow nav = new InlineKeyboardRow();
            if (page.hasPrev()) {
                nav.add(button("◀", CallbackData.rivalsPage(page.playerId(), page.discipline(), page.page() - 1)));
            }
            nav.add(button((page.page() + 1) + "/" + page.totalPages(), CallbackData.NOOP));
            if (page.hasNext()) {
                nav.add(button("▶", CallbackData.rivalsPage(page.playerId(), page.discipline(), page.page() + 1)));
            }
            rows.add(nav);
        }

        rows.add(new InlineKeyboardRow(button("⬅️ К карточке", CallbackData.card(page.playerId()))));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private InlineKeyboardButton button(String text, String callbackData) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(callbackData)
                .build();
    }
}
