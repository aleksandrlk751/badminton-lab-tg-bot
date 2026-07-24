package ru.badmintonlab.bot.handler;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.badmintonlab.bot.model.PartnerPickPage;
import ru.badmintonlab.bot.model.PlayerSearchResult;
import ru.badmintonlab.bot.model.UpcomingTournamentRow;
import ru.badmintonlab.bot.service.PartnerPickService;
import ru.badmintonlab.bot.service.PartnerTournamentResolveService;
import ru.badmintonlab.bot.service.PlayerSearchOperations;
import ru.badmintonlab.bot.session.ChatSession;
import ru.badmintonlab.bot.session.ChatSessionStore;
import ru.badmintonlab.bot.view.CallbackData;
import ru.badmintonlab.bot.view.Keyboards;
import ru.badmintonlab.bot.view.Texts;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class PartnerPickFlowHandler {

    private final PartnerPickService partnerPickService;
    private final PartnerTournamentResolveService tournamentResolveService;
    private final PlayerSearchOperations searchService;
    private final ChatSessionStore sessionStore;
    private final Texts texts;
    private final Keyboards keyboards;

    public PartnerPickFlowHandler(PartnerPickService partnerPickService,
                                  PartnerTournamentResolveService tournamentResolveService,
                                  PlayerSearchOperations searchService,
                                  ChatSessionStore sessionStore,
                                  Texts texts,
                                  Keyboards keyboards) {
        this.partnerPickService = partnerPickService;
        this.tournamentResolveService = tournamentResolveService;
        this.searchService = searchService;
        this.sessionStore = sessionStore;
        this.texts = texts;
        this.keyboards = keyboards;
    }

    public List<BotApiMethod<?>> startFromMenu(long chatId) {
        sessionStore.clear(chatId);
        return List.of(send(chatId, texts.partnerEntry(), keyboards.partnerEntry()));
    }

    public List<BotApiMethod<?>> onCallback(String action, String[] parts, long chatId, int messageId) {
        return switch (action) {
            case CallbackData.PARTNER_NEAR -> showUpcomingList(chatId, messageId);
            case CallbackData.PARTNER_LINK -> {
                sessionStore.put(chatId, ChatSession.partnerPickLink(messageId));
                yield editFlow(chatId, messageId, texts.partnerPasteLink(), null);
            }
            case CallbackData.PARTNER_BACK -> {
                sessionStore.clear(chatId);
                yield editFlow(chatId, messageId, texts.partnerEntry(), keyboards.partnerEntry());
            }
            case CallbackData.PARTNER_BACK_USER -> {
                long tournamentId = Long.parseLong(parts[1]);
                sessionStore.put(chatId, ChatSession.partnerPickUser(tournamentId, messageId));
                yield editFlow(chatId, messageId, texts.partnerWhoAreYou(), null);
            }
            case CallbackData.PARTNER_TOUR -> {
                long tournamentId = Long.parseLong(parts[1]);
                sessionStore.put(chatId, ChatSession.partnerPickUser(tournamentId, messageId));
                yield editFlow(chatId, messageId, texts.partnerWhoAreYou(), null);
            }
            case CallbackData.PARTNER_SELECT_USER -> {
                long tournamentId = Long.parseLong(parts[1]);
                long userId = Long.parseLong(parts[2]);
                sessionStore.clear(chatId);
                yield showResults(chatId, messageId, tournamentId, userId);
            }
            default -> List.of();
        };
    }

    private List<BotApiMethod<?>> showUpcomingList(long chatId, int messageId) {
        List<UpcomingTournamentRow> tournaments = partnerPickService.upcomingTournaments();
        if (tournaments.isEmpty()) {
            return editFlow(chatId, messageId, texts.partnerNoTournaments(), keyboards.partnerEntry());
        }
        return editFlow(chatId, messageId, texts.partnerTournamentList(), keyboards.partnerTournaments(tournaments));
    }

    public List<BotApiMethod<?>> onFreeText(long chatId, String text) {
        Optional<ChatSession> session = sessionStore.get(chatId);
        if (session.isEmpty()) {
            return List.of();
        }
        return switch (session.get().mode()) {
            case PARTNER_PICK_LINK -> handleLinkPaste(chatId, text.trim());
            case PARTNER_PICK_USER -> handleUserSearch(chatId, text, session.get());
            default -> List.of();
        };
    }

    private List<BotApiMethod<?>> handleLinkPaste(long chatId, String text) {
        var result = tournamentResolveService.resolveFromLink(text);
        if (result.error().isPresent()) {
            String msg = switch (result.error().get()) {
                case INVALID_LINK -> texts.partnerLinkInvalid();
                case NOT_DOUBLES -> texts.partnerLinkNotDoubles();
                case ALREADY_STARTED -> texts.partnerLinkAlreadyStarted();
                case NOT_FOUND, SYNC_FAILED -> texts.partnerLinkSyncFailed();
            };
            return List.of(send(chatId, msg, null));
        }
        long tournamentId = result.tournamentId().orElseThrow();
        sessionStore.put(chatId, ChatSession.partnerPickUser(tournamentId, 0));
        return List.of(send(chatId, texts.partnerWhoAreYou(), null));
    }

    private List<BotApiMethod<?>> handleUserSearch(long chatId, String text, ChatSession session) {
        if (session.tournamentId() == null) {
            return List.of();
        }
        if (searchService.isQueryTooShort(text)) {
            return List.of(send(chatId, texts.queryTooShort(), null));
        }
        List<PlayerSearchResult> results = searchService.search(text);
        if (results.isEmpty()) {
            return List.of(send(chatId, texts.notFound(text), null));
        }
        long tournamentId = session.tournamentId();
        return List.of(send(chatId, texts.searchResultsHeader(results.size()),
                keyboards.partnerUserSearch(tournamentId, results)));
    }

    private List<BotApiMethod<?>> showResults(long chatId, int messageId, long tournamentId, long userId) {
        Optional<PartnerPickPage> page = partnerPickService.pick(tournamentId, userId);
        if (page.isEmpty()) {
            return editFlow(chatId, messageId, texts.partnerPickFailed(), keyboards.mainMenu());
        }
        return editFlow(chatId, messageId, texts.partnerPick(page.get()),
                keyboards.partnerPickResult(tournamentId));
    }

    private SendMessage send(long chatId, String text, InlineKeyboardMarkup markup) {
        return SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("HTML")
                .replyMarkup(markup)
                .build();
    }

    private List<BotApiMethod<?>> editFlow(long chatId, int messageId, String text, InlineKeyboardMarkup markup) {
        List<BotApiMethod<?>> out = new ArrayList<>();
        if (messageId > 0) {
            out.add(EditMessageText.builder()
                    .chatId(String.valueOf(chatId))
                    .messageId(messageId)
                    .text(text)
                    .parseMode("HTML")
                    .replyMarkup(markup)
                    .build());
        } else {
            out.add(send(chatId, text, markup));
        }
        return out;
    }
}
