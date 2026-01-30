package com.hyrinth.backend.storage.session;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class LocalAccountSessionStorage implements AccountSessionStorage {

    private final Map<String, AccountSessionRecord> sessions = new ConcurrentHashMap<>();

    @Override
    public Optional<AccountSessionRecord> findByToken(String token) {
        return Optional.ofNullable(sessions.get(token));
    }

    @Override
    public boolean create(AccountSessionRecord session) {
        sessions.put(session.token(), session);
        return true;
    }

    @Override
    public boolean update(AccountSessionRecord session) {
        sessions.put(session.token(), session);
        return true;
    }

    @Override
    public boolean delete(String token) {
        return sessions.remove(token) != null;
    }
}
