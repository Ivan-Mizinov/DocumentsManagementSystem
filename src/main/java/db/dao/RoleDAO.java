package db.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import db.entities.Role;
import db.util.RedisCacheUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class RoleDAO extends BaseDAO<Role> {
    private static final String ALL_ROLES_KEY = "role:all";
    private static final String ROLE_NAME_KEY_TEMPLATE = "role:name:%s";
    private static final TypeReference<List<Role>> ROLE_LIST_TYPE = new TypeReference<>() {};

    public RoleDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    public Role save(Role role) {
        Role saved = super.save(role);
        RedisCacheUtil.evict(ALL_ROLES_KEY);
        if (saved.getName() != null) {
            RedisCacheUtil.cacheValue(nameKey(saved.getName()), saved);
        }
        return saved;
    }

    @Override
    public Role update(Role role) {
        Role updated = super.update(role);
        RedisCacheUtil.evict(ALL_ROLES_KEY);
        if (updated.getName() != null) {
            RedisCacheUtil.cacheValue(nameKey(updated.getName()), updated);
        }
        return updated;
    }

    @Override
    public void delete(Role role) {
        String roleName = role.getName();
        super.delete(role);
        RedisCacheUtil.evict(ALL_ROLES_KEY);
        if (roleName != null) {
            RedisCacheUtil.evict(nameKey(roleName));
        }
    }

    public List<Role> getAllRoles() {
        List<Role> cached = RedisCacheUtil.getValue(ALL_ROLES_KEY, ROLE_LIST_TYPE);
        if (cached != null) {
            return cached;
        }
        try (Session session = getSession()) {
            List<Role> roles = session.createQuery("FROM Role", Role.class).list();
            RedisCacheUtil.cacheValue(ALL_ROLES_KEY, roles);
            return roles;
        }
    }

    public Role findByName(String roleName) {
        String key = nameKey(roleName);
        Role cached = RedisCacheUtil.getValue(key, Role.class);
        if (cached != null) {
            return cached;
        }
        try (Session session = getSession()) {
            Role role = session.createQuery("FROM Role r WHERE r.name = :roleName", Role.class)
                    .setParameter("roleName", roleName)
                    .uniqueResult();
            if (role != null) {
                RedisCacheUtil.cacheValue(key, role);
                cacheEntity(role);
            }
            return role;
        }
    }

    private String nameKey(String name) {
        return String.format(ROLE_NAME_KEY_TEMPLATE, name);
    }
}

