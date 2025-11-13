package db.entities;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Entity
@Table(name = "pages")
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(unique = true, nullable = false)
    private String slug;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 10)
    @JsonManagedReference(value = "page-versions")
    private List<PageVersion> versions;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 10)
    private List<Heading> headings;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 10)
    private List<Block> blocks;

    @OneToMany(mappedBy = "page", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 10)
    private List<Link> links;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "page_tags",
            joinColumns = @JoinColumn(name = "page_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @BatchSize(size = 10)
    private Set<Tag> tags;
}

