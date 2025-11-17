package db.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PageVersionIndexDTO {
    private Long id;
    private Integer versionNumber;
    private String content;
}
