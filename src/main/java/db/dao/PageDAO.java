package db.dao;

import db.entities.Page;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class PageDAO extends BaseDAO<Page> {
    public PageDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public List<Page> findAll() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM Page", Page.class).list();
        }
    }

    public Page findBySlug(String slug) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM Page p WHERE p.slug = :slug", Page.class)
                    .setParameter("slug", slug)
                    .uniqueResult();
        }
    }
}
