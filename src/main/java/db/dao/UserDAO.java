package db.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import db.dto.UserDTO;
import db.entities.Role;
import db.entities.User;
import db.util.RedisCacheUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.stream.Collectors;

public class UserDAO extends BaseDAO<User, UserDTO> {
    private static final String ALL_USERS_KEY = "user:all";
    private static final String USERNAME_KEY_TEMPLATE = "user:username:%s";
    private static final TypeReference<List<UserDTO>> USER_LIST_TYPE = new TypeReference<>() {
    };

    public UserDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    protected UserDTO entityToDTO(User entity) {
        if (entity == null) return null;
        Long roleId = entity.getRole() != null ? entity.getRole().getId() : null;
        String roleName = entity.getRole() != null ? entity.getRole().getName() : null;
        return new UserDTO(
                entity.getId(),
                entity.getUsername(),
                entity.getEmail(),
                entity.getPassword(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                roleId,
                roleName
        );
    }

    @Override
    protected User dtoToEntity(UserDTO dto) {
        if (dto == null) return null;
        User user = new User();
        user.setId(dto.getId());
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setCreatedAt(dto.getCreatedAt());
        user.setUpdatedAt(dto.getUpdatedAt());

        if (dto.getRoleId() != null) {
            try (Session session = getSession()) {
                Role role = session.find(Role.class, dto.getRoleId());
                if (role != null) {
                    user.setRole(role);
                } else {
                    throw new RuntimeException("Роль с ID " + dto.getRoleId() + " не найдена в БД");
                }
            }
        }
        return user;
    }

    @Override
    protected Class<UserDTO> getDTOClass() {
        return UserDTO.class;
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
        RedisCacheUtil.cacheValue(usernameKey(saved.getUsername()), entityToDTO(saved));
        return saved;
    }

    @Override
    public User update(User user) {
        User updated = super.update(user);
        RedisCacheUtil.evict(ALL_USERS_KEY);
        if (updated.getUsername() != null) {
            RedisCacheUtil.cacheValue(usernameKey(updated.getUsername()), entityToDTO(updated));
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
        UserDTO cachedDTO = RedisCacheUtil.getValue(key, UserDTO.class);
        if (cachedDTO != null) {
            return dtoToEntity(cachedDTO);
        }
        try (Session session = getSession()) {
            User user = session.createQuery("FROM User u WHERE u.username = :username", User.class)
                    .setParameter("username", username)
                    .uniqueResult();
            if (user != null) {
                cacheEntity(user);
            }
            return user;
        }
    }


    public List<User> getAllUsers() {
        List<UserDTO> cachedDTOs = RedisCacheUtil.getValue(ALL_USERS_KEY, USER_LIST_TYPE);
        if (cachedDTOs != null) {
            return cachedDTOs.stream().map(this::dtoToEntity).collect(Collectors.toList());
        }
        try (Session session = getSession()) {
            List<User> users = session.createQuery("FROM User", User.class).list();
            List<UserDTO> DTOs = users.stream().map(this::entityToDTO).collect(Collectors.toList());
            RedisCacheUtil.cacheValue(ALL_USERS_KEY, DTOs);
            return users;
        }
    }

    private String usernameKey(String username) {
        return String.format(USERNAME_KEY_TEMPLATE, username);
    }
}