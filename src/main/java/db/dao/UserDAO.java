package db.dao;

import db.entities.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class UserDAO extends BaseDAO<User> {
    public UserDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public User save(User user) {
        if (user == null || user.getUsername() == null) {
            throw new RuntimeException("Имя пользователя обязательно для сохранения");
        }
        String username = user.getUsername();
        User existingUser = findByUsername(username);
        if (existingUser != null) {
            System.out.println("Пользователь '" + username + "' уже существует. Возвращаем существующую запись.");
            return existingUser;
        }
        return super.save(user);
    }

    public User findByUsername(String username) {
        try (Session session = getSession()) {
            return session.createQuery("FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .uniqueResult();
        }
    }


    public List<User> getAllUsers() {
        try (Session session = getSession()) {
            return session.createQuery("FROM User", User.class).list();
        }
    }
}

