package db.dao;

import db.entities.Page;
import db.entities.Tag;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import java.util.List;

public class TagDAO extends BaseDAO<Tag> {
    public TagDAO(SessionFactory sessionFactory) {
        super(sessionFactory);
    }

    public List<Tag> getAllTags() {
        try (Session session = getSession()) {
            return session.createQuery("FROM Tag", Tag.class).list();
        }
    }

    public List<Tag> getTagsByPageId(Long pageId) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "SELECT t FROM Tag t JOIN t.pages p WHERE p.id = :pageId",
                            Tag.class)
                    .setParameter("pageId", pageId)
                    .list();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Page> findPagesByTag(String tagName) {
        try (Session session = getSession()) {
            return session.createQuery(
                            "SELECT p FROM Page p JOIN p.tags t WHERE t.name = :tagName",
                            Page.class)
                    .setParameter("tagName", tagName)
                    .list();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
