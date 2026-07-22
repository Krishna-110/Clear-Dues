package com.fairshare.debt_settlement;

import com.fairshare.debt_settlement.model.Group;
import com.fairshare.debt_settlement.model.Person;
import com.fairshare.debt_settlement.repository.DebtRepository;
import com.fairshare.debt_settlement.repository.GroupRepository;
import com.fairshare.debt_settlement.repository.PersonRepository;
import com.fairshare.debt_settlement.service.GroupService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class GroupServiceTest {

    private GroupRepository groupRepository;
    private PersonRepository personRepository;
    private DebtRepository debtRepository;
    private GroupService groupService;

    private Person owner;

    @BeforeEach
    void setup() {
        groupRepository = mock(GroupRepository.class);
        personRepository = mock(PersonRepository.class);
        debtRepository = mock(DebtRepository.class);
        groupService = new GroupService(groupRepository, personRepository, debtRepository);

        owner = person(1L, "Owner", "owner@x.com");
        when(personRepository.findByEmail("owner@x.com")).thenReturn(Optional.of(owner));
        when(groupRepository.save(any(Group.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Person person(Long id, String name, String email) {
        Person p = new Person();
        p.setId(id);
        p.setName(name);
        p.setEmail(email);
        return p;
    }

    @Test
    void create_generatesCodeAndAddsOwnerAsMember() {
        when(groupRepository.existsByJoinCode(anyString())).thenReturn(false);

        Group g = groupService.create("Goa Trip", "owner@x.com");

        assertThat(g.getName()).isEqualTo("Goa Trip");
        assertThat(g.getOwner()).isEqualTo(owner);
        assertThat(g.getMembers()).contains(owner);
        assertThat(g.getJoinCode()).hasSize(6);
        assertThat(g.getCreatedAt()).isNotNull();
    }

    @Test
    void joinByCode_addsTheUserAsMember() {
        Group g = new Group();
        g.setId(10L);
        g.setJoinCode("ABC234");
        g.getMembers().add(owner);
        when(groupRepository.findByJoinCode("ABC234")).thenReturn(Optional.of(g));
        Person joiner = person(2L, "Joiner", "joiner@x.com");
        when(personRepository.findByEmail("joiner@x.com")).thenReturn(Optional.of(joiner));

        Group res = groupService.joinByCode("abc234", "joiner@x.com"); // lower-case is normalized

        assertThat(res.getMembers()).contains(joiner);
    }

    @Test
    void joinByCode_unknownCode_isRejected() {
        when(groupRepository.findByJoinCode("ZZZZZZ")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> groupService.joinByCode("zzzzzz", "owner@x.com"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void leave_lastMemberDeletesTheGroupAndClearsTaggedDebts() {
        Group g = new Group();
        g.setId(10L);
        g.getMembers().add(owner);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(g));

        groupService.leave(10L, "owner@x.com");

        verify(debtRepository).clearGroupReferences(10L);
        verify(groupRepository).delete(g);
    }

    @Test
    void leave_ownerLeavesButOthersRemain_reassignsOwnership() {
        Person otherMember = person(2L, "Other", "other@x.com");
        Group g = new Group();
        g.setId(10L);
        g.setOwner(owner);
        g.getMembers().add(owner);
        g.getMembers().add(otherMember);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(g));

        groupService.leave(10L, "owner@x.com");

        assertThat(g.getMembers()).doesNotContain(owner);
        assertThat(g.getOwner()).isEqualTo(otherMember); // ownership reassigned, not left dangling
        verify(groupRepository).save(g);
        verify(groupRepository, never()).delete(any());
    }

    @Test
    void leave_nonOwnerMemberLeaves_ownershipUnchanged() {
        Person otherMember = person(2L, "Other", "other@x.com");
        Group g = new Group();
        g.setId(10L);
        g.setOwner(owner);
        g.getMembers().add(owner);
        g.getMembers().add(otherMember);
        when(personRepository.findByEmail("other@x.com")).thenReturn(Optional.of(otherMember));
        when(groupRepository.findById(10L)).thenReturn(Optional.of(g));

        groupService.leave(10L, "other@x.com");

        assertThat(g.getMembers()).containsExactly(owner);
        assertThat(g.getOwner()).isEqualTo(owner);
    }

    @Test
    void listFor_returnsTheUsersGroups() {
        Group g = new Group();
        g.setId(10L);
        when(groupRepository.findByMembers_Id(1L)).thenReturn(List.of(g));

        assertThat(groupService.listFor("owner@x.com")).containsExactly(g);
    }
}
