package db;

import db.dao.*;
import db.entities.*;
import db.service.DocumentationService;
import db.service.DocumentationServiceImpl;
import db.util.HibernateUtil;
import org.hibernate.SessionFactory;
import redis.clients.jedis.Jedis;

import java.util.List;

public class DocManSys {
    public static void main(String[] args) {
        SessionFactory sessionFactory = HibernateUtil.getSessionFactory();
        DocumentationService documentationService = buildDocumentationService(sessionFactory);
        testRedisConnection();

        User guest = createUser(documentationService, "guestUser", "Guest");
        User reader = createUser(documentationService, "readerUser", "Reader");
        User commenter = createUser(documentationService, "commenterUser", "Commenter");
        User editor = createUser(documentationService, "editorUser", "Editor");

        demonstrateUserMethods(documentationService, reader, commenter, editor);
        demonstrateRoleMethods(documentationService, guest);
        demonstratePageMethods(documentationService, editor, commenter);

        HibernateUtil.shutdown();
    }

    private static DocumentationService buildDocumentationService(SessionFactory sessionFactory) {
        return new DocumentationServiceImpl(
                new BlockDAO(sessionFactory),
                new PageDAO(sessionFactory),
                new PageVersionDAO(sessionFactory),
                new RoleDAO(sessionFactory),
                new SearchDAO(sessionFactory),
                new TagDAO(sessionFactory),
                new UserDAO(sessionFactory),
                new CommentDAO(sessionFactory),
                new LinkDAO(sessionFactory)
        );
    }

    private static User createUser(DocumentationService documentationService, String username, String roleName) {
        User user = documentationService.createUser(username, roleName);
        System.out.printf("Создан пользователь %s с ролью %s%n", user.getUsername(), user.getRole().getName());
        return user;
    }

    private static void demonstrateUserMethods(DocumentationService documentationService,
                                               User reader,
                                               User commenter,
                                               User editor) {
        System.out.println("--- Пример использования методов работы с пользователями ---");

        List<User> allUsers = documentationService.getAllUsers();
        System.out.printf("Всего пользователей: %d%n", allUsers.size());

        User loadedEditor = documentationService.getUserById(editor.getId());
        System.out.printf("Загружен пользователь по id: %s%n", loadedEditor.getUsername());

        commenter.setPassword(commenter.getUsername() + "_new_pass");
        documentationService.updateUser(commenter);
        System.out.printf("Пароль пользователя %s обновлен%n", commenter.getUsername());

        User temp = documentationService.createUser("tempUser", "Guest");
        documentationService.deleteUser(temp.getId());
        System.out.println("Временный пользователь создан и удален");
    }

    private static void demonstrateRoleMethods(DocumentationService documentationService, User guest) {
        System.out.println("--- Пример использования методов работы с ролями ---");

        List<Role> roles = documentationService.getAllRoles();
        System.out.printf("Ролей в системе: %d%n", roles.size());

        Role guestRole = documentationService.getRoleById(guest.getRole().getId());
        System.out.printf("Роль по id: %s%n", guestRole.getName());

        Role tempRole = new Role();
        tempRole.setName("Temp");
        tempRole = documentationService.createRole(tempRole);
        System.out.printf("Создана временная роль: %s%n", tempRole.getName());

        tempRole.setName("TempUpdated");
        tempRole = documentationService.updateRole(tempRole);
        System.out.printf("Роль переименована в: %s%n", tempRole.getName());

        documentationService.deleteRole(tempRole.getId());
        System.out.println("Временная роль удалена");
    }

    private static void demonstratePageMethods(DocumentationService documentationService,
                                               User editor,
                                               User commenter) {
        System.out.println("--- Пример использования методов работы со страницами и контентом ---");

        Page page = documentationService.createPage(
                "Главная страница",
                "main-page",
                "Начальный контент страницы",
                editor.getUsername()
        );
        System.out.printf("Создана страница с id %d%n", page.getId());

        Page loadedPage = documentationService.getPageById(page.getId());
        System.out.printf("Страница по id: %s%n", loadedPage.getTitle());

        List<Page> allPages = documentationService.getAllPages();
        System.out.printf("Количество страниц: %d%n", allPages.size());

        PageVersion newVersion = documentationService.updatePageContent(
                page.getId(),
                "Обновленный контент страницы",
                editor.getUsername()
        );
        System.out.printf("Создана версия страницы №%d%n", newVersion.getVersionNumber());

        PageVersion latestVersion = documentationService.getLatestPageVersion(page.getId());
        System.out.printf("Последняя версия страницы №%d%n", latestVersion.getVersionNumber());

        List<PageVersion> versions = documentationService.getPageVersions(page.getId());
        System.out.printf("Всего версий страницы: %d%n", versions.size());

        Tag tag = new Tag();
        tag.setName("example");
        tag.setDescription("Пример тега");
        documentationService.saveTag(tag);
        System.out.println("Создан тег пример");

        List<Page> pagesByTag = documentationService.searchPagesByTag("example");
        System.out.printf("Найдено страниц по тегу 'example': %d%n", pagesByTag == null ? 0 : pagesByTag.size());

        List<Page> pagesByQuery = documentationService.searchPages("Главная");
        System.out.printf("Найдено страниц по запросу 'Главная': %d%n", pagesByQuery.size());

        List<Block> blocks = documentationService.getBlocksByPageId(page.getId());
        System.out.printf("Блоков на странице: %d%n", blocks.size());

        List<Heading> headings = documentationService.getHeadingsByPageId(page.getId());
        System.out.printf("Заголовков на странице: %d%n", headings.size());

        List<Link> links = documentationService.getLinksByPageId(page.getId());
        System.out.printf("Ссылок на странице: %d%n", links.size());

        Comment comment = documentationService.addComment(
                latestVersion.getId(),
                commenter.getUsername(),
                "Отличная работа!"
        );
        System.out.printf("Добавлен комментарий с id %d%n", comment.getId());

        List<Comment> comments = documentationService.getCommentsByPageVersion(latestVersion.getId());
        System.out.printf("Комментариев к версии: %d%n", comments.size());

        documentationService.deletePage(page.getId());
        System.out.println("Страница удалена");
    }

    private static void testRedisConnection() {
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            String response = jedis.ping();
            System.out.println("Redis доступен. Ответ: " + response);
        } catch (Exception e) {
            System.err.println("Ошибка подключения к Redis: " + e.getMessage());
            System.exit(1);
        }
    }
}
