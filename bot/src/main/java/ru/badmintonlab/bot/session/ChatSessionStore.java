package ru.badmintonlab.bot.session;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatSessionStore {

    private final Map<Long, ChatSession> sessions = new ConcurrentHashMap<>();

    public Optional<ChatSession> get(long chatId) {
        return Optional.ofNullable(sessions.get(chatId));
    }

    public void put(long chatId, ChatSession session) {
        sessions.put(chatId, session);
    }

    public void clear(long chatId) {
        sessions.remove(chatId);
    }
}
