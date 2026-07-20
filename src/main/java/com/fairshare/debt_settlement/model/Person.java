package com.fairshare.debt_settlement.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "persons")
@Data
@NoArgsConstructor
public class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(unique = true)
    private String phoneNumber;

    // Profile picture (from Google on login).
    private String pictureUrl;

    // Privacy + notification preferences.
    // @ColumnDefault ensures the ALTER TABLE that adds this NOT NULL column also backfills a
    // default for EXISTING rows - without it, Postgres rejects adding a NOT NULL column to a
    // non-empty table (the Java "= false" default only applies to newly-created objects).
    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean hidePhone = false;

    @Column(nullable = false)
    @ColumnDefault("false")
    private boolean hideEmail = false;

    @Column(nullable = false)
    @ColumnDefault("true")
    private boolean notificationsEnabled = true;

    @ManyToMany
    @JoinTable(
        name = "person_friends",
        joinColumns = @JoinColumn(name = "person_id"),
        inverseJoinColumns = @JoinColumn(name = "friend_id")
    )
    @com.fasterxml.jackson.annotation.JsonIgnore
    @lombok.EqualsAndHashCode.Exclude
    @lombok.ToString.Exclude
    private java.util.Set<Person> friends = new java.util.HashSet<>();

}
