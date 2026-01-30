package com.hyrinth.backend.storage.project;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class LocalProjectStorage implements ProjectStorage {

    private final Map<String, ProjectRecord> projects = new ConcurrentHashMap<>();

    @Override
    public Optional<ProjectRecord> findBySlug(String slug) {
        return Optional.ofNullable(projects.get(slug));
    }

    @Override
    public List<ProjectRecord> findPage(int limit, int offset) {
        if (limit <= 0 || offset < 0) {
            return List.of();
        }
        return projects.values().stream()
                .sorted(Comparator.comparingLong(ProjectRecord::createdAt)
                        .thenComparing(ProjectRecord::slug))
                .skip(offset)
                .limit(limit)
                .toList();
    }

    @Override
    public boolean create(ProjectRecord record) {
        if (record == null || record.slug() == null || record.slug().isBlank()) {
            return false;
        }
        if (projects.containsKey(record.slug())) {
            return false;
        }
        projects.put(record.slug(), record);
        return true;
    }

    @Override
    public boolean update(ProjectRecord record) {
        if (record == null || record.slug() == null || record.slug().isBlank()) {
            return false;
        }
        if (!projects.containsKey(record.slug())) {
            return false;
        }
        projects.put(record.slug(), record);
        return true;
    }

    @Override
    public boolean delete(String slug) {
        return projects.remove(slug) != null;
    }
}
