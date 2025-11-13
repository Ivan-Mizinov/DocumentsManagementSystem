package db.dao;

import db.util.RedisCacheUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.lang.reflect.Method;

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
            } catch (IllegalArgumentException e) {
                System.out.println("Не удалось получить идентификатор сущности: " + e.getMessage());
            }

            if (identifier != null) {
                Object existing = session.find(entity.getClass(), identifier);
                if (existing != null) {
                    throw new RuntimeException("Сущность " + entity.getClass().getSimpleName() + " с id " + identifier + " уже существует");
                }
            }

            session.persist(entity);
            commitTransaction(tx, session);
            cacheEntity(entity);
            return entity;
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                try {
                    tx.rollback();
                } catch (Exception rollbackException) {
                    System.out.println("ОШИБКА при откате транзакции: " + rollbackException.getMessage());
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
                } catch (Exception rollbackException) {
                    System.out.println("ОШИБКА при откате транзакции: " + rollbackException.getMessage());
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
        cacheEntity(entity);
        return entity;
    }

    public void delete(T entity) {
        Session session = getSession();
        Transaction tx = session.beginTransaction();
        Long entityId = extractId(entity);
        session.remove(entity);
        commitTransaction(tx, session);
        if (entityId != null) {
            evictEntity(entity.getClass(), entityId);
        }
    }

    public T findById(Class<T> clazz, Long id) {
        T cached = RedisCacheUtil.getValue(entityKey(clazz, id), clazz);
        if (cached != null) {
            return cached;
        }
        try (Session session = getSession()) {
            return session.find(clazz, id);
        }
    }

    protected void evictEntity(Class<?> clazz, Long id) {
        RedisCacheUtil.evict(entityKey(clazz, id));
    }

    protected void cacheEntity(T entity) {
        Long id = extractId(entity);
        if (id != null) {
            RedisCacheUtil.cacheValue(entityKey(entity.getClass(), id), entity);
        }
    }

    private String entityKey(Class<?> clazz, Long id) {
        return clazz.getSimpleName().toLowerCase() + ":id:" + id;
    }

    private Long extractId(T entity) {
        if (entity == null) {
            return null;
        }
        try {
            Method getId = entity.getClass().getMethod("getId");
            Object value = getId.invoke(entity);
            if (value instanceof Long) {
                return (Long) value;
            }
        } catch (Exception e) {
            System.out.println("Не удалось получить ID сущности: " + e.getMessage());
        }
        return null;
    }
}

