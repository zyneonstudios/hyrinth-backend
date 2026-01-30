package com.hyrinth.backend.storage.account;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class LocalAccountStorage implements AccountStorage {

    private final Map<String, AccountRecord> accounts = new ConcurrentHashMap<>();

    @Override
    public Optional<AccountRecord> findById(String id) {
        return Optional.ofNullable(accounts.get(id));
    }

    @Override
    public Optional<AccountRecord> findByEmail(String email) {
        return accounts.values().stream()
                .filter(record -> record.email().equalsIgnoreCase(email))
                .findFirst();
    }

    @Override
    public Optional<AccountRecord> findByUsername(String username) {
        return accounts.values().stream()
                .filter(record -> record.username().equalsIgnoreCase(username))
                .findFirst();
    }

    @Override
    public List<AccountRecord> findPage(int limit, int offset) {
        if (limit <= 0 || offset < 0) {
            return List.of();
        }
        return accounts.values().stream()
                .sorted(Comparator.comparingLong(AccountRecord::createdAt)
                        .thenComparing(AccountRecord::id))
                .skip(offset)
                .limit(limit)
                .toList();
    }

    @Override
    public boolean hasAdminAccount() {
        return accounts.values().stream().anyMatch(AccountRecord::isAdmin);
    }

    @Override
    public boolean create(AccountRecord record) {
        if (findByEmail(record.email()).isPresent() || findByUsername(record.username()).isPresent()) {
            return false;
        }
        accounts.put(record.id(), record);
        return true;
    }

    @Override
    public boolean updatePasswordHash(String id, String passwordHash, long updatedAt) {
        AccountRecord record = accounts.get(id);
        if (record == null) {
            return false;
        }
        accounts.put(id, new AccountRecord(
                record.id(),
                record.email(),
                record.username(),
                record.profilePicture(),
                record.isHidden(),
                passwordHash,
                record.isAdmin(),
                record.permissions(),
                record.projects(),
                record.teams(),
                record.createdAt(),
                updatedAt
        ));
        return true;
    }

    @Override
    public boolean delete(String id) {
        return accounts.remove(id) != null;
    }

}
