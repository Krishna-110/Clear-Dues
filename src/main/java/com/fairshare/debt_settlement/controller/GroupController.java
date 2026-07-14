package com.fairshare.debt_settlement.controller;

import com.fairshare.debt_settlement.dto.CreateGroupRequest;
import com.fairshare.debt_settlement.dto.JoinGroupRequest;
import com.fairshare.debt_settlement.model.Group;
import com.fairshare.debt_settlement.service.GroupService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@AllArgsConstructor
public class GroupController {

    private final GroupService groupService;

    @PostMapping
    public ResponseEntity<Group> create(@RequestBody CreateGroupRequest request) {
        return ResponseEntity.ok(groupService.create(request.getName(), currentUserEmail()));
    }

    @PostMapping("/join")
    public ResponseEntity<Group> join(@RequestBody JoinGroupRequest request) {
        return ResponseEntity.ok(groupService.joinByCode(request.getCode(), currentUserEmail()));
    }

    @GetMapping
    public ResponseEntity<List<Group>> myGroups() {
        return ResponseEntity.ok(groupService.listFor(currentUserEmail()));
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leave(@PathVariable Long id) {
        groupService.leave(id, currentUserEmail());
        return ResponseEntity.noContent().build();
    }

    private String currentUserEmail() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        return principal.toString();
    }
}
