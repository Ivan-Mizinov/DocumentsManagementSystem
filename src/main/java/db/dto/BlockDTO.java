package db.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BlockDTO {
    private Long id;
    private Long pageId;
    private String type;
    private String content;
    private Integer position;
    private boolean isVisible;
}