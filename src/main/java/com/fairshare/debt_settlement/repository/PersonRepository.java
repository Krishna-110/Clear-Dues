package com.fairshare.debt_settlement.repository;

import com.fairshare.debt_settlement.model.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {



    Optional<Person> findByEmail(String email);

    Optional<Person> findByPhoneNumber(String phoneNumber);

    java.util.List<Person> findAllByPhoneNumberIn(java.util.Collection<String> phoneNumbers);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query(value = "DELETE FROM person_friends WHERE person_id = :personId OR friend_id = :personId", nativeQuery = true)
    void removeAllFriendships(@org.springframework.data.repository.query.Param("personId") Long personId);

}
