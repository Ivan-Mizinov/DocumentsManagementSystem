package db.dao;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import db.entities.Page;
import db.util.ElasticsearchUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class SearchDAO {
    private SessionFactory sessionFactory;
    private final ElasticsearchClient esClient;

    public SearchDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
        this.esClient = ElasticsearchUtil.getClient();
    }

    /*
    public List<Page> searchByTitleOrTag(String query) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(
                            "SELECT DISTINCT p FROM Page p " +
                                    "LEFT JOIN p.tags t " +
                                    "WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
                                    "   OR LOWER(t.name) LIKE LOWER(CONCAT('%', :query, '%'))", Page.class)
                    .setParameter("query", query)
                    .list();
        }
    }
    */

    public List<Page> searchByTitleOrTag(String query) {
        try {
            Query queryBuilder = Query.of(q -> q
                    .bool(b -> b
                            .should(Query.of(q1 -> q1
                                    .match(m -> m
                                            .field("title")
                                            .query(query.toLowerCase())
                                    )
                            ))
                            .should(Query.of(q2 -> q2
                                    .nested(n -> n
                                            .path("tags")
                                            .query(Query.of(q3 -> q3
                                                    .match(m -> m
                                                            .field("tags.name")
                                                            .query(query.toLowerCase())
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
            System.out.println("Совпадений не найдено");
            return List.of();
        }
    }

    /*
    public List<Page> searchByContent(String query) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(
                            "SELECT DISTINCT p FROM Page p " +
                                    "JOIN p.versions v " +
                                    "WHERE LOWER(v.content) LIKE LOWER(CONCAT('%', :query, '%'))", Page.class)
                    .setParameter("query", query)
                    .list();
        }
    }
    */

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

}

