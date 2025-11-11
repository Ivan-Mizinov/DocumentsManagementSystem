package db.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

public abstract class BaseDAO<T> {
    protected SessionFactory sessionFactory;

    public BaseDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    protected Session getSession() {
        return sessionFactory.openSession();
    }

    protected void commitTransaction(Transaction tx, Session session) {
        try (session) {
            tx.commit();
        } catch (Exception e) {
            tx.rollback();
            throw new RuntimeException(e);
        }
    }

    public T save(T entity) {
        Session session = getSession();
        Transaction tx = session.beginTransaction();
        session.persist(entity);
        commitTransaction(tx, session);
        return entity;
    }

    public T update(T entity) {
        Session session = getSession();
        Transaction tx = session.beginTransaction();
        session.merge(entity);
        commitTransaction(tx, session);
        return entity;
    }

    public void delete(T entity) {
        Session session = getSession();
        Transaction tx = session.beginTransaction();
        session.remove(entity);
        commitTransaction(tx, session);
    }

    public T findById(Class<T> clazz, Long id) {
        try (Session session = getSession()) {
            return session.find(clazz, id);
        }
    }
}

