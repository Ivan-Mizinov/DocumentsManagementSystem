package db.dao;

import db.entities.Role;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;

public class RoleDAO {
    private SessionFactory sessionFactory;

    public RoleDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Role findById(Long id) {
        try (Session session = sessionFactory.openSession()) {
            return session.find(Role.class, id);
        }
    }

    public List<Role> getAllRoles() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM Role", Role.class).list();
        }
    }

    public Role save(Role role) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            session.persist(role);
            transaction.commit();
            return role;
        }
    }

    public void update(Role role) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            session.merge(role);
            transaction.commit();
        }
    }

    public void delete(Role role) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            session.remove(role);
            transaction.commit();
        }
    }
}

