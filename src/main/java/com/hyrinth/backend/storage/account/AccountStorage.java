package com.hyrinth.backend.storage.account;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface AccountStorage {

    Optional<AccountRecord> findById(String id);

    Optional<AccountRecord> findByEmail(String email);

    Optional<AccountRecord> findByUsername(String username);

    List<AccountRecord> findPage(int limit, int offset);

    boolean hasAdminAccount();

    default List<AccountRecord> findAll(int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be > 0");
        }
        List<AccountRecord> all = new ArrayList<>();
        int offset = 0;
        while (true) {
            List<AccountRecord> page = findPage(pageSize, offset);
            if (page.isEmpty()) {
                break;
            }
            all.addAll(page);
            offset += page.size();
            if (page.size() < pageSize) {
                break;
            }
        }
        return all;
    }

    boolean create(AccountRecord record);

    boolean updatePasswordHash(String id, String passwordHash, long updatedAt);

    boolean delete(String id);
}
