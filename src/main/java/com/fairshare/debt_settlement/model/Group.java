package com.fairshare.debt_settlement.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "groups")
@Data
@NoArgsConstructor
public class Group {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // Short shareable code others use to join.
    @Column(nullable = false, unique = true, length = 12)
    private String joinCode;

    @ManyToOne
    @JsonIgnoreProperties({"friends", "phoneNumber", "email", "pictureUrl", "hidePhone", "hideEmail", "notificationsEnabled"})
    private Person owner;

    @ManyToMany
    @JoinTable(
            name = "group_members",
            joinColumns = @JoinColumn(name = "group_id"),
            inverseJoinColumns = @JoinColumn(name = "person_id")
    )
    @JsonIgnoreProperties({"friends", "phoneNumber", "email", "pictureUrl", "hidePhone", "hideEmail", "notificationsEnabled"})
    private Set<Person> members = new HashSet<>();

    private LocalDateTime createdAt;
}
