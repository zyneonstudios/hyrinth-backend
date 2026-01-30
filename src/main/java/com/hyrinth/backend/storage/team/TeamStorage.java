package com.hyrinth.backend.storage.team;

import java.util.List;
import java.util.Optional;

public interface TeamStorage {

    Optional<TeamRecord> findById(String id);

    List<TeamRecord> findPage(int limit, int offset);

    boolean create(TeamRecord record);

    boolean update(TeamRecord record);

    boolean delete(String id);
}
