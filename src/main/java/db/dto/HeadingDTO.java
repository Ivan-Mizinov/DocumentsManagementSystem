package db.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HeadingDTO {
    private Long id;
    private Long pageId;
    private Integer level;
    private String text;
    private Integer position;
}