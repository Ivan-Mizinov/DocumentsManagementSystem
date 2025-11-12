package db.dao;

import db.entities.Page;
import db.entities.PageVersion;
import db.entities.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.time.LocalDateTime;
import java.util.List;

public class PageVersionDAO {
    private SessionFactory sessionFactory;

    public PageVersionDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public PageVersion findLatestVersion(Long pageId) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(
                            "FROM PageVersion v WHERE v.page.id = :pageId ORDER BY v.versionNumber DESC", PageVersion.class)
                    .setParameter("pageId", pageId)
                    .setMaxResults(1)
                    .uniqueResult();
        }
    }

    public PageVersion createNewVersion(Page page, User changer, String newContent) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();

            Integer latestVersionNumber = session.createQuery(
                            "SELECT max(v.versionNumber) FROM PageVersion v WHERE v.page.id = :pageId",
                            Integer.class)
                    .setParameter("pageId", page.getId())
                    .uniqueResult();
            int nextVersion = latestVersionNumber == null ? 1 : latestVersionNumber + 1;

            PageVersion version = new PageVersion();
            version.setPage(page);
            version.setVersionNumber(nextVersion);
            version.setContent(newContent);
            version.setChangedBy(changer);
            version.setChangedAt(LocalDateTime.now());
            version.setPublished(false);

            session.persist(version);
            transaction.commit();
            return version;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<PageVersion> findAllVersions(Long pageId) {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery(
                            "FROM PageVersion v WHERE v.page.id = :pageId ORDER BY v.versionNumber", PageVersion.class)
                    .setParameter("pageId", pageId)
                    .list();
        }
    }
}
