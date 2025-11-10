package ru.synergy.db.entities;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "headings")
public class Heading {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;

    @Column(nullable = false)
    private Integer level;

    @Column(nullable = false)
    private String text;

    @Column(nullable = false)
    private Integer order;
}

