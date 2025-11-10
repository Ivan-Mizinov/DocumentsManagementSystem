package ru.synergy.db.entities;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Set;

@Data
@Entity
@Table(name = "tags")
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    @ManyToMany(mappedBy = "tags", fetch = FetchType.LAZY)
    private Set<Page> pages;
}

