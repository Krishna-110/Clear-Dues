package com.fairshare.debt_settlement.repository;

import com.fairshare.debt_settlement.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {

    Optional<Group> findByJoinCode(String joinCode);

    // All groups the given person is a member of.
    List<Group> findByMembers_Id(Long personId);

    boolean existsByJoinCode(String joinCode);
}
