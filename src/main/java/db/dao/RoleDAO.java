package db.dao;

import com.fasterxml.jackson.core.type.TypeReference;
import db.dto.RoleDTO;
import db.entities.Role;
import db.util.RedisCacheUtil;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;
import java.util.stream.Collectors;

public class RoleDAO extends BaseDAO<Role, RoleDTO> {
    private static final String ALL_ROLES_KEY = "role:all";
    private static final String ROLE_NAME_KEY_TEMPLATE = "role:name:%s";
    private static final TypeReference<List<RoleDTO>> ROLE_LIST_TYPE = new TypeReference<>() {};

    public RoleDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    @Override
    protected RoleDTO entityToDTO(Role entity) {
        if (entity == null) return null;
        return new RoleDTO(entity.getId(), entity.getName(), entity.getDescription());
    }

    @Override
    protected Role dtoToEntity(RoleDTO dto) {
        if (dto == null) return null;
        Role role = new Role();
        role.setId(dto.getId());
        role.setName(dto.getName());
        role.setDescription(dto.getDescription());
        return role;
    }

    @Override
    protected Class<RoleDTO> getDTOClass() {
        return RoleDTO.class;
    }

    @Override
    public Role save(Role role) {
        Role saved = super.save(role);
        RedisCacheUtil.evict(ALL_ROLES_KEY);
        if (saved.getName() != null) {
            RedisCacheUtil.cacheValue(nameKey(saved.getName()), entityToDTO(saved));
        }
        return saved;
    }

    @Override
    public Role update(Role role) {
        Role updated = super.update(role);
        RedisCacheUtil.evict(ALL_ROLES_KEY);
        if (updated.getName() != null) {
            RedisCacheUtil.cacheValue(nameKey(updated.getName()), entityToDTO(updated));
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
        List<RoleDTO> cachedDTOs = RedisCacheUtil.getValue(ALL_ROLES_KEY, ROLE_LIST_TYPE);
        if (cachedDTOs != null) {
            return cachedDTOs.stream().map(this::dtoToEntity).collect(Collectors.toList());
        }
        try (Session session = getSession()) {
            List<Role> roles = session.createQuery("FROM Role", Role.class).list();
            List<RoleDTO> DTOs = roles.stream().map(this::entityToDTO).collect(Collectors.toList());
            RedisCacheUtil.cacheValue(ALL_ROLES_KEY, DTOs);
            return roles;
        }
    }

    public Role findByName(String roleName) {
        String key = nameKey(roleName);
        RoleDTO cachedDTO = RedisCacheUtil.getValue(key, RoleDTO.class);
        if (cachedDTO != null) {
            return dtoToEntity(cachedDTO);
        }
        try (Session session = getSession()) {
            Role role = session.createQuery("FROM Role r WHERE r.name = :roleName", Role.class)
                    .setParameter("roleName", roleName)
                    .uniqueResult();
            if (role != null) {
                RedisCacheUtil.cacheValue(key, entityToDTO(role));
                cacheEntity(role);
            }
            return role;
        }
    }

    private String nameKey(String name) {
        return String.format(ROLE_NAME_KEY_TEMPLATE, name);
    }
}

