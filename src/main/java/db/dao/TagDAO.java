package db.dao;

import db.entities.Page;
import db.entities.Tag;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.List;

public class TagDAO {
    private SessionFactory sessionFactory;

    public TagDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public Tag findById(Long id) {
        try (Session session = sessionFactory.openSession()) {
            return session.find(Tag.class, id);
        }
    }

    public List<Tag> getAllTags() {
        try (Session session = sessionFactory.openSession()) {
            return session.createQuery("FROM Tag", Tag.class).list();
        }
    }

    public void save(Tag tag) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            session.persist(tag);
            transaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void update(Tag tag) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            session.merge(tag);
            transaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void delete(Tag tag) {
        try (Session session = sessionFactory.openSession()) {
            Transaction transaction = session.beginTransaction();
            session.remove(tag);
            transaction.commit();
        }
    }

    public List<Tag> getTagsByPageId(Long pageId) {
        try (Session session = sessionFactory.openSession()) {
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
        try (Session session = sessionFactory.openSession()) {
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
