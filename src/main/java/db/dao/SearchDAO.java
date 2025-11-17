package db.dao;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.ObjectMapper;
import db.dto.PageIndexDTO;
import db.dto.PageVersionIndexDTO;
import db.dto.TagIndexDTO;
import db.entities.Page;
import org.hibernate.Hibernate;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SearchDAO {
    private final ElasticsearchClient esClient;
    private final ObjectMapper objectMapper;

    public SearchDAO(ElasticsearchClient esClient) {
        this.esClient = esClient;
        this.objectMapper = new ObjectMapper();
    }

    public List<Page> searchByTitleOrTag(String query) {
        try {
            Query queryBuilder = Query.of(q -> q
                    .bool(b -> b
                            .should(Query.of(q1 -> q1
                                    .match(m -> m
                                            .field("title")
                                            .query(query.toLowerCase())
                                            .fuzziness("AUTO")
                                    )
                            ))
                            .should(Query.of(q2 -> q2
                                    .nested(n -> n
                                            .path("tags")
                                            .query(Query.of(q3 -> q3
                                                    .match(m -> m
                                                            .field("tags.name")
                                                            .query(query.toLowerCase())
                                                            .fuzziness("AUTO")
                                                    )
                                            ))
                                    )
                            ))
                    )
            );

            SearchResponse<Page> response = esClient.search(s -> s
                            .index("pages")
                            .query(queryBuilder)
                            .size(100),
                    Page.class
            );

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .toList();
        } catch (Exception e) {
            System.err.println("Ошибка поиска: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    public List<Page> searchByContent(String query) {
        try {
            Query queryBuilder = Query.of(q -> q
                    .nested(n -> n
                            .path("versions")
                            .query(Query.of(q1 -> q1
                                    .match(m -> m
                                            .field("versions.content")
                                            .query(query.toLowerCase())
                                    )
                            ))
                    )
            );

            SearchResponse<Page> response = esClient.search(s -> s
                            .index("pages")
                            .query(queryBuilder)
                            .size(100),
                    Page.class
            );

            return response.hits().hits().stream()
                    .map(Hit::source)
                    .toList();

        } catch (Exception e) {
            System.out.println("Совпадений не найдено");
            return List.of();
        }
    }

    public void createIndexWithMapping() {
        try {
            boolean indexExists = esClient.indices().exists(e -> e.index("pages")).value();
            if (indexExists) {
                System.out.println("Индекс 'pages' уже существует, создание пропущено");
                return;
            }

            InputStream is = getClass().getResourceAsStream(
                    ("/pages-mapping.json"));
            String mappingJson;
            try (is) {
                assert is != null;
                mappingJson = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }

            esClient.indices().create(c -> c
                    .index("pages")
                    .withJson(Reader.of(mappingJson))
            );
            System.out.println("Индекс 'pages' создан с mapping");
        } catch (Exception e) {
            System.err.println("Ошибка при создании индекса: " + e.getMessage());
        }
    }

    public void indexPage(Page page) {
        try {
            initializeLazyCollections(page);
            PageIndexDTO indexedPage = convertToDTO(page);
            String json = objectMapper.writeValueAsString(indexedPage);
            System.out.println("JSON для индексации: " + json);
            var response = esClient.index(i -> i
                    .index("pages")
                    .id(String.valueOf(page.getId()))
                    .withJson(Reader.of(json))
            );
            esClient.indices().refresh(r -> r.index("pages"));
            System.out.println("Статус индексации: " + response.result());
        } catch (Exception e) {
            System.err.println("Ошибка при индексации: " + e.getMessage());
        }
    }

    private void initializeLazyCollections(Page page) {
        if (page.getVersions() != null) {
            Hibernate.initialize(page.getVersions());
        }
        if (page.getTags() != null) {
            Hibernate.initialize(page.getTags());
        }
    }

    private PageIndexDTO convertToDTO(Page page) {
        PageIndexDTO dto = new PageIndexDTO();
        dto.setId(page.getId());
        dto.setTitle(page.getTitle());
        dto.setSlug(page.getSlug());

        if (page.getTags() != null) {
            dto.setTags(page.getTags().stream()
                    .filter(Objects::nonNull)
                    .map(tag -> new TagIndexDTO(
                            tag.getId(),
                            tag.getName(),
                            tag.getDescription()
                    ))
                    .collect(Collectors.toList()));
        }

        if (page.getVersions() != null) {
            dto.setVersions(page.getVersions().stream()
                    .filter(Objects::nonNull)
                    .map(version -> new PageVersionIndexDTO(
                            version.getId(),
                            version.getVersionNumber(),
                            version.getContent()
                    ))
                    .collect(Collectors.toList()));
        }

        return dto;
    }

    public void deletePageFromIndex(Long pageId) throws IOException {
        esClient.delete(d -> d
                .index("pages")
                .id(String.valueOf(pageId))
        );
    }
}

