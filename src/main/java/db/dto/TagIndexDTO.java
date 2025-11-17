package db.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TagIndexDTO {
    private Long id;
    private String name;
    private String description;
}