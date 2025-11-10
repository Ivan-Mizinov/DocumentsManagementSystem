package db.dao;

import db.entities.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class UserDAO extends BaseDAO<User> {
    public UserDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public User findByUsername(String username) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .uniqueResult();
        }
    }

    public List<User> getAllUsers() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM User", User.class).list();
        }
    }
}

