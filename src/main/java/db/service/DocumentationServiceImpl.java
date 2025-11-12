package db.service;

import db.dao.*;
import db.entities.*;

import java.time.LocalDateTime;
import java.util.List;

public class DocumentationServiceImpl implements DocumentationService {
    private final BlockDAO blockDAO;
    private final PageDAO pageDAO;
    private final PageVersionDAO pageVersionDAO;
    private final RoleDAO roleDAO;
    private final SearchDAO searchDAO;
    private final TagDAO tagDAO;
    private final UserDAO userDAO;

    public DocumentationServiceImpl(BlockDAO blockDAO, PageDAO pageDAO, PageVersionDAO pageVersionDAO, RoleDAO roleDAO, SearchDAO searchDAO, TagDAO tagDAO, UserDAO userDAO) {
        this.blockDAO = blockDAO;
        this.pageDAO = pageDAO;
        this.pageVersionDAO = pageVersionDAO;
        this.roleDAO = roleDAO;
        this.searchDAO = searchDAO;
        this.tagDAO = tagDAO;
        this.userDAO = userDAO;
    }

    @Override
    public Page getPageById(Long id) {
        Page page = pageDAO.findById(Page.class, id);
        if (page == null) {
            throw new RuntimeException("Page not found");
        }
        return page;
    }

    @Override
    public List<Page> getAllPages() {
        return pageDAO.findAll();
    }

    @Override
    public Page createPage(String title, String slug, String content, String username) {
        User author = userDAO.findByUsername(username);
        if (author == null) throw new RuntimeException("User not found");
        if (pageDAO.findBySlug(slug) != null) throw new RuntimeException("Page already exists");

        Page page = new Page();
        page.setTitle(title);
        page.setSlug(slug);
        page.setCreatedAt(LocalDateTime.now());
        page.setUpdatedAt(LocalDateTime.now());
        pageDAO.save(page);
        pageVersionDAO.createNewVersion(page, author, content);
        return page;
    }

    @Override
    public PageVersion updatePageContent(Long pageId, String newContent, String username) {
        Page page = pageDAO.findById(Page.class, pageId);
        if (page == null) throw new RuntimeException("Page not found");

        User editor = userDAO.findByUsername(username);
        if (editor == null) throw new RuntimeException("User not found");

        page.setUpdatedAt(LocalDateTime.now());
        pageDAO.update(page);

        return pageVersionDAO.createNewVersion(page, editor, newContent);
    }

    @Override
    public void deletePage(Long id) {
        Page page = pageDAO.findById(Page.class, id);
        if (page == null) throw new RuntimeException("Page not found");
        pageDAO.delete(page);
    }

    @Override
    public User getUserById(Long id) {
        User user = userDAO.findById(User.class, id);
        if (user == null) throw new RuntimeException("User not found");
        return user;
    }

    @Override
    public List<User> getAllUsers() {
        return userDAO.getAllUsers();
    }

    @Override
    public User createUser(String username, String roleName) {
        Role role = roleDAO.findByName(roleName);
        if (role == null) {
            role = new Role();
            role.setName(roleName);
            roleDAO.save(role);
        }

        User user = new User();
        user.setUsername(username);
        user.setRole(role);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        return userDAO.save(user);
    }

    @Override
    public User updateUser(User user) {
        user.setUpdatedAt(LocalDateTime.now());
        return userDAO.update(user);
    }

    @Override
    public void deleteUser(Long id) {
        User user = userDAO.findById(User.class, id);
        if (user == null) throw new RuntimeException("User not found");
        userDAO.delete(user);
    }

    @Override
    public Role getRoleById(Long id) {
        Role role = roleDAO.findById(Role.class, id);
        if (role == null) throw new RuntimeException("Role not found");
        return role;
    }

    @Override
    public List<Role> getAllRoles() {
        return roleDAO.getAllRoles();
    }

    @Override
    public Role createRole(Role role) {
        return roleDAO.save(role);
    }

    @Override
    public Role updateRole(Role role) {
        return roleDAO.update(role);
    }

    @Override
    public void deleteRole(Long id) {
        Role role = roleDAO.findById(Role.class, id);
        if (role == null) throw new RuntimeException("Role not found");
        roleDAO.delete(role);
    }

    @Override
    public Tag saveTag(Tag tag) {
        return tagDAO.save(tag);
    }

    @Override
    public List<Page> searchPagesByTag(String tagName) {
        return tagDAO.findPagesByTag(tagName);
    }

    @Override
    public List<Page> searchPages(String query) {
        return searchDAO.searchByTitleOrTag(query);
    }

    @Override
    public List<Block> getBlocksByPageId(Long pageId) {
        return blockDAO.getBlocksByPageId(pageId);
    }

    @Override
    public List<Heading> getHeadingsByPageId(Long pageId) {
        return pageDAO.getHeadingsByPageId(pageId);
    }
}
