package db.service;

import db.entities.*;

import java.util.List;

public interface DocumentationService {
    Page getPageById(Long id);
    List<Page> getAllPages();
    Page createPage(Page page);
    Page updatePage(Page page);
    void deletePage(Long id);

    User getUserById(Long id);
    List<User> getAllUsers();
    User createUser(User user);
    User updateUser(User user);
    void deleteUser(Long id);

    Role getRoleById(Long id);
    List<Role> getAllRoles();
    Role createRole(Role role);
    Role updateRole(Role role);
    void deleteRole(Long id);

    Tag saveTag(Tag tag);
    List<Page> searchPagesByTag(String tagName);

    List<Page> searchPages(String query);

    List<Block> getBlocksByPageId(Long pageId);
    List<Heading> getHeadingsByPageId(Long pageId);
}
