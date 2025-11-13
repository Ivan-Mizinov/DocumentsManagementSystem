package db.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageVersionDTO {
    private Long id;
    private Long pageId;
    private Integer versionNumber;
    private String content;
    private Long changedById;
    private LocalDateTime changedAt;
    private boolean isPublished;
}