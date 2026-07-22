package com.fairshare.debt_settlement.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

// Backtick-quoted so Hibernate always escapes this identifier per-dialect: "GROUPS" is a
// reserved keyword in MySQL 8+ (window-function syntax), which broke DDL like
// "ALTER TABLE groups ADD CONSTRAINT ... FOREIGN KEY" with a SQL syntax error. Postgres (the
// production database) doesn't reserve the plural "groups", so this only surfaced against the
// local/test MySQL datasource - but it's a real, previously-silent schema error either way.
@Entity
@Table(name = "`groups`")
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
