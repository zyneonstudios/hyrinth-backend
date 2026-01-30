package com.hyrinth.backend.storage.session;

import java.util.Optional;

public interface AccountSessionStorage {

    Optional<AccountSessionRecord> findByToken(String token);

    boolean create(AccountSessionRecord session);

    boolean update(AccountSessionRecord session);

    boolean delete(String token);
}
