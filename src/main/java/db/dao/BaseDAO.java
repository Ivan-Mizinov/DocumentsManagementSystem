package db.dao;

import db.util.RedisCacheUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.io.Serializable;
import java.lang.reflect.Method;

public abstract class BaseDAO<T, D> {
    protected SessionFactory sessionFactory;

    public BaseDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    protected abstract D entityToDTO(T entity);

    protected abstract T dtoToEntity(D dto);

    protected abstract Class<D> getDTOClass();

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
            Long entityId = extractId(entity);
            if (entityId != null) {
                Object existing = session.find(entity.getClass(), entityId);
                if (existing != null) {
                    throw new RuntimeException(
                            "Сущность уже есть существует"
                    );
                }
            }

            T managedEntity = session.merge(entity);
            if (!session.contains(managedEntity)) {
                throw new RuntimeException(
                        "Не удалось присоединить сущность к сессии после merge()"
                );
            }

            Object identifier = session.getIdentifier(managedEntity);
            if (identifier == null) {
                throw new RuntimeException(
                        "Не удалось получить идентификатор сущности после сохранения"
                );
            }
            if (!(identifier instanceof Long)) {
                throw new RuntimeException(
                        "Идентификатор сущности должен быть типа Long, но получен: " +
                                identifier.getClass()
                );
            }

            commitTransaction(tx, session);
            cacheEntity(managedEntity);
            return managedEntity;
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
        D cachedDTO = RedisCacheUtil.getValue(entityKey(clazz, id), getDTOClass());
        if (cachedDTO != null) {
            return dtoToEntity(cachedDTO);
        }
        try (Session session = getSession()) {
            T entity = session.find(clazz, id);
            if (entity != null) {
                cacheEntity(entity);
            }
            return entity;
        }
    }

    protected void evictEntity(Class<?> clazz, Long id) {
        RedisCacheUtil.evict(entityKey(clazz, id));
    }

    protected void cacheEntity(T entity) {
        Long id = extractId(entity);
        if (id != null) {
            D dto = entityToDTO(entity);
            RedisCacheUtil.cacheValue(entityKey(getDTOClass(), id), dto);
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
            } else {
                if (value == null) {
                    System.out.println("getId() вернул null");
                } else {
                    System.out.println("getId() вернул не Long: " + value.getClass());
                }
                return null;
            }
        } catch (Exception e) {
            System.out.println("Не удалось получить ID сущности: " + e.getMessage());
        }
        return null;
    }
}

