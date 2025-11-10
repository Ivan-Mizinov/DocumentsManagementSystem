package db.dao;

import db.entities.Page;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;

public class PageDAO {
    private SessionFactory sessionFactory;

    public PageDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
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

    public void save(Page page) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            session.persist(page);
            transaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void update(Page page) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            session.merge(page);
            transaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void delete(Page page) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            session.remove(page);
            transaction.commit();
        }
    }
}
