package com.hyrinth.backend.storage.team;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class LocalTeamStorage implements TeamStorage {

    private final Map<String, TeamRecord> teams = new ConcurrentHashMap<>();

    @Override
    public Optional<TeamRecord> findById(String id) {
        return Optional.ofNullable(teams.get(id));
    }

    @Override
    public List<TeamRecord> findPage(int limit, int offset) {
        if (limit <= 0 || offset < 0) {
            return List.of();
        }
        return teams.values().stream()
                .sorted(Comparator.comparingLong(TeamRecord::createdAt)
                        .thenComparing(TeamRecord::id))
                .skip(offset)
                .limit(limit)
                .toList();
    }

    @Override
    public boolean create(TeamRecord record) {
        if (record == null || record.id() == null || record.id().isBlank()) {
            return false;
        }
        if (teams.containsKey(record.id())) {
            return false;
        }
        teams.put(record.id(), record);
        return true;
    }

    @Override
    public boolean update(TeamRecord record) {
        if (record == null || record.id() == null || record.id().isBlank()) {
            return false;
        }
        if (!teams.containsKey(record.id())) {
            return false;
        }
        teams.put(record.id(), record);
        return true;
    }

    @Override
    public boolean delete(String id) {
        return teams.remove(id) != null;
    }
}
