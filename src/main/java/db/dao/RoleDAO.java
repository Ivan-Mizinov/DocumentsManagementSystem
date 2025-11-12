package db.dao;

import db.entities.Role;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class RoleDAO extends BaseDAO<Role> {
    public RoleDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public List<Role> getAllRoles() {
        try (Session session = getSession()) {
            return session.createQuery("FROM Role", Role.class).list();
        }
    }

    public Role findByName(String roleName) {
        try (Session session = getSession()) {
            return session.createQuery("FROM Role r WHERE r.name = :roleName", Role.class)
                    .setParameter("roleName", roleName)
                    .uniqueResult();
        }
    }
}

