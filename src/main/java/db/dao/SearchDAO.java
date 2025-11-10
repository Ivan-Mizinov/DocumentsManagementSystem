package db.dao;

import db.entities.Page;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class SearchDAO {
    private SessionFactory sessionFactory;

    public SearchDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

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
}

