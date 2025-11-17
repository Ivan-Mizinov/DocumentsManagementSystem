package db.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PageIndexDTO {
    private Long id;
    private String title;
    private String slug;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("tags")
    private List<TagIndexDTO> tags;

    @JsonInclude(JsonInclude.Include.ALWAYS)
    @JsonProperty("versions")
    private List<PageVersionIndexDTO> versions;
}
