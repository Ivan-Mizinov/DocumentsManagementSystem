package ru.synergy.db.entities;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "blocks")
public class Block {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "page_id", nullable = false)
    private Page page;

    @Column(nullable = false)
    private String type;

    @Lob
    @Column(columnDefinition = "JSONB")
    private String content;

    @Column(nullable = false)
    private Integer order;

    @Column(nullable = false)
    private boolean isVisible;
}
