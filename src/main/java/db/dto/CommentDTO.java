package db.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDTO {
    private Long id;
    private Long pageVersionId;
    private Long authorId;
    private String text;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean resolved;
}