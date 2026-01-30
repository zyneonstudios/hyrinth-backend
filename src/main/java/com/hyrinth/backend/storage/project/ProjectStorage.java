package com.hyrinth.backend.storage.project;

import java.util.List;
import java.util.Optional;

public interface ProjectStorage {

    Optional<ProjectRecord> findBySlug(String slug);

    List<ProjectRecord> findPage(int limit, int offset);

    boolean create(ProjectRecord record);

    boolean update(ProjectRecord record);

    boolean delete(String slug);
}
