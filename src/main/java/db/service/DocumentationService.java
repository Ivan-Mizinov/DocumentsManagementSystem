package db.service;

import db.entities.*;

import java.util.List;

public interface DocumentationService {
    Page getPageById(Long id);
    List<Page> getAllPages();
    Page createPage(String title, String slug, String content, String username);
    PageVersion updatePageContent(Long pageId, String newContent, String username);
    PageVersion getLatestPageVersion(Long pageId);
    List<PageVersion> getPageVersions(Long pageId);
    void deletePage(Long id);

    User getUserById(Long id);
    List<User> getAllUsers();
    User createUser(String username, String roleName);
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

    List<Link> getLinksByPageId(Long pageId);
    List<Comment> getCommentsByPageVersion(Long pageVersionId);
    Comment addComment(Long pageVersionId, String username, String text);
}
