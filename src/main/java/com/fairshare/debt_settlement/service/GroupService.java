package com.fairshare.debt_settlement.service;

import com.fairshare.debt_settlement.model.Group;
import com.fairshare.debt_settlement.model.Person;
import com.fairshare.debt_settlement.repository.GroupRepository;
import com.fairshare.debt_settlement.repository.PersonRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class GroupService {

    // Unambiguous alphabet (no 0/O/1/I) for the join code.
    private static final char[] CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final GroupRepository groupRepository;
    private final PersonRepository personRepository;

    @Transactional
    public Group create(String name, String ownerEmail) {
        Person owner = requirePerson(ownerEmail);
        Group group = new Group();
        group.setName(name == null || name.isBlank() ? "My Group" : name.trim());
        group.setOwner(owner);
        group.setJoinCode(generateUniqueCode());
        group.setCreatedAt(LocalDateTime.now());
        group.getMembers().add(owner);
        return groupRepository.save(group);
    }

    @Transactional
    public Group joinByCode(String code, String userEmail) {
        if (code == null) throw new IllegalArgumentException("A join code is required.");
        Group group = groupRepository.findByJoinCode(code.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("No group found for that code."));
        Person user = requirePerson(userEmail);
        group.getMembers().add(user); // Set -> no-op if already a member
        return groupRepository.save(group);
    }

    public List<Group> listFor(String userEmail) {
        Person user = requirePerson(userEmail);
        return groupRepository.findByMembers_Id(user.getId());
    }

    @Transactional
    public void leave(Long groupId, String userEmail) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found."));
        Person user = requirePerson(userEmail);
        group.getMembers().removeIf(m -> m.getId().equals(user.getId()));
        if (group.getMembers().isEmpty()) {
            groupRepository.delete(group); // last member out -> remove the empty group
        } else {
            groupRepository.save(group);
        }
    }

    private Person requirePerson(String email) {
        return personRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 25; attempt++) {
            StringBuilder sb = new StringBuilder(6);
            for (int i = 0; i < 6; i++) {
                sb.append(CODE_ALPHABET[RANDOM.nextInt(CODE_ALPHABET.length)]);
            }
            String code = sb.toString();
            if (!groupRepository.existsByJoinCode(code)) return code;
        }
        throw new IllegalStateException("Could not generate a unique join code.");
    }
}
