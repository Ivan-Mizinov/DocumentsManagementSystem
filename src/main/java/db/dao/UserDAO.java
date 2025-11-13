package db.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import db.entities.User;
import db.util.RedisCacheUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class UserDAO extends BaseDAO<User> {
    private static final String ALL_USERS_KEY = "user:all";
    private static final String USERNAME_KEY_TEMPLATE = "user:username:%s";
    private static final TypeReference<List<User>> USER_LIST_TYPE = new TypeReference<>() {};

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
            cacheEntity(existingUser);
            return existingUser;
        }
        User saved = super.save(user);
        RedisCacheUtil.evict(ALL_USERS_KEY);
        RedisCacheUtil.cacheValue(usernameKey(saved.getUsername()), saved);
        return saved;
    }

    @Override
    public User update(User user) {
        User updated = super.update(user);
        RedisCacheUtil.evict(ALL_USERS_KEY);
        if (updated.getUsername() != null) {
            RedisCacheUtil.cacheValue(usernameKey(updated.getUsername()), updated);
        }
        return updated;
    }

    @Override
    public void delete(User user) {
        String username = user.getUsername();
        super.delete(user);
        RedisCacheUtil.evict(ALL_USERS_KEY);
        if (username != null) {
            RedisCacheUtil.evict(usernameKey(username));
        }
    }

    public User findByUsername(String username) {
        String key = usernameKey(username);
        User cached = RedisCacheUtil.getValue(key, User.class);
        if (cached != null) {
            return cached;
        }
        try (Session session = getSession()) {
            User user = session.createQuery("FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .uniqueResult();
            if (user != null) {
                RedisCacheUtil.cacheValue(key, user);
                cacheEntity(user);
            }
            return user;
        }
    }


    public List<User> getAllUsers() {
        List<User> cached = RedisCacheUtil.getValue(ALL_USERS_KEY, USER_LIST_TYPE);
        if (cached != null) {
            return cached;
        }
        try (Session session = getSession()) {
            List<User> users = session.createQuery("FROM User", User.class).list();
            RedisCacheUtil.cacheValue(ALL_USERS_KEY, users);
            return users;
        }
    }

    private String usernameKey(String username) {
        return String.format(USERNAME_KEY_TEMPLATE, username);
    }
}