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
        try {
            tx.commit();
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw new RuntimeException(e);
        } finally {
            if (session != null && session.isOpen()) {
                session.close();
            }
        }
    }

    public T save(T entity) {
        Session session = getSession();
        Transaction tx = session.beginTransaction();
        try {
            if (session.contains(entity)) {
                throw new RuntimeException("Сущность " + entity.getClass().getSimpleName() + " уже присутствует в сессии");
            }

            Object identifier = null;
            try {
                identifier = session.getIdentifier(entity);
            } catch (IllegalArgumentException ignored) {
            }

            if (identifier != null) {
                Object existing = session.find(entity.getClass(), identifier);
                if (existing != null) {
                    throw new RuntimeException("Сущность " + entity.getClass().getSimpleName() + " с id " + identifier + " уже существует");
                }
            }

            session.persist(entity);
            commitTransaction(tx, session);
            return entity;
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (Exception ignored) {
                }
            }
            if (session.isOpen()) {
                session.close();
            }
            throw e;
        } catch (Exception e) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (Exception ignored) {
                }
            }
            if (session != null && session.isOpen()) {
                session.close();
            }
            throw new RuntimeException(e);
        }
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

